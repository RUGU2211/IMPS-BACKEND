package com.hitachi.mockswitch.entity;

import jakarta.persistence.*;

/**
 * Institution Master Entity
 * Used for bank/institution validation in mock switch
 */
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

    // ===== GETTERS & SETTERS =====
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
}
