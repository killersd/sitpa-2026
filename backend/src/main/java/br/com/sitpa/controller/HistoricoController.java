package br.com.sitpa.controller;

import br.com.sitpa.dto.ClassificacaoRealizadaResponse;
import br.com.sitpa.dto.EstatisticaClassificacaoResponse;
import br.com.sitpa.service.HistoricoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/historico")
@Tag(name = "Historico", description = "Classificacoes realizadas, com filtros e estatisticas")
public class HistoricoController {

    private static final int TAMANHO_MAXIMO_PAGINA = 200;

    private final HistoricoService service;

    public HistoricoController(HistoricoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consulta o historico com filtros combinaveis",
            description = "Todos os filtros sao opcionais. As datas usam formato ISO-8601 em UTC, "
                    + "por exemplo 2026-08-17T00:00:00Z.")
    public Page<ClassificacaoRealizadaResponse> consultar(
            @Parameter(description = "Inicio do intervalo, inclusivo")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant de,

            @Parameter(description = "Fim do intervalo, inclusivo")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant ate,

            @RequestParam(required = false) String classificacaoCodigo,
            @RequestParam(required = false) Long fluxogramaId,
            @RequestParam(required = false) Long grupoId,
            @RequestParam(required = false) String usuarioLogin,
            @RequestParam(required = false) String identificadorPaciente,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {

        var filtro = new HistoricoService.Filtro(de, ate, classificacaoCodigo, fluxogramaId, grupoId,
                usuarioLogin, identificadorPaciente);
        var paginacao = PageRequest.of(Math.max(pagina, 0),
                Math.clamp(tamanho, 1, TAMANHO_MAXIMO_PAGINA),
                Sort.by(Sort.Direction.DESC, "realizadaEm"));
        return service.consultar(filtro, paginacao);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um registro do historico")
    public ClassificacaoRealizadaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Distribuicao de atendimentos por nivel de risco")
    public List<EstatisticaClassificacaoResponse> estatisticas() {
        return service.estatisticas();
    }
}
