import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import { atualizarGrupo, criarGrupo, excluirGrupo, listarGrupos, type GrupoEntrada } from '../api/servicos';
import type { Grupo } from '../api/tipos';
import { Carregando, Erro, Sucesso } from '../components/Comuns';

const VAZIO: GrupoEntrada = { codigo: '', nome: '', descricao: '', ativo: true };

export default function GruposPage() {
  const queryClient = useQueryClient();
  const [emEdicao, setEmEdicao] = useState<Grupo | null>(null);
  const [form, setForm] = useState<GrupoEntrada>(VAZIO);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const lista = useQuery({ queryKey: ['grupos'], queryFn: () => listarGrupos() });

  function concluir(mensagem: string) {
    queryClient.invalidateQueries({ queryKey: ['grupos'] });
    queryClient.invalidateQueries({ queryKey: ['fluxogramas'] });
    setSucesso(mensagem);
    setErro(null);
    cancelar();
  }

  const salvar = useMutation({
    mutationFn: (corpo: GrupoEntrada) => (emEdicao ? atualizarGrupo(emEdicao.id, corpo) : criarGrupo(corpo)),
    onSuccess: () => concluir(emEdicao ? 'Grupo atualizado.' : 'Grupo criado.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  const remover = useMutation({
    mutationFn: excluirGrupo,
    onSuccess: () => concluir('Grupo excluido.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  function editar(g: Grupo) {
    setEmEdicao(g);
    setForm({ codigo: g.codigo, nome: g.nome, descricao: g.descricao ?? '', ativo: g.ativo });
    setErro(null);
    setSucesso(null);
  }

  function cancelar() {
    setEmEdicao(null);
    setForm(VAZIO);
  }

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    salvar.mutate(form);
  }

  return (
    <>
      <h1>Grupos</h1>
      <p className="suave">Categorias amplas do protocolo, como Adultos, Criancas ou Traumas.</p>

      <Erro mensagem={erro} />
      <Sucesso mensagem={sucesso} />

      <section className="cartao">
        <h2>{emEdicao ? `Editando ${emEdicao.nome}` : 'Novo grupo'}</h2>
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
            <div className="campo" style={{ flex: '2 1 320px' }}>
              <label htmlFor="descricao">Descricao</label>
              <input
                id="descricao"
                value={form.descricao ?? ''}
                onChange={(e) => setForm({ ...form, descricao: e.target.value })}
              />
            </div>
            <div className="campo" style={{ flex: '0 0 130px' }}>
              <label>Situacao</label>
              <label className={`opcao${form.ativo ? ' marcada' : ''}`}>
                <input type="checkbox" checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
                <span className="rotulo">Ativo</span>
              </label>
            </div>
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="submit" className="primario" disabled={salvar.isPending}>
              {salvar.isPending ? 'Salvando...' : emEdicao ? 'Salvar alteracoes' : 'Criar grupo'}
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
                <th>Descricao</th>
                <th>Fluxogramas</th>
                <th>Situacao</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {lista.data?.map((g) => (
                <tr key={g.id}>
                  <td className="mono">{g.codigo}</td>
                  <td>{g.nome}</td>
                  <td>{g.descricao}</td>
                  <td>{g.quantidadeFluxogramas}</td>
                  <td>{g.ativo ? 'Ativo' : 'Inativo'}</td>
                  <td>
                    <div className="botoes">
                      <button type="button" className="discreto" onClick={() => editar(g)}>
                        Editar
                      </button>
                      <button
                        type="button"
                        className="perigo"
                        disabled={remover.isPending}
                        onClick={() => remover.mutate(g.id)}
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
