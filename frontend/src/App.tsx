import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { useAutenticacao } from './auth/Autenticacao';
import { Carregando } from './components/Comuns';
import LoginPage from './pages/LoginPage';
import TriagemPage from './pages/TriagemPage';
import HistoricoPage from './pages/HistoricoPage';
import ClassificacoesPage from './pages/ClassificacoesPage';
import GruposPage from './pages/GruposPage';
import FluxogramasPage from './pages/FluxogramasPage';
import SintomasPage from './pages/SintomasPage';
import AssociacaoPage from './pages/AssociacaoPage';
import RegrasPage from './pages/RegrasPage';

export default function App() {
  const { usuario, carregando, ehAdmin, sair } = useAutenticacao();

  if (carregando) {
    return <Carregando>Verificando sessao...</Carregando>;
  }

  if (!usuario) {
    return <LoginPage />;
  }

  const classeAtiva = ({ isActive }: { isActive: boolean }) => (isActive ? 'ativo' : undefined);

  return (
    <div className="app">
      <header className="cabecalho">
        <div className="marca">
          SITPa<span>Triagem de pacientes</span>
        </div>
        <nav className="navegacao">
          <NavLink to="/triagem" className={classeAtiva}>
            Triagem
          </NavLink>
          <NavLink to="/historico" className={classeAtiva}>
            Historico
          </NavLink>
          {ehAdmin && (
            <>
              <NavLink to="/classificacoes" className={classeAtiva}>
                Classificacoes
              </NavLink>
              <NavLink to="/grupos" className={classeAtiva}>
                Grupos
              </NavLink>
              <NavLink to="/fluxogramas" className={classeAtiva}>
                Fluxogramas
              </NavLink>
              <NavLink to="/sintomas" className={classeAtiva}>
                Sintomas
              </NavLink>
              <NavLink to="/associacoes" className={classeAtiva}>
                Associacoes
              </NavLink>
              <NavLink to="/regras" className={classeAtiva}>
                Regras
              </NavLink>
            </>
          )}
        </nav>
        <div className="usuario-atual">
          <span>
            {usuario.nome} &middot; {usuario.perfil === 'ADMIN' ? 'Administrador' : 'Triagem'}
          </span>
          <button type="button" onClick={sair}>
            Sair
          </button>
        </div>
      </header>

      <main className="conteudo">
        <Routes>
          <Route path="/triagem" element={<TriagemPage />} />
          <Route path="/historico" element={<HistoricoPage />} />
          {ehAdmin && <Route path="/classificacoes" element={<ClassificacoesPage />} />}
          {ehAdmin && <Route path="/grupos" element={<GruposPage />} />}
          {ehAdmin && <Route path="/fluxogramas" element={<FluxogramasPage />} />}
          {ehAdmin && <Route path="/sintomas" element={<SintomasPage />} />}
          {ehAdmin && <Route path="/associacoes" element={<AssociacaoPage />} />}
          {ehAdmin && <Route path="/regras" element={<RegrasPage />} />}
          <Route path="*" element={<Navigate to="/triagem" replace />} />
        </Routes>
      </main>

      <footer className="rodape">
        SITPa 2.0 &middot; O sistema apoia a decisao do profissional; a classificacao final e sempre dele.
      </footer>
    </div>
  );
}
