import type { ReactNode } from 'react';
import type { Classificacao } from '../api/tipos';

export function Erro({ mensagem }: { mensagem?: string | null }) {
  if (!mensagem) return null;
  return (
    <p className="aviso erro" role="alert">
      {mensagem}
    </p>
  );
}

export function Sucesso({ mensagem }: { mensagem?: string | null }) {
  if (!mensagem) return null;
  return (
    <p className="aviso sucesso" role="status">
      {mensagem}
    </p>
  );
}

export function Informativo({ children }: { children: ReactNode }) {
  return <p className="aviso informativo">{children}</p>;
}

export function Carregando({ children = 'Carregando...' }: { children?: ReactNode }) {
  return (
    <p className="vazio" role="status">
      {children}
    </p>
  );
}

export function Vazio({ children }: { children: ReactNode }) {
  return <p className="vazio">{children}</p>;
}

/** Etiqueta colorida do nivel de risco. A cor vem do cadastro, nunca de uma tabela fixa no codigo. */
export function EtiquetaRisco({ cor, nome }: { cor: string; nome: string }) {
  return (
    <span className="etiqueta-risco" style={{ background: cor }}>
      {nome}
    </span>
  );
}

export function descreverEspera(minutos: number): string {
  if (minutos <= 0) return 'Imediato';
  if (minutos < 60) return `${minutos} min`;
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return resto === 0 ? `${horas} h` : `${horas} h ${resto} min`;
}

export function descreverClassificacao(c: Classificacao): string {
  return `${c.nome} (${c.codigo})`;
}

export function formatarMomento(iso: string): string {
  return new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
}
