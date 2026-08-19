import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import {
  associarSintoma,
  atualizarAssociacao,
  desassociarSintoma,
  listarClassificacoes,
  listarFluxogramas,
  listarSintomas,
  listarSintomasDoFluxograma,
} from '../api/servicos';
import { Carregando, Erro, EtiquetaRisco, Informativo, Sucesso, Vazio } from '../components/Comuns';

export default function AssociacaoPage() {
  const queryClient = useQueryClient();
  const [fluxogramaId, setFluxogramaId] = useState<number | null>(null);
  const [sintomaParaAdicionar, setSintomaParaAdicionar] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const fluxogramas = useQuery({ queryKey: ['fluxogramas', 'todos'], queryFn: () => listarFluxogramas() });
  const sintomas = useQuery({ queryKey: ['sintomas'], queryFn: () => listarSintomas() });
  const classificacoes = useQuery({ queryKey: ['classificacoes'], queryFn: () => listarClassificacoes() });

  const associados = useQuery({
    queryKey: ['fluxograma-sintomas', fluxogramaId],
    queryFn: () => listarSintomasDoFluxograma(fluxogramaId!),
    enabled: fluxogramaId != null,
  });

  function concluir(mensagem: string) {
    queryClient.invalidateQueries({ queryKey: ['fluxograma-sintomas', fluxogramaId] });
    queryClient.invalidateQueries({ queryKey: ['fluxogramas'] });
    setSucesso(mensagem);
    setErro(null);
  }

  function falhar(e: unknown) {
    setErro(mensagemDeErro(e));
    setSucesso(null);
  }

  const associar = useMutation({
    mutationFn: (sintomaId: number) => associarSintoma(fluxogramaId!, { sintomaId }),
    onSuccess: () => {
      setSintomaParaAdicionar('');
      concluir('Sintoma associado.');
    },
    onError: falhar,
  });

  const alterar = useMutation({
    mutationFn: (dados: { sintomaId: number; classificacaoEspecificaId: number | null; ordem: number }) =>
      atualizarAssociacao(fluxogramaId!, dados.sintomaId, dados),
    onSuccess: () => concluir('Associacao atualizada.'),
    onError: falhar,
  });

  const remover = useMutation({
    mutationFn: (sintomaId: number) => desassociarSintoma(fluxogramaId!, sintomaId),
    onSuccess: () => concluir('Sintoma desassociado.'),
    onError: falhar,
  });

  const jaAssociados = new Set((associados.data ?? []).map((a) => a.sintomaId));
  const disponiveis = (sintomas.data ?? []).filter((s) => !jaAssociados.has(s.id));

  return (
    <>
      <h1>Associacao de sintomas</h1>
      <Informativo>
        A <strong>gravidade especifica</strong> vale so dentro deste fluxograma. Use quando o mesmo discriminador pesa
        diferente conforme a queixa &mdash; "dor moderada" costuma ser Urgente, mas em dor toracica merece Muito
        urgente. Deixando em "padrao do sintoma", vale a classificacao do cadastro.
      </Informativo>

      <Erro mensagem={erro} />
      <Sucesso mensagem={sucesso} />

      <section className="cartao">
        <div className="linha">
          <div className="campo" style={{ flex: '2 1 320px' }}>
            <label htmlFor="fluxograma">Fluxograma</label>
            <select
              id="fluxograma"
              value={fluxogramaId ?? ''}
              onChange={(e) => {
                setFluxogramaId(e.target.value ? Number(e.target.value) : null);
                setErro(null);
                setSucesso(null);
              }}
            >
              <option value="">Selecione um fluxograma...</option>
              {fluxogramas.data?.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.grupo.nome} / {f.nome}
                </option>
              ))}
            </select>
          </div>
          {fluxogramaId != null && (
            <>
              <div className="campo" style={{ flex: '2 1 320px' }}>
                <label htmlFor="novo">Adicionar sintoma</label>
                <select id="novo" value={sintomaParaAdicionar} onChange={(e) => setSintomaParaAdicionar(e.target.value)}>
                  <option value="">Selecione...</option>
                  {disponiveis.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.nome} ({s.classificacao.nome})
                    </option>
                  ))}
                </select>
              </div>
              <button
                type="button"
                className="primario"
                disabled={!sintomaParaAdicionar || associar.isPending}
                onClick={() => associar.mutate(Number(sintomaParaAdicionar))}
              >
                Associar
              </button>
            </>
          )}
        </div>
      </section>

      {fluxogramaId != null && (
        <section className="cartao">
          {associados.isLoading && <Carregando />}
          {associados.isError && <Erro mensagem={mensagemDeErro(associados.error)} />}
          {associados.data?.length === 0 && <Vazio>Nenhum sintoma associado a este fluxograma.</Vazio>}
          {associados.data && associados.data.length > 0 && (
            <div className="tabela-rolavel">
              <table>
                <thead>
                  <tr>
                    <th>Ordem</th>
                    <th>Sintoma</th>
                    <th>Padrao do sintoma</th>
                    <th>Gravidade neste fluxograma</th>
                    <th>Vale</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {associados.data.map((a) => (
                    <tr key={a.associacaoId}>
                      <td style={{ width: '90px' }}>
                        <input
                          type="number"
                          min={0}
                          defaultValue={a.ordem}
                          onBlur={(e) => {
                            const ordem = Number(e.target.value);
                            if (ordem !== a.ordem) {
                              alterar.mutate({
                                sintomaId: a.sintomaId,
                                classificacaoEspecificaId: a.classificacaoEspecifica?.id ?? null,
                                ordem,
                              });
                            }
                          }}
                        />
                      </td>
                      <td>
                        {a.nome}
                        <div className="suave mono">{a.codigo}</div>
                      </td>
                      <td>
                        <EtiquetaRisco cor={a.classificacaoPadrao.cor} nome={a.classificacaoPadrao.nome} />
                      </td>
                      <td>
                        <select
                          value={a.classificacaoEspecifica?.id ?? ''}
                          onChange={(e) =>
                            alterar.mutate({
                              sintomaId: a.sintomaId,
                              classificacaoEspecificaId: e.target.value ? Number(e.target.value) : null,
                              ordem: a.ordem,
                            })
                          }
                        >
                          <option value="">Padrao do sintoma</option>
                          {classificacoes.data?.map((c) => (
                            <option key={c.id} value={c.id}>
                              {c.prioridade} - {c.nome}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td>
                        <EtiquetaRisco cor={a.classificacaoEfetiva.cor} nome={a.classificacaoEfetiva.nome} />
                      </td>
                      <td>
                        <button
                          type="button"
                          className="perigo"
                          disabled={remover.isPending}
                          onClick={() => remover.mutate(a.sintomaId)}
                        >
                          Remover
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}
    </>
  );
}
