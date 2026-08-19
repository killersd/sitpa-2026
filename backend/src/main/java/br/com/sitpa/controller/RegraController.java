package br.com.sitpa.controller;

import br.com.sitpa.dto.RegraProtocoloRequest;
import br.com.sitpa.dto.RegraProtocoloResponse;
import br.com.sitpa.rules.EstadoBaseConhecimento;
import br.com.sitpa.service.RegrasService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * Administracao do motor de regras. Todas as rotas exigem perfil administrador.
 */
@RestController
@RequestMapping("/api/v1/regras")
@Tag(name = "Regras", description = "Regras DRL/DMN editaveis e estado da base de conhecimento")
public class RegraController {

    private final RegrasService service;

    public RegraController(RegrasService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as regras cadastradas no banco")
    public List<RegraProtocoloResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma regra pelo id")
    public RegraProtocoloResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra uma regra e recarrega a base",
            description = "A regra e compilada antes do commit. Se nao compilar, a resposta e 422 com os erros "
                    + "do compilador, nada e gravado e a base anterior segue no ar.")
    public ResponseEntity<RegraProtocoloResponse> criar(@Valid @RequestBody RegraProtocoloRequest request,
                                                        UriComponentsBuilder uriBuilder) {
        RegraProtocoloResponse criada = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/regras/{id}").buildAndExpand(criada.id()).toUri())
                .body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera uma regra e recarrega a base")
    public RegraProtocoloResponse atualizar(@PathVariable Long id, @Valid @RequestBody RegraProtocoloRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma regra e recarrega a base")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/base")
    @Operation(summary = "Mostra qual conhecimento esta no ar",
            description = "Versao da base, momento da carga, regras DRL compiladas e modelos DMN disponiveis.")
    public EstadoBaseConhecimento estado() {
        return service.estado();
    }

    @PostMapping("/base/recarregar")
    @Operation(summary = "Forca a recarga da base de conhecimento",
            description = "Recompila os artefatos do classpath somados as regras ativas do banco, sem reiniciar "
                    + "a aplicacao.")
    public EstadoBaseConhecimento recarregar() {
        return service.recarregar();
    }
}
