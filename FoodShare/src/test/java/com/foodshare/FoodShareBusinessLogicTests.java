package com.foodshare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodshare.config.JwtTokenProvider;
import com.foodshare.dto.*;
import com.foodshare.entity.*;
import com.foodshare.exception.FoodExpiredException;
import com.foodshare.exception.InsufficientQuantityException;
import com.foodshare.exception.InvalidTokenException;
import com.foodshare.exception.UnauthorizedException;
import com.foodshare.repository.CollectionTokenRepository;
import com.foodshare.repository.FoodListingRepository;
import com.foodshare.repository.FoodRequestRepository;
import com.foodshare.repository.UserRepository;
import com.foodshare.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.test.context.support.WithMockUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class FoodShareBusinessLogicTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private FoodListingService listingService;

    @Autowired
    private FoodRequestService requestService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PriorityAllocationService priorityService;

    @Autowired
    private DistributionService distributionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodListingRepository listingRepository;

    @Autowired
    private FoodRequestRepository requestRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testProvider;
    private User testRecipient;
    private User testVolunteer;

    @BeforeEach
    void setUp() {
        testProvider = userRepository.findByEmail("provider@foodshare.com").orElse(null);
        testRecipient = userRepository.findByEmail("user@foodshare.com").orElse(null);
        testVolunteer = userRepository.findByEmail("volunteer@foodshare.com").orElse(null);
    }

    @Test
    @DisplayName("Scenario 1 & 2: User & Provider Registration and Login")
    void testUserAndProviderRegistrationAndLogin() {
        String uniqueEmail = "newuser_" + System.currentTimeMillis() + "@test.com";
        RegisterRequest regReq = new RegisterRequest();
        regReq.setEmail(uniqueEmail);
        regReq.setPassword("secret123");
        regReq.setFullName("Test Student Recipient");
        regReq.setPhone("9876500000");
        regReq.setRole(Role.ROLE_RECIPIENT);
        regReq.setCity("Hyderabad");

        AuthResponse regRes = userService.register(regReq);
        assertNotNull(regRes.getToken());
        assertEquals(uniqueEmail, regRes.getEmail());

        // Login
        AuthRequest loginReq = new AuthRequest();
        loginReq.setEmail(uniqueEmail);
        loginReq.setPassword("secret123");
        AuthResponse loginRes = userService.login(loginReq);
        assertNotNull(loginRes.getToken());
        assertEquals("Test Student Recipient", loginRes.getFullName());
    }

    @Test
    @DisplayName("Scenario 3: Adding and Updating Food Listings")
    void testAddAndUpdateFoodListing() {
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Extra Banquet Pulao & Curry");
        req.setDescription("Hot buffet surplus in food warmers");
        req.setCategory(ListingCategory.COOKED_MEALS);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(40);
        req.setUnit("MEALS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Convention Center Hall B");
        req.setExpiryTime(LocalDateTime.now().plusHours(6));

        FoodListingResponse created = listingService.createListing(req, testProvider);
        assertNotNull(created.getId());
        assertEquals(40, created.getOriginalQuantity());
        assertEquals(40, created.getRemainingQuantity());
        assertEquals(ListingStatus.AVAILABLE, created.getStatus());

        // Update listing
        req.setTitle("Extra Banquet Pulao & Paneer Curry (Updated)");
        FoodListingResponse updated = listingService.updateListing(created.getId(), req, testProvider);
        assertEquals("Extra Banquet Pulao & Paneer Curry (Updated)", updated.getTitle());
    }

    @Test
    @DisplayName("Scenario 4 & 5: Free & Discounted Requests and Overbooking Prevention")
    void testFoodRequestsAndOverbookingPrevention() {
        // Create listing with 15 units
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Limited Fresh Bread Loaves");
        req.setCategory(ListingCategory.BAKERY);
        req.setFoodType(FoodType.DISCOUNTED_SALE);
        req.setOriginalPrice(BigDecimal.valueOf(100.0));
        req.setDiscountedPrice(BigDecimal.valueOf(30.0));
        req.setQuantity(15);
        req.setUnit("PACKETS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Bakery Counter");
        req.setExpiryTime(LocalDateTime.now().plusHours(12));

        FoodListingResponse listing = listingService.createListing(req, testProvider);

        // Claim 10 units
        FoodRequestDto claim1 = new FoodRequestDto();
        claim1.setListingId(listing.getId());
        claim1.setQuantityRequested(10);
        FoodRequestResponse res1 = requestService.createRequest(claim1, testRecipient);

        assertEquals(RequestStatus.CONFIRMED, res1.getRequestStatus());
        assertEquals(0, res1.getTotalPrice().compareTo(new BigDecimal("300.00"))); // 30 * 10
        assertNotNull(res1.getTokenCode());
        assertTrue(res1.getTokenCode().startsWith("FS-"));

        // Verify remaining inventory is now 5
        FoodListingResponse afterClaim1 = listingService.getListingById(listing.getId());
        assertEquals(5, afterClaim1.getRemainingQuantity());

        // Attempt overbooking: requesting 8 units when only 5 remain
        FoodRequestDto overbookingClaim = new FoodRequestDto();
        overbookingClaim.setListingId(listing.getId());
        overbookingClaim.setQuantityRequested(8);

        assertThrows(InsufficientQuantityException.class, () -> {
            requestService.createRequest(overbookingClaim, testRecipient);
        }, "Should throw InsufficientQuantityException when claiming more than remaining quantity");

        // Remaining quantity must remain untouched at 5
        FoodListingResponse afterFailedClaim = listingService.getListingById(listing.getId());
        assertEquals(5, afterFailedClaim.getRemainingQuantity());
    }

    @Test
    @DisplayName("Scenario 6: Expired Food Listings are Blocked from Requests")
    void testExpiredFoodAvailability() {
        // Create an expired listing
        FoodListing expiredListing = new FoodListing();
        expiredListing.setProvider(testProvider);
        expiredListing.setTitle("Yesterday Cooked Rice");
        expiredListing.setCategory(ListingCategory.COOKED_MEALS);
        expiredListing.setOriginalQuantity(20);
        expiredListing.setRemainingQuantity(20);
        expiredListing.setUnit("MEALS");
        expiredListing.setFoodType(FoodType.FREE_DONATION);
        expiredListing.setLocationAddress("Old Kitchen");
        expiredListing.setLocationCity("Hyderabad");
        expiredListing.setExpiryTime(LocalDateTime.now().minusHours(2)); // Expired 2 hours ago
        expiredListing.setStatus(ListingStatus.AVAILABLE);
        expiredListing = listingRepository.save(expiredListing);

        FoodRequestDto claim = new FoodRequestDto();
        claim.setListingId(expiredListing.getId());
        claim.setQuantityRequested(5);

        assertThrows(FoodExpiredException.class, () -> {
            requestService.createRequest(claim, testRecipient);
        }, "Should reject claims for expired food items");
    }

    @Test
    @DisplayName("Scenario 7, 8 & 9: Token Generation, Single-Use Verification & Handover Audit")
    void testTokenGenerationVerificationAndBurn() {
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Fresh Dinner Rolls");
        req.setCategory(ListingCategory.BAKERY);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(20);
        req.setUnit("PACKETS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Main Outlet");
        req.setExpiryTime(LocalDateTime.now().plusHours(10));
        FoodListingResponse listing = listingService.createListing(req, testProvider);

        // Make confirmed claim
        FoodRequestDto claim = new FoodRequestDto();
        claim.setListingId(listing.getId());
        claim.setQuantityRequested(5);
        FoodRequestResponse claimRes = requestService.createRequest(claim, testRecipient);

        String tokenCode = claimRes.getTokenCode();
        assertNotNull(tokenCode);

        // Step 1: Verify token (Read-only check)
        TokenVerificationResponse verifyRes = tokenService.verifyToken(tokenCode);
        assertTrue(verifyRes.isValid());
        assertEquals(TokenStatus.ACTIVE, verifyRes.getTokenStatus());
        assertEquals(5, verifyRes.getQuantity());

        // Step 2: Confirm collection (Handover & Burn)
        TokenVerificationResponse confirmRes = tokenService.confirmCollection(tokenCode, testProvider);
        assertEquals(TokenStatus.VERIFIED, confirmRes.getTokenStatus());
        assertFalse(confirmRes.isValid(), "Token should no longer be active after handover");

        // Step 3: Duplicate verification attempt must fail
        assertThrows(InvalidTokenException.class, () -> {
            tokenService.confirmCollection(tokenCode, testProvider);
        }, "Re-redeeming the same token should be blocked");
    }

    @Test
    @DisplayName("Algorithm Requirement: PriorityQueue Request Ordering (CHILDREN > ELDERLY > REGULAR)")
    void testPriorityQueueAlgorithm() {
        FoodRequest rRegular = new FoodRequest();
        rRegular.setPriorityCategory(PriorityCategory.REGULAR);
        rRegular.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        FoodRequest rElderly = new FoodRequest();
        rElderly.setPriorityCategory(PriorityCategory.ELDERLY);
        rElderly.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        FoodRequest rChildren = new FoodRequest();
        rChildren.setPriorityCategory(PriorityCategory.CHILDREN);
        rChildren.setCreatedAt(LocalDateTime.now()); // Arrived latest, but has highest priority

        List<FoodRequest> prioritized = priorityService.prioritizeRequests(List.of(rRegular, rElderly, rChildren));

        assertEquals(3, prioritized.size());
        assertEquals(PriorityCategory.CHILDREN, prioritized.get(0).getPriorityCategory(), "CHILDREN tier must be first in queue");
        assertEquals(PriorityCategory.ELDERLY, prioritized.get(1).getPriorityCategory(), "ELDERLY tier must be second");
        assertEquals(PriorityCategory.REGULAR, prioritized.get(2).getPriorityCategory(), "REGULAR tier must be third");
    }

    @Test
    @DisplayName("Scenario 10: Volunteer Offline Food Distribution Camp")
    void testVolunteerOfflineCampDistribution() {
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Large Banquet Surplus for Community Camp");
        req.setCategory(ListingCategory.COOKED_MEALS);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(100);
        req.setUnit("MEALS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Royal Hall Kitchen");
        req.setExpiryTime(LocalDateTime.now().plusHours(10));
        FoodListingResponse listing = listingService.createListing(req, testProvider);

        DistributionCampRequest campReq = new DistributionCampRequest();
        campReq.setListingId(listing.getId());
        campReq.setQuantity(50);
        campReq.setCampName("Secunderabad Station Food Camp");
        campReq.setCampLocation("Station Entrance West");
        campReq.setNotes("Distributed to homeless individuals and daily wage workers");

        DistributionRecordResponse record = distributionService.createCampDistribution(campReq, testVolunteer);
        assertNotNull(record.getId());
        assertEquals(50, record.getQuantityDistributed());
        assertEquals(DistributionType.OFFLINE_CAMP, record.getDistributionType());

        // Inventory should reflect the camp deduction: 100 - 50 = 50 remaining
        FoodListingResponse updatedListing = listingService.getListingById(listing.getId());
        assertEquals(50, updatedListing.getRemainingQuantity());
    }

    @Test
    @DisplayName("Security: Public Registration Blocks Role Escalation (ADMIN & VOLUNTEER)")
    void testRegistrationPrivilegeEscalationPrevention() {
        // Attempting to register as ROLE_ADMIN must be rejected
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setEmail("hacker_admin_" + System.currentTimeMillis() + "@test.com");
        adminReq.setPassword("password123");
        adminReq.setFullName("Malicious Admin");
        adminReq.setPhone("9999900001");
        adminReq.setRole(Role.ROLE_ADMIN);

        assertThrows(IllegalArgumentException.class, () -> userService.register(adminReq),
                "Public registration with ROLE_ADMIN must throw IllegalArgumentException");

        // Attempting to register as ROLE_VOLUNTEER must be rejected
        RegisterRequest volunteerReq = new RegisterRequest();
        volunteerReq.setEmail("fake_vol_" + System.currentTimeMillis() + "@test.com");
        volunteerReq.setPassword("password123");
        volunteerReq.setFullName("Unauthorized Volunteer");
        volunteerReq.setPhone("9999900002");
        volunteerReq.setRole(Role.ROLE_VOLUNTEER);

        assertThrows(IllegalArgumentException.class, () -> userService.register(volunteerReq),
                "Public registration with ROLE_VOLUNTEER must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("Security: Admin Can Safely Assign Volunteer Roles")
    void testAdminRoleAssignment() {
        // Create standard recipient
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("vol_candidate_" + System.currentTimeMillis() + "@test.com");
        reg.setPassword("password123");
        reg.setFullName("Candidate Volunteer");
        reg.setPhone("9876543299");
        reg.setRole(Role.ROLE_RECIPIENT);
        AuthResponse res = userService.register(reg);

        // Admin assigns ROLE_VOLUNTEER
        UserProfileDTO updated = userService.assignRole(res.getId(), Role.ROLE_VOLUNTEER);
        assertEquals(Role.ROLE_VOLUNTEER, updated.getRole(), "User role must be updated to ROLE_VOLUNTEER by admin");
    }

    @Test
    @DisplayName("Security: IDOR Prevention on Collection Token Retrieval")
    void testTokenIdorAuthorization() {
        // Create listing & confirmed request by testRecipient
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("IDOR Protected Food Batch");
        req.setCategory(ListingCategory.COOKED_MEALS);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(20);
        req.setUnit("MEALS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Hall C");
        req.setExpiryTime(LocalDateTime.now().plusHours(5));
        FoodListingResponse listing = listingService.createListing(req, testProvider);

        FoodRequestDto claim = new FoodRequestDto();
        claim.setListingId(listing.getId());
        claim.setQuantityRequested(2);
        FoodRequestResponse claimRes = requestService.createRequest(claim, testRecipient);

        // Create an unrelated user
        User unrelatedUser = new User("unrelated_" + System.currentTimeMillis() + "@test.com", "pass", "Stranger", "9876500009", Role.ROLE_RECIPIENT);
        unrelatedUser = userRepository.save(unrelatedUser);

        // Unrelated user attempting to access token for testRecipient's request must be rejected
        final User caller = unrelatedUser;
        assertThrows(UnauthorizedException.class, () -> {
            tokenService.getByRequestIdAuthorized(claimRes.getId(), caller);
        }, "Unrelated users must be blocked from accessing other users' collection tokens");

        // The actual recipient can access the token
        CollectionTokenResponse recipientToken = tokenService.getByRequestIdAuthorized(claimRes.getId(), testRecipient);
        assertNotNull(recipientToken);
        assertEquals(claimRes.getTokenCode(), recipientToken.getTokenCode());

        // The food provider can access the token
        CollectionTokenResponse providerToken = tokenService.getByRequestIdAuthorized(claimRes.getId(), testProvider);
        assertNotNull(providerToken);
        assertEquals(claimRes.getTokenCode(), providerToken.getTokenCode());
    }

    @Test
    @DisplayName("Security: Passwords Never Appear in Jackson Serialized Entity JSON")
    void testPasswordNeverSerializedInJson() throws Exception {
        // 1. User entity serialization
        User user = new User("test_serial@foodshare.com", "$2a$10$e84jf849f84hf84", "Test Name", "9876543210", Role.ROLE_RECIPIENT);
        String userJson = objectMapper.writeValueAsString(user);
        assertFalse(userJson.contains("password"), "User JSON serialization must NEVER contain the password property");
        assertFalse(userJson.contains("$2a$10$"), "User JSON serialization must NEVER contain password hash strings");

        // 2. CollectionTokenResponse serialization
        CollectionTokenResponse tokenRes = new CollectionTokenResponse();
        tokenRes.setTokenCode("FS-TEST-1234");
        tokenRes.setStatus(TokenStatus.ACTIVE);
        String tokenJson = objectMapper.writeValueAsString(tokenRes);
        assertFalse(tokenJson.contains("password"));

        // 3. DistributionRecordResponse serialization
        DistributionRecordResponse distRes = new DistributionRecordResponse();
        distRes.setProvider(new DistributionRecordResponse.UserSummary(1L, "Provider Hall"));
        distRes.setRecipient(new DistributionRecordResponse.UserSummary(2L, "Recipient Name"));
        String distJson = objectMapper.writeValueAsString(distRes);
        assertFalse(distJson.contains("password"));

        // 4. VolunteerResponse serialization
        VolunteerResponse volRes = new VolunteerResponse();
        volRes.setUser(new VolunteerResponse.UserSummary(3L, "Volunteer Kiran", "kiran@test.com", "9876543210"));
        String volJson = objectMapper.writeValueAsString(volRes);
        assertFalse(volJson.contains("password"));
    }

    @Test
    @DisplayName("Business Logic: Expired Food Cannot be Redeemed via Token")
    void testExpiredFoodCollectionRejected() {
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Expiring Meals");
        req.setCategory(ListingCategory.COOKED_MEALS);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(10);
        req.setUnit("MEALS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Dining Room");
        req.setExpiryTime(LocalDateTime.now().plusHours(1));
        FoodListingResponse listing = listingService.createListing(req, testProvider);

        FoodRequestDto claim = new FoodRequestDto();
        claim.setListingId(listing.getId());
        claim.setQuantityRequested(2);
        FoodRequestResponse claimRes = requestService.createRequest(claim, testRecipient);
        String tokenCode = claimRes.getTokenCode();

        // Manually artificially expire the listing
        FoodListing listingEntity = listingRepository.findById(listing.getId()).orElseThrow();
        listingEntity.setExpiryTime(LocalDateTime.now().minusMinutes(30));
        listingRepository.save(listingEntity);

        // Verification check should mark it invalid
        TokenVerificationResponse verifyRes = tokenService.verifyToken(tokenCode);
        assertFalse(verifyRes.isValid(), "Verification should report expired food as invalid");

        // Redemption attempt should throw FoodExpiredException
        assertThrows(FoodExpiredException.class, () -> {
            tokenService.confirmCollection(tokenCode, testProvider);
        }, "Token redemption for expired food must be rejected with FoodExpiredException");
    }

    @Test
    @DisplayName("Business Logic: Provider Listing Cancellation Invalidates Tokens")
    void testListingCancellationCascadesToTokens() {
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Food to Cancel");
        req.setCategory(ListingCategory.COOKED_MEALS);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(20);
        req.setUnit("MEALS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Kitchen 1");
        req.setExpiryTime(LocalDateTime.now().plusHours(8));
        FoodListingResponse listing = listingService.createListing(req, testProvider);

        FoodRequestDto claim = new FoodRequestDto();
        claim.setListingId(listing.getId());
        claim.setQuantityRequested(5);
        FoodRequestResponse claimRes = requestService.createRequest(claim, testRecipient);
        String tokenCode = claimRes.getTokenCode();

        // Provider cancels listing
        listingService.cancelListing(listing.getId(), testProvider);

        // Token must now be CANCELLED and non-redeemable
        TokenVerificationResponse verifyRes = tokenService.verifyToken(tokenCode);
        assertFalse(verifyRes.isValid());
        assertEquals(TokenStatus.CANCELLED, verifyRes.getTokenStatus());

        assertThrows(InvalidTokenException.class, () -> {
            tokenService.confirmCollection(tokenCode, testProvider);
        }, "Redeeming token for cancelled listing must fail");
    }

    @Test
    @DisplayName("Business Logic: Provider Self-Claim Prevention")
    void testProviderSelfClaimPrevention() {
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Provider's Own Listing");
        req.setCategory(ListingCategory.COOKED_MEALS);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(10);
        req.setUnit("MEALS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Buffet");
        req.setExpiryTime(LocalDateTime.now().plusHours(4));
        FoodListingResponse listing = listingService.createListing(req, testProvider);

        // Provider attempting to claim their own listing must be blocked
        FoodRequestDto claim = new FoodRequestDto();
        claim.setListingId(listing.getId());
        claim.setQuantityRequested(1);

        assertThrows(IllegalArgumentException.class, () -> {
            requestService.createRequest(claim, testProvider);
        }, "Providers cannot claim their own food listings");
    }

    @Test
    @DisplayName("Security: JWT Provider Validates 256-Bit Cryptographic Secret Requirement")
    void testJwtSecretValidation() {
        // Missing / empty secret must throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 1800000));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(null, 1800000));

        // Short secret (<32 bytes / 256 bits) must throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("short-secret-123", 1800000));

        // Valid 32+ byte secret must succeed
        assertDoesNotThrow(() -> new JwtTokenProvider("12345678901234567890123456789012", 1800000));
    }

    @Test
    @DisplayName("Security: Route Shadowing Fixed — /api/listings/my Requires Auth (HTTP 401, not 500 NPE)")
    void testUnauthenticatedListingMyReturns401() throws Exception {
        // Without authentication, /api/listings/my must return 401 Unauthorized
        mockMvc.perform(get("/api/listings/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: /api/requests/prioritized Requires VOLUNTEER or ADMIN Role")
    void testPrioritizedRequestsUnauthenticatedReturns401() throws Exception {
        // Without authentication, /api/requests/prioritized must return 401 Unauthorized
        mockMvc.perform(get("/api/requests/prioritized"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Security: Public Endpoint /api/listings/available is Open to All")
    void testPublicAvailableListingsReturns200() throws Exception {
        mockMvc.perform(get("/api/listings/available"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Concurrency: Simultaneous Claims Cannot Over-Allocate Inventory")
    void testConcurrentClaimsOverAllocationPrevention() throws Exception {
        // Create listing with exactly 10 units
        FoodListingRequest req = new FoodListingRequest();
        req.setTitle("Concurrent Claim Test Batch");
        req.setCategory(ListingCategory.COOKED_MEALS);
        req.setFoodType(FoodType.FREE_DONATION);
        req.setQuantity(10);
        req.setUnit("MEALS");
        req.setLocationCity("Hyderabad");
        req.setLocationAddress("Kitchen Multi-Thread");
        req.setExpiryTime(LocalDateTime.now().plusHours(6));
        FoodListingResponse listing = listingService.createListing(req, testProvider);

        int numThreads = 4;
        int claimQuantityEach = 5; // 4 * 5 = 20 requested, only 10 available -> exactly 2 must succeed, 2 must fail
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    FoodRequestDto dto = new FoodRequestDto();
                    dto.setListingId(listing.getId());
                    dto.setQuantityRequested(claimQuantityEach);
                    requestService.createRequest(dto, testRecipient);
                    successCount.incrementAndGet();
                } catch (InsufficientQuantityException ex) {
                    failureCount.incrementAndGet();
                } catch (Throwable ex) {
                    System.err.println("Thread caught unexpected error: " + ex);
                    ex.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // fire all threads at once
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Exactly 2 threads claim 5 each (total 10 units), other 2 get InsufficientQuantityException
        assertEquals(2, successCount.get(), "Exactly 2 concurrent claims should succeed for 10 total units");
        assertEquals(2, failureCount.get(), "Remaining 2 concurrent claims should fail due to insufficient quantity");

        // Verify remaining inventory is exactly 0
        FoodListingResponse finalListing = listingService.getListingById(listing.getId());
        assertEquals(0, finalListing.getRemainingQuantity(), "Remaining inventory must be exactly 0, no overbooking");
        assertEquals(ListingStatus.DEPLETED, finalListing.getStatus());
    }

    @Test
    @DisplayName("Admin: Administrator Can Retrieve All Users via GET /api/users")
    @WithMockUser(username = "admin@foodshare.com", roles = {"ADMIN"})
    void testAdminCanRetrieveAllUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].fullName").exists())
                .andExpect(jsonPath("$[0].role").exists());
    }

    @Test
    @DisplayName("Security: Recipient Cannot Retrieve All Users via GET /api/users (HTTP 403)")
    @WithMockUser(username = "user@foodshare.com", roles = {"RECIPIENT"})
    void testRecipientCannotRetrieveAllUsersReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Provider Cannot Retrieve All Users via GET /api/users (HTTP 403)")
    @WithMockUser(username = "provider@foodshare.com", roles = {"PROVIDER"})
    void testProviderCannotRetrieveAllUsersReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Volunteer Cannot Retrieve All Users via GET /api/users (HTTP 403)")
    @WithMockUser(username = "volunteer@foodshare.com", roles = {"VOLUNTEER"})
    void testVolunteerCannotRetrieveAllUsersReturns403() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: User Responses from GET /api/users Never Contain Password Hashes")
    @WithMockUser(username = "admin@foodshare.com", roles = {"ADMIN"})
    void testUserResponsesNeverContainPasswordHashes() throws Exception {
        String responseContent = mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].password").doesNotExist())
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertFalse(responseContent.toLowerCase().contains("password"), "User list response must never contain 'password'");
        assertFalse(responseContent.contains("$2a$10$"), "User list response must never contain BCrypt password hashes");
    }

    @Test
    @DisplayName("Admin: Administrator Can Assign Permitted Roles (ROLE_VOLUNTEER)")
    @WithMockUser(username = "admin@foodshare.com", roles = {"ADMIN"})
    void testAdminCanAssignPermittedRoles() throws Exception {
        // Create candidate recipient
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("assign_target_" + System.currentTimeMillis() + "@test.com");
        reg.setPassword("secret123");
        reg.setFullName("Role Target User");
        reg.setPhone("9876500099");
        reg.setRole(Role.ROLE_RECIPIENT);
        AuthResponse target = userService.register(reg);

        mockMvc.perform(put("/api/users/{userId}/role", target.getId())
                .param("role", "ROLE_VOLUNTEER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId()))
                .andExpect(jsonPath("$.role").value("ROLE_VOLUNTEER"));
    }

    @Test
    @DisplayName("Security: Non-Admin Users Cannot Assign Roles (HTTP 403)")
    @WithMockUser(username = "user@foodshare.com", roles = {"RECIPIENT"})
    void testNonAdminCannotAssignRolesReturns403() throws Exception {
        mockMvc.perform(put("/api/users/{userId}/role", testRecipient.getId())
                .param("role", "ROLE_VOLUNTEER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security: Invalid Role Assignment Rejected with HTTP 400")
    @WithMockUser(username = "admin@foodshare.com", roles = {"ADMIN"})
    void testInvalidRoleAssignmentRejectedWithBadRequest() throws Exception {
        // Assigning an invalid role or ROLE_ADMIN must be rejected with 400
        mockMvc.perform(put("/api/users/{userId}/role", testRecipient.getId())
                .param("role", "ROLE_ADMIN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Admin: Nonexistent User ID for Role Assignment Returns HTTP 404")
    @WithMockUser(username = "admin@foodshare.com", roles = {"ADMIN"})
    void testNonexistentUserIdRoleAssignmentReturns404() throws Exception {
        mockMvc.perform(put("/api/users/{userId}/role", 999999L)
                .param("role", "ROLE_VOLUNTEER"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Security: Administrator Cannot Modify Their Own Role")
    @WithMockUser(username = "admin@foodshare.com", roles = {"ADMIN"})
    void testAdminCannotModifyOwnRole() throws Exception {
        User adminUser = userRepository.findByEmail("admin@foodshare.com").orElseThrow();
        mockMvc.perform(put("/api/users/{userId}/role", adminUser.getId())
                .param("role", "ROLE_RECIPIENT"))
                .andExpect(status().isBadRequest());
    }
}
