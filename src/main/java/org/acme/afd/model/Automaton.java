package org.acme.afd.model;

import lombok.*;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Automaton {
    private Set<State> states;
    private Set<String> alphabet;
    private State initialState;
    private List<Transition> transitions;
}
