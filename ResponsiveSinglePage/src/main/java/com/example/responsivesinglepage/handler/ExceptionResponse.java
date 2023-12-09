package com.example.responsivesinglepage.handler;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ExceptionResponse {
    private String message;
    private Integer statusCode;
    private String date;
}
