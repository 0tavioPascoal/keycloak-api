package br.com.otavio.keycloak_api.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class Auth {

    @GetMapping("/publico")
    public Map<String, String> publico() {
        return Map.of(
                "mensagem", "Este endpoint é público."
        );
    }

    @GetMapping("/usuario")
    public Map<String, Object> usuario(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Map<String, Object> resposta = new LinkedHashMap<>();

        resposta.put("mensagem", "Token validado com sucesso.");
        resposta.put("id", jwt.getSubject());
        resposta.put(
                "usuario",
                jwt.getClaimAsString("preferred_username")
        );
        resposta.put("email", jwt.getClaimAsString("email"));
        resposta.put("emissor", Objects.requireNonNull(jwt.getIssuer()).toString());

        return resposta;
    }

    @GetMapping("/admin")
    public Map<String, Object> admin(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return Map.of(
                "mensagem", "Acesso administrativo autorizado.",
                "usuario", Objects.requireNonNull(jwt.getClaimAsString("preferred_username"))
        );
    }
}
