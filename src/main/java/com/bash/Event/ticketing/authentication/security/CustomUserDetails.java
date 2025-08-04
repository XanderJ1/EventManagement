package com.bash.Event.ticketing.authentication.security;

import com.bash.Event.ticketing.authentication.domain.User;
import com.bash.Event.ticketing.authentication.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
@Data
public class CustomUserDetails implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String username;
    private final String phoneNumber;
    private final String password;
    private final boolean isEnabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public static CustomUserDetails build(User user){
        return new CustomUserDetails(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPassword(),
                user.isEnabled(),
                mapRolesToAuthorities(user.getRole()) // Map roles to authorities
        );
    }

    /**
     * Maps the user's role to Spring Security's GrantedAuthority format.
     * @param role The role of the user.
     * @return A collection of GrantedAuthority representing the user's role.
     */
    private static Collection<? extends GrantedAuthority> mapRolesToAuthorities(UserRole role){
        return Collections.singletonList(new SimpleGrantedAuthority(role.name()));
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomUserDetails)) return false;
        CustomUserDetails user = (CustomUserDetails) o;
        return id.equals(user.id);
    }
    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
