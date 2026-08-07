package com.lootsafe.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.UUID;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter;

    public CustomJwtAuthenticationConverter() {
        this.grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        this.grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        this.grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID uuid = UUID.fromString(jwt.getSubject());

        Collection<GrantedAuthority> authorities = grantedAuthoritiesConverter.convert(jwt);

        return new UsernamePasswordAuthenticationToken(uuid, null, authorities);
    }
}