package com.mintfintech.savingsms.infrastructure.persistence.repository;

import com.mintfintech.savingsms.domain.entities.AppSequenceEntity;
import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Created by jnwanya on
 * Mon, 03 Feb, 2020
 */
public interface AppSequenceRepository extends JpaRepository<AppSequenceEntity, Long> {
    Optional<AppSequenceEntity> findFirstBySequenceType(SequenceType sequenceType);
}
