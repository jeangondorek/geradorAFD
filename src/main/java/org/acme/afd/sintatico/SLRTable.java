package org.acme.afd.sintatico;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representa a tabela de analise SLR(1) completa.
 *
 * @param states      lista de estados (conjuntos de itens LR(0)) do automato
 * @param transitions mapa de transicoes do automato: estado x simbolo -> estado destino
 * @param action      tabela ACTION: estado x terminal -> acao (shift, reduce ou accept)
 * @param goTo        tabela GOTO: estado x nao terminal -> estado destino
 * @param conflicts   lista de conflitos detectados na construcao da tabela (shift-reduce, reduce-reduce)
 */
public record SLRTable(
        List<Set<LR0Item>> states,
        Map<Integer, Map<String, Integer>> transitions,
        Map<Integer, Map<String, ParsingAction>> action,
        Map<Integer, Map<String, Integer>> goTo,
        List<String> conflicts) {
}
