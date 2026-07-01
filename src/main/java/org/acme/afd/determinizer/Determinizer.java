package org.acme.afd.determinizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Implementa o algoritmo de construcao de subconjuntos (subset construction)
 * para converter um AFND em um AFD equivalente.
 * Adiciona automaticamente o estado de erro/sumidouro X para transicoes indefinidas.
 */
@ApplicationScoped
public class Determinizer {

    public static final String SINK_STATE_NAME = "X";

    /** Executa a determinizacao do AFND, retornando o AFD equivalente. */
    public Automaton determinize(Automaton afnd) {
        // Cria o AFD vazio com o mesmo alfabeto do AFND
        Automaton afd = Automaton.builder()
                .states(new LinkedHashSet<>())
                .alphabet(afnd.getAlphabet())
                .transitions(new ArrayList<>())
                .build();

        // Mapeia conjuntos de estados do AFND para estados unicos do AFD
        Map<Set<State>, State> mappedStates = new HashMap<>();

        // Fila para processar os conjuntos de estados pendentes (BFS)
        Queue<Set<State>> queue = new LinkedList<>();

        // Inicializa com o conjunto contendo apenas o estado inicial do AFND
        Set<State> afndInitialSet = new HashSet<>();
        afndInitialSet.add(afnd.getInitialState());

        State afdInitialState = createStateForSet(afndInitialSet);

        afd.getStates().add(afdInitialState);
        afd.setInitialState(afdInitialState);

        mappedStates.put(afndInitialSet, afdInitialState);
        queue.add(afndInitialSet);

        // Processa cada conjunto de estados ate que todos tenham sido explorados
        while (!queue.isEmpty()) {
            Set<State> currentAfndStates = queue.poll();
            State sourceAfdState = mappedStates.get(currentAfndStates);

            // Para cada simbolo do alfabeto, calcula o conjunto de estados alcancaveis
            for (String symbol : afd.getAlphabet()) {
                Set<State> targetAfndStates = new HashSet<>();
                for (State s : currentAfndStates) {
                    targetAfndStates.addAll(getTargets(afnd, s, symbol));
                }

                State targetAfdState;

                // Se nao ha destinos, direciona para o estado de erro (sumidouro)
                if (targetAfndStates.isEmpty()) {
                    targetAfdState = getOrCreateSinkState(afd, mappedStates);
                } else {
                    targetAfdState = mappedStates.get(targetAfndStates);

                    if (targetAfdState == null) {
                        targetAfdState = createStateForSet(targetAfndStates);
                        afd.getStates().add(targetAfdState);
                        mappedStates.put(targetAfndStates, targetAfdState);
                        queue.add(targetAfndStates);
                    }
                }

                afd.getTransitions().add(Transition.builder()
                        .source(sourceAfdState)
                        .symbol(symbol)
                        .target(targetAfdState)
                        .build());
            }
        }

        return afd;
    }

    /** Retorna todos os estados destino no AFND a partir de um estado e simbolo. */
    private Set<State> getTargets(Automaton afnd, State source, String symbol) {
        Set<State> targets = new HashSet<>();
        if (symbol == null || symbol.isEmpty()) {
            return targets;
        }

        for (Transition t : afnd.getTransitions()) {
            if (t.getSource().equals(source) && symbol.equals(t.getSymbol())) {
                targets.add(t.getTarget());
            }
        }
        return targets;
    }

    /** Cria um novo estado do AFD representando um conjunto de estados do AFND. */
    private State createStateForSet(Set<State> stateSet) {
        // O estado e final se qualquer estado do conjunto original for final
        boolean isFinal = stateSet.stream().anyMatch(State::isFinal);

        String label;
        String tokenName;
        if (stateSet.size() == 1) {
            label = stateSet.iterator().next().getLabel();
            tokenName = stateSet.iterator().next().getTokenName();
        } else {
            label = stateSet.stream()
                    .map(State::getLabel)
                    .sorted()
                    .collect(Collectors.joining("-"));
            tokenName = stateSet.stream()
                    .map(State::getTokenName)
                    .sorted()
                    .collect(Collectors.joining("-"));
        }

        return State.builder()
                .label(label)
                .isFinal(isFinal)
                .tokenName(tokenName)
                .build();
    }

    /** Obtem ou cria o estado sumidouro (X), cujas transicoes apontam para ele mesmo. */
    private State getOrCreateSinkState(Automaton afd, Map<Set<State>, State> mappedStates) {
        Set<State> emptySet = Collections.emptySet();
        State sinkState = mappedStates.get(emptySet);

        if (sinkState == null) {
            sinkState = State.builder()
                    .label("")
                    .tokenName(SINK_STATE_NAME)
                    .isFinal(false)
                    .build();

            afd.getStates().add(sinkState);
            mappedStates.put(emptySet, sinkState);

            for (String symbol : afd.getAlphabet()) {
                afd.getTransitions().add(Transition.builder()
                        .source(sinkState)
                        .symbol(symbol)
                        .target(sinkState)
                        .build());
            }
        }
        return sinkState;
    }
}