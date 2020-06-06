package com.mintfintech.savingsms.usecase.data.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by jnwanya on
 * Thu, 20 Feb, 2020
 */
@NoArgsConstructor
@Data
public class PagedDataResponse<T> {
    private long totalRecords;
    private long totalPages;
    private List<T> records;

    public PagedDataResponse(long totalRecords, long totalPages, List<T> records) {
        this.totalRecords = totalRecords;
        this.totalPages = totalPages;
        this.records = records;
    }
}
