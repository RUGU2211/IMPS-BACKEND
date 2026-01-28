package com.hitachi.imps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "response_xpath")
public class ResponseXpath {

    @Id
    private Integer id;

    @Column(name = "status")
    private String status;

    @Column(name = "type")
    private String type;

    @Column(name = "value")
    private String value;

    @Column(name = "xpath")
    private String xpath;

    // ===== getters & setters =====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getXpath() { return xpath; }
    public void setXpath(String xpath) { this.xpath = xpath; }
}
