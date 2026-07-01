package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Calcula os conjuntos FIRST e FOLLOW da gramatica usando iteracao de ponto fixo.
 * Repete o calculo ate que nenhum conjunto seja alterado, garantindo convergencia.
 */
@ApplicationScoped
public class FirstFollowCalculator {

    /** Resultado contendo os mapas de FIRST e FOLLOW para cada nao-terminal. */
    public record FirstFollow(Map<String, Set<String>> first, Map<String, Set<String>> follow) {
    }

    // Inicializa conjuntos vazios para cada nao-terminal, adiciona $ ao FOLLOW
    // do simbolo inicial e itera ate atingir ponto fixo.
    public FirstFollow calculate(Grammar grammar) {
        Map<String, Set<String>> first = initialize(grammar.nonTerminals());
        Map<String, Set<String>> follow = initialize(grammar.nonTerminals());

        follow.get(grammar.startSymbol()).add("$");

        boolean changed;
        do {
            changed = computeFirst(grammar, first) || computeFollow(grammar, first, follow);
        } while (changed);

        return new FirstFollow(first, follow);
    }

    private Map<String, Set<String>> initialize(Set<String> nonTerminals) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (String nt : nonTerminals) {
            map.put(nt, new LinkedHashSet<>());
        }
        return map;
    }

    // Percorre cada producao: se o lado direito e vazio, adiciona epsilon ao FIRST.
    // Caso contrario, propaga terminais e FIRST de nao-terminais, rastreando
    // simbolos anulaveis para decidir se epsilon deve ser incluido.
    private boolean computeFirst(Grammar grammar, Map<String, Set<String>> first) {
        boolean changed = false;

        for (Production p : grammar.productions()) {
            if (p.right().isEmpty()
                    || (p.right().size() == 1 && GrammarLoader.EPSILON.equals(p.right().get(0)))) {
                changed |= first.get(p.left()).add(GrammarLoader.EPSILON);
                continue;
            }

            boolean allNullable = true;
            for (String symbol : p.right()) {
                if (grammar.isTerminal(symbol)) {
                    changed |= first.get(p.left()).add(symbol);
                    allNullable = false;
                    break;
                }

                Set<String> symbolFirst = first.get(symbol);
                if (symbolFirst != null) {
                    for (String t : symbolFirst) {
                        if (!GrammarLoader.EPSILON.equals(t)) {
                            changed |= first.get(p.left()).add(t);
                        }
                    }
                    if (!symbolFirst.contains(GrammarLoader.EPSILON)) {
                        allNullable = false;
                        break;
                    }
                } else {
                    allNullable = false;
                    break;
                }
            }

            if (allNullable) {
                changed |= first.get(p.left()).add(GrammarLoader.EPSILON);
            }
        }

        return changed;
    }

    // Para cada nao-terminal B no lado direito: adiciona FIRST(beta) - {epsilon}
    // ao FOLLOW(B). Se beta e vazio ou anulavel, propaga FOLLOW(A) para FOLLOW(B).
    private boolean computeFollow(Grammar grammar, Map<String, Set<String>> first, Map<String, Set<String>> follow) {
        boolean changed = false;

        for (Production p : grammar.productions()) {
            List<String> right = p.right();
            for (int i = 0; i < right.size(); i++) {
                String b = right.get(i);
                if (!grammar.isNonTerminal(b)) {
                    continue;
                }

                List<String> beta = i + 1 < right.size() ? right.subList(i + 1, right.size()) : List.of();
                Set<String> firstBeta = firstOfSequence(beta, grammar, first);

                for (String x : firstBeta) {
                    if (!GrammarLoader.EPSILON.equals(x)) {
                        changed |= follow.get(b).add(x);
                    }
                }

                if (beta.isEmpty() || firstBeta.contains(GrammarLoader.EPSILON)) {
                    changed |= follow.get(b).addAll(follow.get(p.left()));
                }
            }
        }

        return changed;
    }

    // Calcula FIRST de uma sequencia de simbolos, percorrendo-os ate encontrar
    // um terminal ou um nao-terminal que nao seja anulavel.
    public Set<String> firstOfSequence(List<String> symbols, Grammar grammar, Map<String, Set<String>> first) {
        if (symbols.isEmpty()) {
            return new LinkedHashSet<>(Set.of(GrammarLoader.EPSILON));
        }

        Set<String> result = new LinkedHashSet<>();
        List<String> sequence = new ArrayList<>(symbols);
        boolean nullableSoFar = true;

        for (String s : sequence) {
            if (grammar.isTerminal(s)) {
                result.add(s);
                nullableSoFar = false;
                break;
            }

            Set<String> fs = first.getOrDefault(s, Set.of());
            for (String f : fs) {
                if (!GrammarLoader.EPSILON.equals(f)) {
                    result.add(f);
                }
            }

            if (!fs.contains(GrammarLoader.EPSILON)) {
                nullableSoFar = false;
                break;
            }
        }

        if (nullableSoFar) {
            result.add(GrammarLoader.EPSILON);
        }

        return result;
    }
}
