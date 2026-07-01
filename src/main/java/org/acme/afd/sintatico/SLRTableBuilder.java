package org.acme.afd.sintatico;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Constroi a tabela de parsing SLR(1).
 * Usa BFS para explorar estados via closure + goto, popula as tabelas
 * ACTION e GOTO e detecta conflitos shift/reduce ou reduce/reduce.
 */
@ApplicationScoped
public class SLRTableBuilder {

    @Inject
    FirstFollowCalculator firstFollowCalculator;

    /** Resultado da construcao: tabela SLR, conjuntos FIRST/FOLLOW e gramatica utilizada. */
    public record BuildOutput(
            SLRTable table,
            FirstFollowCalculator.FirstFollow firstFollow,
            Grammar grammar) {
    }

    public BuildOutput build(Grammar grammar) {
        FirstFollowCalculator.FirstFollow firstFollow = firstFollowCalculator.calculate(grammar);

        List<Set<LR0Item>> states = new ArrayList<>();
        Map<Integer, Map<String, Integer>> transitions = new LinkedHashMap<>();

        // Cria o estado inicial I0 a partir do fecho do item [S' -> .S]
        Set<LR0Item> i0 = closure(Set.of(new LR0Item(0, 0)), grammar);
        states.add(i0);

        // BFS: explora cada estado, calcula GOTO para cada simbolo apos o ponto
        // e registra novos estados e transicoes
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(0);

        while (!queue.isEmpty()) {
            int i = queue.poll();
            Set<LR0Item> state = states.get(i);
            Map<String, Integer> stateTransitions = transitions.computeIfAbsent(i, k -> new LinkedHashMap<>());

            Set<String> symbols = symbolsAfterDot(state, grammar);
            for (String symbol : symbols) {
                Set<LR0Item> target = goTo(state, symbol, grammar);
                if (target.isEmpty()) {
                    continue;
                }

                int j = indexOfState(states, target);
                if (j < 0) {
                    states.add(target);
                    j = states.size() - 1;
                    queue.add(j);
                }

                stateTransitions.put(symbol, j);
            }
        }

        // Popula as tabelas ACTION e GOTO a partir dos itens de cada estado:
        // - Item com ponto antes de terminal -> SHIFT
        // - Item com ponto antes de nao-terminal -> GOTO
        // - Item completo (ponto no final) -> REDUCE nos terminais do FOLLOW
        // - Item completo do simbolo aumentado -> ACCEPT
        Map<Integer, Map<String, ParsingAction>> action = new LinkedHashMap<>();
        Map<Integer, Map<String, Integer>> goTo = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();

        for (int i = 0; i < states.size(); i++) {
            Set<LR0Item> state = states.get(i);

            for (LR0Item item : state) {
                Production p = grammar.productions().get(item.productionIndex());

                if (item.dotPosition() < p.right().size()) {
                    String nextSymbol = p.right().get(item.dotPosition());
                    Integer target = transitions.getOrDefault(i, Map.of()).get(nextSymbol);
                    if (target == null) {
                        continue;
                    }

                    if (grammar.isTerminal(nextSymbol)) {
                        putAction(action, i, nextSymbol, new ParsingAction(ActionType.SHIFT, target), conflicts);
                    } else if (grammar.isNonTerminal(nextSymbol)) {
                        goTo.computeIfAbsent(i, k -> new LinkedHashMap<>()).put(nextSymbol, target);
                    }
                    continue;
                }

                if (p.left().equals(grammar.augmentedStartSymbol())) {
                    putAction(action, i, "$", new ParsingAction(ActionType.ACCEPT, -1), conflicts);
                    continue;
                }

                Set<String> followSet = firstFollow.follow().getOrDefault(p.left(), Set.of());
                for (String terminal : followSet) {
                    putAction(action, i, terminal, new ParsingAction(ActionType.REDUCE, p.index()), conflicts);
                }
            }
        }

        return new BuildOutput(new SLRTable(states, transitions, action, goTo, conflicts), firstFollow, grammar);
    }

    // Insere uma acao na tabela ACTION com deteccao de conflito:
    // se ja existir uma acao diferente para o mesmo (estado, terminal), registra o conflito.
    private void putAction(
            Map<Integer, Map<String, ParsingAction>> action,
            int state,
            String terminal,
            ParsingAction newAction,
            List<String> conflicts) {

        Map<String, ParsingAction> row = action.computeIfAbsent(state, k -> new LinkedHashMap<>());
        ParsingAction current = row.get(terminal);
        if (current != null && !current.equals(newAction)) {
            conflicts.add("Conflito em I" + state + ", simbolo '" + terminal + "': " + current + " x " + newAction);
            return;
        }
        row.put(terminal, newAction);
    }

    // Coleta todos os simbolos que aparecem imediatamente apos o ponto
    // nos itens do estado (usados para calcular transicoes GOTO).
    private Set<String> symbolsAfterDot(Set<LR0Item> items, Grammar grammar) {
        Set<String> symbols = new LinkedHashSet<>();
        for (LR0Item item : items) {
            Production p = grammar.productions().get(item.productionIndex());
            if (item.dotPosition() < p.right().size()) {
                symbols.add(p.right().get(item.dotPosition()));
            }
        }
        return symbols;
    }

    // Fecho (closure) dos itens LR(0): para cada item cujo simbolo apos o ponto
    // e um nao-terminal, adiciona todos os itens iniciais das producoes desse nao-terminal.
    private Set<LR0Item> closure(Set<LR0Item> seed, Grammar grammar) {
        Set<LR0Item> closure = new LinkedHashSet<>(seed);

        boolean changed;
        do {
            changed = false;
            Set<LR0Item> toAdd = new LinkedHashSet<>();

            for (LR0Item item : closure) {
                Production p = grammar.productions().get(item.productionIndex());
                if (item.dotPosition() >= p.right().size()) {
                    continue;
                }

                String symbol = p.right().get(item.dotPosition());
                if (!grammar.isNonTerminal(symbol)) {
                    continue;
                }

                for (Production prod : grammar.productions()) {
                    if (prod.left().equals(symbol)) {
                        toAdd.add(new LR0Item(prod.index(), 0));
                    }
                }
            }

            if (closure.addAll(toAdd)) {
                changed = true;
            }
        } while (changed);

        return sortItems(closure);
    }

    // Funcao de transicao GOTO: avanca o ponto sobre o simbolo dado
    // e retorna o fecho do conjunto resultante.
    private Set<LR0Item> goTo(Set<LR0Item> state, String symbol, Grammar grammar) {
        Set<LR0Item> moved = new LinkedHashSet<>();
        for (LR0Item item : state) {
            Production p = grammar.productions().get(item.productionIndex());
            if (item.dotPosition() < p.right().size() && p.right().get(item.dotPosition()).equals(symbol)) {
                moved.add(new LR0Item(item.productionIndex(), item.dotPosition() + 1));
            }
        }
        if (moved.isEmpty()) {
            return Set.of();
        }
        return closure(moved, grammar);
    }

    private int indexOfState(List<Set<LR0Item>> states, Set<LR0Item> state) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(state)) {
                return i;
            }
        }
        return -1;
    }

    private Set<LR0Item> sortItems(Set<LR0Item> items) {
        List<LR0Item> sorted = items.stream()
                .sorted(Comparator.comparingInt(LR0Item::productionIndex).thenComparingInt(LR0Item::dotPosition))
                .toList();
        return new LinkedHashSet<>(sorted);
    }
}
