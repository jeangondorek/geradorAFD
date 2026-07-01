package org.acme.afd.sintatico;

import java.util.List;
import java.util.Set;

/**
 * Representa uma Gramatica Livre de Contexto (GLC).
 *
 * @param startSymbol           simbolo inicial da gramatica original
 * @param augmentedStartSymbol  simbolo inicial aumentado (S') usado na construcao SLR
 * @param productions           lista ordenada de producoes da gramatica
 * @param nonTerminals          conjunto de nao terminais
 * @param terminals             conjunto de terminais
 */
public record Grammar(
        String startSymbol,
        String augmentedStartSymbol,
        List<Production> productions,
        Set<String> nonTerminals,
        Set<String> terminals) {

    // Verifica se o simbolo pertence ao conjunto de nao terminais
    public boolean isNonTerminal(String symbol) {
        return nonTerminals.contains(symbol);
    }

    // Verifica se o simbolo pertence ao conjunto de terminais
    public boolean isTerminal(String symbol) {
        return terminals.contains(symbol);
    }
}
