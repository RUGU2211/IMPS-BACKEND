package com.hitachi.imps.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "xml_path_req_pay")
public class XmlPathReqPay {

    @Id
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    private String status;

    @Column(name = "sub_field")
    private String subField;

    @Column(name = "type")
    private String type;

    @Column(name = "value")
    private String value;

    @Column(name = "x_path")
    private String xPath;

    // ===== getters & setters =====
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubField() { return subField; }
    public void setSubField(String subField) { this.subField = subField; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getxPath() { return xPath; }
    public void setxPath(String xPath) { this.xPath = xPath; }
}
