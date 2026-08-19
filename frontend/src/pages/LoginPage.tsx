import { useState, type FormEvent } from 'react';
import { useAutenticacao } from '../auth/Autenticacao';
import { mensagemDeErro } from '../api/cliente';
import { Erro } from '../components/Comuns';

export default function LoginPage() {
  const { entrar } = useAutenticacao();
  const [login, setLogin] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      await entrar(login.trim(), senha);
    } catch (e) {
      setErro(mensagemDeErro(e));
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="tela-centralizada">
      <form className="cartao" onSubmit={aoEnviar}>
        <h1>SITPa</h1>
        <p className="suave">Sistema Inteligente de Triagem de Pacientes</p>

        <Erro mensagem={erro} />

        <div style={{ marginBottom: '0.75rem' }}>
          <label htmlFor="login">Usuario</label>
          <input
            id="login"
            value={login}
            onChange={(e) => setLogin(e.target.value)}
            autoComplete="username"
            autoFocus
            required
          />
        </div>

        <div style={{ marginBottom: '1rem' }}>
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            value={senha}
            onChange={(e) => setSenha(e.target.value)}
            autoComplete="current-password"
            required
          />
        </div>

        <button type="submit" className="primario" disabled={enviando} style={{ width: '100%' }}>
          {enviando ? 'Entrando...' : 'Entrar'}
        </button>

        <p className="suave" style={{ marginTop: '1rem', marginBottom: 0 }}>
          Ambiente de demonstracao: <span className="mono">admin / admin123</span> ou{' '}
          <span className="mono">triagem / triagem123</span>.
        </p>
      </form>
    </div>
  );
}
