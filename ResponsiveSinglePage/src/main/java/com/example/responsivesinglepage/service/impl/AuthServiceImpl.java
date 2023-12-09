package com.example.responsivesinglepage.service.impl;

import com.example.responsivesinglepage.dto.request.LoginRequestDTO;
import com.example.responsivesinglepage.dto.request.UserRegisterDTO;
import com.example.responsivesinglepage.dto.response.UserLoginResponse;
import com.example.responsivesinglepage.dto.response.UserResponse;
import com.example.responsivesinglepage.entity.Roles;
import com.example.responsivesinglepage.entity.Users;
import com.example.responsivesinglepage.handler.*;
import com.example.responsivesinglepage.repository.UserRepository;
import com.example.responsivesinglepage.security.JwtService;
import com.example.responsivesinglepage.service.interfaces.AuthService;
import com.example.responsivesinglepage.utils.UserLogInUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public UserResponse registerUser(UserRegisterDTO userRegisterDTO) {
      Users findUser =  userRepository.findByName(userRegisterDTO.getName()).orElse(null);
      if(findUser==null) {
          Users users = mappUser(userRegisterDTO);
          userRepository.insert(users);
          return mappUserToResponse(users);
      }else{
          throw new InvalidCredentialsException("User Already Exist");
      }
    }
    private Users mappUser(UserRegisterDTO userRegisterDTO){
        return Users.builder()
                .agree(userRegisterDTO.getAgree())
                .sectors(userRegisterDTO.getSectors())
                .role(Roles.USER)
                .password(passwordEncoder.encode(userRegisterDTO.getPassword()))
                .name(userRegisterDTO.getName())
                .build();

    }
    private UserResponse mappUserToResponse(Users users){
        return UserResponse.builder()
                .id(users.getId())
                .agree(users.getAgree())
                .sectors(users.getSectors())
                .role(Roles.USER)
                .name(users.getName())
                .build();

    }
    private UserLoginResponse mapptoLogin(String jwt,Users users){
        return UserLoginResponse.builder()
                .id(users.getId())
                .agree(users.getAgree())
                .sectors(users.getSectors())
                .role(Roles.USER)
                .jwt(jwt)
                .name(users.getName())
                .build();

    }

    @Override
    public UserLoginResponse login(LoginRequestDTO loginRequestDTO) {
        Users users = userRepository.findByName(loginRequestDTO.getName())
                .orElseThrow(()->new UserNotFoundException("User not found"));
        if(passwordEncoder.matches(loginRequestDTO.getPassword(),users.getPassword())){
            Authentication authentication = new UsernamePasswordAuthenticationToken(users.getName(),users.getPassword());
            String jwtToken = jwtService.generateToken(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return mapptoLogin(jwtToken,users);
        }
        throw  new InvalidCredentialsException("INVALID_CREDENTIALS");
    }



    @Override
    public UserResponse updateUser(UserRegisterDTO userRegisterDTO) {
        String loginUser = UserLogInUtils.getUser();
        log.info("user {} " ,loginUser);

        Users users = userRepository.findByName(loginUser)
                .orElseThrow(()-> new UserNotFoundException("User not found"));
        users.setAgree(userRegisterDTO.getAgree());
        users.setName(userRegisterDTO.getName());
        users.setPassword(passwordEncoder.encode(userRegisterDTO.getPassword()));
        users.setSectors(userRegisterDTO.getSectors());
        userRepository.save(users);
        return mappUserToResponse(users);
    }

    @Override
    public String deleteUser(String userId) {
        return null;
    }
}
