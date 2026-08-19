import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { mensagemDeErro } from '../api/cliente';
import {
  atualizarClassificacao,
  criarClassificacao,
  excluirClassificacao,
  listarClassificacoes,
  type ClassificacaoEntrada,
} from '../api/servicos';
import type { Classificacao } from '../api/tipos';
import { Carregando, Erro, EtiquetaRisco, Informativo, Sucesso, descreverEspera } from '../components/Comuns';

const VAZIO: ClassificacaoEntrada = {
  codigo: '',
  nome: '',
  cor: '#1565c0',
  prioridade: 1,
  tempoMaximoEsperaMinutos: 60,
  localAtendimento: '',
  tipoAtendimento: '',
  ativo: true,
};

export default function ClassificacoesPage() {
  const queryClient = useQueryClient();
  const [emEdicao, setEmEdicao] = useState<Classificacao | null>(null);
  const [form, setForm] = useState<ClassificacaoEntrada>(VAZIO);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState<string | null>(null);

  const lista = useQuery({ queryKey: ['classificacoes'], queryFn: () => listarClassificacoes() });

  function aoConcluir(mensagem: string) {
    queryClient.invalidateQueries({ queryKey: ['classificacoes'] });
    setSucesso(mensagem);
    setErro(null);
    cancelar();
  }

  const salvar = useMutation({
    mutationFn: (corpo: ClassificacaoEntrada) =>
      emEdicao ? atualizarClassificacao(emEdicao.id, corpo) : criarClassificacao(corpo),
    onSuccess: () => aoConcluir(emEdicao ? 'Classificacao atualizada.' : 'Classificacao criada.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  const remover = useMutation({
    mutationFn: excluirClassificacao,
    onSuccess: () => aoConcluir('Classificacao excluida.'),
    onError: (e) => { setErro(mensagemDeErro(e)); setSucesso(null); },
  });

  function editar(c: Classificacao) {
    setEmEdicao(c);
    setForm({
      codigo: c.codigo,
      nome: c.nome,
      cor: c.cor,
      prioridade: c.prioridade,
      tempoMaximoEsperaMinutos: c.tempoMaximoEsperaMinutos,
      localAtendimento: c.localAtendimento,
      tipoAtendimento: c.tipoAtendimento,
      ativo: c.ativo,
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
      <h1>Niveis de risco</h1>
      <Informativo>
        A gravidade e definida pela <strong>prioridade</strong>: 1 e o mais grave. E ela, e nao o nome ou a cor, que o
        motor usa para eleger a classificacao vencedora, entao cada prioridade e unica no protocolo.
      </Informativo>

      <Erro mensagem={erro} />
      <Sucesso mensagem={sucesso} />

      <section className="cartao">
        <h2>{emEdicao ? `Editando ${emEdicao.nome}` : 'Nova classificacao'}</h2>
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
            <div className="campo" style={{ flex: '0 0 110px' }}>
              <label htmlFor="cor">Cor</label>
              <input id="cor" type="color" value={form.cor} onChange={(e) => setForm({ ...form, cor: e.target.value })} />
            </div>
            <div className="campo" style={{ flex: '0 0 130px' }}>
              <label htmlFor="prioridade">Prioridade</label>
              <input
                id="prioridade"
                type="number"
                min={1}
                value={form.prioridade}
                onChange={(e) => setForm({ ...form, prioridade: Number(e.target.value) })}
                required
              />
            </div>
            <div className="campo" style={{ flex: '0 0 160px' }}>
              <label htmlFor="espera">Espera (min)</label>
              <input
                id="espera"
                type="number"
                min={0}
                value={form.tempoMaximoEsperaMinutos}
                onChange={(e) => setForm({ ...form, tempoMaximoEsperaMinutos: Number(e.target.value) })}
                required
              />
            </div>
          </div>
          <div className="linha" style={{ marginTop: '0.75rem' }}>
            <div className="campo">
              <label htmlFor="local">Local de atendimento</label>
              <input
                id="local"
                value={form.localAtendimento}
                onChange={(e) => setForm({ ...form, localAtendimento: e.target.value })}
                required
              />
            </div>
            <div className="campo">
              <label htmlFor="tipo">Tipo de atendimento</label>
              <input
                id="tipo"
                value={form.tipoAtendimento}
                onChange={(e) => setForm({ ...form, tipoAtendimento: e.target.value })}
                required
              />
            </div>
            <div className="campo" style={{ flex: '0 0 140px' }}>
              <label>Situacao</label>
              <label className={`opcao${form.ativo ? ' marcada' : ''}`}>
                <input type="checkbox" checked={form.ativo} onChange={(e) => setForm({ ...form, ativo: e.target.checked })} />
                <span className="rotulo">Ativa</span>
              </label>
            </div>
          </div>
          <div className="botoes" style={{ marginTop: '1rem' }}>
            <button type="submit" className="primario" disabled={salvar.isPending}>
              {salvar.isPending ? 'Salvando...' : emEdicao ? 'Salvar alteracoes' : 'Criar classificacao'}
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
                <th>Prioridade</th>
                <th>Classificacao</th>
                <th>Codigo</th>
                <th>Espera</th>
                <th>Local</th>
                <th>Tipo</th>
                <th>Situacao</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {lista.data?.map((c) => (
                <tr key={c.id}>
                  <td>{c.prioridade}</td>
                  <td>
                    <EtiquetaRisco cor={c.cor} nome={c.nome} />
                  </td>
                  <td className="mono">{c.codigo}</td>
                  <td>{descreverEspera(c.tempoMaximoEsperaMinutos)}</td>
                  <td>{c.localAtendimento}</td>
                  <td>{c.tipoAtendimento}</td>
                  <td>{c.ativo ? 'Ativa' : 'Inativa'}</td>
                  <td>
                    <div className="botoes">
                      <button type="button" className="discreto" onClick={() => editar(c)}>
                        Editar
                      </button>
                      <button
                        type="button"
                        className="perigo"
                        disabled={remover.isPending}
                        onClick={() => remover.mutate(c.id)}
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
