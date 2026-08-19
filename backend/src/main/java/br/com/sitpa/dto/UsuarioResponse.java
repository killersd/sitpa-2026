package br.com.sitpa.dto;

import br.com.sitpa.domain.Perfil;

public record UsuarioResponse(Long id, String login, String nome, Perfil perfil) {
}
