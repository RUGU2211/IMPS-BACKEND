package com.hitachi.mockswitch.entity;

import jakarta.persistence.*;

/**
 * Account Type Mapping Entity
 * Maps account types to ISO codes for validation
 */
@Entity
@Table(name = "account_type_mapping")
public class AccountTypeMapping {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "account_type_mapping_id_seq"
    )
    @SequenceGenerator(
        name = "account_type_mapping_id_seq",
        sequenceName = "account_type_mapping_id_seq",
        allocationSize = 1
    )
    private Integer id;

    @Column(name = "acc_type")
    private String accType;

    @Column(name = "acc_type_iso_code")
    private String accTypeIsoCode;

    // ===== GETTERS & SETTERS =====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getAccType() { return accType; }
    public void setAccType(String accType) { this.accType = accType; }

    public String getAccTypeIsoCode() { return accTypeIsoCode; }
    public void setAccTypeIsoCode(String accTypeIsoCode) {
        this.accTypeIsoCode = accTypeIsoCode;
    }
}
