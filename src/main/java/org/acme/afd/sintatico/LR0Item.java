package org.acme.afd.sintatico;

/**
 * Representa um item LR(0), composto por uma producao e a posicao do ponto (dot).
 * O ponto indica ate onde a leitura da producao progrediu durante a analise sintatica.
 *
 * @param productionIndex indice da producao na lista de producoes da gramatica
 * @param dotPosition     posicao do ponto no lado direito da producao (0 = inicio)
 */
public record LR0Item(int productionIndex, int dotPosition) {
}
