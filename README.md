# JCart - E-Commerce Platform

A Java-based e-commerce platform built with Jakarta Servlet, PostgreSQL, and JSP.

## Tech Stack

- **Backend**: Jakarta Servlet 6.0, JDBC
- **Database**: PostgreSQL 15.1
- **Server**: Apache Tomcat 10.1
- **Build**: Manual compilation with `javac`

## Database Setup

1. Create the PostgreSQL database:
```sql
CREATE DATABASE jcart;
```

2. Run the seed script:
```bash
psql -d jcart -f seed/init_db.sql
```

> Ensure `products.csv` is in the `seed/` folder or update the `COPY` path in `init_db.sql`.

## Build & Deploy

**Compile:**
```bash
javac -cp "lib/*" -d WEB-INF/classes src/**/*.java
```

**Deploy:**
```bash
./build/deploy.sh
```

## Configuration

- JNDI name: `java:comp/env/jdbc/JCart`
- Configure `context.xml` in Tomcat with your database credentials

## Project Structure
```
JCart/
├── build/
│   └── deploy.sh                           # Deployment script
├── seed/
│   ├── init_db.sql                         # Database schema & seed data
│   └── products.csv                        # Initial product data
├── src/
│   ├── config/
│   │   └── AsyncExecutor.java              # Thread pool for async operations
│   ├── controller/
│   │   ├── AdminController.java            # Admin endpoints (login, profile, password)
│   │   ├── AdminManagementController.java  # Admin CRUD operations
│   │   ├── BaseController.java             # Base controller with common methods
│   │   ├── CustomerController.java         # Customer endpoints (register, login, profile)
│   │   └── CustomerManagementController.java # Customer management for admins
│   ├── dao/
│   │   ├── AdminDAO.java                   # Admin database operations
│   │   ├── CustomerDAO.java                # Customer database operations
│   │   └── SessionDAO.java                 # Session database operations
│   ├── dto/
│   │   ├── AdminLoginRequest.java          # Admin login DTO
│   │   ├── AdminRegisterRequest.java       # Admin registration DTO
│   │   ├── AdminUpdateRequest.java         # Admin profile update DTO
│   │   ├── ApiResponse.java                # Standard API response wrapper
│   │   ├── CustomerLoginRequest.java       # Customer login DTO
│   │   ├── CustomerRegisterRequest.java    # Customer registration DTO
│   │   ├── CustomerUpdateRequest.java      # Customer profile update DTO
│   │   └── PasswordChangeRequest.java      # Password change DTO
│   ├── filter/
│   │   ├── AdminAuthFilter.java            # Authentication filter for admin endpoints
│   │   └── CustomerAuthFilter.java         # Authentication filter for customer endpoints
│   ├── model/
│   │   ├── Admin.java                      # Admin entity
│   │   ├── Customer.java                   # Customer entity
│   │   └── Session.java                    # Session entity with rolling expiry
│   ├── service/
│   │   ├── AdminService.java               # Admin business logic
│   │   └── CustomerService.java            # Customer business logic
│   └── util/
│       ├── DBUtil.java                     # Database connection utility
│       ├── JsonUtil.java                   # JSON serialization/deserialization
│       ├── PasswordUtil.java               # Password hashing & verification
│       ├── SessionCache.java               # In-memory session cache with periodic persistence
│       └── SessionPersister.java           # Background session persistence
├── WEB-INF/
│   ├── classes/                            # Compiled .class files
│   └── web.xml                             # Servlet configuration
├── .gitignore
└── README.md
```

## Features

1. Initial Setup - build automation, database seeding, async executor, and JNDI connection pool
   - Added `build/deploy.sh` for deployment automation
   - Added `seed/init_db.sql` and `seed/products.csv` for database initialization
   - Configured async executor with thread pool
   - Set up database connection pool via JNDI

2. Customer Authentication - registration, login, session management, and profile operations
   - Customer Registration - Create new customer account with hashed password
   - Customer Login - Authenticate and create session with rolling 24-hour expiry
   - Customer Logout - Invalidate session and clear cookie
   - Session Management - Hybrid approach with in-memory cache + periodic DB persistence
   - Profile Management - View and update customer profile (username, email, phone)
   - Password Change - Change password with old password verification
   - Account Deactivation - Soft delete (deactivate) customer account
   - Session Cache - ConcurrentHashMap with background sync every 10 minutes
   - Password Security - SHA-256 hashing with random salt
   - JSON Utilities - Lightweight JSON parsing without external dependencies
   - DTO Pattern - Clean separation between request/response and database entities
   - Auth Filter - Protects all `/customer/*` endpoints except login/register

3. Admin Authentication & Management - admin auth, role-based permissions, and customer management for admins
   - Admin Login - Authenticate with superadmin seeded in database
   - Admin Profile Management - View and update own profile
   - Admin Password Change - Change password with old password verification
   - Admin CRUD - List, get, create, update, and deactivate admins
   - Role-Based Permissions - Array-based permission system (`admin:view`, `admin:create`, `admin:update`, `admin:delete`, `customers:view`, `customers:delete`)
   - Superadmin Protection - Superadmin cannot be deactivated or modified
   - Customer Management - Admins can list, get, and deactivate customers
   - Auth Filter - Protects all `/admin/*` endpoints with separate filter
   - Soft Delete - Deactivation sets `is_active = false` for both admins and customers

## Customer Endpoints

| Endpoint              | Method          | Description                              |
|-----------------------|-----------------|------------------------------------------|
| `/customer/register`  | POST            | Create new customer account              |
| `/customer/login`     | POST            | Authenticate and get session token       |
| `/customer/logout`    | POST            | Invalidate current session               |
| `/customer/profile`   | GET             | Get logged-in customer profile           |
| `/customer/profile`   | POST (PATCH)    | Update profile (username, email, phone)  |
| `/customer/password`  | POST            | Change password                          |
| `/customer/account`   | POST (DELETE)   | Deactivate own account                   |

## Admin Endpoints

| Endpoint                    | Method        | Required Permission  | Description                              |
|-----------------------------|---------------|----------------------|------------------------------------------|
| `/admin/login`              | POST          | Public               | Admin login                              |
| `/admin/logout`             | POST          | Authenticated        | Admin logout                             |
| `/admin/profile`            | GET           | Authenticated        | Get own profile                          |
| `/admin/profile`            | POST (PATCH)  | Authenticated        | Update own profile                       |
| `/admin/profile/password`   | POST          | Authenticated        | Change own password                      |
| `/admin/admins`             | GET           | `admin:view`         | List all admins                          |
| `/admin/admins/{id}`        | GET           | `admin:view`         | Get specific admin                       |
| `/admin/admins`             | POST          | `admin:create`       | Create new admin                         |
| `/admin/admins/{id}`        | POST (PATCH)  | `admin:update`       | Update admin (role, permissions, status) |
| `/admin/admins/{id}`        | POST (DELETE) | `admin:delete`       | Deactivate admin                         |
| `/admin/customers`          | GET           | `customers:view`     | List all customers                       |
| `/admin/customers/{id}`     | GET           | `customers:view`     | Get specific customer                    |
| `/admin/customers/{id}`     | POST (DELETE) | `customers:delete`   | Deactivate customer                      |