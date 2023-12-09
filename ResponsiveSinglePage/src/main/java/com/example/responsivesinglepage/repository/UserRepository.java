package com.example.responsivesinglepage.repository;

import com.example.responsivesinglepage.entity.Users;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface UserRepository extends MongoRepository<Users, String> {
    Optional<Users> findByName(String name);
}
