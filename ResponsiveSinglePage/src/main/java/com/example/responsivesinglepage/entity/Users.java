package com.example.responsivesinglepage.entity;



import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.annotation.Collation;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@Collation(value="user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {
    @Id
   private String id;
    private String name;
    private String sectors;
    private String agree;
    private String password;

    private Roles role;
}
