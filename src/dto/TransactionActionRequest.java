package dto;

/**
 * TransactionActionRequest holds request details for refund actions.
 * 
 * Includes:
 * - Action type (APPROVE or REJECT)
 * - Reason for the action (required for rejection)
 */
public class TransactionActionRequest {
    private String action;
    private String reason;
    
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}