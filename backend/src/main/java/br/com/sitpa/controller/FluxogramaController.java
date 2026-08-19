package br.com.sitpa.controller;

import br.com.sitpa.dto.AssociacaoSintomaRequest;
import br.com.sitpa.dto.FluxogramaRequest;
import br.com.sitpa.dto.FluxogramaResponse;
import br.com.sitpa.dto.SintomaAssociadoResponse;
import br.com.sitpa.service.FluxogramaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fluxogramas")
@Tag(name = "Fluxogramas", description = "Queixas apresentadas e a associacao delas com sintomas")
public class FluxogramaController {

    private final FluxogramaService service;

    public FluxogramaController(FluxogramaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista fluxogramas, opcionalmente filtrando por grupo")
    public List<FluxogramaResponse> listar(@RequestParam(required = false) Long grupoId,
                                           @RequestParam(defaultValue = "false") boolean apenasAtivos) {
        return service.listar(grupoId, apenasAtivos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um fluxograma pelo id")
    public FluxogramaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cria um fluxograma vinculado a um grupo (perfil administrador)")
    public ResponseEntity<FluxogramaResponse> criar(@Valid @RequestBody FluxogramaRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        FluxogramaResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/fluxogramas/{id}").buildAndExpand(criado.id()).toUri())
                .body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um fluxograma (perfil administrador)")
    public FluxogramaResponse atualizar(@PathVariable Long id, @Valid @RequestBody FluxogramaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um fluxograma sem sintomas associados (perfil administrador)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // --- Associacao de sintomas ---

    @GetMapping("/{id}/sintomas")
    @Operation(summary = "Lista os sintomas associados, com a gravidade que vale neste fluxograma")
    public List<SintomaAssociadoResponse> listarSintomas(@PathVariable Long id) {
        return service.listarSintomas(id);
    }

    @PostMapping("/{id}/sintomas")
    @Operation(summary = "Associa um sintoma ao fluxograma (perfil administrador)")
    public ResponseEntity<SintomaAssociadoResponse> associar(@PathVariable Long id,
                                                             @Valid @RequestBody AssociacaoSintomaRequest request) {
        return ResponseEntity.ok(service.associarSintoma(id, request));
    }

    @PutMapping("/{id}/sintomas/{sintomaId}")
    @Operation(summary = "Altera a gravidade especifica ou a ordem de um sintoma associado (perfil administrador)")
    public SintomaAssociadoResponse atualizarAssociacao(@PathVariable Long id,
                                                        @PathVariable Long sintomaId,
                                                        @Valid @RequestBody AssociacaoSintomaRequest request) {
        return service.atualizarAssociacao(id, sintomaId, request);
    }

    @DeleteMapping("/{id}/sintomas/{sintomaId}")
    @Operation(summary = "Remove a associacao entre fluxograma e sintoma (perfil administrador)")
    public ResponseEntity<Void> desassociar(@PathVariable Long id, @PathVariable Long sintomaId) {
        service.desassociarSintoma(id, sintomaId);
        return ResponseEntity.noContent().build();
    }
}
