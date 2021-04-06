package com.mintfintech.savingsms.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resource_file")
public class ResourceFileEntity extends AbstractBaseEntity<Long> {

    private String fileId;

    private boolean remoteResource;

    private String url;

    @Column(nullable = false)
    private String fileName;

    private long fileSizeInKB;

    @Lob
    private byte[] resourceData;

}

