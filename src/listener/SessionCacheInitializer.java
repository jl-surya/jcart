package listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import util.SessionCache;

/**
 * SessionCacheInitializer loads active sessions from database on application startup.
 * This ensures sessions persist across Tomcat restarts.
 */
@WebListener
public class SessionCacheInitializer implements ServletContextListener {
    
    /* 
     * On application startup, load active sessions from the database into the SessionCache.
     * This allows users to remain logged in even if Tomcat is restarted.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("=== JCart Application Starting ===");
        System.out.println("Loading active sessions from database...");
        
        int sessionsLoaded = SessionCache.loadFromDatabase();
        
        System.out.println("Session cache initialized with " + sessionsLoaded + " active sessions");
        System.out.println("=== JCart Application Started Successfully ===");
    }
    
    /*
     * On application shutdown, force sync all dirty sessions to the database.
     * This ensures that any changes to sessions are not lost when Tomcat shuts down.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("=== JCart Application Shutting Down ===");
        System.out.println("Syncing dirty sessions to database...");
        
        SessionCache.forceSync();
        
        System.out.println("=== Session cache synced. Shutdown complete ===");
    }
}