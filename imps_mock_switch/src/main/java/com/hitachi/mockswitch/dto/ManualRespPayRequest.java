package com.hitachi.mockswitch.dto;

/**
 * DTO for manual RespPay trigger from Postman/API.
 * 
 * This allows testing the flow: Switch (ISO) → IMPS Backend → NPCI (XML)
 */
public class ManualRespPayRequest {
    
    private String rrn;           // DE37 - Retrieval Reference Number
    private String amount;        // DE4 - Transaction Amount (e.g., "1000.00")
    private String payerAccount;  // DE102 - Payer Account Number
    private String payeeAccount;  // DE103 - Payee Account Number
    private String txnId;         // DE120 - Transaction ID
    private String responseCode;  // DE39 - Response Code (default "00" for success)
    private String approvalNumber; // DE38 - Approval Number (auto-generated if empty)
    private String processingCode; // DE3 - Processing Code (default "400000")
    private String currency;      // DE49 - Currency (default "356" for INR)
    
    // Constructors
    public ManualRespPayRequest() {
    }
    
    // Getters and Setters
    public String getRrn() {
        return rrn;
    }
    
    public void setRrn(String rrn) {
        this.rrn = rrn;
    }
    
    public String getAmount() {
        return amount;
    }
    
    public void setAmount(String amount) {
        this.amount = amount;
    }
    
    public String getPayerAccount() {
        return payerAccount;
    }
    
    public void setPayerAccount(String payerAccount) {
        this.payerAccount = payerAccount;
    }
    
    public String getPayeeAccount() {
        return payeeAccount;
    }
    
    public void setPayeeAccount(String payeeAccount) {
        this.payeeAccount = payeeAccount;
    }
    
    public String getTxnId() {
        return txnId;
    }
    
    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }
    
    public String getResponseCode() {
        return responseCode;
    }
    
    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }
    
    public String getApprovalNumber() {
        return approvalNumber;
    }
    
    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }
    
    public String getProcessingCode() {
        return processingCode;
    }
    
    public void setProcessingCode(String processingCode) {
        this.processingCode = processingCode;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    @Override
    public String toString() {
        return "ManualRespPayRequest{" +
                "rrn='" + rrn + '\'' +
                ", amount='" + amount + '\'' +
                ", payerAccount='" + payerAccount + '\'' +
                ", payeeAccount='" + payeeAccount + '\'' +
                ", txnId='" + txnId + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", approvalNumber='" + approvalNumber + '\'' +
                '}';
    }
}
