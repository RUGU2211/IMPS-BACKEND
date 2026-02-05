package com.hitachi.imps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "institution_master")
public class InstitutionMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bin_code")
    private String binCode;

    @Column(name = "n_bin_code")
    private String nBinCode;

    @Column(name = "request_org_id")
    private String requestOrgId;

    @Column(name = "switch_ip")
    private String switchIp;

    @Column(name = "switch_port")
    private String switchPort;

    @Column(name = "name")
    private String name;

    @Column(name = "active", nullable = false)
    private Boolean active;

    /* 7.4.3 AccPvd optional (0..n) */
    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "spoc_name", length = 100)
    private String spocName;

    @Column(name = "spoc_email", length = 100)
    private String spocEmail;

    @Column(name = "spoc_phone", length = 20)
    private String spocPhone;

    @Column(name = "last_modified_ts")
    private java.time.OffsetDateTime lastModifiedTs;

    /* ===== GETTERS & SETTERS ===== */
    public Boolean getActive() {
		return active;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBinCode() { return binCode; }
    public void setBinCode(String binCode) { this.binCode = binCode; }

    public String getnBinCode() { return nBinCode; }
    public void setnBinCode(String nBinCode) { this.nBinCode = nBinCode; }

    public String getRequestOrgId() { return requestOrgId; }
    public void setRequestOrgId(String requestOrgId) { this.requestOrgId = requestOrgId; }

    public String getSwitchIp() { return switchIp; }
    public void setSwitchIp(String switchIp) { this.switchIp = switchIp; }

    public String getSwitchPort() { return switchPort; }
    public void setSwitchPort(String switchPort) { this.switchPort = switchPort; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSpocName() { return spocName; }
    public void setSpocName(String spocName) { this.spocName = spocName; }

    public String getSpocEmail() { return spocEmail; }
    public void setSpocEmail(String spocEmail) { this.spocEmail = spocEmail; }

    public String getSpocPhone() { return spocPhone; }
    public void setSpocPhone(String spocPhone) { this.spocPhone = spocPhone; }

    public java.time.OffsetDateTime getLastModifiedTs() { return lastModifiedTs; }
    public void setLastModifiedTs(java.time.OffsetDateTime lastModifiedTs) { this.lastModifiedTs = lastModifiedTs; }
}
