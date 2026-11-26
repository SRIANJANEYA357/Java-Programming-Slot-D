# FoodShare: Food Wastage Reduction & Redistribution Platform

> **Final-Year B.Tech Computer Science Engineering Full-Stack Capstone Project**  
> Developed using **Java 21**, **Spring Boot 3.3.5**, **Spring Data JPA**, **Spring Security**, **MySQL 8.0**, and **HTML5/CSS3/JavaScript**.

---

## 1. Project Summary

**FoodShare** is an enterprise-grade full-stack web application designed to eliminate food waste in urban communities by establishing a real-time, transparent distribution bridge between commercial food donors (wedding banquet halls, restaurants, corporate caterers, individual donors) and vulnerable groups (orphanages, elderly care centers, shelters, community beneficiaries).

### Key Highlights:
- **Free Donations & Subsidized Sales**: Providers can list surplus as 100% free donation or at discounted prices to recover logistics costs.
- **Concurrency-Safe Inventory Allocation**: Row-level pessimistic locking (`SELECT ... FOR UPDATE`) prevents overbooking even under high concurrency.
- **Cryptographic Collection Tokens**: Unique `FS-XXXX-XXXX` tokens guarantee verified, single-use food collections.
- **Priority Queue Allocation**: Implements a `PriorityQueue` giving scheduling precedence to vulnerable groups (`CHILDREN` > `ELDERLY` > `REGULAR`).
- **Offline Community Food Camps**: Supports volunteer-managed offline distribution camps in underserved areas with immutable audit logging.

---

## 2. Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | HTML5, CSS3, ES6 JavaScript, Responsive Flexbox & Grid, Modern Green/White Theme |
| **Backend** | Java 21, Spring Boot 3.3.5, Spring Web, Spring Data JPA, Spring Security |
| **Security** | BCrypt Password Hashing, JWT (JSON Web Tokens), Role-Based Access Control (RBAC) |
| **Concurrency & Algorithms**| JPA Pessimistic Locking (`PESSIMISTIC_WRITE`), Java `PriorityQueue`, `SecureRandom` |
| **Databases** | MySQL 8.0 (Relational Store) & H2 (Dual-Profile In-Memory Fallback) |
| **Build Tool** | Apache Maven 3.9.9 |

---

## 3. Project Architecture & Directory Layout

```
foodshare/
├── pom.xml                               # Maven project descriptor & dependencies
├── src/
│   ├── main/
│   │   ├── java/com/foodshare/
│   │   │   ├── FoodShareApplication.java # Spring Boot main runner & scheduler
│   │   │   ├── config/
│   │   │   │   ├── DataInitializer.java  # Seeds initial demo accounts & records
│   │   │   │   ├── JwtAuthenticationFilter.java # JWT extraction & validation filter
│   │   │   │   ├── JwtTokenProvider.java # HMAC-SHA256 token signer
│   │   │   │   ├── SecurityConfig.java   # Spring Security filter chain & CORS
│   │   │   │   └── UserDetailsServiceImpl.java # Spring Security user details bridge
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java   # /api/auth/register, /api/auth/login
│   │   │   │   ├── DistributionController.java # /api/distributions/*
│   │   │   │   ├── ListingController.java # /api/listings/*
│   │   │   │   ├── RequestController.java # /api/requests/*
│   │   │   │   ├── TokenController.java   # /api/tokens/*
│   │   │   │   ├── UserController.java    # /api/users/*
│   │   │   │   └── VolunteerController.java # /api/volunteers/*
│   │   │   ├── dto/                      # Data Transfer Objects & validation schemas
│   │   │   ├── entity/                   # JPA Entity definitions
│   │   │   │   ├── User.java, FoodListing.java, FoodRequest.java
│   │   │   │   ├── CollectionToken.java, Volunteer.java, DistributionRecord.java
│   │   │   │   └── Enums (Role, ProviderType, FoodType, RequestStatus, etc.)
│   │   │   ├── exception/                # Global exception handling advice
│   │   │   ├── repository/               # Spring Data JPA repositories with custom queries
│   │   │   └── service/                  # Transactional business logic & algorithms
│   │   └── resources/
│   │       ├── application.properties    # Base configuration (Server port 8081)
│   │       ├── application-dev.properties # Embedded H2 development mode
│   │       ├── application-mysql.properties # MySQL 8.0 production mode
│   │       ├── schema-mysql.sql          # Pure MySQL DDL script with indexes
│   │       └── static/                   # 15 Interactive Frontend Pages
│   │           ├── index.html            # Home page & live impact stats
│   │           ├── about.html            # Mission, architecture & system info
│   │           ├── register.html         # Multi-role user registration
│   │           ├── login.html            # Secure authentication
│   │           ├── user-dashboard.html   # Recipient dashboard & active tokens
│   │           ├── provider-dashboard.html # Provider inventory & claim management
│   │           ├── volunteer-dashboard.html # Volunteer portal & offline camp logs
│   │           ├── add-listing.html      # Surplus listing form (Free / Discounted)
│   │           ├── available-food.html   # Real-time search, filter & discover
│   │           ├── food-details.html     # Food details & priority claim modal
│   │           ├── my-requests.html      # Recipient claim lifecycle & token access
│   │           ├── collection-token.html # Printable collection pass with QR code
│   │           ├── token-verification.html # Provider token verify & single-use burn
│   │           ├── distribution-history.html # Immutable distribution audit log
│   │           ├── profile.html          # Profile settings & role info
│   │           ├── css/style.css         # Modern green & white UI design system
│   │           └── js/api.js             # Unified REST client with JWT storage
│   └── test/java/com/foodshare/
│       └── FoodShareBusinessLogicTests.java # 7 Full Integration & Algorithm Tests
```

---

## 4. Database Schema (MySQL 8.0)

The relational database schema is stored in `src/main/resources/schema-mysql.sql`.

### Relational Tables:
1. `users`: Stores credentials, roles (`ROLE_PROVIDER`, `ROLE_RECIPIENT`, `ROLE_VOLUNTEER`, `ROLE_ADMIN`), and contact details.
2. `food_listings`: Surplus postings, categories, remaining inventory, location, and expiry timestamp.
3. `food_requests`: Claims with requested quantities, priority tags (`CHILDREN`, `ELDERLY`, `REGULAR`), and statuses.
4. `collection_tokens`: Cryptographic tokens (`FS-XXXX-XXXX`), single-use status (`ACTIVE` -> `VERIFIED`).
5. `volunteers`: Volunteer transport capacities and active statuses.
6. `distribution_records`: Audit log linking listing, request, token, provider, recipient, and volunteer.

---

## 5. Pre-Seeded Demo Accounts

The database comes pre-seeded with test accounts (Password: `password123` for all):

| Role | Email | Password | Details |
|---|---|---|---|
| **Provider** | `provider@foodshare.com` | `password123` | Royal Grand Banquet Hall |
| **Provider** | `spice@foodshare.com` | `password123` | Spice Delight Restaurant |
| **Recipient** | `user@foodshare.com` | `password123` | Rahul Sharma (Individual) |
| **Recipient** | `asha@foodshare.com` | `password123` | Asha Care Home (Priority: Children) |
| **Volunteer** | `volunteer@foodshare.com` | `password123` | Kiran Verma (Vehicle: Four Wheeler) |
| **Admin** | `admin@foodshare.com` | `password123` | FoodShare Administrator |

---

## 6. How to Run the Application

### Option A: Out-of-the-Box Mode (Embedded In-Memory H2 in MySQL Mode)
No manual database setup required! Starts in 5 seconds:

```powershell
cd C:\Users\anji3\.gemini\antigravity\scratch\foodshare
& "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" -jar target\foodshare-1.0.0.jar
```

Or using Maven:
```powershell
mvn spring-boot:run
```

Open your browser at: **`http://localhost:8081`**

---

### Option B: Running with Local MySQL 8.0
1. Open MySQL Workbench or MySQL CLI:
   ```sql
   CREATE DATABASE foodshare_db;
   ```
2. Verify credentials in `src/main/resources/application-mysql.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/foodshare_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```
3. Launch with the MySQL profile:
   ```powershell
   & "C:\Program Files\Java\jdk-21.0.12.1\bin\java.exe" -jar target\foodshare-1.0.0.jar --spring.profiles.active=mysql
   ```

---

## 7. Automated Test Suite Results

To execute all integration and algorithm tests:
```powershell
mvn test
```

### Verified Scenarios:
- **Scenario 1 & 2**: User and Provider registration, duplicate email rejection, BCrypt hashing, JWT issuance.
- **Scenario 3**: Adding and updating food listings with real-time remaining inventory tracking.
- **Scenario 4 & 5**: Free vs Discounted calculations, pessimistic row-level locking (`SELECT FOR UPDATE`), overbooking prevention.
- **Scenario 6**: Auto-rejection of expired food listings.
- **Scenario 7, 8 & 9**: Secure token generation, valid verification, token burn upon handover, prevention of duplicate redemption.
- **Algorithm Verification**: Java `PriorityQueue` ordering (`CHILDREN` > `ELDERLY` > `REGULAR`).
- **Scenario 10**: Volunteer offline community food camp dispatch and audit recording.

**Result: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` (BUILD SUCCESS)**
