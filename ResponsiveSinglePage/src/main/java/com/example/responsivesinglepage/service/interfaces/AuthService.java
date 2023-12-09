package com.example.responsivesinglepage.service.interfaces;

import com.example.responsivesinglepage.dto.request.LoginRequestDTO;
import com.example.responsivesinglepage.dto.request.UserRegisterDTO;
import com.example.responsivesinglepage.dto.response.UserLoginResponse;
import com.example.responsivesinglepage.dto.response.UserResponse;

public interface AuthService {
    UserResponse registerUser(UserRegisterDTO userRegisterDTO);
    UserLoginResponse login(LoginRequestDTO userLoginResponse);
    UserResponse updateUser(UserRegisterDTO userRegisterDTO);
    String deleteUser(String userId);
}
