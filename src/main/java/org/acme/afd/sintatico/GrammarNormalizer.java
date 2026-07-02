package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Normaliza e fatora a gramatica sintatica.
 * A normalizacao consiste em remover producoes inuteis (regras que nunca 
 * geram sentencas de terminais ou nao sao alcancaveis a partir do simbolo inicial).
 * A fatoracao a esquerda tenta agrupar producoes que possuem prefixos comuns, 
 * ajudando a reduzir a quantidade de conflitos (Shift/Reduce e Reduce/Reduce) 
 * na geracao da tabela SLR.
 */
@ApplicationScoped
public class GrammarNormalizer {

    /**
     * Ponto de entrada para normalizar uma gramatica.
     * Primeiro remove simbolos inuteis, depois aplica fatoracao simples.
     * 
     * @param grammar A gramatica bruta original
     * @return Uma nova instancia de Grammar devidamente limpa e fatorada
     */
    public Grammar normalize(Grammar grammar) {
        return factor(removeUselessProductions(grammar));
    }

    /**
     * Etapa 1: Remove nao-terminais inuteis.
     * Um nao-terminal e util se for produtivo (deriva pelo menos uma cadeia de terminais)
     * E se for alcancavel (pode ser atingido a partir do simbolo inicial S).
     */
    private Grammar removeUselessProductions(Grammar grammar) {
        // Encontra nao-terminais que conseguem gerar cadeias validas de terminais
        Set<String> productive = findProductiveNonTerminals(grammar);
        
        // Encontra nao-terminais que podem ser acessados a partir do simbolo inicial
        Set<String> reachable = findReachableNonTerminals(grammar, productive);
        
        List<Production> usefulProductions = new ArrayList<>();

        // Filtra e mantem apenas producoes validas e uteis
        for (Production production : grammar.productions()) {
            // Se o lado esquerdo nao for alcancavel, ignora a producao
            if (!reachable.contains(production.left())) {
                continue;
            }
            // Se algum simbolo nao-terminal no lado direito nao for produtivo, ignora a producao
            if (production.right().stream().anyMatch(grammar::isNonTerminal)
                    && !productive.containsAll(production.right().stream().filter(grammar::isNonTerminal).toList())) {
                continue;
            }
            usefulProductions.add(production);
        }

        // Reconstrói a gramatica com as producoes limpas
        return rebuild(grammar.startSymbol(), usefulProductions);
    }

    /**
     * Algoritmo de Ponto Fixo para encontrar Nao-Terminais Produtivos.
     * Um nao-terminal e produtivo se existe uma regra dele que contem apenas 
     * terminais ou outros nao-terminais ja marcados como produtivos.
     */
    private Set<String> findProductiveNonTerminals(Grammar grammar) {
        Set<String> productive = new LinkedHashSet<>();
        boolean changed;

        do {
            changed = false;
            for (Production production : grammar.productions()) {
                // Checa se todos os simbolos do lado direito sao terminais ou ja sao marcados produtivos
                boolean rightIsProductive = production.right().stream()
                        .allMatch(symbol -> grammar.isTerminal(symbol) || productive.contains(symbol));
                
                if (rightIsProductive) {
                    // Adiciona o lado esquerdo ao conjunto produtivo. Se mudou algo, itera novamente.
                    changed |= productive.add(production.left());
                }
            }
        } while (changed); // Continua ate que nenhuma nova producao seja adicionada (convergencia)

        return productive;
    }

    /**
     * Algoritmo de Ponto Fixo para encontrar Nao-Terminais Alcancaveis.
     * Inicia a partir do simbolo inicial e propaga para todos os nao-terminais 
     * que aparecem nos lados direitos das producoes alcancaveis.
     */
    private Set<String> findReachableNonTerminals(Grammar grammar, Set<String> productive) {
        Set<String> reachable = new LinkedHashSet<>();
        // O simbolo inicial e o estendido sao sempre alcancaveis
        reachable.add(grammar.augmentedStartSymbol());
        reachable.add(grammar.startSymbol());

        boolean changed;
        do {
            changed = false;
            for (Production production : grammar.productions()) {
                // Se a regra pertence a um nao-terminal que nao e alcancavel, ignora por enquanto
                if (!reachable.contains(production.left())) {
                    continue;
                }

                // Qualquer nao-terminal valido presente no lado direito torna-se alcancavel
                for (String symbol : production.right()) {
                    if (grammar.isNonTerminal(symbol) && productive.contains(symbol)) {
                        changed |= reachable.add(symbol);
                    }
                }
            }
        } while (changed); // Continua ate que nenhum novo simbolo seja marcado (ponto fixo)

        return reachable;
    }

    /**
     * Etapa 2: Fatoracao Simples a Esquerda.
     * Agrupa as producoes que comecam com o mesmo simbolo para criar novos 
     * nao-terminais auxiliares (ex: A -> a B | a C vira A -> a A_F1; A_F1 -> B | C).
     */
    private Grammar factor(Grammar grammar) {
        // Agrupa as regras de producao pelo simbolo no lado esquerdo
        Map<String, List<Production>> byLeft = new LinkedHashMap<>();
        for (Production production : grammar.productions()) {
            // Ignora a producao inicial estendida (S' -> S)
            if (production.left().equals(grammar.augmentedStartSymbol())) {
                continue;
            }
            byLeft.computeIfAbsent(production.left(), key -> new ArrayList<>()).add(production);
        }

        List<Production> factored = new ArrayList<>();
        // Adiciona a regra inicial estendida na primeira posicao (indice 0)
        factored.add(new Production(0, grammar.augmentedStartSymbol(), List.of(grammar.startSymbol())));

        // Aplica a fatoracao para cada grupo de producoes
        for (Map.Entry<String, List<Production>> entry : byLeft.entrySet()) {
            addFactoredProductions(entry.getKey(), entry.getValue(), factored, grammar.nonTerminals());
        }

        return rebuild(grammar.startSymbol(), factored);
    }

    /**
     * Analisa as producoes de um mesmo nao-terminal e fatora as que iniciam
     * com o mesmo simbolo (prefixo).
     */
    private void addFactoredProductions(
            String left,
            List<Production> productions,
            List<Production> output,
            Set<String> existingNonTerminals) {

        // Agrupa producoes pelo primeiro simbolo do seu lado direito
        Map<String, List<Production>> byFirstSymbol = new LinkedHashMap<>();
        List<Production> emptyProductions = new ArrayList<>();

        for (Production production : productions) {
            // Se for producao vazia (epsilon), isola
            if (production.right().isEmpty()) {
                emptyProductions.add(production);
                continue;
            }
            // Agrupa pelo primeiro simbolo do lado direito
            byFirstSymbol.computeIfAbsent(production.right().get(0), key -> new ArrayList<>()).add(production);
        }

        // Regras vazias sao adicionadas diretamente sem fatoracao
        output.addAll(emptyProductions);

        // Processa cada grupo de primeiro simbolo
        for (Map.Entry<String, List<Production>> group : byFirstSymbol.entrySet()) {
            List<Production> groupedProductions = group.getValue();
            
            // Se so existe uma regra com esse primeiro simbolo, nao precisa fatorar
            if (groupedProductions.size() == 1) {
                output.add(groupedProductions.get(0));
                continue;
            }

            // Calcula o maior prefixo comum de simbolos entre as regras deste grupo
            List<String> prefix = longestCommonPrefix(groupedProductions);
            
            // Se nao houver prefixo comum util, adiciona todas as producoes originais
            if (prefix.isEmpty()) {
                output.addAll(groupedProductions);
                continue;
            }

            // Cria um nome de variavel nao-terminal auxiliar unico (ex: PROGRAMA_F1)
            String helper = nextHelperName(left, existingNonTerminals, output);
            
            // Cria a nova producao do pai apontando para o prefixo + o novo auxiliar (A -> prefixo HELPER)
            List<String> parentRight = new ArrayList<>(prefix);
            parentRight.add(helper);
            output.add(new Production(0, left, parentRight));

            // Cria as producoes derivadas do auxiliar com os sufixos restantes (HELPER -> sufixo)
            for (Production production : groupedProductions) {
                List<String> suffix = production.right().subList(prefix.size(), production.right().size());
                output.add(new Production(0, helper, new ArrayList<>(suffix)));
            }
        }
    }

    /**
     * Encontra o maior prefixo comum entre as listas de simbolos no lado direito das producoes.
     */
    private List<String> longestCommonPrefix(List<Production> productions) {
        List<String> prefix = new ArrayList<>(productions.get(0).right());

        for (Production production : productions) {
            int i = 0;
            // Percorre ate encontrar disparidade entre os simbolos correspondentes das regras
            while (i < prefix.size()
                    && i < production.right().size()
                    && prefix.get(i).equals(production.right().get(i))) {
                i++;
            }
            // Corta o prefixo comum ate onde encontrou correspondencia
            prefix = new ArrayList<>(prefix.subList(0, i));
        }

        return prefix;
    }

    /**
     * Gera um nome unico para o nao-terminal auxiliar gerado durante a fatoracao (ex: NOME_F1, NOME_F2).
     */
    private String nextHelperName(String left, Set<String> existingNonTerminals, List<Production> output) {
        int counter = 1;
        String candidate;
        do {
            candidate = left + "_F" + counter++;
        } while (existingNonTerminals.contains(candidate) || containsLeftSide(output, candidate));
        return candidate;
    }

    /**
     * Auxiliar para checar se a lista temporaria de producoes ja possui o lado esquerdo candidato.
     */
    private boolean containsLeftSide(List<Production> productions, String left) {
        for (Production production : productions) {
            if (production.left().equals(left)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reconstrói o objeto Grammar final enumerando de forma sequencial (0..N) 
     * e remapeando os conjuntos finais de Terminais e Nao-Terminais.
     */
    private Grammar rebuild(String startSymbol, List<Production> productions) {
        String augmentedStart = startSymbol + "'";
        Set<String> nonTerminals = new LinkedHashSet<>();
        
        // Coleta todos os nao-terminais restantes do lado esquerdo
        for (Production production : productions) {
            nonTerminals.add(production.left());
        }

        List<Production> rebuiltProductions = new ArrayList<>();
        // Garante a regra estendida no indice 0
        rebuiltProductions.add(new Production(0, augmentedStart, List.of(startSymbol)));

        // Reindexa todas as outras producoes sequencialmente a partir de 1
        int index = 1;
        for (Production production : productions) {
            if (production.left().equals(augmentedStart)) {
                continue;
            }
            rebuiltProductions.add(new Production(index++, production.left(), production.right()));
        }

        nonTerminals.add(augmentedStart);

        // Recalcula o conjunto de terminais
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
