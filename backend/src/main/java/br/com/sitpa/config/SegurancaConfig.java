package br.com.sitpa.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import br.com.sitpa.security.ServicoDetalhesUsuario;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguranca da API: stateless, com JWT assinado em HMAC-SHA256.
 *
 * <p>Os dois perfis do enunciado viram autorizacoes por metodo HTTP: quem faz triagem
 * (ROLE_TRIAGEM) le o protocolo e registra atendimentos; alterar o protocolo em si &mdash; grupos,
 * fluxogramas, sintomas, classificacoes e regras &mdash; exige ROLE_ADMIN. A checagem fica nas
 * regras de URL para que a autorizacao seja auditavel em um lugar so.</p>
 */
@Configuration
@EnableConfigurationProperties(PropriedadesSitpa.class)
@EnableMethodSecurity
public class SegurancaConfig {

    private static final String[] ROTAS_PUBLICAS = {
            "/api/v1/auth/login",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/health/**",
            "/actuator/info"
    };

    /** Cadastros do protocolo: leitura liberada aos autenticados, escrita so para o administrador. */
    private static final String[] ROTAS_PROTOCOLO = {
            "/api/v1/grupos/**",
            "/api/v1/fluxogramas/**",
            "/api/v1/sintomas/**",
            "/api/v1/classificacoes/**"
    };

    private final PropriedadesSitpa propriedades;

    public SegurancaConfig(PropriedadesSitpa propriedades) {
        this.propriedades = propriedades;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                // Sem sessao e sem cookie de autenticacao, nao ha superficie para CSRF.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, ROTAS_PROTOCOLO).authenticated()
                        .requestMatchers(ROTAS_PROTOCOLO).hasRole("ADMIN")
                        .requestMatchers("/api/v1/regras/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/triagens/**").authenticated()
                        .requestMatchers("/api/v1/historico/**").authenticated()
                        .requestMatchers("/api/v1/auth/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(jwtDecoder)
                        .jwtAuthenticationConverter(conversorDeAutoridades())));
        return http.build();
    }

    private JwtAuthenticationConverter conversorDeAutoridades() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        // A claim ja carrega "ROLE_ADMIN"/"ROLE_TRIAGEM", entao nao ha prefixo a acrescentar.
        autoridades.setAuthoritiesClaimName("perfis");
        autoridades.setAuthorityPrefix("");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(autoridades);
        return conversor;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // setAllowedOriginPatterns, e nao setAllowedOrigins, porque aceita curingas alem de
        // origens exatas. Isso permite liberar "http://localhost:*" em desenvolvimento: o Vite
        // troca de porta sozinho quando a 5173 esta ocupada, e uma lista fixa faria o login
        // falhar com um 403 do filtro de CORS, sem nenhuma pista do motivo na tela.
        // Em producao, defina SITPA_CORS_ORIGENS com as origens exatas.
        config.setAllowedOriginPatterns(propriedades.origens());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(ServicoDetalhesUsuario servicoUsuarios,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(servicoUsuarios);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(chave()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private SecretKeySpec chave() {
        String segredo = propriedades.jwtSegredo();
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "sitpa.jwt-segredo ausente ou curto demais: HMAC-SHA256 exige ao menos 32 bytes. "
                            + "Defina a variavel de ambiente SITPA_JWT_SEGREDO.");
        }
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
