package com.example.responsivesinglepage.controllers;

import com.example.responsivesinglepage.dto.request.LoginRequestDTO;
import com.example.responsivesinglepage.dto.request.UserRegisterDTO;
import com.example.responsivesinglepage.dto.response.UserLoginResponse;
import com.example.responsivesinglepage.dto.response.UserResponse;
import com.example.responsivesinglepage.service.interfaces.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("api/v1/auth")
@RestController
public class AuthControllers {
    @Autowired
    private AuthService authService;
    public AuthControllers(AuthService authService){
        this.authService=authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody UserRegisterDTO userRegisterDTO){
        return new ResponseEntity<>(authService.registerUser(userRegisterDTO), HttpStatus.CREATED);
    }
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody LoginRequestDTO loginRequestDTO){
        return new ResponseEntity<>(authService.login(loginRequestDTO), HttpStatus.OK);
    }
    @PutMapping("/update")
    public ResponseEntity<UserResponse> update(@RequestBody UserRegisterDTO userRegisterDTO){
        return new ResponseEntity<>(authService.updateUser(userRegisterDTO), HttpStatus.OK);
    }

}
