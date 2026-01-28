package com.hitachi.imps.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hitachi.imps.entity.ResponseXpath;

public interface ResponseXpathRepository
        extends JpaRepository<ResponseXpath, Integer> {

    List<ResponseXpath> findByStatus(String status);
}
