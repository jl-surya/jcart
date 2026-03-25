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
│   └── deploy.sh                                   # Deployment script
├── seed/
│   ├── init_db.sql                                 # Database schema & seed data
│   └── products.csv                                # Initial product data
├── src/
│   ├── config/
│   │   ├── AsyncExecutor.java                      # Thread pool for async operations
│   │   └── TaskExecutor.java                       # Scheduled tasks (session, cart, order cleanup)
│   ├── controller/
│   │   ├── AddressController.java                  # Customer address management
│   │   ├── AdminController.java                    # Admin endpoints (login, profile, password)
│   │   ├── AdminManagementController.java          # Admin CRUD operations for admins
│   │   ├── OrderManagementController.java          # Order management for admins
│   │   ├── TransactionManagementController.java    # Transaction management (payments & refunds) for admins
│   │   ├── BaseController.java                     # Base controller with common methods
│   │   ├── CartController.java                     # Customer cart management
│   │   ├── CustomerController.java                 # Customer endpoints (register, login, profile)
│   │   ├── CustomerManagementController.java       # Customer management for admins
│   │   ├── OrderController.java                    # Customer order endpoints
│   │   ├── ProductController.java                  # Customer product endpoints (search, view)
│   │   ├── ProductManagementController.java        # Product management for admins
│   │   └── TransactionController.java              # Customer transaction viewing
│   ├── dao/
│   │   ├── AddressDAO.java                         # Address database operations
│   │   ├── AdminDAO.java                           # Admin database operations
│   │   ├── CartDAO.java                            # Cart database operations
│   │   ├── CustomerDAO.java                        # Customer database operations
│   │   ├── OrderDAO.java                           # Order database operations with pagination
│   │   ├── OrderItemDAO.java                       # Order items database operations
│   │   ├── ProductDAO.java                         # Product database operations with search filters
│   │   ├── SessionDAO.java                         # Session database operations
│   │   └── TransactionDAO.java                     # Transaction database operations
│   ├── dto/
│   │   ├── AddressRequest.java                     # Address create/update DTO
│   │   ├── AdminLoginRequest.java                  # Admin login DTO
│   │   ├── AdminRegisterRequest.java               # Admin registration DTO
│   │   ├── AdminUpdateRequest.java                 # Admin profile update DTO
│   │   ├── ApiResponse.java                        # Standard API response wrapper
│   │   ├── CartItemRequest.java                    # Add to cart DTO
│   │   ├── CustomerLoginRequest.java               # Customer login DTO
│   │   ├── CustomerRegisterRequest.java            # Customer registration DTO
│   │   ├── CustomerUpdateRequest.java              # Customer profile update DTO
│   │   ├── DirectOrderRequest.java                 # Direct buy now order DTO
│   │   ├── OrderFilterRequest.java                 # Order list filters DTO
│   │   ├── OrderRequest.java                       # Create order from cart DTO
│   │   ├── OrderResponse.java                      # Order details response with invoice
│   │   ├── OrderStatusUpdateRequest.java           # Order status update DTO
│   │   ├── PasswordChangeRequest.java              # Password change DTO
│   │   ├── ProductCreateRequest.java               # Product creation DTO
│   │   ├── ProductSearchRequest.java               # Product search filters DTO
│   │   ├── ProductUpdateRequest.java               # Product update DTO
│   │   ├── TransactionActionRequest.java           # Refund approve/reject DTO
│   │   ├── TransactionFilterRequest.java           # Transaction list filters DTO
│   │   ├── UpdateCartItemRequest.java              # Update cart quantity DTO
│   │   └── UpdateOrderAddressRequest.java          # Update order address DTO
│   ├── filter/
│   │   ├── AdminAuthFilter.java                    # Authentication filter for admin endpoints
│   │   └── CustomerAuthFilter.java                 # Authentication filter for customer endpoints
│   ├── model/
│   │   ├── Address.java                            # Address entity with default flag
│   │   ├── Admin.java                              # Admin entity with permissions
│   │   ├── CartItem.java                           # Cart item entity with expiry
│   │   ├── Customer.java                           # Customer entity
│   │   ├── Order.java                              # Order entity with status and payment tracking
│   │   ├── OrderItem.java                          # Order item entity
│   │   ├── Product.java                            # Product entity with active status
│   │   ├── Session.java                            # Session entity with rolling expiry
│   │   └── Transaction.java                        # Transaction entity (payment & refund)
│   ├── service/
│   │   ├── AddressService.java                     # Address business logic
│   │   ├── AdminService.java                       # Admin business logic
│   │   ├── CartService.java                        # Cart business logic
│   │   ├── CustomerService.java                    # Customer business logic
│   │   ├── OrderService.java                       # Order business logic (atomic operations)
│   │   ├── PaymentGateway.java                     # Mock payment gateway
│   │   ├── ProductService.java                     # Product business logic with search & filters
│   │   └── TransactionService.java                 # Transaction & refund business logic
│   └── util/
│       ├── DBUtil.java                             # Database connection utility
│       ├── JsonUtil.java                           # JSON serialization/deserialization
│       ├── PasswordUtil.java                       # Password hashing & verification
│       ├── SessionCache.java                       # In-memory session cache with periodic persistence
│       └── SessionPersister.java                   # Background session persistence
├── WEB-INF/
│   ├── classes/                                    # Compiled .class files
│   └── web.xml                                     # Servlet configuration
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
   - Role-Based Permissions - Array-based permission system (`admins:view`, `admins:create`, `admins:update`, `admins:delete`, `customers:view` `customers:delete`, `products:view`, `products:create`, `products:update`, `products:delete`, `orders:view`, `orders:update`, `transactions:view`, `transactions:update`)
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

5. Shopping Cart & Address Management - cart operations and address book for customers
   - Cart Management - Add, update quantity, remove items, and clear cart
   - Cart Expiration - Cart items automatically expire after 30 days
   - Stock Status Display - Shows stock availability and warnings for low/out of stock items
   - Quantity Limits - Maximum 50 items per product in cart
   - Product Validation - Cannot add inactive or unavailable products to cart
   - Address Management - Create, update, delete, and view shipping addresses
   - Default Address - Each customer can have one default address, automatically managed
   - Address Limits - Maximum 10 addresses per customer
   - Address Validation - Required fields validation (recipient name, address line, city, postal code, country)
   - Scheduled Cleanup - Background task removes expired cart items hourly

6. Order & Transaction Management - order creation, payment processing, and transaction tracking
   - Order Creation - Create orders from cart or direct buy now with address selection (saved or one-time)
   - Payment Processing - Mock payment gateway with transaction audit trail (INITIATED → COMPLETED/FAILED)
   - Stock Management - Stock deducted at order creation, restored on cancellation or expiry
   - Payment Deadline - 5-minute window for payment completion with automatic expiry cleanup
   - Order Status Flow - PENDING → PROCESSING → SHIPPED → DELIVERED with validation rules
   - Order Cancellation - Customer can cancel PROCESSING orders with automatic refund request
   - Address Update - Customers can modify shipping address for PENDING/PROCESSING orders
   - Admin Order Management - View all orders, update status (SHIPPED/DELIVERED) with permission checks
   - Refund Management - Admin can approve or reject refund requests with transaction tracking
   - Transaction History - Complete audit trail for payments and refunds with pagination and filters
   - Invoice Generation - Auto-generated invoice numbers for each order
   - Scheduled Cleanup - Background task cancels expired orders and restores stock every minute
   - Status Validation - Enforced order and payment status transitions with proper error messages
   - Filters & Pagination - Both orders and transactions support filtering by status, date, amount, and pagination

## Endpoints

### Authentication

| Endpoint             | Method     | Role     | Permission    | Description                    |
|----------------------|------------|----------|---------------|--------------------------------|
| `/customer/register` | POST       | Public   | -             | Create new customer account    |
| `/customer/login`    | POST       | Public   | -             | Customer login and get session |
| `/customer/logout`   | POST       | Customer | Authenticated | Invalidate current session     |
| `/admin/login`       | POST       | Public   | -             | Admin login and get session    |
| `/admin/logout`      | POST       | Admin    | Authenticated | Invalidate admin session       |

### Customer Profile

| Endpoint             | Method     | Role     | Permission    | Description                    |
|----------------------|------------|----------|---------------|--------------------------------|
| `/customer/profile`  | GET, PATCH | Customer | Authenticated | Get or update profile          |
| `/customer/password` | POST       | Customer | Authenticated | Change password                |
| `/customer/account`  | DELETE     | Customer | Authenticated | Deactivate own account         |

### Admin Profile

| Endpoint                  | Method     | Role  | Permission    | Description               |
|---------------------------|------------|-------|---------------|---------------------------|
| `/admin/profile`          | GET, PATCH | Admin | Authenticated | Get or update own profile |
| `/admin/profile/password` | POST       | Admin | Authenticated | Change own password       |

### Admin Management

| Endpoint             | Method             | Role  | Permission                      | Description                      |
|----------------------|--------------------|-------|---------------------------------|----------------------------------|
| `/admin/admins`      | GET, POST          | Admin | `admins:view \ create`          | List all or create admin         |
| `/admin/admins/{id}` | GET, PATCH, DELETE | Admin | `admins:view \ update \ delete` | Get, update, or deactivate admin |

### Customer Management

| Endpoint                | Method      | Role  | Permission                | Description                |
|-------------------------|-------------|-------|---------------------------|----------------------------|
| `/admin/customers`      | GET         | Admin | `customers:view`          | List all customers         |
| `/admin/customers/{id}` | GET, DELETE | Admin | `customers:view \ delete` | Get or deactivate customer |

### Products

| Endpoint                        | Method             | Role     | Permission                        | Description                           |
|---------------------------------|--------------------|----------|-----------------------------------|---------------------------------------|
| `/products/search`              | POST               | Customer | Authenticated                     | Search products with filters          |
| `/products/{id}`                | GET                | Customer | Authenticated                     | Get single product details            |
| `/products/filter-options`      | GET                | Customer | Authenticated                     | Get available filter options for UI   |
| `/admin/products/search`        | POST               | Admin    | `products:view`                   | Search products (active and inactive) |
| `/admin/products`               | POST               | Admin    | `products:create`                 | Create new product                    |
| `/admin/products/{id}`          | GET, PATCH, DELETE | Admin    | `products:view \ update \ delete` | Get, update, or delete product        |
| `/admin/products/{id}/activate` | PATCH              | Admin    | `products:update`                 | Activate product (set active)         |

### Cart

| Endpoint                     | Method        | Role     | Permission    | Description                         |
|------------------------------|---------------|----------|---------------|-------------------------------------|
| `/customer/cart`             | GET, POST     | Customer | Authenticated | Get cart items or add item to cart  |
| `/customer/cart/{productId}` | PATCH, DELETE | Customer | Authenticated | Update quantity or remove from cart |
| `/customer/cart/clear`       | POST          | Customer | Authenticated | Clear entire cart                   |

### Addresses

| Endpoint                           | Method             | Role     | Permission    | Description                      |
|------------------------------------|--------------------|----------|---------------|----------------------------------|
| `/customer/addresses`              | GET, POST          | Customer | Authenticated | List all addresses or create new |
| `/customer/addresses/{id}`         | GET, PATCH, DELETE | Customer | Authenticated | Get, update, or delete address   |
| `/customer/addresses/default`      | GET                | Customer | Authenticated | Get default address              |
| `/customer/addresses/{id}/default` | POST               | Customer | Authenticated | Set address as default           |

### Orders

| Endpoint                             | Method      | Role     | Permission      | Description                              |
|--------------------------------------|-------------|----------|-----------------|------------------------------------------|
| `/customer/orders/cart`              | POST        | Customer | Authenticated   | Create order from cart                   |
| `/customer/orders/direct`            | POST        | Customer | Authenticated   | Create direct order (buy now)            |
| `/customer/orders`                   | GET         | Customer | Authenticated   | List customer orders (paginated, filters)|
| `/customer/orders/{orderId}`         | GET         | Customer | Authenticated   | Get order details with invoice           |
| `/customer/orders/{orderId}/cancel`  | POST        | Customer | Authenticated   | Cancel order (PROCESSING only)           |
| `/customer/orders/{orderId}/address` | PATCH       | Customer | Authenticated   | Update shipping address                  |
| `/admin/orders`                      | GET         | Admin    | `orders:view`   | List all orders (paginated, filters)     |
| `/admin/orders/{orderId}`            | GET         | Admin    | `orders:view`   | Get order details                        |
| `/admin/orders/{orderId}/status`     | PATCH       | Admin    | `orders:update` | Update order status (SHIPPED/DELIVERED)  |

### Transactions

| Endpoint                                  | Method | Role     | Permission            | Description                               |
|-------------------------------------------|--------|----------|-----------------------|-------------------------------------------|
| `/customer/transactions`                  | GET    | Customer | Authenticated         | List customer transactions (paginated)    |
| `/customer/transactions/{id}`             | GET    | Customer | Authenticated         | Get transaction details                   |
| `/admin/transactions`                     | GET    | Admin    | `transactions:view`   | List all transactions (paginated, filters)|
| `/admin/transactions/{id}`                | GET    | Admin    | `transactions:view`   | Get transaction details                   |
| `/admin/transactions/{id}/action`         | POST   | Admin    | `transactions:update` | Approve or reject refund request          |