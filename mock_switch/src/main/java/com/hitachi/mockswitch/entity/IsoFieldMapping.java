package com.hitachi.mockswitch.entity;

import jakarta.persistence.*;

/**
 * ISO Field Mapping Entity
 * Maps ISO 8583 fields for validation and processing
 */
@Entity
@Table(name = "iso_field_mapping")
public class IsoFieldMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "iso_field")
    private String isoField;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "field_type")
    private String fieldType;

    @Column(name = "field_length")
    private Integer fieldLength;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_mandatory")
    private Boolean isMandatory;

    // ===== GETTERS & SETTERS =====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getIsoField() { return isoField; }
    public void setIsoField(String isoField) { this.isoField = isoField; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public Integer getFieldLength() { return fieldLength; }
    public void setFieldLength(Integer fieldLength) { this.fieldLength = fieldLength; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsMandatory() { return isMandatory; }
    public void setIsMandatory(Boolean isMandatory) { this.isMandatory = isMandatory; }
}
