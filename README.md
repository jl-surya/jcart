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
│   └── deploy.sh                   # Deployment script
├── seed/
│   ├── init_db.sql                 # Database schema & seed data
│   └── products.csv                # Initial product data
├── src/                            # Java source files
│   ├── config/
│   │   └── AsyncExecutor.java      # Thread pool for async operations
│   └── util/
│       └── DBUtil.java             # Database connection utility
├── WEB-INF/
│   ├── classes/                    # Compiled .class files
│   └── web.xml                     # Servlet configuration
├── .gitignore
└── README.md
```

## Features

1. Initial Setup - build automation, database seeding, async executor, and JNDI connection pool
   - Added `build/deploy.sh` for deployment automation
   - Added `seed/init_db.sql` and `seed/products.csv` for database initialization
   - Configured async executor with thread pool
   - Set up database connection pool via JNDI