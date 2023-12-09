package com.example.responsivesinglepage.utils;

import org.springframework.security.core.context.SecurityContextHolder;

public class UserLogInUtils {
    public static String getUser(){
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
