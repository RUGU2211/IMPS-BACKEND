package com.hitachi.imps.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "txn_id")
    private String txnId;

    @Column(name = "txn_type")
    private String txnType;

    @Column(name = "req_in_date_time")
    private String reqInDateTime;

    @Column(name = "req_out_date_time")
    private String reqOutDateTime;

    @Column(name = "resp_in_date_time")
    private String respInDateTime;

    @Column(name = "resp_out_date_time")
    private LocalDateTime respOutDateTime;

    @Column(name = "req_xml", columnDefinition = "TEXT")
    private String reqXml;

    @Column(name = "resp_xml", columnDefinition = "TEXT")
    private String respXml;

    @Column(name = "switch_status")
    private String switchStatus;


	@Column(name = "de11")
    private String de11;

    @Column(name = "de37")
    private String de37;

    @Column(name = "de12")
    private String de12;

    @Column(name = "de13")
    private String de13;
    
    @Column(name = "approval_number")
    private String approvalNumber;

    /* ===== GETTERS & SETTERS ===== */

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }

    public String getTxnType() { return txnType; }
    public void setTxnType(String txnType) { this.txnType = txnType; }

    public String getReqInDateTime() { return reqInDateTime; }
    public void setReqInDateTime(String reqInDateTime) { this.reqInDateTime = reqInDateTime; }

    public String getReqOutDateTime() { return reqOutDateTime; }
    public void setReqOutDateTime(String reqOutDateTime) { this.reqOutDateTime = reqOutDateTime; }

    public String getRespInDateTime() { return respInDateTime; }
    public void setRespInDateTime(String respInDateTime) { this.respInDateTime = respInDateTime; }

    public LocalDateTime getRespOutDateTime() { return respOutDateTime; }
    public void setRespOutDateTime(LocalDateTime localDateTime) { this.respOutDateTime = localDateTime; }

    public String getReqXml() { return reqXml; }
    public void setReqXml(String reqXml) { this.reqXml = reqXml; }

    public String getRespXml() { return respXml; }
    public void setRespXml(String respXml) { this.respXml = respXml; }

    public String getSwitchStatus() { return switchStatus; }
    public void setSwitchStatus(String switchStatus) { this.switchStatus = switchStatus; }

    public String getDe12() { return de12; }
    public void setDe12(String de12) { this.de12 = de12; }

    public String getDe13() { return de13; }
    public void setDe13(String de13) { this.de13 = de13; }
    
    public String getDe11() {
		return de11;
	}
	public void setDe11(String de11) {
		this.de11 = de11;
	}
	public String getDe37() {
		return de37;
	}
	public void setDe37(String de37) {
		this.de37 = de37;
	}


    public String getApprovalNumber() {
        return approvalNumber;
    }
    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }
}
