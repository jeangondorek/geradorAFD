package org.acme.afd.sintatico;

/**
 * Tipos de acao possiveis na tabela ACTION do parser SLR.
 */
public enum ActionType {
    /** Empilha o proximo token e avanca para um novo estado. */
    SHIFT,
    /** Reduz simbolos do topo da pilha aplicando uma producao. */
    REDUCE,
    /** Indica que a entrada foi aceita pela gramatica. */
    ACCEPT
}
