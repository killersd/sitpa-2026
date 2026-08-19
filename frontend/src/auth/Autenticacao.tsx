import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { descartarToken, EVENTO_SESSAO_EXPIRADA, guardarToken, lerToken } from '../api/cliente';
import { entrar as entrarNaApi, usuarioCorrente } from '../api/servicos';
import type { Usuario } from '../api/tipos';

interface ContextoAutenticacao {
  usuario: Usuario | null;
  carregando: boolean;
  ehAdmin: boolean;
  entrar: (login: string, senha: string) => Promise<void>;
  sair: () => void;
}

const Contexto = createContext<ContextoAutenticacao | null>(null);

export function ProvedorAutenticacao({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);

  const sair = useCallback(() => {
    descartarToken();
    setUsuario(null);
  }, []);

  // Ao abrir a aplicacao com um token guardado, confirmamos com o servidor quem e o usuario
  // em vez de confiar no que esta no localStorage: o token pode ter expirado ou o perfil mudado.
  useEffect(() => {
    if (!lerToken()) {
      setCarregando(false);
      return;
    }
    usuarioCorrente()
      .then(setUsuario)
      .catch(() => descartarToken())
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => {
    const aoExpirar = () => setUsuario(null);
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    return () => window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
  }, []);

  const entrar = useCallback(async (login: string, senha: string) => {
    const resposta = await entrarNaApi(login, senha);
    guardarToken(resposta.token);
    setUsuario(resposta.usuario);
  }, []);

  const valor = useMemo<ContextoAutenticacao>(
    () => ({ usuario, carregando, ehAdmin: usuario?.perfil === 'ADMIN', entrar, sair }),
    [usuario, carregando, entrar, sair],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useAutenticacao(): ContextoAutenticacao {
  const contexto = useContext(Contexto);
  if (!contexto) {
    throw new Error('useAutenticacao precisa estar dentro de ProvedorAutenticacao.');
  }
  return contexto;
}
