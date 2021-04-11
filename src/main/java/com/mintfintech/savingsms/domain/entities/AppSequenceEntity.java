package com.mintfintech.savingsms.domain.entities;

import com.mintfintech.savingsms.domain.entities.enums.SequenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

/**
 * Created by jnwanya on
 * Tue, 18 Feb, 2020
 */
@Entity
@AllArgsConstructor
@Table(name = "app_sequence")
public class AppSequenceEntity {
    public AppSequenceEntity(SequenceType sequenceType){
        this.sequenceType = sequenceType;
        value = 0;
    }

    public AppSequenceEntity() {
        value = 0;
    }

    @Id
    @Enumerated(EnumType.STRING)
    private SequenceType sequenceType;
    private long value;

    @Version
    private long version; // for optimistic locking

    public long getVersion() {
        return version;
    }
    /**
     * Gets the current sequence value.
     *
     * @return The current sequence value.
     */
    public Long getValue() {
        return ++value;
    }
}
