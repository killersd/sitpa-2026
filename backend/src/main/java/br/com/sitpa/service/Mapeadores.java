package br.com.sitpa.service;

import br.com.sitpa.domain.*;
import br.com.sitpa.dto.*;

import java.util.List;

/** Conversoes entidade -> DTO. Concentradas aqui para nao espalhar montagem de resposta pelos servicos. */
public final class Mapeadores {

    private Mapeadores() {
    }

    public static ClassificacaoResponse paraResponse(Classificacao c) {
        if (c == null) {
            return null;
        }
        return new ClassificacaoResponse(c.getId(), c.getCodigo(), c.getNome(), c.getCor(), c.getPrioridade(),
                c.getTempoMaximoEsperaMinutos(), c.getLocalAtendimento(), c.getTipoAtendimento(), c.isAtivo());
    }

    public static GrupoResponse paraResponse(Grupo g, long quantidadeFluxogramas) {
        return new GrupoResponse(g.getId(), g.getCodigo(), g.getNome(), g.getDescricao(), g.isAtivo(),
                quantidadeFluxogramas);
    }

    public static SintomaResponse paraResponse(Sintoma s) {
        return new SintomaResponse(s.getId(), s.getCodigo(), s.getNome(), s.getDescricao(),
                paraResponse(s.getClassificacao()), s.isAtivo());
    }

    public static FluxogramaResponse paraResponse(Fluxograma f, long quantidadeSintomas) {
        return new FluxogramaResponse(f.getId(), f.getCodigo(), f.getNome(), f.getDescricao(),
                referencia(f.getGrupo()), f.getModeloDmn(), f.isAtivo(), quantidadeSintomas);
    }

    public static SintomaAssociadoResponse paraResponse(FluxogramaSintoma fs) {
        Sintoma s = fs.getSintoma();
        return new SintomaAssociadoResponse(fs.getId(), s.getId(), s.getCodigo(), s.getNome(), s.getDescricao(),
                paraResponse(s.getClassificacao()),
                paraResponse(fs.getClassificacaoEspecifica()),
                paraResponse(fs.classificacaoEfetiva()),
                fs.getOrdem(), s.isAtivo());
    }

    public static ClassificacaoRealizadaResponse paraResponse(ClassificacaoRealizada cr) {
        return new ClassificacaoRealizadaResponse(cr.getId(), cr.getRealizadaEm(), cr.getGrupoNome(),
                cr.getFluxogramaNome(), cr.getClassificacaoCodigo(), cr.getClassificacaoNome(),
                cr.getClassificacaoCor(), cr.getClassificacaoPrioridade(), cr.getTempoMaximoEsperaMinutos(),
                cr.getLocalAtendimento(), cr.getTipoAtendimento(),
                cr.getSintomas() == null ? List.of() : List.copyOf(cr.getSintomas()),
                cr.getOrigemDecisao(), cr.getRegraAplicada(), cr.getJustificativa(),
                cr.getIdentificadorPaciente(), cr.getIdadeAnos(), cr.getObservacoes(), cr.getUsuarioLogin());
    }

    public static RegraProtocoloResponse paraResponse(RegraProtocolo r) {
        return new RegraProtocoloResponse(r.getId(), r.getNome(), r.getDescricao(), r.getTipo(), r.getConteudo(),
                r.isAtivo(), r.getVersao(), r.getAtualizadoEm());
    }

    public static UsuarioResponse paraResponse(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getLogin(), u.getNome(), u.getPerfil());
    }

    public static ReferenciaResponse referencia(Grupo g) {
        return g == null ? null : new ReferenciaResponse(g.getId(), g.getCodigo(), g.getNome());
    }

    public static ReferenciaResponse referencia(Fluxograma f) {
        return f == null ? null : new ReferenciaResponse(f.getId(), f.getCodigo(), f.getNome());
    }

    public static ReferenciaResponse referencia(Sintoma s) {
        return s == null ? null : new ReferenciaResponse(s.getId(), s.getCodigo(), s.getNome());
    }
}
