package com.mintfintech.savingsms.infrastructure.web.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Created by jnwanya on
 * Fri, 14 Feb, 2020
 */

@Data
public class AuthenticatedUser implements UserDetails {
    private Long id;
    private String accountId;
    private String userId;
    private String password;
    private boolean active = true;
    private Collection<SimpleGrantedAuthority> authorities;
    private String accessPlatform;
    private String clientType;

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return active;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return active;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }
}
