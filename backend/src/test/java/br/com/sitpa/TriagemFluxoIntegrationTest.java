package br.com.sitpa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita o sistema de ponta a ponta sobre o protocolo de demonstracao: login, leitura do
 * cadastro, inferencia, confirmacao, historico e autorizacao por perfil.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"h2", "test"})
class TriagemFluxoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Mapper proprio do teste. O contexto publica um mapper Jackson 3 e outro Jackson 2; aqui
     * so precisamos ler o JSON da resposta, e um mapper local evita depender de qual deles vence.
     */
    private final ObjectMapper json = new ObjectMapper();

    private String tokenAdmin;
    private String tokenTriagem;

    @BeforeEach
    void autenticar() throws Exception {
        tokenAdmin = login("admin", "admin123");
        tokenTriagem = login("triagem", "triagem123");
    }

    // --- Autenticacao ---

    @Test
    @DisplayName("login invalido nao revela se o usuario existe")
    void loginInvalidoRetorna401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"admin","senha":"senha-errada"}"""))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"nao-existe","senha":"qualquer"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("login funciona de qualquer porta local, nao so da 5173")
    void loginAceitaQualquerPortaLocalNoOrigin() throws Exception {
        // O servidor de desenvolvimento do Vite troca de porta sozinho quando a 5173 esta
        // ocupada, e repassa ao backend o Origin do navegador. Com uma lista fixa de origens,
        // o filtro de CORS respondia 403 e a tela de login so mostrava "status code 403".
        for (String origem : new String[]{"http://localhost:5173", "http://localhost:5174",
                "http://localhost:4173", "http://localhost", "http://127.0.0.1:5173"}) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("Origin", origem)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"admin","senha":"admin123"}"""))
                    .andExpect(status().is2xxSuccessful());
        }
    }

    @Test
    @DisplayName("endpoint protegido sem token responde 401")
    void semTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/grupos")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("perfil de triagem le o protocolo mas nao altera o cadastro")
    void perfilTriagemNaoAlteraProtocolo() throws Exception {
        mockMvc.perform(get("/api/v1/grupos").header("Authorization", "Bearer " + tokenTriagem))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/grupos")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"TESTE","nome":"Teste"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/regras").header("Authorization", "Bearer " + tokenTriagem))
                .andExpect(status().isForbidden());
    }

    // --- Fluxo de classificacao ---

    @Test
    @DisplayName("discriminador de emergencia leva a VERMELHO")
    void convulsaoEmCursoResultaVermelho() throws Exception {
        JsonNode resultado = avaliar("CONVULSOES", List.of("CONVULSAO_EM_CURSO"), null, false);

        assertThat(resultado.at("/classificacao/codigo").asText()).isEqualTo("VERMELHO");
        assertThat(resultado.at("/classificacao/tempoMaximoEsperaMinutos").asInt()).isZero();
        assertThat(resultado.at("/classificacao/localAtendimento").asText()).isEqualTo("Sala de emergencia");
    }

    @Test
    @DisplayName("entre varios sintomas prevalece sempre a classificacao mais grave")
    void prevaleceAClassificacaoMaisGrave() throws Exception {
        JsonNode resultado = avaliar("CONVULSOES",
                List.of("PROBLEMA_SEM_MELHORA", "CEFALEIA_INTENSA", "CHOQUE", "PROBLEMA_RECENTE"), 40, false);

        assertThat(resultado.at("/classificacao/codigo").asText()).isEqualTo("VERMELHO");

        // As propostas descartadas continuam visiveis: o profissional precisa enxergar o raciocinio.
        List<String> propostas = new ArrayList<>();
        resultado.get("propostas").forEach(p -> propostas.add(p.get("classificacaoCodigo").asText()));
        assertThat(propostas).contains("VERMELHO", "AMARELO", "VERDE", "AZUL");
        assertThat(resultado.get("propostas")).anyMatch(p -> p.get("vencedora").asBoolean());
    }

    @Test
    @DisplayName("queixa cronica sem gravidade resulta em AZUL")
    void problemaSemMelhoraResultaAzul() throws Exception {
        JsonNode resultado = avaliar("CONVULSOES", List.of("PROBLEMA_SEM_MELHORA"), 30, false);
        assertThat(resultado.at("/classificacao/codigo").asText()).isEqualTo("AZUL");
    }

    @Test
    @DisplayName("regra DRL de combinacao supera a gravidade cadastrada dos sintomas isolados")
    void criseFebrilEmLactenteEscalona() throws Exception {
        // Isolados, ambos os sintomas sao no maximo LARANJA/AMARELO; em menor de 2 anos a regra
        // especializada mantem LARANJA e assume a decisao, com justificativa propria.
        JsonNode resultado = avaliar("CONVULSOES", List.of("CONVULSAO_RECENTE", "FEBRE_ALTA"), 1, false);

        assertThat(resultado.at("/classificacao/codigo").asText()).isEqualTo("LARANJA");
        assertThat(resultado.at("/origemDecisao").asText()).isEqualTo("DRL");
        assertThat(resultado.at("/regraAplicada").asText()).isEqualTo("Convulsoes - crise febril em lactente");
    }

    @Test
    @DisplayName("gestante com convulsao recente sobe para VERMELHO por suspeita de eclampsia")
    void gestanteComConvulsaoSobeParaVermelho() throws Exception {
        JsonNode semGestacao = avaliar("CONVULSOES", List.of("CONVULSAO_RECENTE"), 28, false);
        assertThat(semGestacao.at("/classificacao/codigo").asText()).isEqualTo("LARANJA");

        JsonNode comGestacao = avaliar("CONVULSOES", List.of("CONVULSAO_RECENTE"), 28, true);
        assertThat(comGestacao.at("/classificacao/codigo").asText()).isEqualTo("VERMELHO");
        assertThat(comGestacao.at("/regraAplicada").asText()).isEqualTo("Convulsoes - suspeita de eclampsia");
    }

    @Test
    @DisplayName("gravidade especifica do fluxograma sobrescreve a padrao do sintoma")
    void classificacaoEspecificaDoFluxogramaPrevalece() throws Exception {
        // "Dor moderada" e AMARELO no cadastro, mas a associacao com Dor toracica a eleva a LARANJA.
        JsonNode emCefaleia = avaliar("CEFALEIA", List.of("DOR_MODERADA"), 45, false);
        assertThat(emCefaleia.at("/classificacao/codigo").asText()).isEqualTo("AMARELO");

        JsonNode emDorToracica = avaliar("DOR_TORACICA", List.of("DOR_MODERADA"), 45, false);
        assertThat(emDorToracica.at("/classificacao/codigo").asText()).isEqualTo("LARANJA");
    }

    @Test
    @DisplayName("triagem sem sintomas nao fica sem resposta e sinaliza que nao houve inferencia")
    void triagemSemSintomasCaiNoMenorRisco() throws Exception {
        JsonNode resultado = avaliar("CEFALEIA", List.of(), 30, false);

        assertThat(resultado.at("/classificacao/codigo").asText()).isEqualTo("AZUL");
        assertThat(resultado.at("/origemDecisao").asText()).isEqualTo("NENHUMA");
        assertThat(resultado.at("/justificativa").asText()).contains("revise antes de confirmar");
    }

    @Test
    @DisplayName("sintoma que nao pertence ao fluxograma e recusado")
    void sintomaForaDoFluxogramaRetorna400() throws Exception {
        Long fluxograma = idPorCodigo("/api/v1/fluxogramas", "CEFALEIA");
        Long sintomaDeOutroFluxograma = idPorCodigo("/api/v1/sintomas", "CHOQUE");
        Long grupo = idPorCodigo("/api/v1/grupos", "ADULTOS");

        mockMvc.perform(post("/api/v1/triagens/avaliar")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "grupoId", grupo,
                                "fluxogramaId", fluxograma,
                                "sintomasIds", List.of(sintomaDeOutroFluxograma)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("fluxograma de outro grupo e recusado")
    void fluxogramaDeOutroGrupoRetorna400() throws Exception {
        Long grupoCriancas = idPorCodigo("/api/v1/grupos", "CRIANCAS");
        Long fluxogramaDeAdultos = idPorCodigo("/api/v1/fluxogramas", "CONVULSOES");

        mockMvc.perform(post("/api/v1/triagens/avaliar")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "grupoId", grupoCriancas,
                                "fluxogramaId", fluxogramaDeAdultos,
                                "sintomasIds", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("corpo com tipo errado responde 400, e nao 500")
    void corpoComTipoErradoRetorna400() throws Exception {
        // Um cliente que envia "grupoId": [1] cometeu um erro de chamada, nao provocou uma
        // falha do servidor; devolver 500 aqui esconderia o problema de quem precisa corrigi-lo.
        mockMvc.perform(post("/api/v1/triagens/avaliar")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"grupoId":[1],"fluxogramaId":1,"sintomasIds":[]}"""))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/triagens/avaliar")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("isso nao e json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("id de rota nao numerico responde 400")
    void idDeRotaInvalidoRetorna400() throws Exception {
        mockMvc.perform(get("/api/v1/grupos/abc").header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isBadRequest());
    }

    // --- Confirmacao e historico ---

    @Test
    @DisplayName("confirmar a triagem grava o historico e ele aparece nos filtros")
    void confirmarGravaHistorico() throws Exception {
        Long grupo = idPorCodigo("/api/v1/grupos", "ADULTOS");
        Long fluxograma = idPorCodigo("/api/v1/fluxogramas", "CONVULSOES");
        Long sintoma = idPorCodigo("/api/v1/sintomas", "CHOQUE");
        String identificador = "SENHA-" + System.nanoTime();

        MvcResult criacao = mockMvc.perform(post("/api/v1/triagens")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "grupoId", grupo,
                                "fluxogramaId", fluxograma,
                                "sintomasIds", List.of(sintoma),
                                "idadeAnos", 52,
                                "identificadorPaciente", identificador,
                                "observacoes", "Paciente trazido pelo SAMU."))))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registro = json.readTree(criacao.getResponse().getContentAsString());
        assertThat(registro.get("classificacaoCodigo").asText()).isEqualTo("VERMELHO");
        assertThat(registro.get("usuarioLogin").asText()).isEqualTo("triagem");
        assertThat(registro.get("sintomas").get(0).asText()).isEqualTo("Choque");
        // O snapshot guarda os dados do atendimento, nao uma referencia ao cadastro vivo.
        assertThat(registro.get("localAtendimento").asText()).isEqualTo("Sala de emergencia");

        MvcResult consulta = mockMvc.perform(get("/api/v1/historico")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .param("classificacaoCodigo", "VERMELHO")
                        .param("identificadorPaciente", identificador))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode pagina = json.readTree(consulta.getResponse().getContentAsString());
        assertThat(pagina.get("totalElements").asLong()).isEqualTo(1);
        assertThat(pagina.at("/content/0/identificadorPaciente").asText()).isEqualTo(identificador);

        mockMvc.perform(get("/api/v1/historico/estatisticas")
                        .header("Authorization", "Bearer " + tokenTriagem))
                .andExpect(status().isOk());
    }

    // --- Documentacao ---

    @Test
    @DisplayName("a especificacao OpenAPI e publicada")
    void openApiDisponivel() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode doc = json.readTree(resultado.getResponse().getContentAsString());
        assertThat(doc.at("/info/title").asText()).isEqualTo("SITPa 2.0 - API");
        assertThat(doc.get("paths").has("/api/v1/triagens/avaliar")).isTrue();
    }

    // --- Auxiliares ---

    private String login(String usuario, String senha) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", usuario, "senha", senha))))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString()).get("token").asText();
    }

    private Long idPorCodigo(String caminho, String codigo) throws Exception {
        MvcResult resultado = mockMvc.perform(get(caminho).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        for (JsonNode item : json.readTree(resultado.getResponse().getContentAsString())) {
            if (codigo.equals(item.get("codigo").asText())) {
                return item.get("id").asLong();
            }
        }
        throw new AssertionError("Nao encontrei " + codigo + " em " + caminho);
    }

    private JsonNode avaliar(String codigoFluxograma, List<String> codigosSintomas,
                             Integer idade, boolean gestante) throws Exception {
        Long fluxogramaId = idPorCodigo("/api/v1/fluxogramas", codigoFluxograma);
        MvcResult sintomasDoFluxograma = mockMvc.perform(
                        get("/api/v1/fluxogramas/" + fluxogramaId + "/sintomas")
                                .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();

        List<Long> ids = new ArrayList<>();
        for (JsonNode associado : json.readTree(sintomasDoFluxograma.getResponse().getContentAsString())) {
            if (codigosSintomas.contains(associado.get("codigo").asText())) {
                ids.add(associado.get("sintomaId").asLong());
            }
        }
        assertThat(ids).as("sintomas %s no fluxograma %s", codigosSintomas, codigoFluxograma)
                .hasSameSizeAs(codigosSintomas);

        MvcResult fluxograma = mockMvc.perform(get("/api/v1/fluxogramas/" + fluxogramaId)
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andReturn();
        long grupoId = json.readTree(fluxograma.getResponse().getContentAsString()).at("/grupo/id").asLong();

        Map<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("grupoId", grupoId);
        corpo.put("fluxogramaId", fluxogramaId);
        corpo.put("sintomasIds", ids);
        corpo.put("gestante", gestante);
        if (idade != null) {
            corpo.put("idadeAnos", idade);
        }

        MvcResult resultado = mockMvc.perform(post("/api/v1/triagens/avaliar")
                        .header("Authorization", "Bearer " + tokenTriagem)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(corpo)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(resultado.getResponse().getContentAsString());
    }
}
