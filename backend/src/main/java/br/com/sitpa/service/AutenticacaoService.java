package br.com.sitpa.service;

import br.com.sitpa.config.PropriedadesSitpa;
import br.com.sitpa.domain.Usuario;
import br.com.sitpa.dto.LoginRequest;
import br.com.sitpa.dto.LoginResponse;
import br.com.sitpa.dto.UsuarioResponse;
import br.com.sitpa.exception.RecursoNaoEncontradoException;
import br.com.sitpa.repository.UsuarioRepository;
import br.com.sitpa.security.UsuarioAutenticado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Autenticacao por login e senha, com emissao de JWT. */
@Service
public class AutenticacaoService {

    private static final Logger log = LoggerFactory.getLogger(AutenticacaoService.class);

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final UsuarioRepository usuarioRepository;
    private final PropriedadesSitpa propriedades;

    public AutenticacaoService(AuthenticationManager authenticationManager,
                               JwtEncoder jwtEncoder,
                               UsuarioRepository usuarioRepository,
                               PropriedadesSitpa propriedades) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.usuarioRepository = usuarioRepository;
        this.propriedades = propriedades;
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.login(), request.senha()));
        } catch (AuthenticationException e) {
            // Nao distinguimos "usuario inexistente" de "senha errada" na resposta, para nao
            // permitir descobrir logins validos por tentativa e erro.
            log.warn("Tentativa de login malsucedida para \"{}\": {}", request.login(), e.getMessage());
            throw new BadCredentialsException("Usuario ou senha invalidos.");
        }

        Usuario usuario = ((UsuarioAutenticado) autenticacao.getPrincipal()).getUsuario();
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(Duration.ofMinutes(propriedades.validadeMinutos()));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sitpa")
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .subject(usuario.getLogin())
                .claim("nome", usuario.getNome())
                .claim("perfis", List.of(usuario.getPerfil().authority()))
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        log.info("Login efetuado: usuario={} perfil={}", usuario.getLogin(), usuario.getPerfil());
        return new LoginResponse(token, expiraEm, Mapeadores.paraResponse(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse perfilDe(String login) {
        return usuarioRepository.findByLoginIgnoreCase(login)
                .map(Mapeadores::paraResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", login));
    }
}
