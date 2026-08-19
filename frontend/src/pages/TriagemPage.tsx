import { useMemo, useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import {
  avaliarTriagem,
  confirmarTriagem,
  listarFluxogramas,
  listarGrupos,
  listarSintomasDoFluxograma,
} from '../api/servicos';
import type { ClassificacaoRealizada, ResultadoTriagem, TriagemRequest } from '../api/tipos';
import { Carregando, Erro, EtiquetaRisco, Vazio, descreverEspera, formatarMomento } from '../components/Comuns';

type Passo = 'grupo' | 'fluxograma' | 'sintomas' | 'revisao' | 'concluido';

const PASSOS: { chave: Passo; rotulo: string }[] = [
  { chave: 'grupo', rotulo: 'Grupo' },
  { chave: 'fluxograma', rotulo: 'Queixa' },
  { chave: 'sintomas', rotulo: 'Sintomas' },
  { chave: 'revisao', rotulo: 'Revisao' },
];

export default function TriagemPage() {
  const [passo, setPasso] = useState<Passo>('grupo');
  const [grupoId, setGrupoId] = useState<number | null>(null);
  const [fluxogramaId, setFluxogramaId] = useState<number | null>(null);
  const [sintomasIds, setSintomasIds] = useState<number[]>([]);
  const [idade, setIdade] = useState('');
  const [gestante, setGestante] = useState(false);
  const [identificador, setIdentificador] = useState('');
  const [observacoes, setObservacoes] = useState('');
  const [resultado, setResultado] = useState<ResultadoTriagem | null>(null);
  const [registro, setRegistro] = useState<ClassificacaoRealizada | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  const grupos = useQuery({ queryKey: ['grupos', 'ativos'], queryFn: () => listarGrupos(true) });

  const fluxogramas = useQuery({
    queryKey: ['fluxogramas', grupoId],
    queryFn: () => listarFluxogramas(grupoId!, true),
    enabled: grupoId != null,
  });

  const sintomas = useQuery({
    queryKey: ['fluxograma-sintomas', fluxogramaId],
    queryFn: () => listarSintomasDoFluxograma(fluxogramaId!),
    enabled: fluxogramaId != null,
  });

  const requisicao: TriagemRequest | null = useMemo(() => {
    if (grupoId == null || fluxogramaId == null) return null;
    return {
      grupoId,
      fluxogramaId,
      sintomasIds,
      idadeAnos: idade.trim() === '' ? null : Number(idade),
      gestante,
      identificadorPaciente: identificador.trim() || undefined,
      observacoes: observacoes.trim() || undefined,
    };
  }, [grupoId, fluxogramaId, sintomasIds, idade, gestante, identificador, observacoes]);

  const avaliacao = useMutation({
    mutationFn: avaliarTriagem,
    onSuccess: (r) => {
      setResultado(r);
      setPasso('revisao');
      setErro(null);
    },
    onError: (e) => setErro(mensagemDeErro(e)),
  });

  const confirmacao = useMutation({
    mutationFn: confirmarTriagem,
    onSuccess: (r) => {
      setRegistro(r);
      setPasso('concluido');
      setErro(null);
    },
    onError: (e) => setErro(mensagemDeErro(e)),
  });

  function reiniciar() {
    setPasso('grupo');
    setGrupoId(null);
    setFluxogramaId(null);
    setSintomasIds([]);
    setIdade('');
    setGestante(false);
    setIdentificador('');
    setObservacoes('');
    setResultado(null);
    setRegistro(null);
    setErro(null);
  }

  function alternarSintoma(id: number) {
    setSintomasIds((atual) => (atual.includes(id) ? atual.filter((x) => x !== id) : [...atual, id]));
  }

  const indicePasso = PASSOS.findIndex((p) => p.chave === passo);

  return (
    <>
      <h1>Classificacao de risco</h1>

      {passo !== 'concluido' && (
        <ol className="passos">
          {PASSOS.map((p, indice) => (
            <li
              key={p.chave}
              className={indice === indicePasso ? 'atual' : indice < indicePasso ? 'concluido' : undefined}
            >
              {p.rotulo}
            </li>
          ))}
        </ol>
      )}

      <Erro mensagem={erro} />

      {/* --- Passo 1: grupo --- */}
      {passo === 'grupo' && (
        <section className="cartao">
          <h2>Qual e o grupo do paciente?</h2>
          {grupos.isLoading && <Carregando />}
          {grupos.isError && <Erro mensagem={mensagemDeErro(grupos.error)} />}
          {grupos.data?.length === 0 && <Vazio>Nenhum grupo ativo cadastrado.</Vazio>}
          <div className="opcoes">
            {grupos.data?.map((grupo) => (
              <label key={grupo.id} className={`opcao${grupoId === grupo.id ? ' marcada' : ''}`}>
                <input
                  type="radio"
                  name="grupo"
                  checked={grupoId === grupo.id}
                  onChange={() => {
                    setGrupoId(grupo.id);
                    setFluxogramaId(null);
                    setSintomasIds([]);
                  }}
                />
                <span className="rotulo">
                  {grupo.nome}
                  {grupo.descricao && <span className="auxiliar">{grupo.descricao}</span>}
                </span>
              </label>
            ))}
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="button" className="primario" disabled={grupoId == null} onClick={() => setPasso('fluxograma')}>
              Continuar
            </button>
          </div>
        </section>
      )}

      {/* --- Passo 2: fluxograma --- */}
      {passo === 'fluxograma' && (
        <section className="cartao">
          <h2>Qual e a queixa apresentada?</h2>
          {fluxogramas.isLoading && <Carregando />}
          {fluxogramas.isError && <Erro mensagem={mensagemDeErro(fluxogramas.error)} />}
          {fluxogramas.data?.length === 0 && <Vazio>Este grupo ainda nao possui fluxogramas ativos.</Vazio>}
          <div className="opcoes">
            {fluxogramas.data?.map((f) => (
              <label key={f.id} className={`opcao${fluxogramaId === f.id ? ' marcada' : ''}`}>
                <input
                  type="radio"
                  name="fluxograma"
                  checked={fluxogramaId === f.id}
                  onChange={() => {
                    setFluxogramaId(f.id);
                    setSintomasIds([]);
                  }}
                />
                <span className="rotulo">
                  {f.nome}
                  <span className="auxiliar">
                    {f.descricao ?? `${f.quantidadeSintomas} sintoma(s)`}
                    {f.modeloDmn ? ' - avaliado tambem por tabela DMN' : ''}
                  </span>
                </span>
              </label>
            ))}
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="button" onClick={() => setPasso('grupo')}>
              Voltar
            </button>
            <button
              type="button"
              className="primario"
              disabled={fluxogramaId == null}
              onClick={() => setPasso('sintomas')}
            >
              Continuar
            </button>
          </div>
        </section>
      )}

      {/* --- Passo 3: sintomas --- */}
      {passo === 'sintomas' && (
        <>
          <section className="cartao">
            <h2>Dados do atendimento</h2>
            <div className="linha">
              <div className="campo">
                <label htmlFor="identificador">Identificacao (senha ou prontuario)</label>
                <input
                  id="identificador"
                  value={identificador}
                  onChange={(e) => setIdentificador(e.target.value)}
                  placeholder="Opcional"
                />
              </div>
              <div className="campo">
                <label htmlFor="idade">Idade (anos)</label>
                <input
                  id="idade"
                  type="number"
                  min={0}
                  max={130}
                  value={idade}
                  onChange={(e) => setIdade(e.target.value)}
                  placeholder="Opcional"
                />
              </div>
              <div className="campo">
                <label>Gestacao</label>
                <label className={`opcao${gestante ? ' marcada' : ''}`}>
                  <input type="checkbox" checked={gestante} onChange={(e) => setGestante(e.target.checked)} />
                  <span className="rotulo">Paciente gestante</span>
                </label>
              </div>
            </div>
          </section>

          <section className="cartao">
            <h2>Sintomas apresentados</h2>
            <p className="suave">
              Marque todos os discriminadores observados. O sistema devolve sempre a classificacao mais grave.
            </p>
            {sintomas.isLoading && <Carregando />}
            {sintomas.isError && <Erro mensagem={mensagemDeErro(sintomas.error)} />}
            {sintomas.data?.length === 0 && <Vazio>Este fluxograma ainda nao tem sintomas associados.</Vazio>}
            <div className="opcoes">
              {sintomas.data
                ?.filter((s) => s.ativo)
                .map((s) => (
                  <label key={s.sintomaId} className={`opcao${sintomasIds.includes(s.sintomaId) ? ' marcada' : ''}`}>
                    <input
                      type="checkbox"
                      checked={sintomasIds.includes(s.sintomaId)}
                      onChange={() => alternarSintoma(s.sintomaId)}
                    />
                    <span className="rotulo">
                      {s.nome}
                      <span className="auxiliar">{s.descricao}</span>
                    </span>
                    <EtiquetaRisco cor={s.classificacaoEfetiva.cor} nome={s.classificacaoEfetiva.nome} />
                  </label>
                ))}
            </div>
            <div className="botoes" style={{ marginTop: '1rem' }}>
              <button type="button" onClick={() => setPasso('fluxograma')}>
                Voltar
              </button>
              <button
                type="button"
                className="primario"
                disabled={!requisicao || avaliacao.isPending}
                onClick={() => requisicao && avaliacao.mutate(requisicao)}
              >
                {avaliacao.isPending ? 'Avaliando...' : 'Avaliar classificacao'}
              </button>
            </div>
          </section>
        </>
      )}

      {/* --- Passo 4: revisao --- */}
      {passo === 'revisao' && resultado && (
        <>
          <section className="cartao">
            <div className="painel-risco" style={{ background: resultado.classificacao.cor }}>
              <div className="nivel">{resultado.classificacao.nome}</div>
              <div>{resultado.justificativa}</div>
              <dl className="detalhes">
                <div>
                  <dt>Tempo maximo de espera</dt>
                  <dd>{descreverEspera(resultado.classificacao.tempoMaximoEsperaMinutos)}</dd>
                </div>
                <div>
                  <dt>Local de atendimento</dt>
                  <dd>{resultado.classificacao.localAtendimento}</dd>
                </div>
                <div>
                  <dt>Tipo de atendimento</dt>
                  <dd>{resultado.classificacao.tipoAtendimento}</dd>
                </div>
                <div>
                  <dt>Origem da decisao</dt>
                  <dd>
                    {resultado.origemDecisao}
                    {resultado.regraAplicada ? ` - ${resultado.regraAplicada}` : ''}
                  </dd>
                </div>
              </dl>
            </div>
          </section>

          <section className="cartao">
            <h2>Confira antes de confirmar</h2>
            <p>
              <strong>{resultado.grupo.nome}</strong> &rarr; <strong>{resultado.fluxograma.nome}</strong>
            </p>
            <p className="suave">
              Sintomas marcados:{' '}
              {resultado.sintomasSelecionados.length === 0
                ? 'nenhum'
                : resultado.sintomasSelecionados.map((s) => s.nome).join(', ')}
            </p>

            <h3>Como o motor chegou nesse resultado</h3>
            <div className="tabela-rolavel">
              <table>
                <thead>
                  <tr>
                    <th>Nivel</th>
                    <th>Origem</th>
                    <th>Regra</th>
                    <th>Justificativa</th>
                  </tr>
                </thead>
                <tbody>
                  {resultado.propostas.map((p, indice) => (
                    <tr key={`${p.regra}-${indice}`} style={p.vencedora ? { fontWeight: 600 } : undefined}>
                      <td>
                        <EtiquetaRisco cor={p.cor} nome={p.classificacaoNome} />
                        {p.vencedora && <span className="suave"> aplicada</span>}
                      </td>
                      <td>{p.origem}</td>
                      <td className="mono">{p.regra}</td>
                      <td>{p.justificativa}</td>
                    </tr>
                  ))}
                  {resultado.propostas.length === 0 && (
                    <tr>
                      <td colSpan={4} className="suave">
                        Nenhuma regra do protocolo se aplicou aos sintomas marcados.
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <div style={{ marginTop: '1rem' }}>
              <label htmlFor="observacoes">Observacoes do profissional</label>
              <textarea
                id="observacoes"
                value={observacoes}
                onChange={(e) => setObservacoes(e.target.value)}
                placeholder="Registre aqui qualquer informacao relevante do atendimento."
              />
            </div>

            <div className="botoes" style={{ marginTop: '1rem' }}>
              <button type="button" onClick={() => setPasso('sintomas')}>
                Corrigir sintomas
              </button>
              <button
                type="button"
                className="primario"
                disabled={!requisicao || confirmacao.isPending}
                onClick={() => requisicao && confirmacao.mutate(requisicao)}
              >
                {confirmacao.isPending ? 'Registrando...' : 'Confirmar classificacao'}
              </button>
            </div>
            <p className="suave" style={{ marginTop: '0.75rem', marginBottom: 0 }}>
              Ao confirmar, a inferencia e refeita no servidor a partir dos sintomas marcados.
            </p>
          </section>
        </>
      )}

      {/* --- Conclusao --- */}
      {passo === 'concluido' && registro && (
        <section className="cartao">
          <div className="painel-risco" style={{ background: registro.classificacaoCor }}>
            <div className="nivel">{registro.classificacaoNome}</div>
            <div>
              Triagem #{registro.id} registrada em {formatarMomento(registro.realizadaEm)}.
            </div>
            <dl className="detalhes">
              <div>
                <dt>Paciente</dt>
                <dd>{registro.identificadorPaciente ?? 'Nao identificado'}</dd>
              </div>
              <div>
                <dt>Encaminhar para</dt>
                <dd>{registro.localAtendimento}</dd>
              </div>
              <div>
                <dt>Atender em ate</dt>
                <dd>{descreverEspera(registro.tempoMaximoEsperaMinutos)}</dd>
              </div>
            </dl>
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="button" className="primario" onClick={reiniciar}>
              Nova triagem
            </button>
          </div>
        </section>
      )}
    </>
  );
}
