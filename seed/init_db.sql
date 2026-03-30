-- JCart Database Initialization Script
-- Drops existing tables and creates fresh schema with sequences, tables, indexes, and triggers
-- Includes sample data for admins, customers, addresses, and products

-- Drop tables in reverse dependency order
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS products CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS sessions CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS admins CASCADE;

-- Create sequences for auto-generated IDs
CREATE SEQUENCE admin_seq;
CREATE SEQUENCE customer_seq;
CREATE SEQUENCE product_seq START 1;

-- Admins table: Stores administrator accounts with role-based permissions
CREATE TABLE admins (
    admin_id VARCHAR(8) PRIMARY KEY DEFAULT 'A' || LPAD(nextval('admin_seq')::TEXT, 7, '0'),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL,
    permissions TEXT[] DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    is_super_admin BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER SEQUENCE admin_seq OWNED BY admins.admin_id;

-- Customers table: Stores customer account information
CREATE TABLE customers (
    customer_id VARCHAR(8) PRIMARY KEY DEFAULT 'C' || LPAD(nextval('customer_seq')::TEXT, 7, '0'),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(120) UNIQUE NOT NULL,
    password TEXT NOT NULL,
    phone VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER SEQUENCE customer_seq OWNED BY customers.customer_id;

-- Sessions table: Stores user session data for authentication
CREATE TABLE sessions (
    session_id UUID PRIMARY KEY,
    user_type TEXT NOT NULL,
    user_id VARCHAR(8) NOT NULL,
    session_token TEXT UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_sync_at TIMESTAMP
);

-- Indexes for performance optimization
CREATE INDEX idx_sessions_user ON sessions(user_type, user_id);
CREATE INDEX idx_sessions_token ON sessions(session_token);
CREATE INDEX idx_sessions_expires ON sessions(expires_at);

-- Addresses table: Stores customer shipping addresses
CREATE TABLE addresses (
    address_id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(8) NOT NULL,
    recipient_name VARCHAR(100) NOT NULL,
    address_line VARCHAR(200) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Indexes for performance optimization
CREATE UNIQUE INDEX idx_addresses_customer_default ON addresses(customer_id) WHERE is_default = TRUE;

-- Products table: Stores product catalog information
CREATE TABLE products (
    product_id VARCHAR(8) PRIMARY KEY DEFAULT 'P' || LPAD(nextval('product_seq')::TEXT, 7, '0'),
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    price NUMERIC(12,2),
    discount NUMERIC(5,2),
    tax_rate NUMERIC(5,2),
    stock_level INT,
    age_group VARCHAR(50),
    location VARCHAR(100),
    gender VARCHAR(20),
    shipping_cost NUMERIC(10,2),
    shipping_method VARCHAR(100),
    seasonality VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER SEQUENCE product_seq OWNED BY products.product_id;

-- Indexes for performance optimization
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_gender ON products(gender);
CREATE INDEX idx_products_age_group ON products(age_group);
CREATE INDEX idx_products_seasonality ON products(seasonality);
CREATE INDEX idx_products_location ON products(location);
CREATE INDEX idx_products_stock_level ON products(stock_level);
CREATE INDEX idx_products_name ON products(product_name);
CREATE INDEX idx_products_is_active ON products(is_active);

-- Cart items table: Stores items in customer shopping cart
CREATE TABLE cart_items (
    customer_id VARCHAR(8) NOT NULL,
    product_id VARCHAR(8) NOT NULL,
    quantity INT NOT NULL CHECK (quantity BETWEEN 1 AND 50),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP + INTERVAL '30 days',
    PRIMARY KEY (customer_id, product_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- Indexes for performance optimization
CREATE INDEX idx_cart_items_expires ON cart_items(expires_at);

-- Orders table: Stores customer orders
CREATE TABLE orders (
    order_id BIGSERIAL PRIMARY KEY,
    customer_id VARCHAR(8) NOT NULL,
    order_status VARCHAR(30) NOT NULL CHECK (order_status IN ('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    payment_status VARCHAR(30) NOT NULL CHECK (payment_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDING', 'REFUNDED', 'REJECTED')),
    total_amount NUMERIC(12,2) NOT NULL,
    shipping_name VARCHAR(100) NOT NULL,
    shipping_address_line VARCHAR(200) NOT NULL,
    shipping_city VARCHAR(100) NOT NULL,
    shipping_state VARCHAR(100),
    shipping_postal_code VARCHAR(20) NOT NULL,
    shipping_country VARCHAR(100) NOT NULL,
    invoice_number VARCHAR(50) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    cancelled_by VARCHAR(8),
    payment_deadline TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL '5 minutes'),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_orders_customer_status ON orders(customer_id, order_status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_orders_payment_deadline ON orders(payment_deadline);

-- Order items table: Stores individual items within an order
CREATE TABLE order_items (
    order_item_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id VARCHAR(8) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    quantity INT NOT NULL,
    discount NUMERIC(5,2),
    tax_rate NUMERIC(5,2),
    subtotal NUMERIC(12,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE INDEX idx_order_items_order ON order_items(order_id);

-- Transactions table: Stores payment transactions for orders
CREATE TABLE transactions (
    transaction_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('PAYMENT', 'REFUND')),
    transaction_method VARCHAR(50) NOT NULL,
    transaction_status VARCHAR(30) NOT NULL CHECK (transaction_status IN ('PENDING', 'PAID', 'FAILED', 'REFUNDED', 'REJECTED')),
    amount NUMERIC(12,2) NOT NULL,
    transaction_reference VARCHAR(200) UNIQUE,
    refund_reason TEXT,
    processed_by_type TEXT,
    processed_by VARCHAR(8),
    processed_at TIMESTAMP,
    verified_by VARCHAR(8),
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

CREATE INDEX idx_transactions_order ON transactions(order_id);
CREATE INDEX idx_transactions_order_type ON transactions(order_id, transaction_type);
CREATE INDEX idx_transactions_status ON transactions(transaction_status);
CREATE INDEX idx_transactions_reference ON transactions(transaction_reference);

-- Trigger function to protect super admin accounts from deletion or modification
CREATE OR REPLACE FUNCTION protect_super_admin()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.is_super_admin = TRUE THEN
        IF TG_OP = 'UPDATE' THEN
            IF NEW.is_active = FALSE THEN
                RAISE EXCEPTION 'Cannot deactivate SUPER_ADMIN account';
            END IF;
            IF NEW.is_super_admin = FALSE THEN
                RAISE EXCEPTION 'Cannot remove SUPER_ADMIN status';
            END IF;
            IF NEW.role != 'SUPER_ADMIN' THEN
                RAISE EXCEPTION 'Cannot change SUPER_ADMIN role';
            END IF;
            RETURN NEW;
        ELSIF TG_OP = 'DELETE' THEN
            RAISE EXCEPTION 'Cannot delete SUPER_ADMIN account';
        END IF;
    END IF;
    
    IF TG_OP = 'UPDATE' THEN
        RETURN NEW;
    ELSE
        RETURN OLD;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger to admins table
CREATE TRIGGER protect_super_admin_trigger
    BEFORE UPDATE OR DELETE ON admins
    FOR EACH ROW
    EXECUTE FUNCTION protect_super_admin();

-- Insert super admin account (hashed password)
INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin)
VALUES (
    'superadmin',
    'superadmin@jcart.com',
    'tZzybcrw6p/o4evfqqHe07FdGWExcnAdw87rVaa0mtnEJBbZclS17p7f3mlJA0N/',
    '9999999991',
    'SUPER_ADMIN',
    ARRAY['*'],
    TRUE,
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- Import products from CSV file
COPY products (
    product_name,
    category,
    price,
    discount,
    tax_rate,
    stock_level,
    age_group,
    location,
    gender,
    shipping_cost,
    shipping_method,
    seasonality
)
FROM '/opt/postgres/products.csv'
DELIMITER ','
CSV HEADER;

-- Reset product sequence after import
SELECT setval(
    'product_seq',
    COALESCE(
        (SELECT MAX(CAST(SUBSTRING(product_id FROM 2) AS INT)) FROM products),
        1
    )
);

-- Insert sample customers with hashed password
INSERT INTO customers (username, email, password, phone)
SELECT 
    'customer_' || i,
    'customer_' || i || '@example.com',
    'nDS5cgYkkGXuUS0kB8CiMKTGIF6mP6MSQrLpmOEBkpho+noGuSeg+vqTUJym2mgM',
    (9000000000 + i)::TEXT
FROM generate_series(1, 20) AS s(i);

-- Insert sample addresses for customers
INSERT INTO addresses (
    customer_id,
    recipient_name,
    address_line,
    city,
    state,
    postal_code,
    country,
    phone,
    is_default
)
SELECT 
    c.customer_id,
    c.username,
    'Street ' || a || ', Area ' || c.customer_id,
    'Chennai',
    'Tamil Nadu',
    '60000' || (ROW_NUMBER() OVER () % 10),
    'India',
    c.phone,
    CASE 
        WHEN ROW_NUMBER() OVER (PARTITION BY c.customer_id ORDER BY g.a) = 1 
        THEN TRUE 
        ELSE FALSE 
    END AS is_default
FROM customers c
CROSS JOIN generate_series(1, 2) AS g(a);

-- Reset customer sequence after sample insert
SELECT setval(
    'customer_seq',
    COALESCE(
        (SELECT MAX(CAST(SUBSTRING(customer_id FROM 2) AS INT)) FROM customers),
        1
    )
);
-- ADMIN MANAGER (Full admin control)
INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin)
VALUES (
    'admin_manager',
    'admin_manager@jcart.com',
    'tZzybcrw6p/o4evfqqHe07FdGWExcnAdw87rVaa0mtnEJBbZclS17p7f3mlJA0N/',
    '9999999992',
    'ADMIN_MANAGER',
    ARRAY['admins:view', 'admins:create', 'admins:update', 'admins:delete'],
    TRUE,
    FALSE
) ON CONFLICT (username) DO NOTHING;


-- CUSTOMER MANAGER
INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin)
VALUES (
    'customer_manager',
    'customer_manager@jcart.com',
    'tZzybcrw6p/o4evfqqHe07FdGWExcnAdw87rVaa0mtnEJBbZclS17p7f3mlJA0N/',
    '9999999993',
    'CUSTOMER_MANAGER',
    ARRAY['customers:view', 'customers:delete'],
    TRUE,
    FALSE
) ON CONFLICT (username) DO NOTHING;


-- PRODUCT MANAGER
INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin)
VALUES (
    'product_manager',
    'product_manager@jcart.com',
    'tZzybcrw6p/o4evfqqHe07FdGWExcnAdw87rVaa0mtnEJBbZclS17p7f3mlJA0N/',
    '9999999994',
    'PRODUCT_MANAGER',
    ARRAY['products:view', 'products:create', 'products:update', 'products:delete'],
    TRUE,
    FALSE
) ON CONFLICT (username) DO NOTHING;


-- ORDER MANAGER
INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin)
VALUES (
    'order_manager',
    'order_manager@jcart.com',
    'tZzybcrw6p/o4evfqqHe07FdGWExcnAdw87rVaa0mtnEJBbZclS17p7f3mlJA0N/',
    '9999999995',
    'ORDER_MANAGER',
    ARRAY['orders:view', 'orders:update'],
    TRUE,
    FALSE
) ON CONFLICT (username) DO NOTHING;


-- TRANSACTION MANAGER
INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin)
VALUES (
    'transaction_manager',
    'transaction_manager@jcart.com',
    'tZzybcrw6p/o4evfqqHe07FdGWExcnAdw87rVaa0mtnEJBbZclS17p7f3mlJA0N/',
    '9999999996',
    'TRANSACTION_MANAGER',
    ARRAY['transactions:view', 'transactions:update'],
    TRUE,
    FALSE
) ON CONFLICT (username) DO NOTHING;


-- VIEWER (Read-only across modules)
INSERT INTO admins (username, email, password, phone, role, permissions, is_active, is_super_admin)
VALUES (
    'viewer',
    'viewer@jcart.com',
    'tZzybcrw6p/o4evfqqHe07FdGWExcnAdw87rVaa0mtnEJBbZclS17p7f3mlJA0N/',
    '9999999997',
    'VIEWER',
    ARRAY['admins:view', 'customers:view', 'products:view', 'orders:view', 'transactions:view'],
    TRUE,
    FALSE
) ON CONFLICT (username) DO NOTHING;