package br.com.sitpa.config;

import br.com.sitpa.domain.*;
import br.com.sitpa.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Carga inicial: usuarios e um protocolo de demonstracao baseado no Protocolo de Manchester.
 *
 * <p>Fica em Java, e nao em uma migracao SQL, porque as senhas precisam passar pelo
 * {@link PasswordEncoder} e porque os identificadores gerados sao resolvidos pelo proprio ORM,
 * o que mantem a carga portavel entre PostgreSQL e H2. Roda apenas com as tabelas vazias, entao
 * reiniciar a aplicacao nunca sobrescreve o protocolo que a unidade de saude cadastrou.</p>
 *
 * <p><b>Aviso:</b> o conteudo clinico e ilustrativo, para demonstracao academica. O protocolo real
 * de cada unidade deve ser cadastrado e validado por sua equipe tecnica.</p>
 */
@Component
@Order(1)
public class SemeadorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SemeadorInicial.class);

    private final PropriedadesSitpa propriedades;
    private final PasswordEncoder passwordEncoder;
    private final ClassificacaoRepository classificacoes;
    private final GrupoRepository grupos;
    private final FluxogramaRepository fluxogramas;
    private final SintomaRepository sintomas;
    private final FluxogramaSintomaRepository associacoes;
    private final UsuarioRepository usuarios;

    @Value("${sitpa.senha-admin-inicial:admin123}")
    private String senhaAdmin;

    @Value("${sitpa.senha-triagem-inicial:triagem123}")
    private String senhaTriagem;

    public SemeadorInicial(PropriedadesSitpa propriedades,
                           PasswordEncoder passwordEncoder,
                           ClassificacaoRepository classificacoes,
                           GrupoRepository grupos,
                           FluxogramaRepository fluxogramas,
                           SintomaRepository sintomas,
                           FluxogramaSintomaRepository associacoes,
                           UsuarioRepository usuarios) {
        this.propriedades = propriedades;
        this.passwordEncoder = passwordEncoder;
        this.classificacoes = classificacoes;
        this.grupos = grupos;
        this.fluxogramas = fluxogramas;
        this.sintomas = sintomas;
        this.associacoes = associacoes;
        this.usuarios = usuarios;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!propriedades.deveSemear()) {
            return;
        }
        semearUsuarios();
        if (classificacoes.count() > 0 || grupos.count() > 0) {
            return;
        }
        semearProtocoloDemonstracao();
    }

    private void semearUsuarios() {
        if (usuarios.count() > 0) {
            return;
        }
        criarUsuario("admin", "Administrador do Protocolo", senhaAdmin, Perfil.ADMIN);
        criarUsuario("triagem", "Profissional de Triagem", senhaTriagem, Perfil.TRIAGEM);
        log.warn("Usuarios iniciais criados (admin e triagem) com as senhas padrao. "
                + "Troque-as antes de qualquer uso real.");
    }

    private void criarUsuario(String login, String nome, String senha, Perfil perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setNome(nome);
        u.setSenhaHash(passwordEncoder.encode(senha));
        u.setPerfil(perfil);
        u.setAtivo(true);
        usuarios.save(u);
    }

    private void semearProtocoloDemonstracao() {
        Map<String, Classificacao> risco = new LinkedHashMap<>();
        risco.put("VERMELHO", classificacao("VERMELHO", "Emergente", "#D32F2F", 1, 0,
                "Sala de emergencia", "Atendimento imediato"));
        risco.put("LARANJA", classificacao("LARANJA", "Muito urgente", "#EF6C00", 2, 10,
                "Sala de estabilizacao", "Avaliacao medica prioritaria"));
        risco.put("AMARELO", classificacao("AMARELO", "Urgente", "#F9A825", 3, 60,
                "Consultorio de urgencia", "Avaliacao medica"));
        risco.put("VERDE", classificacao("VERDE", "Pouco urgente", "#2E7D32", 4, 120,
                "Consultorio ambulatorial", "Atendimento eletivo"));
        risco.put("AZUL", classificacao("AZUL", "Nao urgente", "#1565C0", 5, 240,
                "Acolhimento e orientacao", "Encaminhamento ambulatorial"));

        Grupo adultos = grupo("ADULTOS", "Adultos", "Pacientes com 13 anos ou mais.");
        Grupo criancas = grupo("CRIANCAS", "Criancas", "Pacientes com menos de 13 anos.");
        Grupo traumas = grupo("TRAUMAS", "Traumas", "Queixas decorrentes de trauma ou violencia.");

        // Apenas Convulsoes usa a tabela DMN; os demais ficam so com DRL + camada base,
        // deixando visivel que as duas abordagens convivem no mesmo protocolo.
        Fluxograma convulsoes = fluxograma("CONVULSOES", "Convulsoes", adultos,
                "Crise convulsiva atual ou recente.", "TriagemConvulsoes");
        Fluxograma cefaleia = fluxograma("CEFALEIA", "Cefaleia", adultos, "Dor de cabeca.", null);
        Fluxograma dorToracica = fluxograma("DOR_TORACICA", "Dor toracica", adultos, "Dor no peito.", null);
        Fluxograma criancaIrritada = fluxograma("CRIANCA_IRRITADA", "Crianca irritada", criancas,
                "Crianca com choro persistente ou irritabilidade.", null);
        Fluxograma agressao = fluxograma("AGRESSAO", "Agressao", traumas,
                "Lesoes decorrentes de agressao fisica.", null);

        Map<String, Sintoma> s = new LinkedHashMap<>();
        s.put("VIA_AEREA_COMPROMETIDA", sintoma("VIA_AEREA_COMPROMETIDA", "Comprometimento da via aerea",
                risco.get("VERMELHO"), "Obstrucao ou incapacidade de manter a via aerea pervia."));
        s.put("RESPIRACAO_INADEQUADA", sintoma("RESPIRACAO_INADEQUADA", "Respiracao inadequada",
                risco.get("VERMELHO"), "Frequencia, profundidade ou saturacao insuficientes."));
        s.put("CHOQUE", sintoma("CHOQUE", "Choque", risco.get("VERMELHO"),
                "Sinais de ma perfusao: hipotensao, pele fria e umida, pulso filiforme."));
        s.put("CONVULSAO_EM_CURSO", sintoma("CONVULSAO_EM_CURSO", "Convulsionando no momento",
                risco.get("VERMELHO"), "Crise convulsiva ativa durante a triagem."));
        s.put("SANGRAMENTO_INCONTROLAVEL", sintoma("SANGRAMENTO_INCONTROLAVEL", "Hemorragia incontrolavel",
                risco.get("VERMELHO"), "Sangramento que nao cessa com compressao direta."));

        s.put("ALTERACAO_NIVEL_CONSCIENCIA", sintoma("ALTERACAO_NIVEL_CONSCIENCIA",
                "Alteracao do nivel de consciencia", risco.get("LARANJA"), "Sonolencia, confusao ou torpor."));
        s.put("CONVULSAO_RECENTE", sintoma("CONVULSAO_RECENTE", "Crise convulsiva nas ultimas 24 horas",
                risco.get("LARANJA"), "Episodio convulsivo recente, ja encerrado."));
        s.put("HIPOGLICEMIA", sintoma("HIPOGLICEMIA", "Hipoglicemia", risco.get("LARANJA"),
                "Glicemia capilar abaixo do valor de referencia."));
        s.put("DOR_INTENSA", sintoma("DOR_INTENSA", "Dor intensa", risco.get("LARANJA"),
                "Dor de forte intensidade referida pelo paciente."));
        s.put("DOR_PRECORDIAL_TIPICA", sintoma("DOR_PRECORDIAL_TIPICA", "Dor precordial tipica",
                risco.get("LARANJA"), "Dor em aperto, com irradiacao para braco ou mandibula."));
        s.put("SUDORESE_FRIA", sintoma("SUDORESE_FRIA", "Sudorese fria", risco.get("LARANJA"),
                "Sudorese profusa acompanhada de palidez."));

        s.put("HISTORIA_INCONSCIENCIA", sintoma("HISTORIA_INCONSCIENCIA", "Historia de inconsciencia",
                risco.get("AMARELO"), "Perda de consciencia relatada, ja recuperada."));
        s.put("CEFALEIA_INTENSA", sintoma("CEFALEIA_INTENSA", "Cefaleia intensa", risco.get("AMARELO"),
                "Dor de cabeca de forte intensidade."));
        s.put("VOMITO_PERSISTENTE", sintoma("VOMITO_PERSISTENTE", "Vomito persistente", risco.get("AMARELO"),
                "Vomitos repetidos que impedem hidratacao oral."));
        s.put("FEBRE_ALTA", sintoma("FEBRE_ALTA", "Febre alta", risco.get("AMARELO"),
                "Temperatura axilar igual ou superior a 38,5 graus."));
        s.put("DOR_MODERADA", sintoma("DOR_MODERADA", "Dor moderada", risco.get("AMARELO"),
                "Dor de intensidade media referida pelo paciente."));
        s.put("CHORO_INCONSOLAVEL", sintoma("CHORO_INCONSOLAVEL", "Choro inconsolavel", risco.get("AMARELO"),
                "Crianca que nao se acalma com as medidas habituais."));
        s.put("DEFORMIDADE_MEMBRO", sintoma("DEFORMIDADE_MEMBRO", "Deformidade de membro", risco.get("AMARELO"),
                "Angulacao anormal sugestiva de fratura."));

        s.put("DOR_LEVE", sintoma("DOR_LEVE", "Dor leve", risco.get("VERDE"), "Dor de baixa intensidade."));
        s.put("PROBLEMA_RECENTE", sintoma("PROBLEMA_RECENTE", "Problema de inicio recente", risco.get("VERDE"),
                "Queixa iniciada nos ultimos dias."));

        s.put("PROBLEMA_SEM_MELHORA", sintoma("PROBLEMA_SEM_MELHORA", "Problema cronico sem melhora",
                risco.get("AZUL"), "Queixa antiga, estavel, sem piora."));

        associar(convulsoes, s, "VIA_AEREA_COMPROMETIDA", "RESPIRACAO_INADEQUADA", "CHOQUE", "CONVULSAO_EM_CURSO",
                "ALTERACAO_NIVEL_CONSCIENCIA", "CONVULSAO_RECENTE", "HIPOGLICEMIA", "HISTORIA_INCONSCIENCIA",
                "CEFALEIA_INTENSA", "VOMITO_PERSISTENTE", "FEBRE_ALTA", "PROBLEMA_RECENTE", "PROBLEMA_SEM_MELHORA");

        associar(cefaleia, s, "ALTERACAO_NIVEL_CONSCIENCIA", "CEFALEIA_INTENSA", "VOMITO_PERSISTENTE",
                "DOR_MODERADA", "DOR_LEVE", "PROBLEMA_RECENTE", "PROBLEMA_SEM_MELHORA");

        associar(dorToracica, s, "RESPIRACAO_INADEQUADA", "CHOQUE", "DOR_PRECORDIAL_TIPICA", "SUDORESE_FRIA",
                "DOR_INTENSA", "PROBLEMA_RECENTE");
        // Demonstra a sobrescrita por fluxograma: "dor moderada" e Amarelo no geral,
        // mas em dor toracica sobe para Laranja.
        associarCom(dorToracica, s.get("DOR_MODERADA"), risco.get("LARANJA"), 90);

        associar(criancaIrritada, s, "ALTERACAO_NIVEL_CONSCIENCIA", "CHORO_INCONSOLAVEL", "FEBRE_ALTA",
                "VOMITO_PERSISTENTE", "PROBLEMA_RECENTE", "PROBLEMA_SEM_MELHORA");

        associar(agressao, s, "SANGRAMENTO_INCONTROLAVEL", "ALTERACAO_NIVEL_CONSCIENCIA", "DEFORMIDADE_MEMBRO",
                "DOR_INTENSA", "DOR_MODERADA", "DOR_LEVE", "PROBLEMA_RECENTE");

        log.info("Protocolo de demonstracao criado: {} classificacoes, {} grupos, {} fluxogramas, {} sintomas.",
                risco.size(), 3, 5, s.size());
    }

    // --- Auxiliares de criacao ---

    private Classificacao classificacao(String codigo, String nome, String cor, int prioridade,
                                        int tempoEspera, String local, String tipo) {
        Classificacao c = new Classificacao();
        c.setCodigo(codigo);
        c.setNome(nome);
        c.setCor(cor);
        c.setPrioridade(prioridade);
        c.setTempoMaximoEsperaMinutos(tempoEspera);
        c.setLocalAtendimento(local);
        c.setTipoAtendimento(tipo);
        c.setAtivo(true);
        return classificacoes.save(c);
    }

    private Grupo grupo(String codigo, String nome, String descricao) {
        Grupo g = new Grupo();
        g.setCodigo(codigo);
        g.setNome(nome);
        g.setDescricao(descricao);
        g.setAtivo(true);
        return grupos.save(g);
    }

    private Fluxograma fluxograma(String codigo, String nome, Grupo grupo, String descricao, String modeloDmn) {
        Fluxograma f = new Fluxograma();
        f.setCodigo(codigo);
        f.setNome(nome);
        f.setDescricao(descricao);
        f.setGrupo(grupo);
        f.setModeloDmn(modeloDmn);
        f.setAtivo(true);
        return fluxogramas.save(f);
    }

    private Sintoma sintoma(String codigo, String nome, Classificacao classificacao, String descricao) {
        Sintoma s = new Sintoma();
        s.setCodigo(codigo);
        s.setNome(nome);
        s.setDescricao(descricao);
        s.setClassificacao(classificacao);
        s.setAtivo(true);
        return sintomas.save(s);
    }

    private void associar(Fluxograma fluxograma, Map<String, Sintoma> disponiveis, String... codigos) {
        int ordem = 0;
        for (String codigo : codigos) {
            associarCom(fluxograma, disponiveis.get(codigo), null, ordem);
            ordem += 10;
        }
    }

    private void associarCom(Fluxograma fluxograma, Sintoma sintoma, Classificacao especifica, int ordem) {
        associacoes.save(new FluxogramaSintoma(fluxograma, sintoma, especifica, ordem));
    }
}
