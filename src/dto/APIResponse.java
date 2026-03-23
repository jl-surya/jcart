package dto;

/**
 * APIResponse provides standardized response structure for all API endpoints.
 * 
 * Includes:
 * - Success flag indicating operation result
 * - Message for user feedback
 * - Data payload for response content
 */
public class APIResponse {
    private boolean success;
    private String message;
    private Object data;
    
    /**
     * Constructs an API response.
     *
     * @param success whether the operation was successful
     * @param message response message
     * @param data response data payload
     */
    public APIResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}