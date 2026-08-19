package br.com.sitpa.repository;

import br.com.sitpa.domain.ClassificacaoRealizada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ClassificacaoRealizadaRepository
        extends JpaRepository<ClassificacaoRealizada, Long>, JpaSpecificationExecutor<ClassificacaoRealizada> {

    /** Distribuicao de atendimentos por nivel de risco, para o painel de estatisticas. */
    @Query("""
            select cr.classificacaoCodigo as codigo,
                   cr.classificacaoNome as nome,
                   cr.classificacaoCor as cor,
                   cr.classificacaoPrioridade as prioridade,
                   count(cr) as total
            from ClassificacaoRealizada cr
            group by cr.classificacaoCodigo, cr.classificacaoNome, cr.classificacaoCor, cr.classificacaoPrioridade
            order by cr.classificacaoPrioridade asc
            """)
    List<ResumoPorClassificacao> resumirPorClassificacao();

    /** Projecao do agrupamento por nivel de risco. */
    interface ResumoPorClassificacao {

        String getCodigo();

        String getNome();

        String getCor();

        Integer getPrioridade();

        long getTotal();
    }
}
