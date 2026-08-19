package br.com.sitpa.controller;

import br.com.sitpa.dto.ClassificacaoRequest;
import br.com.sitpa.dto.ClassificacaoResponse;
import br.com.sitpa.service.ClassificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/classificacoes")
@Tag(name = "Classificacoes", description = "Niveis de risco: prioridade, cor, tempo de espera e local de atendimento")
public class ClassificacaoController {

    private final ClassificacaoService service;

    public ClassificacaoController(ClassificacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os niveis de risco, do mais grave para o menos grave")
    public List<ClassificacaoResponse> listar(@RequestParam(defaultValue = "false") boolean apenasAtivas) {
        return service.listar(apenasAtivas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um nivel de risco pelo id")
    public ClassificacaoResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @Operation(summary = "Cria um nivel de risco (perfil administrador)")
    public ResponseEntity<ClassificacaoResponse> criar(@Valid @RequestBody ClassificacaoRequest request,
                                                       UriComponentsBuilder uriBuilder) {
        ClassificacaoResponse criada = service.criar(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/classificacoes/{id}").buildAndExpand(criada.id()).toUri())
                .body(criada);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um nivel de risco (perfil administrador)")
    public ClassificacaoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ClassificacaoRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Exclui um nivel de risco que nao esteja em uso (perfil administrador)")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
