package com.mintfintech.savingsms.domain.models;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by jnwanya on
 * Thu, 02 Apr, 2020
 */
@Data
@AllArgsConstructor
public class EventModel<T> {
    private T data;
}
