package com.bash.Event.ticketing.authentication.security;

import com.bash.Event.ticketing.authentication.domain.User;
import com.bash.Event.ticketing.authentication.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with identity: " + username)
                );

        if(!user.isEnabled()){
            throw new UsernameNotFoundException("User account is not activated. Please verify your email first");
        }
        return CustomUserDetails.build(user);
    }


}
