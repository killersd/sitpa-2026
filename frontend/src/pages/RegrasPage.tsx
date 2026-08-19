import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import {
  atualizarRegra,
  criarRegra,
  estadoBaseRegras,
  excluirRegra,
  listarRegras,
  recarregarBaseRegras,
  type RegraEntrada,
} from '../api/servicos';
import type { Regra } from '../api/tipos';
import { Carregando, Erro, Informativo, Sucesso, Vazio, formatarMomento } from '../components/Comuns';

const MODELO_DRL = `package br.com.sitpa.rules;

import br.com.sitpa.rules.model.FatoTriagem;
import br.com.sitpa.rules.model.PropostaClassificacao;

global java.util.List propostas;

rule "Minha regra"
    salience 50
    when
        $f : FatoTriagem( fluxogramaCodigo == "CEFALEIA",
                          idadeMaiorOuIgualA(75) == true,
                          temSintoma("DOR_MODERADA") == true )
    then
        propostas.add(new PropostaClassificacao("AMARELO", drools.getRule().getName(),
                "Cefaleia em maior de 75 anos exige avaliacao antecipada."));
end
`;

const VAZIO: RegraEntrada = { nome: '', descricao: '', tipo: 'DRL', conteudo: MODELO_DRL, ativo: true };

export default function RegrasPage() {
  const queryClient = useQueryClient();
  const [emEdicao, setEmEdicao] = useState<Regra | null>(null);
  const [form, setForm] = useState<RegraEntrada>(VAZIO);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const lista = useQuery({ queryKey: ['regras'], queryFn: listarRegras });
  const base = useQuery({ queryKey: ['base-regras'], queryFn: estadoBaseRegras });

  function concluir(mensagem: string) {
    queryClient.invalidateQueries({ queryKey: ['regras'] });
    queryClient.invalidateQueries({ queryKey: ['base-regras'] });
    setSucesso(mensagem);
    setErro(null);
  }

  function falhar(e: unknown) {
    setErro(mensagemDeErro(e));
    setSucesso(null);
  }

  const salvar = useMutation({
    mutationFn: (corpo: RegraEntrada) => (emEdicao ? atualizarRegra(emEdicao.id, corpo) : criarRegra(corpo)),
    onSuccess: () => {
      concluir(emEdicao ? 'Regra atualizada e base recarregada.' : 'Regra criada e base recarregada.');
      cancelar();
    },
    onError: falhar,
  });

  const remover = useMutation({
    mutationFn: excluirRegra,
    onSuccess: () => { concluir('Regra removida e base recarregada.'); cancelar(); },
    onError: falhar,
  });

  const recarregar = useMutation({
    mutationFn: recarregarBaseRegras,
    onSuccess: (estado) => concluir(`Base recarregada: versao ${estado.versao}.`),
    onError: falhar,
  });

  function editar(r: Regra) {
    setEmEdicao(r);
    setForm({ nome: r.nome, descricao: r.descricao ?? '', tipo: r.tipo, conteudo: r.conteudo, ativo: r.ativo });
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
      <h1>Regras do motor</h1>
      <Informativo>
        As regras cadastradas aqui sao compiladas <strong>antes de serem gravadas</strong>. Se o DRL ou o DMN nao
        compilar, nada e salvo e a base que esta no ar continua intacta &mdash; uma regra com erro nunca derruba a
        triagem. Nenhuma alteracao exige reiniciar a aplicacao.
      </Informativo>

      <Erro mensagem={erro} />
      <Sucesso mensagem={sucesso} />

      <section className="cartao">
        <div className="entre">
          <h2 style={{ margin: 0 }}>Base de conhecimento no ar</h2>
          <button type="button" onClick={() => recarregar.mutate()} disabled={recarregar.isPending}>
            {recarregar.isPending ? 'Recarregando...' : 'Recarregar base'}
          </button>
        </div>
        {base.isLoading && <Carregando />}
        {base.isError && <Erro mensagem={mensagemDeErro(base.error)} />}
        {base.data && (
          <div className="grade" style={{ marginTop: '0.75rem' }}>
            <div>
              <label>Versao</label>
              <div>
                {base.data.versao} &middot; carregada em {formatarMomento(base.data.carregadaEm)}
              </div>
            </div>
            <div>
              <label>Regras DRL compiladas ({base.data.regrasDrl.length})</label>
              <ul className="suave" style={{ margin: 0, paddingLeft: '1.1rem' }}>
                {base.data.regrasDrl.map((r) => (
                  <li key={r}>{r}</li>
                ))}
              </ul>
            </div>
            <div>
              <label>Modelos DMN ({base.data.modelosDmn.length})</label>
              <ul className="suave" style={{ margin: 0, paddingLeft: '1.1rem' }}>
                {base.data.modelosDmn.map((m) => (
                  <li key={m}>{m}</li>
                ))}
              </ul>
            </div>
            <div>
              <label>Artefatos</label>
              <ul className="suave mono" style={{ margin: 0, paddingLeft: '1.1rem' }}>
                {base.data.artefatos.map((a) => (
                  <li key={a}>{a}</li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </section>

      <section className="cartao">
        <h2>{emEdicao ? `Editando ${emEdicao.nome}` : 'Nova regra'}</h2>
        <form onSubmit={enviar}>
          <div className="linha">
            <div className="campo" style={{ flex: '2 1 280px' }}>
              <label htmlFor="nome">Nome</label>
              <input id="nome" value={form.nome} onChange={(e) => setForm({ ...form, nome: e.target.value })} required />
            </div>
            <div className="campo" style={{ flex: '0 0 140px' }}>
              <label htmlFor="tipo">Tipo</label>
              <select
                id="tipo"
                value={form.tipo}
                onChange={(e) => setForm({ ...form, tipo: e.target.value as 'DRL' | 'DMN' })}
              >
                <option value="DRL">DRL</option>
                <option value="DMN">DMN</option>
              </select>
            </div>
            <div className="campo" style={{ flex: '2 1 280px' }}>
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
                <span className="rotulo">Ativa</span>
              </label>
            </div>
          </div>
          <div style={{ marginTop: '0.75rem' }}>
            <label htmlFor="conteudo">{form.tipo === 'DRL' ? 'Codigo DRL' : 'XML do modelo DMN'}</label>
            <textarea
              id="conteudo"
              className="codigo"
              value={form.conteudo}
              onChange={(e) => setForm({ ...form, conteudo: e.target.value })}
              spellCheck={false}
              required
            />
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="submit" className="primario" disabled={salvar.isPending}>
              {salvar.isPending ? 'Compilando e salvando...' : emEdicao ? 'Salvar e recarregar' : 'Criar e recarregar'}
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
        <h2>Regras cadastradas</h2>
        {lista.isLoading && <Carregando />}
        {lista.isError && <Erro mensagem={mensagemDeErro(lista.error)} />}
        {lista.data?.length === 0 && (
          <Vazio>Nenhuma regra no banco. O motor esta usando apenas os artefatos entregues com a aplicacao.</Vazio>
        )}
        {lista.data && lista.data.length > 0 && (
          <div className="tabela-rolavel">
            <table>
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Tipo</th>
                  <th>Descricao</th>
                  <th>Situacao</th>
                  <th>Atualizada</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {lista.data.map((r) => (
                  <tr key={r.id}>
                    <td>{r.nome}</td>
                    <td className="mono">{r.tipo}</td>
                    <td>{r.descricao}</td>
                    <td>{r.ativo ? 'Ativa' : 'Inativa'}</td>
                    <td>{formatarMomento(r.atualizadoEm)}</td>
                    <td>
                      <div className="botoes">
                        <button type="button" className="discreto" onClick={() => editar(r)}>
                          Editar
                        </button>
                        <button
                          type="button"
                          className="perigo"
                          disabled={remover.isPending}
                          onClick={() => remover.mutate(r.id)}
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
        )}
      </section>
    </>
  );
}
