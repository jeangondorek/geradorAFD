package org.acme.afd.model;

import java.util.List;
import java.util.Set;

/**
 * Modelo que representa um automato finito (deterministico ou nao-deterministico).
 * Contem o conjunto de estados, o alfabeto de simbolos aceitos,
 * o estado inicial e a lista de transicoes entre estados.
 */
public class Automaton {
    /** Conjunto de estados do automato. */
    private Set<State> states;
    /** Alfabeto de simbolos reconhecidos. */
    private Set<String> alphabet;
    /** Estado inicial do automato. */
    private State initialState;
    /** Lista de transicoes que definem o comportamento do automato. */
    private List<Transition> transitions;


    public Automaton(Set<State> states, Set<String> alphabet, State initialState, List<Transition> transitions) {
        this.states = states;
        this.alphabet = alphabet;
        this.initialState = initialState;
        this.transitions = transitions;
    }

    public Set<State> getStates() {
        return states;
    }

    public void setStates(Set<State> states) {
        this.states = states;
    }

    public Set<String> getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(Set<String> alphabet) {
        this.alphabet = alphabet;
    }

    public State getInitialState() {
        return initialState;
    }

    public void setInitialState(State initialState) {
        this.initialState = initialState;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Set<State> states;
        private Set<String> alphabet;
        private State initialState;
        private List<Transition> transitions;

        public Builder states(Set<State> states) {
            this.states = states;
            return this;
        }

        public Builder alphabet(Set<String> alphabet) {
            this.alphabet = alphabet;
            return this;
        }

        public Builder initialState(State initialState) {
            this.initialState = initialState;
            return this;
        }

        public Builder transitions(List<Transition> transitions) {
            this.transitions = transitions;
            return this;
        }

        public Automaton build() {
            return new Automaton(states, alphabet, initialState, transitions);
        }
    }
}
