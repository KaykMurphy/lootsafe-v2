package com.lootsafe.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Interceptor de canal STOMP responsável por autenticar o usuário
 * no frame STOMP CONNECT e propagar a autenticação para os frames subsequentes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final CustomJwtAuthenticationConverter authenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        // 1. Frame CONNECT: validar JWT e associar usuário
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (authorization == null || authorization.isBlank()) {
                authorization = accessor.getFirstNativeHeader("authorization");
            }

            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new BadCredentialsException("Authorization deve utilizar o esquema Bearer.");
            }

            String token = authorization.substring(7).trim();
            if (token.isBlank()) {
                throw new BadCredentialsException("Token JWT não informado.");
            }

            try {
                Jwt jwt = jwtDecoder.decode(token);
                AbstractAuthenticationToken authentication = authenticationConverter.convert(jwt);

                if (authentication == null) {
                    throw new BadCredentialsException("Não foi possível criar a autenticação.");
                }

                accessor.setUser(authentication);

                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("user", authentication);
                    accessor.getSessionAttributes().put("simpUser", authentication);
                }

                log.debug("Sessão STOMP autenticada para o usuário {}", authentication.getName());
                return message;

            } catch (JwtException e) {
                throw new BadCredentialsException("Token JWT inválido ou expirado.", e);
            }
        }

        // 2. Frames subsequentes (SUBSCRIBE, SEND, etc.): restaurar Principal caso não esteja presente
        if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            Principal user = (Principal) accessor.getSessionAttributes().get("user");
            if (user == null) {
                user = (Principal) accessor.getSessionAttributes().get("simpUser");
            }
            if (user != null) {
                accessor.setUser(user);
            }
        }

        return message;
    }
}
