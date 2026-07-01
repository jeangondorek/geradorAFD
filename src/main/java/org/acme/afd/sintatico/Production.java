package org.acme.afd.sintatico;

import java.util.List;

/**
 * Representa uma producao de uma gramatica livre de contexto.
 *
 * @param index indice da producao na lista de producoes da gramatica
 * @param left  lado esquerdo (nao terminal) da producao
 * @param right lado direito da producao (lista de simbolos terminais e/ou nao terminais)
 */
public record Production(int index, String left, List<String> right) {
}
