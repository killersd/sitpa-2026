import axios, { AxiosError } from 'axios';

const CHAVE_TOKEN = 'sitpa.token';

export const cliente = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

export function guardarToken(token: string) {
  localStorage.setItem(CHAVE_TOKEN, token);
}

export function lerToken(): string | null {
  return localStorage.getItem(CHAVE_TOKEN);
}

export function descartarToken() {
  localStorage.removeItem(CHAVE_TOKEN);
}

cliente.interceptors.request.use((config) => {
  const token = lerToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Disparado quando o token expira, para que a aplicacao volte ao login sem recarregar a pagina. */
export const EVENTO_SESSAO_EXPIRADA = 'sitpa:sessao-expirada';

cliente.interceptors.response.use(
  (resposta) => resposta,
  (erro: AxiosError) => {
    if (erro.response?.status === 401 && lerToken()) {
      descartarToken();
      window.dispatchEvent(new Event(EVENTO_SESSAO_EXPIRADA));
    }
    return Promise.reject(erro);
  },
);

interface CorpoErro {
  mensagem?: string;
  erro?: string;
  camposInvalidos?: Record<string, string>;
  detalhes?: string[];
}

/**
 * Traduz o erro da API em uma frase util para quem esta na tela.
 *
 * O backend devolve mensagens de dominio ja em portugues; aqui so escolhemos a mais especifica
 * disponivel, para nao mostrar "Request failed with status code 409" a um profissional de saude.
 */
export function mensagemDeErro(erro: unknown): string {
  if (!axios.isAxiosError(erro)) {
    return erro instanceof Error ? erro.message : 'Erro inesperado.';
  }
  const corpo = erro.response?.data as CorpoErro | undefined;
  if (corpo?.camposInvalidos) {
    const campos = Object.entries(corpo.camposInvalidos)
      .map(([campo, motivo]) => `${campo}: ${motivo}`)
      .join('; ');
    if (campos) return campos;
  }
  if (corpo?.detalhes?.length) {
    return `${corpo.mensagem ?? 'Erro'} ${corpo.detalhes.join(' | ')}`;
  }
  if (corpo?.mensagem) return corpo.mensagem;
  if (erro.code === 'ERR_NETWORK') return 'Nao foi possivel falar com o servidor.';
  return erro.message;
}
