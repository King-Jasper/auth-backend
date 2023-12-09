package com.example.responsivesinglepage.dto.request;

import lombok.*;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class LoginRequestDTO {
    private String name;
    private String password;

}
