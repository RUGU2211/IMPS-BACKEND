package com.hitachi.imps.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.imps.entity.XmlPathReqPay;

public interface XmlPathReqPayRepository
        extends JpaRepository<XmlPathReqPay, Integer> {

    List<XmlPathReqPay> findByStatus(String status);
}
