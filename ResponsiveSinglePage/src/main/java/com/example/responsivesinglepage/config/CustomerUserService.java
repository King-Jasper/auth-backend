package com.example.responsivesinglepage.config;


import com.example.responsivesinglepage.entity.Users;
import com.example.responsivesinglepage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomerUserService implements UserDetailsService {

    private final UserRepository customerRepository;


    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        Users customer = customerRepository.findByName(name)
                .orElseThrow(()-> new UsernameNotFoundException("USER NOT FOUND"));
        return new User(customer.getName(),customer.getPassword(),getAuthorities(customer));
    }

    private Collection<? extends GrantedAuthority> getAuthorities(Users user){
        return Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name()));
    }
}