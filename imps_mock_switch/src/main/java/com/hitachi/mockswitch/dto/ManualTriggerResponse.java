package com.hitachi.mockswitch.dto;

/**
 * Response DTO for manual trigger operations.
 */
public class ManualTriggerResponse {
    
    private boolean success;
    private String message;
    private String isoHex;        // Hex representation of ISO sent
    private String backendResponse; // Response from IMPS Backend
    private String rrn;
    private String stan;
    private String approvalNumber;
    
    // Constructors
    public ManualTriggerResponse() {
    }
    
    public ManualTriggerResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Static factory methods
    public static ManualTriggerResponse success(String message) {
        return new ManualTriggerResponse(true, message);
    }
    
    public static ManualTriggerResponse error(String message) {
        return new ManualTriggerResponse(false, message);
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getIsoHex() {
        return isoHex;
    }
    
    public void setIsoHex(String isoHex) {
        this.isoHex = isoHex;
    }
    
    public String getBackendResponse() {
        return backendResponse;
    }
    
    public void setBackendResponse(String backendResponse) {
        this.backendResponse = backendResponse;
    }
    
    public String getRrn() {
        return rrn;
    }
    
    public void setRrn(String rrn) {
        this.rrn = rrn;
    }
    
    public String getStan() {
        return stan;
    }
    
    public void setStan(String stan) {
        this.stan = stan;
    }
    
    public String getApprovalNumber() {
        return approvalNumber;
    }
    
    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }
}
