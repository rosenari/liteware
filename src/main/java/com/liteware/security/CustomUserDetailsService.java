package com.liteware.security;

import com.liteware.model.entity.User;
import com.liteware.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLoginIdWithRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        
        return CustomUserPrincipal.builder()
                .userId(user.getUserId())
                .username(user.getLoginId())
                .password(user.getPassword())
                .email(user.getEmail())
                .name(user.getName())
                .authorities(user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getRoleCode()))
                        .collect(Collectors.toList()))
                .accountNonExpired(true)
                .accountNonLocked(!user.isAccountLocked())
                .credentialsNonExpired(true)
                .enabled(user.getStatus() == com.liteware.model.entity.UserStatus.ACTIVE)
                .build();
    }
}