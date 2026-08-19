package br.com.sitpa.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SITPa 2.0 - API")
                        .version("2.0.0")
                        .description("""
                                Sistema Inteligente Baseado em Regras Adaptaveis para Triagem de Pacientes.

                                A unidade de saude cadastra o proprio protocolo (grupos, fluxogramas, sintomas e
                                niveis de risco) e o motor Drools/DMN infere a classificacao a partir dos sintomas
                                marcados, devolvendo sempre a gravidade mais alta entre as regras que dispararam.

                                Autenticacao: obtenha o token em POST /api/v1/auth/login e informe-o
                                em "Authorize" como Bearer token.

                                Aviso: o protocolo de demonstracao tem conteudo clinico ilustrativo, para fins
                                academicos. Nao utilize em atendimento real sem validacao da equipe tecnica.
                                """)
                        .contact(new Contact().name("Equipe SITPa"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtido em /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }
}
