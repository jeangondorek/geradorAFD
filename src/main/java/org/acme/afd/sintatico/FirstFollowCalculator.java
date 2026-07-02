package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Calculador dos conjuntos FIRST (Primeiro) e FOLLOW (Seguir) para a gramatica.
 * Utiliza o algoritmo iterativo de ponto fixo. Ele repete os calculos continuamente
 * ate que nenhuma mudanca ocorra nos conjuntos, garantindo a convergencia dos valores.
 * 
 * Esses conjuntos sao fundamentais para determinar quais acoes tomar na tabela SLR(1)
 * (por exemplo, decidir em quais terminais aplicar uma reducao).
 */
@ApplicationScoped
public class FirstFollowCalculator {

    /** 
     * Estrutura de dados contendo os mapas calculados de FIRST e FOLLOW de cada nao-terminal.
     */
    public record FirstFollow(Map<String, Set<String>> first, Map<String, Set<String>> follow) {
    }

    /**
     * Executa o calculo completo de FIRST e FOLLOW da gramatica fornecida.
     * 
     * @param grammar A gramatica cujos simbolos serao analisados
     * @return O resultado consolidado FIRST e FOLLOW
     */
    public FirstFollow calculate(Grammar grammar) {
        // Inicializa conjuntos vazios para cada nao-terminal
        Map<String, Set<String>> first = initialize(grammar.nonTerminals());
        Map<String, Set<String>> follow = initialize(grammar.nonTerminals());

        // Regra inicial do FOLLOW: O simbolo inicial da gramatica contem sempre o delimitador de fim de sentenca '$'
        follow.get(grammar.startSymbol()).add("$");

        // Algoritmo de ponto fixo: repete o calculo ate que nenhuma alteracao aconteca em nenhum dos dois conjuntos
        boolean changed;
        do {
            changed = computeFirst(grammar, first) || computeFollow(grammar, first, follow);
        } while (changed);

        return new FirstFollow(first, follow);
    }

    /**
     * Auxiliar para criar conjuntos vazios ordenados (LinkedHashSet) para os nao-terminais.
     */
    private Map<String, Set<String>> initialize(Set<String> nonTerminals) {
        Map<String, Set<String>> map = new LinkedHashMap<>();
        for (String nt : nonTerminals) {
            map.put(nt, new LinkedHashSet<>());
        }
        return map;
    }

    /**
     * Calcula o FIRST de cada nao-terminal.
     * FIRST(A) e o conjunto de terminais que podem iniciar cadeias derivadas de A.
     */
    private boolean computeFirst(Grammar grammar, Map<String, Set<String>> first) {
        boolean changed = false;

        // Percorre cada uma das regras de producao da gramatica
        for (Production p : grammar.productions()) {
            // Se a producao for vazia (ex: A -> epsilon), entao epsilon pertence ao FIRST(A)
            if (p.right().isEmpty()
                    || (p.right().size() == 1 && GrammarLoader.EPSILON.equals(p.right().get(0)))) {
                changed |= first.get(p.left()).add(GrammarLoader.EPSILON);
                continue;
            }

            boolean allNullable = true;
            
            // Analisa cada simbolo do lado direito da producao da esquerda para a direita
            for (String symbol : p.right()) {
                // Se for um terminal, adiciona-o ao FIRST do pai e encerra a analise desta regra
                if (grammar.isTerminal(symbol)) {
                    changed |= first.get(p.left()).add(symbol);
                    allNullable = false;
                    break; // Nao continua analisando os simbolos seguintes desta regra
                }

                // Se for um nao-terminal, adiciona tudo do FIRST(symbol) - {epsilon} ao FIRST(pai)
                Set<String> symbolFirst = first.get(symbol);
                if (symbolFirst != null) {
                    for (String t : symbolFirst) {
                        if (!GrammarLoader.EPSILON.equals(t)) {
                            changed |= first.get(p.left()).add(t);
                        }
                    }
                    // Se o FIRST deste simbolo nao contem epsilon, ele nao e anulavel. 
                    // Logo, nao propagamos os proximos simbolos.
                    if (!symbolFirst.contains(GrammarLoader.EPSILON)) {
                        allNullable = false;
                        break;
                    }
                } else {
                    allNullable = false;
                    break;
                }
            }

            // Se todos os simbolos do lado direito da producao puderem derivar epsilon,
            // entao epsilon tambem pertence ao FIRST do pai.
            if (allNullable) {
                changed |= first.get(p.left()).add(GrammarLoader.EPSILON);
            }
        }

        return changed;
    }

    /**
     * Calcula o FOLLOW de cada nao-terminal.
     * FOLLOW(A) e o conjunto de terminais que podem aparecer imediatamente a direita de A.
     */
    private boolean computeFollow(Grammar grammar, Map<String, Set<String>> first, Map<String, Set<String>> follow) {
        boolean changed = false;

        // Varre cada producao para encontrar onde nao-terminais aparecem no lado direito
        for (Production p : grammar.productions()) {
            List<String> right = p.right();
            
            for (int i = 0; i < right.size(); i++) {
                String b = right.get(i);
                
                // So calcula o FOLLOW para variaveis Nao-Terminais
                if (!grammar.isNonTerminal(b)) {
                    continue;
                }

                // Encontra a sequencia de simbolos "beta" que vem apos "b" (ex: em A -> alpha b beta, pega beta)
                List<String> beta = i + 1 < right.size() ? right.subList(i + 1, right.size()) : List.of();
                
                // Calcula o FIRST de toda a cadeia beta
                Set<String> firstBeta = firstOfSequence(beta, grammar, first);

                // Regra 1: Tudo do FIRST(beta) - {epsilon} entra no FOLLOW(b)
                for (String x : firstBeta) {
                    if (!GrammarLoader.EPSILON.equals(x)) {
                        changed |= follow.get(b).add(x);
                    }
                }

                // Regra 2: Se beta for vazio ou se o FIRST(beta) contiver epsilon (anulavel),
                // entao tudo do FOLLOW(pai) entra no FOLLOW(b)
                if (beta.isEmpty() || firstBeta.contains(GrammarLoader.EPSILON)) {
                    changed |= follow.get(b).addAll(follow.get(p.left()));
                }
            }
        }

        return changed;
    }

    /**
     * Calcula o FIRST de uma cadeia (sequencia) de simbolos.
     * Utilizado principalmente para calcular o FIRST do sufixo beta durante o calculo do FOLLOW.
     * 
     * @param symbols Sequencia de simbolos
     * @param grammar Gramatica de contexto
     * @param first Tabela atual de FIRST dos nao-terminais
     * @return O conjunto FIRST resultante da cadeia
     */
    public Set<String> firstOfSequence(List<String> symbols, Grammar grammar, Map<String, Set<String>> first) {
        if (symbols.isEmpty()) {
            return new LinkedHashSet<>(Set.of(GrammarLoader.EPSILON));
        }

        Set<String> result = new LinkedHashSet<>();
        List<String> sequence = new ArrayList<>(symbols);
        boolean nullableSoFar = true;

        for (String s : sequence) {
            // Se encontrar terminal, adiciona e para a analise da sequencia
            if (grammar.isTerminal(s)) {
                result.add(s);
                nullableSoFar = false;
                break;
            }

            // Se for nao-terminal, adiciona FIRST(s) - {epsilon}
            Set<String> fs = first.getOrDefault(s, Set.of());
            for (String f : fs) {
                if (!GrammarLoader.EPSILON.equals(f)) {
                    result.add(f);
                }
            }

            // Se nao puder derivar vazio (epsilon), para a analise
            if (!fs.contains(GrammarLoader.EPSILON)) {
                nullableSoFar = false;
                break;
            }
        }

        // Se toda a sequencia for anulavel (todos os termos derivam epsilon), entao epsilon esta no FIRST
        if (nullableSoFar) {
            result.add(GrammarLoader.EPSILON);
        }

        return result;
    }
}
