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
 * Construtor da Tabela de Parsing SLR(1) (Simple LR).
 * Esta classe analisa a gramatica e mapeia os estados LR(0) usando o fecho (closure)
 * e a operacao de transição (goto). A partir desses estados e dos conjuntos FIRST e FOLLOW,
 * popula as tabelas ACTION (deslocamentos e reducoes) e GOTO (desvios de variaveis),
 * alem de detectar conflitos sintaticos de ambiguidade na gramatica.
 */
@ApplicationScoped
public class SLRTableBuilder {

    @Inject
    FirstFollowCalculator firstFollowCalculator;

    /** 
     * Record de Saida que encapsula a tabela de parsing construida, 
     * os conjuntos FIRST/FOLLOW e a gramatica normalizada usada.
     */
    public record BuildOutput(
            SLRTable table,
            FirstFollowCalculator.FirstFollow firstFollow,
            Grammar grammar) {
    }

    /**
     * Constroi a tabela SLR(1) a partir de uma gramatica fornecida.
     * 
     * @param grammar Gramatica de entrada
     * @return O objeto BuildOutput contendo a tabela gerada
     */
    public BuildOutput build(Grammar grammar) {
        // Passo 1: Calcula os conjuntos FIRST e FOLLOW de todos os nao-terminais
        FirstFollowCalculator.FirstFollow firstFollow = firstFollowCalculator.calculate(grammar);

        // Lista de estados da tabela. Cada estado e representado por um conjunto de itens LR(0)
        List<Set<LR0Item>> states = new ArrayList<>();
        
        // Mapa temporario para rastrear as transicoes brutas entre estados (origem -> (simbolo -> destino))
        Map<Integer, Map<String, Integer>> transitions = new LinkedHashMap<>();

        // Passo 2: Define o estado inicial I0. 
        // Ele e gerado a partir do fecho do item estendido [S' -> .S], ou seja, regra de indice 0 com ponto na posicao 0.
        Set<LR0Item> i0 = closure(Set.of(new LR0Item(0, 0)), grammar);
        states.add(i0);

        // Fila para realizar a busca em largura (BFS) no grafo de estados
        Queue<Integer> queue = new ArrayDeque<>();
        queue.add(0);

        // Passo 3: BFS para explorar todos os estados LR(0) possiveis
        while (!queue.isEmpty()) {
            int i = queue.poll();
            Set<LR0Item> state = states.get(i);
            Map<String, Integer> stateTransitions = transitions.computeIfAbsent(i, k -> new LinkedHashMap<>());

            // Identifica todos os simbolos (terminais e nao-terminais) que aparecem logo apos o ponto nos itens do estado
            Set<String> symbols = symbolsAfterDot(state, grammar);
            
            // Para cada simbolo, calcula o estado destino gerado pela funcao GOTO
            for (String symbol : symbols) {
                Set<LR0Item> target = goTo(state, symbol, grammar);
                if (target.isEmpty()) {
                    continue;
                }

                // Verifica se este conjunto de itens (estado) ja foi descoberto antes
                int j = indexOfState(states, target);
                if (j < 0) {
                    // Se for um novo estado, adiciona-o a lista e a fila de processamento
                    states.add(target);
                    j = states.size() - 1;
                    queue.add(j);
                }

                // Registra a transicao: I_i lendo 'symbol' vai para I_j
                stateTransitions.put(symbol, j);
            }
        }

        // Tabelas oficiais do analisador SLR:
        // ACTION: Determina se deslocamos (SHIFT), reduzimos (REDUCE), aceitamos (ACCEPT) ou se e erro.
        // GOTO: Controla as transicoes de estado apos uma reducao (desvios de nao-terminais).
        Map<Integer, Map<String, ParsingAction>> action = new LinkedHashMap<>();
        Map<Integer, Map<String, Integer>> goTo = new LinkedHashMap<>();
        List<String> conflicts = new ArrayList<>();

        // Passo 4: Popula as tabelas ACTION e GOTO analisando os itens de cada estado
        for (int i = 0; i < states.size(); i++) {
            Set<LR0Item> state = states.get(i);

            for (LR0Item item : state) {
                Production p = grammar.productions().get(item.productionIndex());

                // Caso A: O ponto NAO esta no final da regra (ex: A -> alpha . X beta)
                if (item.dotPosition() < p.right().size()) {
                    String nextSymbol = p.right().get(item.dotPosition());
                    Integer target = transitions.getOrDefault(i, Map.of()).get(nextSymbol);
                    if (target == null) {
                        continue;
                    }

                    // Se o simbolo depois do ponto e Terminal: acao e SHIFT (desloca e vai para o estado destino)
                    if (grammar.isTerminal(nextSymbol)) {
                        putAction(action, i, nextSymbol, new ParsingAction(ActionType.SHIFT, target), conflicts);
                    // Se o simbolo depois do ponto e Nao-Terminal: preenche a tabela GOTO
                    } else if (grammar.isNonTerminal(nextSymbol)) {
                        goTo.computeIfAbsent(i, k -> new LinkedHashMap<>()).put(nextSymbol, target);
                    }
                    continue;
                }

                // Caso B: O ponto esta no FINAL da regra (ex: A -> alpha .)
                // Subcaso B1: Se for a regra inicial estendida (S' -> S .), a acao para o terminal '$' e ACCEPT (sucesso)
                if (p.left().equals(grammar.augmentedStartSymbol())) {
                    putAction(action, i, "$", new ParsingAction(ActionType.ACCEPT, -1), conflicts);
                    continue;
                }

                // Subcaso B2: Regra de reducao comum (A -> alpha .). 
                // A reducao e aplicada para todos os terminais contidos no FOLLOW do lado esquerdo (FOLLOW(A)).
                Set<String> followSet = firstFollow.follow().getOrDefault(p.left(), Set.of());
                for (String terminal : followSet) {
                    putAction(action, i, terminal, new ParsingAction(ActionType.REDUCE, p.index()), conflicts);
                }
            }
        }

        return new BuildOutput(new SLRTable(states, transitions, action, goTo, conflicts), firstFollow, grammar);
    }

    /**
     * Insere uma acao na tabela ACTION do parser. 
     * Se ja existir uma acao cadastrada para o mesmo par (estado, terminal), 
     * isso indica que a gramatica e ambigua e gera um conflito (Shift/Reduce ou Reduce/Reduce).
     */
    private void putAction(
            Map<Integer, Map<String, ParsingAction>> action,
            int state,
            String terminal,
            ParsingAction newAction,
            List<String> conflicts) {

        Map<String, ParsingAction> row = action.computeIfAbsent(state, k -> new LinkedHashMap<>());
        ParsingAction current = row.get(terminal);
        
        // Se ja existe uma acao e ela difere da nova acao sendo inserida
        if (current != null && !current.equals(newAction)) {
            conflicts.add("Conflito em I" + state + ", simbolo '" + terminal + "': " + current + " x " + newAction);
            return; // Evita sobrescrever para registrar o primeiro conflito encontrado
        }
        row.put(terminal, newAction);
    }

    /**
     * Analisa todos os itens de um estado e retorna o conjunto de simbolos que
     * estao imediatamente posicionados a direita do ponto ('.').
     */
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

    /**
     * Calcula o fecho (closure) de um conjunto de itens LR(0).
     * Se um item possui o ponto antes de um Nao-Terminal B (ex: [A -> alpha . B beta]),
     * todas as producoes de B devem ser adicionadas ao fecho com o ponto no inicio [B -> . gamma].
     */
    private Set<LR0Item> closure(Set<LR0Item> seed, Grammar grammar) {
        Set<LR0Item> closure = new LinkedHashSet<>(seed);
        boolean changed;

        do {
            changed = false;
            Set<LR0Item> toAdd = new LinkedHashSet<>();

            for (LR0Item item : closure) {
                Production p = grammar.productions().get(item.productionIndex());
                // Se o ponto ja chegou ao final da regra, nao ha simbolo apos o ponto
                if (item.dotPosition() >= p.right().size()) {
                    continue;
                }

                String symbol = p.right().get(item.dotPosition());
                // Se o simbolo logo apos o ponto nao for uma variavel nao-terminal, ignora
                if (!grammar.isNonTerminal(symbol)) {
                    continue;
                }

                // Varre as producoes e adiciona as regras que definem esta variavel nao-terminal
                for (Production prod : grammar.productions()) {
                    if (prod.left().equals(symbol)) {
                        toAdd.add(new LR0Item(prod.index(), 0)); // Adiciona com o ponto no inicio (posicao 0)
                    }
                }
            }

            // Adiciona os novos itens ao fecho. Se mudou algo, repete
            if (closure.addAll(toAdd)) {
                changed = true;
            }
        } while (changed);

        return sortItems(closure);
    }

    /**
     * Calcula a transicao GOTO de um estado por um determinado simbolo.
     * Avança o ponto ('.') sobre o simbolo passado para todos os itens compativeis,
     * e entao retorna o fecho (closure) do conjunto resultante.
     */
    private Set<LR0Item> goTo(Set<LR0Item> state, String symbol, Grammar grammar) {
        Set<LR0Item> moved = new LinkedHashSet<>();
        
        for (LR0Item item : state) {
            Production p = grammar.productions().get(item.productionIndex());
            // Se o ponto nao estiver no fim e o proximo simbolo for exatamente o procurado
            if (item.dotPosition() < p.right().size() && p.right().get(item.dotPosition()).equals(symbol)) {
                // Avanca a posicao do ponto em 1 unidade
                moved.add(new LR0Item(item.productionIndex(), item.dotPosition() + 1));
            }
        }
        
        if (moved.isEmpty()) {
            return Set.of();
        }
        
        // Calcula o fecho a partir dos itens cujo ponto foi avancado
        return closure(moved, grammar);
    }

    /**
     * Procura o indice correspondente a um estado na lista de estados ja descobertos.
     * Retorna -1 se for um estado inedito.
     */
    private int indexOfState(List<Set<LR0Item>> states, Set<LR0Item> state) {
        for (int i = 0; i < states.size(); i++) {
            if (states.get(i).equals(state)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Ordena os itens LR(0) de um estado para manter uma representacao visual estavel
     * nos arquivos CSV. Ordena por indice da producao e depois pela posicao do ponto.
     */
    private Set<LR0Item> sortItems(Set<LR0Item> items) {
        List<LR0Item> sorted = items.stream()
                .sorted(Comparator.comparingInt(LR0Item::productionIndex).thenComparingInt(LR0Item::dotPosition))
                .toList();
        return new LinkedHashSet<>(sorted);
    }
}
