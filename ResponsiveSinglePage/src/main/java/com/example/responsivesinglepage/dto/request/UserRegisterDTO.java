package com.example.responsivesinglepage.dto.request;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserRegisterDTO {
    private String name;
    private String sectors;
    private String agree;
    private String password;
}
