package br.com.sitpa.domain;

/** Perfis de acesso do SITPa. */
public enum Perfil {

    /** Administra o protocolo: CRUDs, associacoes e regras. */
    ADMIN,

    /** Executa a triagem e consulta o historico. */
    TRIAGEM;

    /** Authority no formato esperado pelo Spring Security. */
    public String authority() {
        return "ROLE_" + name();
    }
}
