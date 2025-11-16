package org.acme.afd.determinizer;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class Determinizer {

    public static final String SINK_STATE_NAME = "ERROR";

    public Automaton determinize(Automaton afnd) {
        Automaton afd = Automaton.builder()
                .states(new LinkedHashSet<>())
                .alphabet(afnd.getAlphabet())
                .transitions(new ArrayList<>())
                .build();

        Map<Set<State>, State> mappedStates = new HashMap<>();

        Queue<Set<State>> queue = new LinkedList<>();

        Set<State> afndInitialSet = new HashSet<>();
        afndInitialSet.add(afnd.getInitialState());

        State afdInitialState = createStateForSet(afndInitialSet);

        afd.getStates().add(afdInitialState);
        afd.setInitialState(afdInitialState);

        mappedStates.put(afndInitialSet, afdInitialState);
        queue.add(afndInitialSet);

        while (!queue.isEmpty()) {
            Set<State> currentAfndStates = queue.poll();
            State sourceAfdState = mappedStates.get(currentAfndStates);

            for (String symbol : afd.getAlphabet()) {
                Set<State> targetAfndStates = new HashSet<>();
                for (State s : currentAfndStates) {
                    targetAfndStates.addAll(getTargets(afnd, s, symbol));
                }

                State targetAfdState;

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

    private State createStateForSet(Set<State> stateSet) {
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