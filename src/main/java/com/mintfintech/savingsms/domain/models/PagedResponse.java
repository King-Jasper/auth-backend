package com.mintfintech.savingsms.domain.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by jnwanya on
 * Fri, 03 Apr, 2020
 */
@Data
public class PagedResponse<T> {
    private long totalRecords;
    private int totalPages;
    private List<T> records;

    public PagedResponse(long totalRecords, int totalPages, List<T> records) {
        this.totalPages = totalPages;
        this.totalRecords = totalRecords;
        this.records = records;
    }
}
