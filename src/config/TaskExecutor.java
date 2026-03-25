package config;

import dao.CartDAO;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import service.OrderService;
import util.SessionCache;
import util.SessionPersister;

/**
 * TaskExecutor manages background scheduled tasks for the application.
 * 
 * Includes:
 * - Session cleanup to remove expired sessions from cache
 * - Session persistence to sync dirty sessions with database
 * - Cart cleanup to remove expired cart items
 * - Expired order cleanup to cancel pending orders beyond expiry
 * - Graceful shutdown of scheduled tasks
 */
public class TaskExecutor {
    
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private static final SessionPersister sessionPersister = new SessionPersister();
    private static final CartDAO cartDAO = new CartDAO();
    private static final OrderService orderService = new OrderService();
    
    static {
        scheduleSessionCleanup();
        scheduleSessionPersistence();
        scheduleCartCleanup();
        // scheduleExpiredOrderCleanup();
    }
    
    /**
     * Schedules session cleanup task to run every 5 minutes.
     * Removes expired sessions from memory cache.
     */
    private static void scheduleSessionCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                SessionCache.cleanExpiredSessions();    
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * Schedules session persistence task to run every 10 minutes.
     * Syncs dirty sessions from cache to database.
     */
    private static void scheduleSessionPersistence() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sessionPersister.persistDirtySessions(SessionCache.getAllSessions());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.MINUTES);
    }
    
    /**
     * Schedules cart cleanup task to run every hour.
     * Removes expired items from shopping carts.
     */
    private static void scheduleCartCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                cartDAO.deleteExpiredItems();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.HOURS);
    }

    /**
     * Schedules expired order cleanup task to run every minute.
     * Cancels orders that are pending and have exceeded expiry time.
     */
    private static void scheduleExpiredOrderCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                orderService.cancelExpiredOrders();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * Shuts down the scheduler gracefully.
     * Should be called during application shutdown.
     * Waits up to 30 seconds for tasks to complete before forcing shutdown.
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}