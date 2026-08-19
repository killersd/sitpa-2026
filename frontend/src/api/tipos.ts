/** Tipos espelhando os DTOs da API. Mantidos a mao para nao acoplar o build a um gerador. */

export type Perfil = 'ADMIN' | 'TRIAGEM';

export type OrigemDecisao = 'DRL' | 'DMN' | 'BASE' | 'NENHUMA';

export type TipoRegra = 'DRL' | 'DMN';

export interface Usuario {
  id: number;
  login: string;
  nome: string;
  perfil: Perfil;
}

export interface RespostaLogin {
  token: string;
  expiraEm: string;
  usuario: Usuario;
}

export interface Referencia {
  id: number;
  codigo: string;
  nome: string;
}

export interface Classificacao {
  id: number;
  codigo: string;
  nome: string;
  cor: string;
  prioridade: number;
  tempoMaximoEsperaMinutos: number;
  localAtendimento: string;
  tipoAtendimento: string;
  ativo: boolean;
}

export interface Grupo {
  id: number;
  codigo: string;
  nome: string;
  descricao?: string;
  ativo: boolean;
  quantidadeFluxogramas: number;
}

export interface Fluxograma {
  id: number;
  codigo: string;
  nome: string;
  descricao?: string;
  grupo: Referencia;
  modeloDmn?: string;
  ativo: boolean;
  quantidadeSintomas: number;
}

export interface Sintoma {
  id: number;
  codigo: string;
  nome: string;
  descricao?: string;
  classificacao: Classificacao;
  ativo: boolean;
}

export interface SintomaAssociado {
  associacaoId: number;
  sintomaId: number;
  codigo: string;
  nome: string;
  descricao?: string;
  classificacaoPadrao: Classificacao;
  classificacaoEspecifica?: Classificacao;
  classificacaoEfetiva: Classificacao;
  ordem: number;
  ativo: boolean;
}

export interface Proposta {
  classificacaoCodigo: string;
  classificacaoNome: string;
  cor: string;
  prioridade: number;
  origem: OrigemDecisao;
  regra?: string;
  justificativa?: string;
  vencedora: boolean;
}

export interface ResultadoTriagem {
  grupo: Referencia;
  fluxograma: Referencia;
  sintomasSelecionados: Referencia[];
  classificacao: Classificacao;
  origemDecisao: OrigemDecisao;
  regraAplicada?: string;
  justificativa?: string;
  propostas: Proposta[];
  versaoBaseRegras: number;
}

export interface TriagemRequest {
  grupoId: number;
  fluxogramaId: number;
  sintomasIds: number[];
  idadeAnos?: number | null;
  gestante?: boolean;
  identificadorPaciente?: string;
  observacoes?: string;
}

export interface ClassificacaoRealizada {
  id: number;
  realizadaEm: string;
  grupoNome: string;
  fluxogramaNome: string;
  classificacaoCodigo: string;
  classificacaoNome: string;
  classificacaoCor: string;
  classificacaoPrioridade: number;
  tempoMaximoEsperaMinutos: number;
  localAtendimento: string;
  tipoAtendimento: string;
  sintomas: string[];
  origemDecisao: OrigemDecisao;
  regraAplicada?: string;
  justificativa?: string;
  identificadorPaciente?: string;
  idadeAnos?: number;
  observacoes?: string;
  usuarioLogin: string;
}

export interface Pagina<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface EstatisticaClassificacao {
  codigo: string;
  nome: string;
  cor: string;
  prioridade: number;
  total: number;
  percentual: number;
}

export interface Regra {
  id: number;
  nome: string;
  descricao?: string;
  tipo: TipoRegra;
  conteudo: string;
  ativo: boolean;
  versao: number;
  atualizadoEm: string;
}

export interface EstadoBaseRegras {
  versao: number;
  carregadaEm: string;
  regrasDrl: string[];
  modelosDmn: string[];
  artefatos: string[];
}
