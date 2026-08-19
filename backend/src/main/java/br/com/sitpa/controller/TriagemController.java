package br.com.sitpa.controller;

import br.com.sitpa.dto.ClassificacaoRealizadaResponse;
import br.com.sitpa.dto.ResultadoTriagemResponse;
import br.com.sitpa.dto.TriagemRequest;
import br.com.sitpa.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/triagens")
@Tag(name = "Triagem", description = "Fluxo de classificacao de risco do paciente")
public class TriagemController {

    private final TriagemService service;

    public TriagemController(TriagemService service) {
        this.service = service;
    }

    @PostMapping("/avaliar")
    @Operation(summary = "Infere a classificacao sem gravar",
            description = "Alimenta a tela de revisao. Devolve a classificacao vencedora e tambem todas as "
                    + "propostas avaliadas, para que o profissional veja por que o motor chegou a esse nivel de risco.")
    public ResultadoTriagemResponse avaliar(@Valid @RequestBody TriagemRequest request) {
        return service.avaliar(request);
    }

    @PostMapping
    @Operation(summary = "Confirma a triagem e grava no historico",
            description = "A inferencia e refeita no servidor a partir dos sintomas enviados; o resultado exibido "
                    + "na revisao nao e aceito como entrada.")
    public ResponseEntity<ClassificacaoRealizadaResponse> registrar(@Valid @RequestBody TriagemRequest request,
                                                                    @AuthenticationPrincipal Jwt jwt,
                                                                    UriComponentsBuilder uriBuilder) {
        ClassificacaoRealizadaResponse registrada = service.registrar(request, jwt.getSubject());
        return ResponseEntity
                .created(uriBuilder.path("/api/v1/historico/{id}").buildAndExpand(registrada.id()).toUri())
                .body(registrada);
    }
}
