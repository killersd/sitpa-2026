import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import {
  atualizarFluxograma,
  criarFluxograma,
  estadoBaseRegras,
  excluirFluxograma,
  listarFluxogramas,
  listarGrupos,
  type FluxogramaEntrada,
} from '../api/servicos';
import type { Fluxograma } from '../api/tipos';
import { Carregando, Erro, Informativo, Sucesso } from '../components/Comuns';

const VAZIO: FluxogramaEntrada = { codigo: '', nome: '', descricao: '', grupoId: 0, modeloDmn: '', ativo: true };

export default function FluxogramasPage() {
  const queryClient = useQueryClient();
  const [emEdicao, setEmEdicao] = useState<Fluxograma | null>(null);
  const [form, setForm] = useState<FluxogramaEntrada>(VAZIO);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const lista = useQuery({ queryKey: ['fluxogramas', 'todos'], queryFn: () => listarFluxogramas() });
  const grupos = useQuery({ queryKey: ['grupos'], queryFn: () => listarGrupos() });
  const baseRegras = useQuery({ queryKey: ['base-regras'], queryFn: estadoBaseRegras });

  function concluir(mensagem: string) {
    queryClient.invalidateQueries({ queryKey: ['fluxogramas'] });
    queryClient.invalidateQueries({ queryKey: ['grupos'] });
    setSucesso(mensagem);
    setErro(null);
    cancelar();
  }

  const salvar = useMutation({
    mutationFn: (corpo: FluxogramaEntrada) =>
      emEdicao ? atualizarFluxograma(emEdicao.id, corpo) : criarFluxograma(corpo),
    onSuccess: () => concluir(emEdicao ? 'Fluxograma atualizado.' : 'Fluxograma criado.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  const remover = useMutation({
    mutationFn: excluirFluxograma,
    onSuccess: () => concluir('Fluxograma excluido.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  function editar(f: Fluxograma) {
    setEmEdicao(f);
    setForm({
      codigo: f.codigo,
      nome: f.nome,
      descricao: f.descricao ?? '',
      grupoId: f.grupo.id,
      modeloDmn: f.modeloDmn ?? '',
      ativo: f.ativo,
    });
    setErro(null);
    setSucesso(null);
  }

  function cancelar() {
    setEmEdicao(null);
    setForm(VAZIO);
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    salvar.mutate({ ...form, modeloDmn: form.modeloDmn?.trim() ? form.modeloDmn.trim() : undefined });
  }

  return (
    <>
      <h1>Fluxogramas</h1>
      <Informativo>
        Cada fluxograma e uma queixa apresentada. Preencher o <strong>modelo DMN</strong> faz o motor executar tambem a
        tabela de decisao correspondente; deixando em branco, valem apenas as regras DRL e a gravidade cadastrada nos
        sintomas.
      </Informativo>

      <Erro mensagem={erro} />
      <Sucesso mensagem={sucesso} />

      <section className="cartao">
        <h2>{emEdicao ? `Editando ${emEdicao.nome}` : 'Novo fluxograma'}</h2>
        <form onSubmit={enviar}>
          <div className="linha">
            <div className="campo">
              <label htmlFor="codigo">Codigo</label>
              <input
                id="codigo"
                value={form.codigo}
                onChange={(e) => setForm({ ...form, codigo: e.target.value.toUpperCase() })}
                pattern="[A-Z0-9_]+"
                title="Somente letras maiusculas, numeros e underscore"
                required
              />
            </div>
            <div className="campo">
              <label htmlFor="nome">Nome</label>
              <input id="nome" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </div>
            <div className="campo">
              <label htmlFor="grupo">Grupo</label>
              <select
                id="grupo"
                value={form.grupoId || ''}
                onChange={(e) => setForm({ ...form, grupoId: Number(e.target.value) })}
                required
              >
                <option value="">Selecione...</option>
                {grupos.data?.map((g) => (
                  <option key={g.id} value={g.id}>
                    {g.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="campo">
              <label htmlFor="dmn">Modelo DMN</label>
              <select
                id="dmn"
                value={form.modeloDmn ?? ''}
                onChange={(e) => setForm({ ...form, modeloDmn: e.target.value })}
              >
                <option value="">Nenhum</option>
                {baseRegras.data?.modelosDmn.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </div>
            <div className="campo" style={{ flex: '0 0 130px' }}>
              <label>Situacao</label>
              <label className={`opcao${form.ativo ? ' marcada' : ''}`}>
                <input type="checkbox" checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
                <span className="rotulo">Ativo</span>
              </label>
            </div>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <label htmlFor="descricao">Descricao</label>
            <input
              id="descricao"
              value={form.descricao ?? ''}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
            />
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="submit" className="primario" disabled={salvar.isPending}>
              {salvar.isPending ? 'Salvando...' : emEdicao ? 'Salvar alteracoes' : 'Criar fluxograma'}
            </button>
            {emEdicao && (
              <button type="button" onClick={cancelar}>
                Cancelar
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="cartao">
        {lista.isLoading && <Carregando />}
        {lista.isError && <Erro mensagem={mensagemDeErro(lista.error)} />}
        <div className="tabela-rolavel">
          <table>
            <thead>
              <tr>
                <th>Codigo</th>
                <th>Nome</th>
                <th>Grupo</th>
                <th>Sintomas</th>
                <th>Modelo DMN</th>
                <th>Situacao</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {lista.data?.map((f) => (
                <tr key={f.id}>
                  <td className="mono">{f.codigo}</td>
                  <td>{f.nome}</td>
                  <td>{f.grupo.nome}</td>
                  <td>{f.quantidadeSintomas}</td>
                  <td className="mono">{f.modeloDmn ?? '-'}</td>
                  <td>{f.ativo ? 'Ativo' : 'Inativo'}</td>
                  <td>
                    <div className="botoes">
                      <button type="button" className="discreto" onClick={() => editar(f)}>
                        Editar
                      </button>
                      <button
                        type="button"
                        className="perigo"
                        disabled={remover.isPending}
                        onClick={() => remover.mutate(f.id)}
                      >
                        Excluir
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}
