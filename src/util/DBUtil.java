package util;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * DBUtil provides database connection management through JNDI DataSource.
 * 
 * Includes:
 * - JNDI lookup for connection pool configuration
 * - Singleton DataSource initialization
 * - Connection retrieval for database operations
 */
public class DBUtil {

    private static final String JNDI_NAME = "java:comp/env/jdbc/JCart";
    private static DataSource dataSource;

    static {
        initDataSource();
    }

    /**
     * Initializes DataSource by performing JNDI lookup.
     * Called once during class loading.
     *
     * @throws RuntimeException if DataSource lookup fails or returns null
     */
    private static void initDataSource() {
        try {
            Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(JNDI_NAME);

            if (dataSource == null) {
                throw new RuntimeException("JNDI lookup returned null for: " + JNDI_NAME);
            }

        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup DataSource: " + JNDI_NAME, e);
        }
    }

    /**
     * Retrieves a database connection from the connection pool.
     *
     * @return a Connection object from the DataSource
     * @throws SQLException if connection cannot be obtained or DataSource not initialized
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }
}