package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GrammarNormalizer {

    public Grammar normalize(Grammar grammar) {
        return factor(removeUselessProductions(grammar));
    }

    // Etapa 1: remove nao terminais que nao geram sentencas ou nao sao alcancaveis.
    private Grammar removeUselessProductions(Grammar grammar) {
        Set<String> productive = findProductiveNonTerminals(grammar);
        Set<String> reachable = findReachableNonTerminals(grammar, productive);
        List<Production> usefulProductions = new ArrayList<>();

        for (Production production : grammar.productions()) {
            if (!reachable.contains(production.left())) {
                continue;
            }
            if (production.right().stream().anyMatch(grammar::isNonTerminal)
                    && !productive.containsAll(production.right().stream().filter(grammar::isNonTerminal).toList())) {
                continue;
            }
            usefulProductions.add(production);
        }

        return rebuild(grammar.startSymbol(), usefulProductions);
    }

    private Set<String> findProductiveNonTerminals(Grammar grammar) {
        Set<String> productive = new LinkedHashSet<>();
        boolean changed;

        do {
            changed = false;
            for (Production production : grammar.productions()) {
                boolean rightIsProductive = production.right().stream()
                        .allMatch(symbol -> grammar.isTerminal(symbol) || productive.contains(symbol));
                if (rightIsProductive) {
                    changed |= productive.add(production.left());
                }
            }
        } while (changed);

        return productive;
    }

    private Set<String> findReachableNonTerminals(Grammar grammar, Set<String> productive) {
        Set<String> reachable = new LinkedHashSet<>();
        reachable.add(grammar.augmentedStartSymbol());
        reachable.add(grammar.startSymbol());

        boolean changed;
        do {
            changed = false;
            for (Production production : grammar.productions()) {
                if (!reachable.contains(production.left())) {
                    continue;
                }

                for (String symbol : production.right()) {
                    if (grammar.isNonTerminal(symbol) && productive.contains(symbol)) {
                        changed |= reachable.add(symbol);
                    }
                }
            }
        } while (changed);

        return reachable;
    }

    // Etapa 1: aplica fatoracao simples agrupando alternativas com prefixo comum.
    private Grammar factor(Grammar grammar) {
        Map<String, List<Production>> byLeft = new LinkedHashMap<>();
        for (Production production : grammar.productions()) {
            if (production.left().equals(grammar.augmentedStartSymbol())) {
                continue;
            }
            byLeft.computeIfAbsent(production.left(), key -> new ArrayList<>()).add(production);
        }

        List<Production> factored = new ArrayList<>();
        factored.add(new Production(0, grammar.augmentedStartSymbol(), List.of(grammar.startSymbol())));

        for (Map.Entry<String, List<Production>> entry : byLeft.entrySet()) {
            addFactoredProductions(entry.getKey(), entry.getValue(), factored, grammar.nonTerminals());
        }

        return rebuild(grammar.startSymbol(), factored);
    }

    private void addFactoredProductions(
            String left,
            List<Production> productions,
            List<Production> output,
            Set<String> existingNonTerminals) {

        Map<String, List<Production>> byFirstSymbol = new LinkedHashMap<>();
        List<Production> emptyProductions = new ArrayList<>();

        for (Production production : productions) {
            if (production.right().isEmpty()) {
                emptyProductions.add(production);
                continue;
            }
            byFirstSymbol.computeIfAbsent(production.right().get(0), key -> new ArrayList<>()).add(production);
        }

        output.addAll(emptyProductions);

        for (Map.Entry<String, List<Production>> group : byFirstSymbol.entrySet()) {
            List<Production> groupedProductions = group.getValue();
            if (groupedProductions.size() == 1) {
                output.add(groupedProductions.get(0));
                continue;
            }

            List<String> prefix = longestCommonPrefix(groupedProductions);
            if (prefix.isEmpty()) {
                output.addAll(groupedProductions);
                continue;
            }

            String helper = nextHelperName(left, existingNonTerminals, output);
            List<String> parentRight = new ArrayList<>(prefix);
            parentRight.add(helper);
            output.add(new Production(0, left, parentRight));

            for (Production production : groupedProductions) {
                List<String> suffix = production.right().subList(prefix.size(), production.right().size());
                output.add(new Production(0, helper, new ArrayList<>(suffix)));
            }
        }
    }

    private List<String> longestCommonPrefix(List<Production> productions) {
        List<String> prefix = new ArrayList<>(productions.get(0).right());

        for (Production production : productions) {
            int i = 0;
            while (i < prefix.size()
                    && i < production.right().size()
                    && prefix.get(i).equals(production.right().get(i))) {
                i++;
            }
            prefix = new ArrayList<>(prefix.subList(0, i));
        }

        return prefix;
    }

    private String nextHelperName(String left, Set<String> existingNonTerminals, List<Production> output) {
        int counter = 1;
        String candidate;
        do {
            candidate = left + "_F" + counter++;
        } while (existingNonTerminals.contains(candidate) || containsLeftSide(output, candidate));
        return candidate;
    }

    private boolean containsLeftSide(List<Production> productions, String left) {
        for (Production production : productions) {
            if (production.left().equals(left)) {
                return true;
            }
        }
        return false;
    }

    private Grammar rebuild(String startSymbol, List<Production> productions) {
        String augmentedStart = startSymbol + "'";
        Set<String> nonTerminals = new LinkedHashSet<>();
        for (Production production : productions) {
            nonTerminals.add(production.left());
        }

        List<Production> rebuiltProductions = new ArrayList<>();
        rebuiltProductions.add(new Production(0, augmentedStart, List.of(startSymbol)));

        int index = 1;
        for (Production production : productions) {
            if (production.left().equals(augmentedStart)) {
                continue;
            }
            rebuiltProductions.add(new Production(index++, production.left(), production.right()));
        }

        nonTerminals.add(augmentedStart);

        Set<String> terminals = new LinkedHashSet<>();
        for (Production production : rebuiltProductions) {
            for (String symbol : production.right()) {
                if (!nonTerminals.contains(symbol)) {
                    terminals.add(symbol);
                }
            }
        }
        terminals.add("$");

        return new Grammar(startSymbol, augmentedStart, rebuiltProductions, nonTerminals, terminals);
    }
}
