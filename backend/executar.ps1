<#
.SYNOPSIS
    Sobe o backend do SITPa com um JDK 21, independente do JAVA_HOME do shell.

.DESCRIPTION
    O projeto exige Java 21, mas e comum a maquina ter um JDK mais antigo como padrao. Quando
    isso acontece o Maven nao reclama de imediato: se target/ ja estiver compilado, ele apenas
    lanca a JVM antiga sobre classes novas e o erro que aparece e um UnsupportedClassVersionError
    ("class file version 65.0 ... up to 61.0"), que nao diz o que fazer.

    Este script procura um JDK 21+ nos lugares habituais, confere a versao antes de usar e so
    entao chama o Maven. Se nao achar nenhum, explica onde instalar em vez de deixar o erro
    obscuro aparecer.

.PARAMETER Goal
    Objetivo do Maven. Padrao: spring-boot:run.

.EXAMPLE
    .\executar.ps1
    Sobe a aplicacao em http://localhost:8090

.EXAMPLE
    .\executar.ps1 -Goal test
    Roda a suite de testes com o JDK correto.

.EXAMPLE
    .\executar.ps1 -Goal spring-boot:run -ArgumentosMaven '-Dspring-boot.run.profiles=postgres'
    Sobe apontando para o PostgreSQL em vez do H2.
#>
[CmdletBinding()]
param(
    [string]$Goal = 'spring-boot:run',
    [string[]]$ArgumentosMaven = @()
)

$ErrorActionPreference = 'Stop'

function Get-VersaoMaior {
    param([string]$CaminhoJdk)

    if (-not $CaminhoJdk -or -not (Test-Path (Join-Path $CaminhoJdk 'bin\java.exe'))) { return 0 }

    # A versao e lida do arquivo "release", presente em qualquer distribuicao do JDK. E texto
    # simples e evita executar "java -version": no PowerShell 5.1, capturar a saida de erro de um
    # executavel nativo gera um NativeCommandError que, sob ErrorActionPreference = Stop,
    # aborta a deteccao mesmo quando o java respondeu normalmente.
    $release = Join-Path $CaminhoJdk 'release'
    if (Test-Path $release) {
        $linha = Select-String -Path $release -Pattern '^JAVA_VERSION="?(\d+)' -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($linha) { return [int]$linha.Matches[0].Groups[1].Value }
    }
    return 0
}

# Ordem de busca: o que o shell ja define vem primeiro, para respeitar quem configurou o ambiente.
$candidatos = @()
if ($env:JAVA_HOME) { $candidatos += $env:JAVA_HOME }
$candidatos += @(
    (Join-Path $env:USERPROFILE '.jdks\jdk-21'),
    'C:\Program Files\Eclipse Adoptium\jdk-21',
    'C:\Program Files\Java\jdk-21',
    'C:\Program Files\Microsoft\jdk-21'
)
# Qualquer jdk-21* dentro de .jdks ou do Adoptium tambem serve.
foreach ($raiz in @((Join-Path $env:USERPROFILE '.jdks'), 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java')) {
    if (Test-Path $raiz) {
        $candidatos += (Get-ChildItem $raiz -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'jdk-?2[1-9]' } |
            Select-Object -ExpandProperty FullName)
    }
}

$jdk = $null
foreach ($candidato in ($candidatos | Where-Object { $_ } | Select-Object -Unique)) {
    if ((Get-VersaoMaior $candidato) -ge 21) { $jdk = $candidato; break }
}

if (-not $jdk) {
    Write-Host ''
    Write-Host 'Nenhum JDK 21 ou superior encontrado.' -ForegroundColor Red
    Write-Host ''
    Write-Host 'O SITPa exige Java 21. Resolva de uma destas formas:'
    Write-Host '  1. Extraia um Temurin 21 em: ' -NoNewline
    Write-Host (Join-Path $env:USERPROFILE '.jdks\jdk-21') -ForegroundColor Cyan
    Write-Host '     Download: https://adoptium.net/temurin/releases/?version=21'
    Write-Host '  2. Ou instale com: ' -NoNewline
    Write-Host 'winget install EclipseAdoptium.Temurin.21.JDK' -ForegroundColor Cyan
    Write-Host '  3. Ou defina JAVA_HOME apontando para um JDK 21 ja instalado.'
    Write-Host ''
    exit 1
}

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$env:PATH"

Write-Host "JDK ....: $jdk" -ForegroundColor DarkGray
Write-Host "Goal ...: $Goal" -ForegroundColor DarkGray
if ($Goal -eq 'spring-boot:run') {
    Write-Host 'API ....: http://localhost:8090  (Swagger em /swagger-ui.html)' -ForegroundColor DarkGray
}
Write-Host ''

Push-Location $PSScriptRoot
try {
    # O Maven (e o Logback, na subida) escrevem avisos na saida de erro. No PowerShell 5.1 isso
    # vira NativeCommandError e, sob ErrorActionPreference = Stop, abortaria a execucao por causa
    # de um simples aviso. Quem decide se deu certo aqui e o codigo de saida do Maven.
    $ErrorActionPreference = 'Continue'
    & mvn $Goal @ArgumentosMaven
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
