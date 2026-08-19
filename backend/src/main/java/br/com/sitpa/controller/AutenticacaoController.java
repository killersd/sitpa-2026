package br.com.sitpa.controller;

import br.com.sitpa.dto.LoginRequest;
import br.com.sitpa.dto.LoginResponse;
import br.com.sitpa.dto.UsuarioResponse;
import br.com.sitpa.service.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticacao", description = "Login e perfil do usuario corrente")
public class AutenticacaoController {

    private final AutenticacaoService service;

    public AutenticacaoController(AutenticacaoService service) {
        this.service = service;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e devolve o token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.autenticar(request);
    }

    @GetMapping("/eu")
    @Operation(summary = "Dados do usuario autenticado")
    public UsuarioResponse eu(@AuthenticationPrincipal Jwt jwt) {
        return service.perfilDe(jwt.getSubject());
    }
}
