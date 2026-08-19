import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import {
  atualizarSintoma,
  criarSintoma,
  excluirSintoma,
  listarClassificacoes,
  listarSintomas,
  type SintomaEntrada,
} from '../api/servicos';
import type { Sintoma } from '../api/tipos';
import { Carregando, Erro, EtiquetaRisco, Informativo, Sucesso } from '../components/Comuns';

const VAZIO: SintomaEntrada = { codigo: '', nome: '', descricao: '', classificacaoId: 0, ativo: true };

export default function SintomasPage() {
  const queryClient = useQueryClient();
  const [emEdicao, setEmEdicao] = useState<Sintoma | null>(null);
  const [form, setForm] = useState<SintomaEntrada>(VAZIO);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const lista = useQuery({ queryKey: ['sintomas'], queryFn: () => listarSintomas() });
  const classificacoes = useQuery({ queryKey: ['classificacoes'], queryFn: () => listarClassificacoes() });

  function concluir(mensagem: string) {
    queryClient.invalidateQueries({ queryKey: ['sintomas'] });
    queryClient.invalidateQueries({ queryKey: ['fluxograma-sintomas'] });
    setSucesso(mensagem);
    setErro(null);
    cancelar();
  }

  const salvar = useMutation({
    mutationFn: (corpo: SintomaEntrada) => (emEdicao ? atualizarSintoma(emEdicao.id, corpo) : criarSintoma(corpo)),
    onSuccess: () => concluir(emEdicao ? 'Sintoma atualizado.' : 'Sintoma criado.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  const remover = useMutation({
    mutationFn: excluirSintoma,
    onSuccess: () => concluir('Sintoma excluido.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  function editar(s: Sintoma) {
    setEmEdicao(s);
    setForm({
      codigo: s.codigo,
      nome: s.nome,
      descricao: s.descricao ?? '',
      classificacaoId: s.classificacao.id,
      ativo: s.ativo,
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
    salvar.mutate(form);
  }

  return (
    <>
      <h1>Sintomas</h1>
      <Informativo>
        O codigo do sintoma e o nome pelo qual as regras DRL e as tabelas DMN se referem a ele. Mudar o codigo de um
        sintoma ja citado em uma regra faz a regra deixar de encontra-lo.
      </Informativo>

      <Erro mensagem={erro} />
      <Sucesso mensagem={sucesso} />

      <section className="cartao">
        <h2>{emEdicao ? `Editando ${emEdicao.nome}` : 'Novo sintoma'}</h2>
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
              <label htmlFor="classificacao">Classificacao padrao</label>
              <select
                id="classificacao"
                value={form.classificacaoId || ''}
                onChange={(e) => setForm({ ...form, classificacaoId: Number(e.target.value) })}
                required
              >
                <option value="">Selecione...</option>
                {classificacoes.data?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.prioridade} - {c.nome}
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
              placeholder="Texto que orienta o profissional na hora de marcar"
            />
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="submit" className="primario" disabled={salvar.isPending}>
              {salvar.isPending ? 'Salvando...' : emEdicao ? 'Salvar alteracoes' : 'Criar sintoma'}
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
                <th>Classificacao padrao</th>
                <th>Descricao</th>
                <th>Situacao</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {lista.data?.map((s) => (
                <tr key={s.id}>
                  <td className="mono">{s.codigo}</td>
                  <td>{s.nome}</td>
                  <td>
                    <EtiquetaRisco cor={s.classificacao.cor} nome={s.classificacao.nome} />
                  </td>
                  <td>{s.descricao}</td>
                  <td>{s.ativo ? 'Ativo' : 'Inativo'}</td>
                  <td>
                    <div className="botoes">
                      <button type="button" className="discreto" onClick={() => editar(s)}>
                        Editar
                      </button>
                      <button
                        type="button"
                        className="perigo"
                        disabled={remover.isPending}
                        onClick={() => remover.mutate(s.id)}
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
