package br.com.sitpa.security;

import br.com.sitpa.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Adaptador entre {@link Usuario} e o contrato do Spring Security. */
public class UsuarioAutenticado implements UserDetails {

    private final Usuario usuario;

    public UsuarioAutenticado(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(usuario.getPerfil().authority()));
    }

    @Override
    public String getPassword() {
        return usuario.getSenhaHash();
    }

    @Override
    public String getUsername() {
        return usuario.getLogin();
    }

    @Override
    public boolean isEnabled() {
        return usuario.isAtivo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return usuario.isAtivo();
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.isAtivo();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
