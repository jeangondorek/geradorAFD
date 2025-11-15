package org.acme.afd.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@RequiredArgsConstructor
public class Automaton {
    private final Set<State> states;
    private final Set<String> alphabet;
    private State initialState;
    private final Set<State> finalStates;
    private final List<Transition> transitions;
    private final Map<String, Set<Transition>> transitionTable;
}
