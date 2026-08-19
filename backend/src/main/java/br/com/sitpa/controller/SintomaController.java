package br.com.sitpa.controller;

import br.com.sitpa.dto.SintomaRequest;
import br.com.sitpa.dto.SintomaResponse;
import br.com.sitpa.service.SintomaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sintomas")
@Tag(name = "Sintomas", description = "Discriminadores avaliados na triagem, cada um com sua gravidade padrao")
public class SintomaController {

    private final SintomaService service;

    public SintomaController(SintomaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os sintomas cadastrados")
    public List<SintomaResponse> listar(@RequestParam(defaultValue = "false") boolean apenasAtivos) {
        return service.listar(apenasAtivos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um sintoma pelo id")
    public SintomaResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cria um sintoma vinculado a uma classificacao (perfil administrador)")
    public ResponseEntity<SintomaResponse> criar(@Valid @RequestBody SintomaRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        SintomaResponse criado = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/sintomas/{id}").buildAndExpand(criado.id()).toUri())
                .body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um sintoma (perfil administrador)")
    public SintomaResponse atualizar(@PathVariable Long id, @Valid @RequestBody SintomaRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um sintoma nao associado a fluxogramas (perfil administrador)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
