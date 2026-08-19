package br.com.sitpa.controller;

import br.com.sitpa.dto.GrupoRequest;
import br.com.sitpa.dto.GrupoResponse;
import br.com.sitpa.service.GrupoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grupos")
@Tag(name = "Grupos", description = "Categorias amplas do protocolo (Adultos, Criancas, Traumas...)")
public class GrupoController {

    private final GrupoService service;

    public GrupoController(GrupoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os grupos do protocolo")
    public List<GrupoResponse> listar(@RequestParam(defaultValue = "false") boolean apenasAtivos) {
        return service.listar(apenasAtivos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um grupo pelo id")
    public GrupoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cria um grupo (perfil administrador)")
    public ResponseEntity<GrupoResponse> criar(@Valid @RequestBody GrupoRequest request,
                                               UriComponentsBuilder uriBuilder) {
        GrupoResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/grupos/{id}").buildAndExpand(criado.id()).toUri())
                .body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um grupo (perfil administrador)")
    public GrupoResponse atualizar(@PathVariable Long id, @Valid @RequestBody GrupoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um grupo sem fluxogramas vinculados (perfil administrador)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
