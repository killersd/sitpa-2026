import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import {
  consultarHistorico,
  estatisticas,
  listarClassificacoes,
  listarFluxogramas,
  listarGrupos,
  type FiltroHistorico,
} from '../api/servicos';
import { Carregando, Erro, EtiquetaRisco, Vazio, descreverEspera, formatarMomento } from '../components/Comuns';

const TAMANHO_PAGINA = 20;

/** Converte a data do input (yyyy-MM-dd, hora local) no instante ISO que a API espera. */
function comoInstante(data: string, fimDoDia: boolean): string | undefined {
  if (!data) return undefined;
  const [ano, mes, dia] = data.split('-').map(Number);
  const momento = fimDoDia
    ? new Date(ano, mes - 1, dia, 23, 59, 59, 999)
    : new Date(ano, mes - 1, dia, 0, 0, 0, 0);
  return momento.toISOString();
}

export default function HistoricoPage() {
  const [de, setDe] = useState('');
  const [ate, setAte] = useState('');
  const [classificacaoCodigo, setClassificacaoCodigo] = useState('');
  const [grupoId, setGrupoId] = useState('');
  const [fluxogramaId, setFluxogramaId] = useState('');
  const [identificador, setIdentificador] = useState('');
  const [pagina, setPagina] = useState(0);

  const grupos = useQuery({ queryKey: ['grupos'], queryFn: () => listarGrupos() });
  const fluxogramas = useQuery({ queryKey: ['fluxogramas', 'todos'], queryFn: () => listarFluxogramas() });
  const classificacoes = useQuery({ queryKey: ['classificacoes'], queryFn: () => listarClassificacoes() });
  const resumo = useQuery({ queryKey: ['estatisticas'], queryFn: estatisticas });

  const filtro: FiltroHistorico = {
    de: comoInstante(de, false),
    ate: comoInstante(ate, true),
    classificacaoCodigo: classificacaoCodigo || undefined,
    grupoId: grupoId ? Number(grupoId) : undefined,
    fluxogramaId: fluxogramaId ? Number(fluxogramaId) : undefined,
    identificadorPaciente: identificador.trim() || undefined,
    pagina,
    tamanho: TAMANHO_PAGINA,
  };

  const historico = useQuery({
    queryKey: ['historico', filtro],
    queryFn: () => consultarHistorico(filtro),
  });

  function limpar() {
    setDe('');
    setAte('');
    setClassificacaoCodigo('');
    setGrupoId('');
    setFluxogramaId('');
    setIdentificador('');
    setPagina(0);
  }

  const fluxogramasVisiveis = grupoId
    ? (fluxogramas.data ?? []).filter((f) => String(f.grupo.id) === grupoId)
    : (fluxogramas.data ?? []);

  const totalAtendimentos = (resumo.data ?? []).reduce((soma, r) => soma + r.total, 0);

  return (
    <>
      <h1>Historico de classificacoes</h1>

      <section className="cartao">
        <h2>Distribuicao por nivel de risco</h2>
        {resumo.isLoading && <Carregando />}
        {totalAtendimentos === 0 && !resumo.isLoading && <Vazio>Nenhuma triagem registrada ainda.</Vazio>}
        {totalAtendimentos > 0 && (
          <div className="grade">
            {resumo.data?.map((r) => (
              <div key={r.codigo}>
                <div className="entre">
                  <EtiquetaRisco cor={r.cor} nome={r.nome} />
                  <strong>{r.total}</strong>
                </div>
                <div className="barra" style={{ marginTop: '0.4rem' }}>
                  <span style={{ width: `${r.percentual}%`, background: r.cor }} />
                </div>
                <span className="suave">{r.percentual}% dos atendimentos</span>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="cartao">
        <h2>Filtros</h2>
        <div className="linha">
          <div className="campo">
            <label htmlFor="de">De</label>
            <input id="de" type="date" value={de} onChange={(e) => { setDe(e.target.value); setPagina(0); }} />
          </div>
          <div className="campo">
            <label htmlFor="ate">Ate</label>
            <input id="ate" type="date" value={ate} onChange={(e) => { setAte(e.target.value); setPagina(0); }} />
          </div>
          <div className="campo">
            <label htmlFor="classificacao">Classificacao</label>
            <select
              id="classificacao"
              value={classificacaoCodigo}
              onChange={(e) => { setClassificacaoCodigo(e.target.value); setPagina(0); }}
            >
              <option value="">Todas</option>
              {classificacoes.data?.map((c) => (
                <option key={c.id} value={c.codigo}>
                  {c.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="campo">
            <label htmlFor="grupo">Grupo</label>
            <select
              id="grupo"
              value={grupoId}
              onChange={(e) => { setGrupoId(e.target.value); setFluxogramaId(''); setPagina(0); }}
            >
              <option value="">Todos</option>
              {grupos.data?.map((g) => (
                <option key={g.id} value={g.id}>
                  {g.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="campo">
            <label htmlFor="fluxograma">Fluxograma</label>
            <select
              id="fluxograma"
              value={fluxogramaId}
              onChange={(e) => { setFluxogramaId(e.target.value); setPagina(0); }}
            >
              <option value="">Todos</option>
              {fluxogramasVisiveis.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.nome}
                </option>
              ))}
            </select>
          </div>
          <div className="campo">
            <label htmlFor="identificador">Identificacao do paciente</label>
            <input
              id="identificador"
              value={identificador}
              onChange={(e) => { setIdentificador(e.target.value); setPagina(0); }}
              placeholder="Senha ou prontuario"
            />
          </div>
          <button type="button" onClick={limpar}>
            Limpar
          </button>
        </div>
      </section>

      <section className="cartao">
        {historico.isLoading && <Carregando />}
        {historico.isError && <Erro mensagem={mensagemDeErro(historico.error)} />}
        {historico.data && historico.data.content.length === 0 && (
          <Vazio>Nenhum registro para os filtros informados.</Vazio>
        )}
        {historico.data && historico.data.content.length > 0 && (
          <>
            <div className="tabela-rolavel">
              <table>
                <thead>
                  <tr>
                    <th>Data</th>
                    <th>Paciente</th>
                    <th>Grupo / Queixa</th>
                    <th>Classificacao</th>
                    <th>Encaminhamento</th>
                    <th>Sintomas</th>
                    <th>Decisao</th>
                    <th>Usuario</th>
                  </tr>
                </thead>
                <tbody>
                  {historico.data.content.map((r) => (
                    <tr key={r.id}>
                      <td>{formatarMomento(r.realizadaEm)}</td>
                      <td>{r.identificadorPaciente ?? '-'}</td>
                      <td>
                        {r.grupoNome}
                        <span className="auxiliar suave"> / {r.fluxogramaNome}</span>
                      </td>
                      <td>
                        <EtiquetaRisco cor={r.classificacaoCor} nome={r.classificacaoNome} />
                      </td>
                      <td>
                        {r.localAtendimento}
                        <div className="suave">{descreverEspera(r.tempoMaximoEsperaMinutos)}</div>
                      </td>
                      <td>{r.sintomas.length === 0 ? '-' : r.sintomas.join(', ')}</td>
                      <td>
                        {r.origemDecisao}
                        {r.regraAplicada && <div className="suave mono">{r.regraAplicada}</div>}
                      </td>
                      <td>{r.usuarioLogin}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="entre" style={{ marginTop: '0.9rem' }}>
              <span className="suave">
                {historico.data.totalElements} registro(s) &middot; pagina {historico.data.number + 1} de{' '}
                {Math.max(historico.data.totalPages, 1)}
              </span>
              <div className="botoes">
                <button type="button" disabled={pagina === 0} onClick={() => setPagina((p) => p - 1)}>
                  Anterior
                </button>
                <button
                  type="button"
                  disabled={pagina + 1 >= historico.data.totalPages}
                  onClick={() => setPagina((p) => p + 1)}
                >
                  Proxima
                </button>
              </div>
            </div>
          </>
        )}
      </section>
    </>
  );
}
