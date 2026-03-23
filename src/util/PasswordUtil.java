package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PasswordUtil provides secure password hashing and verification.
 * 
 * Includes:
 * - Salted password hashing using SHA-256
 * - Secure random salt generation
 * - Constant-time password verification
 * - Base64 encoding for storage
 */
public class PasswordUtil {
    
    private static final int SALT_LENGTH = 16;
    private static final String HASH_ALGORITHM = "SHA-256";
    
    /**
     * Hashes a password with a randomly generated salt.
     * Returns a Base64 encoded string containing both salt and hash.
     *
     * @param password the plain text password
     * @return Base64 encoded hashed password with salt
     */
    public static String hashPassword(String password) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Verifies a plain text password against a stored hashed password.
     * Uses constant-time comparison to prevent timing attacks.
     *
     * @param password the plain text password to verify
     * @param hashedPassword the stored Base64 encoded hashed password
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            byte[] combined = Base64.getDecoder().decode(hashedPassword);
            
            byte[] salt = new byte[SALT_LENGTH];
            byte[] storedHash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, storedHash, 0, storedHash.length);
            
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] computedHash = md.digest(password.getBytes());
            
            return MessageDigest.isEqual(computedHash, storedHash);
            
        } catch (NoSuchAlgorithmException | IllegalArgumentException e) {
            return false;
        }
    }
}