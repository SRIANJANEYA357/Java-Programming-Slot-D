package com.foodshare.config;

import com.foodshare.entity.*;
import com.foodshare.repository.*;
import com.foodshare.service.TokenService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile({"dev", "test"})
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FoodListingRepository listingRepository;
    private final FoodRequestRepository requestRepository;
    private final CollectionTokenRepository tokenRepository;
    private final VolunteerRepository volunteerRepository;
    private final DistributionRecordRepository distributionRecordRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public DataInitializer(UserRepository userRepository,
                           FoodListingRepository listingRepository,
                           FoodRequestRepository requestRepository,
                           CollectionTokenRepository tokenRepository,
                           VolunteerRepository volunteerRepository,
                           DistributionRecordRepository distributionRecordRepository,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService) {
        this.userRepository = userRepository;
        this.listingRepository = listingRepository;
        this.requestRepository = requestRepository;
        this.tokenRepository = tokenRepository;
        this.volunteerRepository = volunteerRepository;
        this.distributionRecordRepository = distributionRecordRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return; // Already seeded
        }

        System.out.println(">>> Seeding FoodShare initial database records...");

        // 1. Providers
        User provider1 = new User("provider@foodshare.com", passwordEncoder.encode("password123"),
                "Royal Grand Banquet Hall", "9876543210", Role.ROLE_PROVIDER);
        provider1.setProviderType(ProviderType.MARRIAGE_HALL);
        provider1.setOrganizationName("Royal Grand Events & Banquets");
        provider1.setAddress("45 Ring Road, Near Celebration Point");
        provider1.setCity("Hyderabad");
        provider1 = userRepository.save(provider1);

        User provider2 = new User("spice@foodshare.com", passwordEncoder.encode("password123"),
                "Spice Delight Restaurant", "9123456780", Role.ROLE_PROVIDER);
        provider2.setProviderType(ProviderType.RESTAURANT);
        provider2.setOrganizationName("Spice Delight Caterers");
        provider2.setAddress("12 Jubilee Hills, Road No. 3");
        provider2.setCity("Hyderabad");
        provider2 = userRepository.save(provider2);

        // 2. Recipients
        User recipient1 = new User("user@foodshare.com", passwordEncoder.encode("password123"),
                "Rahul Sharma", "9988776655", Role.ROLE_RECIPIENT);
        recipient1.setAddress("Flat 302, Green Meadows");
        recipient1.setCity("Hyderabad");
        recipient1 = userRepository.save(recipient1);

        User recipient2 = new User("asha@foodshare.com", passwordEncoder.encode("password123"),
                "Asha Shelter Care", "9776655443", Role.ROLE_RECIPIENT);
        recipient2.setOrganizationName("Asha Orphanage & Elder Care");
        recipient2.setAddress("Plot 18, Gandhi Nagar");
        recipient2.setCity("Hyderabad");
        recipient2 = userRepository.save(recipient2);

        // 3. Volunteer
        User volunteerUser = new User("volunteer@foodshare.com", passwordEncoder.encode("password123"),
                "Kiran Verma", "9554433221", Role.ROLE_VOLUNTEER);
        volunteerUser.setAddress("H-Block, Madhapur");
        volunteerUser.setCity("Hyderabad");
        volunteerUser = userRepository.save(volunteerUser);

        Volunteer volunteer = new Volunteer(volunteerUser, "FOUR_WHEELER", 50);
        volunteerRepository.save(volunteer);

        // 4. Admin
        User admin = new User("admin@foodshare.com", passwordEncoder.encode("password123"),
                "FoodShare Administrator", "9000000000", Role.ROLE_ADMIN);
        admin.setCity("Hyderabad");
        userRepository.save(admin);

        // 5. Seed Food Listings
        FoodListing listing1 = new FoodListing();
        listing1.setProvider(provider1);
        listing1.setTitle("Surplus Wedding Buffet Meals (Paneer Curry, Dal, Rice & Naan)");
        listing1.setDescription("Freshly cooked high-grade wedding feast meals in hygienic insulated containers. Perfect condition, prepared 2 hours ago.");
        listing1.setCategory(ListingCategory.COOKED_MEALS);
        listing1.setOriginalQuantity(60);
        listing1.setRemainingQuantity(50);
        listing1.setUnit("MEALS");
        listing1.setFoodType(FoodType.FREE_DONATION);
        listing1.setLocationAddress("Royal Grand Banquet, Ring Road");
        listing1.setLocationCity("Hyderabad");
        listing1.setExpiryTime(LocalDateTime.now().plusHours(8));
        listing1.setImageUrl("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80");
        listing1.setStatus(ListingStatus.AVAILABLE);
        listing1 = listingRepository.save(listing1);

        FoodListing listing2 = new FoodListing();
        listing2.setProvider(provider2);
        listing2.setTitle("Fresh Artisanal Bread, Rolls & Pastries");
        listing2.setDescription("End of day surplus bakery items. Premium quality multi-grain bread loaves, dinner rolls and fruit buns.");
        listing2.setCategory(ListingCategory.BAKERY);
        listing2.setOriginalQuantity(30);
        listing2.setRemainingQuantity(25);
        listing2.setUnit("PACKETS");
        listing2.setFoodType(FoodType.DISCOUNTED_SALE);
        listing2.setOriginalPrice(BigDecimal.valueOf(150.00));
        listing2.setDiscountedPrice(BigDecimal.valueOf(40.00));
        listing2.setLocationAddress("Spice Delight Cafe, Jubilee Hills Road No. 3");
        listing2.setLocationCity("Hyderabad");
        listing2.setExpiryTime(LocalDateTime.now().plusHours(18));
        listing2.setImageUrl("https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=600&q=80");
        listing2.setStatus(ListingStatus.AVAILABLE);
        listing2 = listingRepository.save(listing2);

        FoodListing listing3 = new FoodListing();
        listing3.setProvider(provider2);
        listing3.setTitle("Organic Fresh Vegetables & Greens Assortment");
        listing3.setDescription("Crisp bell peppers, tomatoes, spinach and zucchini directly received from kitchen inventory.");
        listing3.setCategory(ListingCategory.RAW_VEGETABLES);
        listing3.setOriginalQuantity(40);
        listing3.setRemainingQuantity(40);
        listing3.setUnit("KG");
        listing3.setFoodType(FoodType.FREE_DONATION);
        listing3.setLocationAddress("Spice Delight Central Store, Jubilee Hills");
        listing3.setLocationCity("Hyderabad");
        listing3.setExpiryTime(LocalDateTime.now().plusHours(24));
        listing3.setImageUrl("https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=600&q=80");
        listing3.setStatus(ListingStatus.AVAILABLE);
        listing3 = listingRepository.save(listing3);

        // 6. Seed a sample confirmed request with token for user1
        FoodRequest req1 = new FoodRequest();
        req1.setListing(listing1);
        req1.setRecipient(recipient1);
        req1.setQuantityRequested(10);
        req1.setTotalPrice(BigDecimal.ZERO);
        req1.setIsPriority(false);
        req1.setPriorityCategory(PriorityCategory.REGULAR);
        req1.setRequestStatus(RequestStatus.CONFIRMED);
        req1.setNotes("Pickup by 7 PM in personal containers.");
        req1 = requestRepository.save(req1);

        CollectionToken token1 = tokenService.generateTokenForRequest(req1);

        // 7. Seed an already collected request and token for Asha Shelter (Audit demonstration)
        FoodRequest req2 = new FoodRequest();
        req2.setListing(listing2);
        req2.setRecipient(recipient2);
        req2.setQuantityRequested(5);
        req2.setTotalPrice(BigDecimal.valueOf(200.00));
        req2.setIsPriority(true);
        req2.setPriorityCategory(PriorityCategory.CHILDREN);
        req2.setRequestStatus(RequestStatus.COLLECTED);
        req2.setNotes("For evening snack time for children home.");
        req2 = requestRepository.save(req2);

        CollectionToken token2 = new CollectionToken(req2, "FS-DEMO-COLLECTED");
        token2.setStatus(TokenStatus.VERIFIED);
        token2.setVerifiedAt(LocalDateTime.now().minusHours(2));
        token2.setVerifiedBy(provider2);
        token2 = tokenRepository.save(token2);

        DistributionRecord record1 = new DistributionRecord();
        record1.setListing(listing2);
        record1.setRequest(req2);
        record1.setToken(token2);
        record1.setProvider(provider2);
        record1.setRecipient(recipient2);
        record1.setVolunteer(volunteerUser);
        record1.setQuantityDistributed(5);
        record1.setDistributionType(DistributionType.VOLUNTEER_DELIVERY);
        record1.setCollectedAt(LocalDateTime.now().minusHours(2));
        record1.setNotes("Delivered to children shelter by volunteer Kiran.");
        distributionRecordRepository.save(record1);

        System.out.println(">>> FoodShare database successfully seeded with demo accounts, listings, tokens & audit records!");
    }
}
