package br.com.sitpa.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiraEm, UsuarioResponse usuario) {
}
