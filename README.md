# SITPa 2.0

**Sistema Inteligente Baseado em Regras Adaptáveis para Triagem de Pacientes em Unidades de Saúde.**

Reconstrução do TCC de 2021 com stack atual. O sistema apoia o profissional de saúde na classificação
de risco: a unidade cadastra o **próprio protocolo** (grupos, fluxogramas, sintomas e níveis de risco),
e um motor de regras Drools/DMN infere a gravidade a partir dos sintomas marcados, devolvendo sempre a
classificação mais grave entre as regras que dispararam.

> **Aviso.** O protocolo de demonstração que acompanha o projeto tem conteúdo clínico ilustrativo, para
> fins acadêmicos. Não utilize em atendimento real sem validação da equipe técnica da unidade. O sistema
> apoia a decisão; a classificação final é sempre do profissional.

---

## Sumário

- [Stack](#stack)
- [Decisões de arquitetura](#decisões-de-arquitetura)
- [Modelo de dados](#modelo-de-dados)
- [Como o motor de regras funciona](#como-o-motor-de-regras-funciona)
- [Como rodar](#como-rodar)
- [Exemplos de uso da API](#exemplos-de-uso-da-api)
- [Testes](#testes)
- [Estrutura de pastas](#estrutura-de-pastas)

---

## Stack

| Camada | Tecnologia | Versão |
|---|---|---|
| Backend | Java + Spring Boot | 21 (LTS) + 4.1.0 |
| Motor de regras | Drools (DRL) + DMN | 10.2.0 |
| Persistência | Spring Data JPA + Flyway | Boot 4.1 / Flyway 12 |
| Banco | PostgreSQL (produção) / H2 em modo PostgreSQL (dev e testes) | 17 / 2.4 |
| Segurança | Spring Security + OAuth2 Resource Server (JWT HS256) | 7.1 |
| Documentação | springdoc-openapi | 3.1.0 |
| Frontend | React + TypeScript + Vite + TanStack Query | 19 / 5.7 / 6 |
| Infra | Docker + docker-compose + nginx | — |

---

## Decisões de arquitetura

### Drools embarcado em vez de Kogito — e por quê

Iniciamente era "Drools rodando via Kogito". **Essa combinação não é viável hoje**, e a razão é
verificável: o último release estável do `kogito-spring-boot-starter` é o **1.44.1.Final**, da linha
Kogito 1.x, alinhada a Spring Boot 2.7/3.0 — nada compatível com Spring Boot 4.x. O que existe acima disso no Maven Central é `2.44.0.Alpha`, inadequado para uso real.

Há um segundo motivo, independente de versões: **Kogito é geração de código em tempo de build.** Ele lê
os `.drl`/`.dmn` e gera classes e endpoints durante a compilação. Isso conflita diretamente com o
requisito não funcional de *"adicionar/editar regras sem recompilar toda a aplicação, se possível com
hot-reload"*. Com Kogito, toda mudança de regra é um novo build e um novo deploy.

A solução adotada usa **Drools 10.2 embarcado**, compilando DRL e DMN **em tempo de execução** pelo
`KieBuilder`. O ganho é exatamente o requisito que Kogito impediria: o administrador cadastra ou edita
uma regra pela API, e ela entra em vigor na próxima triagem — sem recompilar, sem reiniciar. O DMN é
preservado como pedido, via `DMNRuntime`, para que profissionais não técnicos revisem tabelas de decisão
em qualquer editor DMN.

### Duas camadas de conhecimento

O motor combina duas fontes, e é isso que faz o sistema servir a um protocolo qualquer:

1. **Camada base (orientada a dados).** Uma única regra DRL genérica, que não conhece nenhum sintoma
   específico: ela apenas propaga a gravidade que a unidade cadastrou para cada sintoma. Um protocolo
   recém-criado, sem uma linha de DRL escrita, **já funciona**.
2. **Camada especializada (DRL/DMN).** Regras que expressam o que a tabela plana *sintoma → gravidade*
   não alcança: combinações ("crise em curso" + "crise recente" = suspeita de estado de mal epiléptico),
   dependência de idade (crise febril em menor de 2 anos) e de gestação (eclâmpsia).

### A escala de gravidade vive no cadastro, não nas regras

Uma proposta de classificação carrega apenas o **código** do nível de risco (`"VERMELHO"`), nunca a
prioridade numérica. Quem sabe qual código é mais grave é a tabela `classificacao`. Assim a unidade
reordena, renomeia ou acrescenta níveis de risco sem tocar em nenhum DRL, e o motor nunca opera com uma
cópia desatualizada da escala.

### Desempate por ordem de disparo

Quando duas regras propõem a mesma gravidade, quem leva o crédito é a que disparou primeiro — ordem que
o autor do protocolo controla pela `salience` do DRL. As regras coletam propostas em uma lista (global
por sessão) em vez de inserir fatos na memória de trabalho, justamente para preservar essa ordem. Nenhum
critério arbitrário (como nome da regra) decide qual regra explica a decisão.

### O histórico guarda cópias, não referências

`classificacao_realizada` grava os textos (nome do grupo, da classificação, cor, tempo de espera,
sintomas) em vez de apenas chaves estrangeiras. O protocolo é editável; se o histórico apontasse para as
entidades vivas, renomear uma classificação ou alterar seu tempo de espera **reescreveria retroativamente
o registro clínico**. As FKs são mantidas apenas para filtro e estatística, e são anuláveis.

### Associação sintoma ↔ fluxograma como entidade

O mesmo discriminador pode pesar diferente conforme a queixa. `fluxograma_sintoma` carrega uma
`classificacao_especifica` opcional: "dor moderada" é Urgente no geral, mas em *Dor torácica* sobe para
Muito urgente. Sem a sobrescrita, vale a classificação padrão do sintoma.

### Reinferência na confirmação

`POST /triagens/avaliar` só infere (alimenta a tela de revisão); `POST /triagens` **refaz a inferência**
a partir dos sintomas e grava. O resultado exibido na revisão nunca é aceito como entrada — um payload
adulterado não consegue gravar no histórico uma classificação que o motor jamais produziu.

### Regra inválida não derruba a triagem

Cadastrar uma regra compila os artefatos **dentro da transação**. Se o DRL não compilar, a API responde
**422** com a lista de erros do compilador, nada é gravado e a base de conhecimento que está no ar
continua intacta.

### O esquema pertence ao Flyway

`ddl-auto: validate` faz a aplicação recusar-se a subir se o mapeamento JPA divergir das migrações. As
migrações usam apenas SQL padrão (`GENERATED BY DEFAULT AS IDENTITY`, `TIMESTAMP WITH TIME ZONE`), de
modo que **a mesma migração** roda no PostgreSQL de produção e no H2 dos testes — o esquema testado é o
esquema entregue.

---

## Modelo de dados

```
                  ┌───────────────┐
                  │    grupo      │  Adultos, Crianças, Traumas
                  └───────┬───────┘
                          │ 1:N
                  ┌───────▼───────┐
                  │  fluxograma   │  Convulsões, Cefaleia, Agressão
                  │  + modelo_dmn │  (opcional: tabela DMN da queixa)
                  └───────┬───────┘
                          │ 1:N
              ┌───────────▼────────────┐
              │  fluxograma_sintoma    │  N:N com atributo próprio:
              │  + classificacao_      │  gravidade específica desta
              │    especifica (opc.)   │  queixa + ordem de exibição
              └───────┬────────────────┘
                      │ N:1
              ┌───────▼───────┐        ┌────────────────────┐
              │   sintoma     │──N:1──▶│   classificacao    │
              └───────────────┘        │  prioridade (1=+   │
                                       │  grave), cor,      │
              ┌───────────────┐        │  tempo espera,     │
              │regra_protocolo│        │  local, tipo       │
              │ DRL/DMN texto │        └────────────────────┘
              │ (hot-reload)  │
              └───────────────┘        ┌────────────────────────┐
                                       │ classificacao_realizada│
              ┌───────────────┐        │ snapshot imutável do   │
              │   usuario     │        │ que foi decidido       │
              │ ADMIN/TRIAGEM │        └────────────────────────┘
              └───────────────┘
```

---

## Como o motor de regras funciona

```
  sintomas marcados
         │
         ▼
  ┌──────────────────────────────────────────────┐
  │ Fatos: FatoTriagem + SintomaObservado[]      │
  │ (cada sintoma já com sua gravidade efetiva)  │
  └───────────────┬──────────────────────────────┘
                  │
        ┌─────────┴──────────┐
        ▼                    ▼
  ┌───────────┐        ┌───────────┐
  │ Regras DRL│        │ Tabela DMN│  (só se o fluxograma
  │ base +    │        │ do fluxo- │   apontar um modelo)
  │ especiali-│        │ grama     │
  │ zadas     │        └─────┬─────┘
  └─────┬─────┘              │
        └────────┬───────────┘
                 ▼
     ┌────────────────────────────┐
     │ Propostas de classificação │  (todas devolvidas na API)
     └────────────┬───────────────┘
                  ▼
     ┌────────────────────────────────────────┐
     │ Arbitragem: menor prioridade vence.    │
     │ Empate → DRL/DMN antes de BASE.        │
     │ Empate persistente → ordem de disparo. │
     └────────────┬───────────────────────────┘
                  ▼
          classificação final
```

A API devolve **todas** as propostas, não só a vencedora, com origem, regra e justificativa. Em triagem
quem assina a classificação é o profissional, e ele precisa enxergar por que o sistema chegou àquele
nível de risco — um resultado sem rastro seria uma caixa-preta sobre uma decisão clínica.

### O fluxograma "Convulsões" como validação

O exemplo pedido no enunciado está implementado nas **duas** notações, para comparação:

- [`10-convulsoes.drl`](backend/src/main/resources/rules/10-convulsoes.drl) — discriminadores do
  Protocolo de Manchester em DRL, mais três regras de combinação que só o motor expressa.
- [`convulsoes.dmn`](backend/src/main/resources/rules/convulsoes.dmn) — a mesma lógica como tabela de
  decisão DMN 1.4, política de acerto `FIRST`, **uma linha por discriminador**, lida de cima para baixo
  do mais grave ao menos grave — a mesma leitura que o profissional faz do fluxograma impresso. Abre em
  qualquer editor DMN (Kie Sandbox, Camunda Modeler) para revisão por não técnicos.

---

## Como rodar

### Opção 1 — Docker (ambiente completo)

```bash
cp .env.example .env
# edite .env e gere o segredo do JWT:  openssl rand -base64 48
docker compose up --build
```

| Serviço | URL |
|---|---|
| Interface | http://localhost |
| API | http://localhost:8090/api/v1 |
| Swagger UI | http://localhost:8090/swagger-ui.html |
| OpenAPI JSON | http://localhost:8090/v3/api-docs |

### Opção 2 — Local, sem Docker

Requisitos: **JDK 21** e **Node 20+**.

```powershell
# Backend (H2 em memória, protocolo de demonstração criado automaticamente)
cd backend
.\executar.ps1
# API em http://localhost:8090 · Swagger em http://localhost:8090/swagger-ui.html
# Console H2 em http://localhost:8090/h2-console (JDBC URL: jdbc:h2:mem:sitpa)
```

`executar.ps1` localiza um JDK 21+ (em `JAVA_HOME`, em `%USERPROFILE%\.jdks`, ou nas instalações
padrão do Adoptium/Oracle) e só então chama o Maven. Ele existe por um motivo concreto: com um JDK
mais antigo como padrão do sistema, `mvn spring-boot:run` **não** falha na compilação — se `target/`
já estiver compilado, o Maven apenas lança a JVM antiga sobre as classes novas, e o erro que aparece é
um `UnsupportedClassVersionError: class file version 65.0 ... up to 61.0`, que não diz o que fazer. O
script confere a versão antes e, se não achar um JDK 21, explica onde obtê-lo.

Se preferir o caminho direto, `mvn spring-boot:run` funciona normalmente desde que `JAVA_HOME` aponte
para um JDK 21. Outros alvos também passam pelo script: `.\executar.ps1 -Goal test`.

```bash
# Frontend (em outro terminal)
cd frontend
npm install
npm run dev
# Interface em http://localhost:5173 (proxy /api → localhost:8090)
```

Se a porta 5173 estiver ocupada, o Vite sobe na 5174, 5175 e assim por diante. Ele repassa ao backend
o `Origin` do navegador, então o padrão de `sitpa.cors-origens` libera **qualquer porta local**
(`http://localhost:*`) — sem isso, o login falharia com um `403` do filtro de CORS e a tela mostraria
apenas *"Request failed with status code 403"*, sem dizer o motivo. Origens fora de `localhost` seguem
bloqueadas.

### Usuários iniciais

| Usuário | Senha | Perfil | Pode |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | tudo, incluindo cadastro do protocolo e das regras |
| `triagem` | `triagem123` | TRIAGEM | ler o protocolo, classificar pacientes, consultar histórico |

> Criados apenas quando a tabela de usuários está vazia. **Troque as senhas antes de qualquer uso real**
> (`SITPA_SENHA_ADMIN` / `SITPA_SENHA_TRIAGEM`), e use `SITPA_SEED_INICIAL=false` para subir sem o
> protocolo de demonstração.

---

## Exemplos de uso da API

### 1. Autenticar

```bash
TOKEN=$(curl -s -X POST http://localhost:8090/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"triagem","senha":"triagem123"}' | jq -r .token)
```

### 2. Navegar no protocolo

```bash
curl -s http://localhost:8090/api/v1/grupos?apenasAtivos=true -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8090/api/v1/fluxogramas?grupoId=1&apenasAtivos=true" -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8090/api/v1/fluxogramas/1/sintomas -H "Authorization: Bearer $TOKEN"
```

### 3. Avaliar (tela de revisão, não grava)

```bash
curl -s -X POST http://localhost:8090/api/v1/triagens/avaliar \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"grupoId":1,"fluxogramaId":1,"sintomasIds":[4],"idadeAnos":52}'
```

```jsonc
{
  "grupo":       { "id": 1, "codigo": "ADULTOS",    "nome": "Adultos" },
  "fluxograma":  { "id": 1, "codigo": "CONVULSOES", "nome": "Convulsoes" },
  "sintomasSelecionados": [{ "id": 4, "codigo": "CONVULSAO_EM_CURSO", "nome": "Convulsionando no momento" }],
  "classificacao": {
    "codigo": "VERMELHO", "nome": "Emergente", "cor": "#D32F2F", "prioridade": 1,
    "tempoMaximoEsperaMinutos": 0,
    "localAtendimento": "Sala de emergencia",
    "tipoAtendimento":  "Atendimento imediato"
  },
  "origemDecisao": "DRL",
  "regraAplicada": "Convulsoes - risco imediato de vida",
  "justificativa": "Discriminador de emergencia do fluxograma Convulsoes presente: atendimento imediato.",
  "propostas": [                       // todas as sugestões, inclusive as descartadas
    { "classificacaoCodigo": "VERMELHO", "origem": "DRL",  "vencedora": true,  "regra": "Convulsoes - risco imediato de vida" },
    { "classificacaoCodigo": "VERMELHO", "origem": "DMN",  "vencedora": false, "regra": "DMN: TriagemConvulsoes" },
    { "classificacaoCodigo": "VERMELHO", "origem": "BASE", "vencedora": false, "regra": "Base - gravidade cadastrada do sintoma" }
  ],
  "versaoBaseRegras": 1
}
```

### 4. Confirmar (reinfere no servidor e grava)

```bash
curl -s -X POST http://localhost:8090/api/v1/triagens \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"grupoId":1,"fluxogramaId":1,"sintomasIds":[4],"idadeAnos":52,
       "identificadorPaciente":"SENHA-042","observacoes":"Trazido pelo SAMU."}'
```

### 5. Consultar o histórico

```bash
curl -s "http://localhost:8090/api/v1/historico?classificacaoCodigo=VERMELHO&de=2026-01-01T00:00:00Z&tamanho=20" \
  -H "Authorization: Bearer $TOKEN"

curl -s http://localhost:8090/api/v1/historico/estatisticas -H "Authorization: Bearer $TOKEN"
```

### 6. Adicionar uma regra a quente (perfil ADMIN)

Este é o requisito de hot-reload em ação. A regra abaixo eleva cefaleia em idosos, algo que a tabela
*sintoma → gravidade* não consegue expressar:

```bash
ADMIN=$(curl -s -X POST http://localhost:8090/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","senha":"admin123"}' | jq -r .token)

curl -s -X POST http://localhost:8090/api/v1/regras \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d @- <<'JSON'
{
  "nome": "cefaleia-idoso",
  "descricao": "Cefaleia em maior de 75 anos exige avaliacao antecipada.",
  "tipo": "DRL",
  "ativo": true,
  "conteudo": "package br.com.sitpa.rules;\n\nimport br.com.sitpa.rules.model.FatoTriagem;\nimport br.com.sitpa.rules.model.PropostaClassificacao;\n\nglobal java.util.List propostas;\n\nrule \"Cefaleia - idoso com dor\"\n    salience 50\n    when\n        $f : FatoTriagem( fluxogramaCodigo == \"CEFALEIA\", idadeMaiorOuIgualA(75) == true, temSintoma(\"DOR_MODERADA\") == true )\n    then\n        propostas.add(new PropostaClassificacao(\"AMARELO\", drools.getRule().getName(), \"Cefaleia em maior de 75 anos exige avaliacao antecipada.\"));\nend\n"
}
JSON
```

A próxima triagem já usa a regra nova — **sem recompilar e sem reiniciar**. Um DRL com erro de sintaxe
devolve `422` com as mensagens do compilador e não é gravado:

```jsonc
{
  "status": 422,
  "erro": "Regra invalida",
  "mensagem": "As regras nao compilam; a base de conhecimento anterior continua ativa.",
  "detalhes": ["br/com/sitpa/regras/minha-regra.drl:7 [ERR 102] mismatched input ..."]
}
```

Para inspecionar ou forçar a recarga da base:

```bash
curl -s http://localhost:8090/api/v1/regras/base -H "Authorization: Bearer $ADMIN"
curl -s -X POST http://localhost:8090/api/v1/regras/base/recarregar -H "Authorization: Bearer $ADMIN"
```

### Referência rápida dos endpoints

| Método | Rota | Perfil |
|---|---|---|
| `POST` | `/api/v1/auth/login` · `GET /auth/eu` | público · autenticado |
| `GET` | `/api/v1/{grupos,fluxogramas,sintomas,classificacoes}` | autenticado |
| `POST` `PUT` `DELETE` | `/api/v1/{grupos,fluxogramas,sintomas,classificacoes}` | ADMIN |
| `GET` `POST` `PUT` `DELETE` | `/api/v1/fluxogramas/{id}/sintomas[/{sintomaId}]` | leitura autenticado · escrita ADMIN |
| `POST` | `/api/v1/triagens/avaliar` · `/api/v1/triagens` | autenticado |
| `GET` | `/api/v1/historico` · `/historico/{id}` · `/historico/estatisticas` | autenticado |
| `GET` `POST` `PUT` `DELETE` | `/api/v1/regras` · `/regras/base` · `/regras/base/recarregar` | ADMIN |

---

## Testes

```bash
cd backend && mvn test        # 26 testes
cd frontend && npm run build  # inclui a checagem de tipos
```

**`BaseConhecimentoTest`** (10) cobre o motor: compilação de DRL e DMN em runtime, a regra base
propagando gravidade de um sintoma inventado na hora, combinações que só o DRL expressa, isolamento
entre fluxogramas, a tabela DMN pela política `FIRST`, a `salience` determinando a ordem de disparo, o
hot-reload de uma regra vinda do banco e a garantia de que uma regra inválida é rejeitada **sem derrubar
a base que está no ar**.

**`TriagemFluxoIntegrationTest`** (16) sobe o contexto inteiro sobre H2 + Flyway com `ddl-auto: validate`
e exercita o fluxo real por HTTP: login, autorização por perfil (o usuário de triagem recebe `403` ao
tentar alterar o protocolo), inferência, prevalência da classificação mais grave, sobrescrita de
gravidade por fluxograma, recusa de sintoma que não pertence ao fluxograma, corpo malformado devolvendo
`400` (e não `500`), confirmação, histórico com filtros e publicação do OpenAPI.

> **Nota sobre pastas sincronizadas.** Se o projeto estiver dentro de uma pasta do OneDrive/Dropbox, o
> serviço de sincronização às vezes mantém arquivos de `target/` e `node_modules/` abertos, e o
> `mvn clean` falha com *"Failed to delete ... target\classes"*. Não é problema do projeto: pause a
> sincronização, ou mantenha o repositório fora da pasta sincronizada.

---

## Estrutura de pastas

```
sitpa/
├── docker-compose.yml
├── .env.example
├── README.md
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/br/com/sitpa/
│       │   │   ├── config/       PropriedadesSitpa, SegurancaConfig, OpenApiConfig, SemeadorInicial
│       │   │   ├── controller/   REST: grupos, fluxogramas, sintomas, classificações, triagem,
│       │   │   │                 histórico, regras, autenticação
│       │   │   ├── domain/       entidades JPA + enums
│       │   │   ├── dto/          records de request/response
│       │   │   ├── exception/    exceções de domínio + @RestControllerAdvice
│       │   │   ├── repository/   Spring Data JPA
│       │   │   ├── rules/        BaseConhecimento (hot-reload), artefatos, fatos do motor
│       │   │   ├── security/     UserDetails e adaptadores do Spring Security
│       │   │   └── service/      regras de negócio e orquestração do motor
│       │   └── resources/
│       │       ├── application*.yml      perfis: h2 (padrão) e postgres
│       │       ├── logback-spring.xml    texto em dev, JSON estruturado em produção
│       │       ├── db/migration/         V1__esquema_inicial.sql
│       │       └── rules/                00-base.drl, 10-convulsoes.drl, convulsoes.dmn
│       └── test/java/br/com/sitpa/
└── frontend/
    ├── package.json · vite.config.ts · tsconfig.json
    ├── Dockerfile · nginx.conf
    └── src/
        ├── api/         cliente axios, tipos e serviços
        ├── auth/        contexto de autenticação
        ├── components/  componentes compartilhados
        ├── pages/       Login, Triagem, Histórico, Classificações, Grupos,
        │                Fluxogramas, Sintomas, Associações, Regras
        └── styles/      design system em CSS
```

### Telas

**Triagem** é um fluxo em quatro passos — grupo → queixa → sintomas → revisão — com alvos de toque de
44 px, pensado para tablet e uso com luvas. A tela de revisão mostra a classificação em destaque e, logo
abaixo, a tabela de como o motor chegou nela. Cor é usada com parcimônia na interface justamente para
que o único elemento fortemente colorido da tela seja o nível de risco do paciente.

**Administração** cobre os CRUDs, a associação de sintomas com sobrescrita de gravidade por fluxograma,
e um editor de regras que mostra a base de conhecimento no ar (versão, regras DRL compiladas, modelos
DMN) e permite recarregá-la.

---

## Configuração

Tudo por variável de ambiente; os padrões servem para desenvolvimento.

| Variável | Padrão | Observação |
|---|---|---|
| `SITPA_JWT_SEGREDO` | segredo de dev | **≥ 32 bytes**; obrigatório no docker-compose |
| `SITPA_JWT_VALIDADE_MINUTOS` | `480` | validade do token |
| `SITPA_DB_URL` / `_USUARIO` / `_SENHA` | PostgreSQL local | perfil `postgres` |
| `SITPA_CORS_ORIGENS` | `http://localhost`, `http://localhost:*`, `http://127.0.0.1:*` | lista separada por vírgula; aceita curinga. Em produção, informe as origens exatas |
| `SITPA_SEED_INICIAL` | `true` | cria o protocolo de demonstração se o banco estiver vazio |
| `SITPA_SENHA_ADMIN` / `_TRIAGEM` | `admin123` / `triagem123` | senhas iniciais |
| `SITPA_REGRAS_RECONCILIACAO_MS` | `60000` | intervalo de reconciliação da base entre instâncias |
| `SPRING_PROFILES_ACTIVE` | `h2` | use `postgres` em produção (também liga o log JSON) |
