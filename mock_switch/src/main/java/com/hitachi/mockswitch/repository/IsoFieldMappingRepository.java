package com.hitachi.mockswitch.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hitachi.mockswitch.entity.IsoFieldMapping;

@Repository
public interface IsoFieldMappingRepository extends JpaRepository<IsoFieldMapping, Integer> {
    
    Optional<IsoFieldMapping> findByIsoField(String isoField);
}
