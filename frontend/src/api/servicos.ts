import { cliente } from './cliente';
import type {
  Classificacao,
  ClassificacaoRealizada,
  EstadoBaseRegras,
  EstatisticaClassificacao,
  Fluxograma,
  Grupo,
  Pagina,
  Regra,
  RespostaLogin,
  ResultadoTriagem,
  Sintoma,
  SintomaAssociado,
  TriagemRequest,
  Usuario,
} from './tipos';

// --- Autenticacao ---

export async function entrar(login: string, senha: string): Promise<RespostaLogin> {
  const { data } = await cliente.post<RespostaLogin>('/auth/login', { login, senha });
  return data;
}

export async function usuarioCorrente(): Promise<Usuario> {
  const { data } = await cliente.get<Usuario>('/auth/eu');
  return data;
}

// --- Grupos ---

export async function listarGrupos(apenasAtivos = false): Promise<Grupo[]> {
  const { data } = await cliente.get<Grupo[]>('/grupos', { params: { apenasAtivos } });
  return data;
}

export type GrupoEntrada = Pick<Grupo, 'codigo' | 'nome' | 'descricao' | 'ativo'>;

export const criarGrupo = (corpo: GrupoEntrada) => cliente.post<Grupo>('/grupos', corpo).then((r) => r.data);
export const atualizarGrupo = (id: number, corpo: GrupoEntrada) =>
  cliente.put<Grupo>(`/grupos/${id}`, corpo).then((r) => r.data);
export const excluirGrupo = (id: number) => cliente.delete<void>(`/grupos/${id}`).then(() => undefined);

// --- Classificacoes ---

export async function listarClassificacoes(apenasAtivas = false): Promise<Classificacao[]> {
  const { data } = await cliente.get<Classificacao[]>('/classificacoes', { params: { apenasAtivas } });
  return data;
}

export type ClassificacaoEntrada = Omit<Classificacao, 'id'>;

export const criarClassificacao = (corpo: ClassificacaoEntrada) =>
  cliente.post<Classificacao>('/classificacoes', corpo).then((r) => r.data);
export const atualizarClassificacao = (id: number, corpo: ClassificacaoEntrada) =>
  cliente.put<Classificacao>(`/classificacoes/${id}`, corpo).then((r) => r.data);
export const excluirClassificacao = (id: number) =>
  cliente.delete<void>(`/classificacoes/${id}`).then(() => undefined);

// --- Sintomas ---

export async function listarSintomas(apenasAtivos = false): Promise<Sintoma[]> {
  const { data } = await cliente.get<Sintoma[]>('/sintomas', { params: { apenasAtivos } });
  return data;
}

export interface SintomaEntrada {
  codigo: string;
  nome: string;
  descricao?: string;
  classificacaoId: number;
  ativo: boolean;
}

export const criarSintoma = (corpo: SintomaEntrada) => cliente.post<Sintoma>('/sintomas', corpo).then((r) => r.data);
export const atualizarSintoma = (id: number, corpo: SintomaEntrada) =>
  cliente.put<Sintoma>(`/sintomas/${id}`, corpo).then((r) => r.data);
export const excluirSintoma = (id: number) => cliente.delete<void>(`/sintomas/${id}`).then(() => undefined);

// --- Fluxogramas ---

export async function listarFluxogramas(grupoId?: number, apenasAtivos = false): Promise<Fluxograma[]> {
  const { data } = await cliente.get<Fluxograma[]>('/fluxogramas', { params: { grupoId, apenasAtivos } });
  return data;
}

export interface FluxogramaEntrada {
  codigo: string;
  nome: string;
  descricao?: string;
  grupoId: number;
  modeloDmn?: string;
  ativo: boolean;
}

export const criarFluxograma = (corpo: FluxogramaEntrada) =>
  cliente.post<Fluxograma>('/fluxogramas', corpo).then((r) => r.data);
export const atualizarFluxograma = (id: number, corpo: FluxogramaEntrada) =>
  cliente.put<Fluxograma>(`/fluxogramas/${id}`, corpo).then((r) => r.data);
export const excluirFluxograma = (id: number) => cliente.delete<void>(`/fluxogramas/${id}`).then(() => undefined);

// --- Associacao de sintomas ---

export async function listarSintomasDoFluxograma(fluxogramaId: number): Promise<SintomaAssociado[]> {
  const { data } = await cliente.get<SintomaAssociado[]>(`/fluxogramas/${fluxogramaId}/sintomas`);
  return data;
}

export const associarSintoma = (
  fluxogramaId: number,
  corpo: { sintomaId: number; classificacaoEspecificaId?: number | null; ordem?: number },
) => cliente.post<SintomaAssociado>(`/fluxogramas/${fluxogramaId}/sintomas`, corpo).then((r) => r.data);

export const atualizarAssociacao = (
  fluxogramaId: number,
  sintomaId: number,
  corpo: { sintomaId: number; classificacaoEspecificaId?: number | null; ordem?: number },
) => cliente.put<SintomaAssociado>(`/fluxogramas/${fluxogramaId}/sintomas/${sintomaId}`, corpo).then((r) => r.data);

export const desassociarSintoma = (fluxogramaId: number, sintomaId: number) =>
  cliente.delete<void>(`/fluxogramas/${fluxogramaId}/sintomas/${sintomaId}`).then(() => undefined);

// --- Triagem ---

export async function avaliarTriagem(corpo: TriagemRequest): Promise<ResultadoTriagem> {
  const { data } = await cliente.post<ResultadoTriagem>('/triagens/avaliar', corpo);
  return data;
}

export async function confirmarTriagem(corpo: TriagemRequest): Promise<ClassificacaoRealizada> {
  const { data } = await cliente.post<ClassificacaoRealizada>('/triagens', corpo);
  return data;
}

// --- Historico ---

export interface FiltroHistorico {
  de?: string;
  ate?: string;
  classificacaoCodigo?: string;
  fluxogramaId?: number;
  grupoId?: number;
  usuarioLogin?: string;
  identificadorPaciente?: string;
  pagina?: number;
  tamanho?: number;
}

export async function consultarHistorico(filtro: FiltroHistorico): Promise<Pagina<ClassificacaoRealizada>> {
  const params = Object.fromEntries(
    Object.entries(filtro).filter(([, valor]) => valor !== undefined && valor !== '' && valor !== null),
  );
  const { data } = await cliente.get<Pagina<ClassificacaoRealizada>>('/historico', { params });
  return data;
}

export async function estatisticas(): Promise<EstatisticaClassificacao[]> {
  const { data } = await cliente.get<EstatisticaClassificacao[]>('/historico/estatisticas');
  return data;
}

// --- Regras ---

export async function listarRegras(): Promise<Regra[]> {
  const { data } = await cliente.get<Regra[]>('/regras');
  return data;
}

export interface RegraEntrada {
  nome: string;
  descricao?: string;
  tipo: 'DRL' | 'DMN';
  conteudo: string;
  ativo: boolean;
}

export const criarRegra = (corpo: RegraEntrada) => cliente.post<Regra>('/regras', corpo).then((r) => r.data);
export const atualizarRegra = (id: number, corpo: RegraEntrada) =>
  cliente.put<Regra>(`/regras/${id}`, corpo).then((r) => r.data);
export const excluirRegra = (id: number) => cliente.delete<void>(`/regras/${id}`).then(() => undefined);

export async function estadoBaseRegras(): Promise<EstadoBaseRegras> {
  const { data } = await cliente.get<EstadoBaseRegras>('/regras/base');
  return data;
}

export async function recarregarBaseRegras(): Promise<EstadoBaseRegras> {
  const { data } = await cliente.post<EstadoBaseRegras>('/regras/base/recarregar');
  return data;
}
