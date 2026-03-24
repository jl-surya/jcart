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
│   └── deploy.sh                               # Deployment script
├── seed/
│   ├── init_db.sql                             # Database schema & seed data
│   └── products.csv                            # Initial product data
├── src/
│   ├── config/
│   │   └── AsyncExecutor.java                  # Thread pool for async operations
│   ├── controller/
│   │   ├── AdminController.java                # Admin endpoints (login, profile, password)
│   │   ├── AdminManagementController.java      # Admin CRUD operations
│   │   ├── BaseController.java                 # Base controller with common methods
│   │   ├── CustomerController.java             # Customer endpoints (register, login, profile)
│   │   ├── CustomerManagementController.java   # Customer management for admins
│   │   ├── ProductController.java              # Customer product endpoints (search, view)
│   │   └── ProductManagementController.java    # Admin product management (CRUD, activate/deactivate)
│   ├── dao/
│   │   ├── AdminDAO.java                       # Admin database operations
│   │   ├── CustomerDAO.java                    # Customer database operations
│   │   ├── ProductDAO.java                     # Product database operations with search filters
│   │   └── SessionDAO.java                     # Session database operations
│   ├── dto/
│   │   ├── AdminLoginRequest.java              # Admin login DTO
│   │   ├── AdminRegisterRequest.java           # Admin registration DTO
│   │   ├── AdminUpdateRequest.java             # Admin profile update DTO
│   │   ├── ApiResponse.java                    # Standard API response wrapper
│   │   ├── CustomerLoginRequest.java           # Customer login DTO
│   │   ├── CustomerRegisterRequest.java        # Customer registration DTO
│   │   ├── CustomerUpdateRequest.java          # Customer profile update DTO
│   │   ├── PasswordChangeRequest.java          # Password change DTO
│   │   ├── ProductCreateRequest.java           # Product creation DTO
│   │   ├── ProductSearchRequest.java           # Product search filters DTO
│   │   └── ProductUpdateRequest.java           # Product update DTO
│   ├── filter/
│   │   ├── AdminAuthFilter.java                # Authentication filter for admin endpoints
│   │   └── CustomerAuthFilter.java             # Authentication filter for customer endpoints
│   ├── model/
│   │   ├── Admin.java                          # Admin entity with permissions
│   │   ├── Customer.java                       # Customer entity
│   │   ├── Product.java                        # Product entity with active status
│   │   └── Session.java                        # Session entity with rolling expiry
│   ├── service/
│   │   ├── AdminService.java                   # Admin business logic
│   │   ├── CustomerService.java                # Customer business logic
│   │   └── ProductService.java                 # Product business logic with search & filters
│   └── util/
│       ├── DBUtil.java                         # Database connection utility
│       ├── JsonUtil.java                       # JSON serialization/deserialization
│       ├── PasswordUtil.java                   # Password hashing & verification
│       ├── SessionCache.java                   # In-memory session cache with periodic persistence
│       └── SessionPersister.java               # Background session persistence
├── WEB-INF/
│   ├── classes/                                # Compiled .class files
│   └── web.xml                                 # Servlet configuration
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
   - Role-Based Permissions - Array-based permission system (`admins:view`, `admins:create`, `admins:update`, `admins:delete`, `customers:view`, `customers:delete`, `products:view`, `products:create`, `products:update`, `products:delete`)
   - Superadmin Protection - Superadmin cannot be deactivated or modified
   - Customer Management - Admins can list, get, and deactivate customers
   - Auth Filter - Protects all `/admin/*` endpoints with separate filter
   - Soft Delete - Deactivation sets `is_active = false` for admins, customers, and products

4. Product Catalog - product browsing, search, filtering, and admin management
   - Product Browsing - Customers can search and view products with multi-field keyword search (name + location)
   - Advanced Filtering - Filter by category, gender, age group, seasonality, price range, and stock availability
   - Pagination & Sorting - Configurable page size and sorting options (price, name, creation date, etc.)
   - Filter Options API - Dynamic filter options (categories, age groups, genders, locations) for UI dropdowns
   - Product Management - Admins can create, update, view, and delete products
   - Soft Delete - Deactivated products are hidden from customers but visible to admins
   - Product Activation - Admins can reactivate previously deactivated products
   - Active Status Filter - Admins can filter products by active/inactive status
   - Consistent Ordering - Results sorted by `product_id DESC` by default for deterministic pagination
   - Search Performance - Indexed on category, gender, age group, seasonality, location, and product name

## Endpoints

### Authentication

| Endpoint             | Method     | Role     | Permission    | Description                     |
|----------------------|------------|----------|---------------|---------------------------------|
| `/customer/register` | POST       | Public   | -             | Create new customer account     |
| `/customer/login`    | POST       | Public   | -             | Customer login and get session  |
| `/customer/logout`   | POST       | Customer | Authenticated | Invalidate current session      |
| `/admin/login`       | POST       | Public   | -             | Admin login and get session     |
| `/admin/logout`      | POST       | Admin    | Authenticated | Invalidate admin session        |

### Customer Profile

| Endpoint             | Method      | Role     | Permission    | Description                    |
|----------------------|-------------|----------|---------------|--------------------------------|
| `/customer/profile`  | GET, PATCH  | Customer | Authenticated | Get or update profile          |
| `/customer/password` | POST        | Customer | Authenticated | Change password                |
| `/customer/account`  | DELETE      | Customer | Authenticated | Deactivate own account         |

### Admin Profile

| Endpoint                  | Method     | Role  | Permission    | Description                   |
|---------------------------|------------|-------|---------------|-------------------------------|
| `/admin/profile`          | GET, PATCH | Admin | Authenticated | Get or update own profile     |
| `/admin/profile/password` | POST       | Admin | Authenticated | Change own password           |

### Admin Management

| Endpoint              | Method                  | Role  | Permission                     | Description                       |
|-----------------------|-------------------------|-------|--------------------------------|-----------------------------------|
| `/admin/admins`       | GET, POST               | Admin | `admins:view \ create`          | List all or create admin         |
| `/admin/admins/{id}`  | GET, PATCH, DELETE      | Admin | `admins:view \ update \ delete` | Get, update, or deactivate admin |

### Customer Management

| Endpoint                | Method       | Role  | Permission                  | Description                    |
|-------------------------|--------------|-------|-----------------------------|--------------------------------|
| `/admin/customers`      | GET          | Admin | `customers:view`            | List all customers             |
| `/admin/customers/{id}` | GET, DELETE  | Admin | `customers:view \ delete`   | Get or deactivate customer     |

### Products

| Endpoint                        | Method             | Role     | Permission                        | Description                            |
|---------------------------------|--------------------|----------|-----------------------------------|----------------------------------------|
| `/products/search`              | POST               | Customer | Authenticated                     | Search products with filters           |
| `/products/{id}`                | GET                | Customer | Authenticated                     | Get single product details             |
| `/products/filter-options`      | GET                | Customer | Authenticated                     | Get available filter options for UI    |
| `/admin/products/search`        | POST               | Admin    | `products:view`                   | Search products (active and inactive)  |
| `/admin/products`               | POST               | Admin    | `products:create`                 | Create new product                     |
| `/admin/products/{id}`          | GET, PATCH, DELETE | Admin    | `products:view \ update \ delete` | Get, update, or delete product         |
| `/admin/products/{id}/activate` | PATCH              | Admin    | `products:update`                 | Activate product (set active)          |