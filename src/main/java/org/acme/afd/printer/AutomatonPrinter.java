package org.acme.afd.printer;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import java.util.*;

public class AutomatonPrinter {

    public static void printTransitionTable(Automaton automaton, String title) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(80));

        Set<State> states = automaton.getStates();
        Set<String> alphabet = automaton.getAlphabet();

        System.out.print("Estado\t");
        for (String symbol : alphabet) {
            System.out.print(symbol + "\t");
        }
        System.out.println("(Final)");

        System.out.println("-".repeat(80));

        for (State state : states) {
            String stateLabel = state.getId();
            
            if (state.equals(automaton.getInitialState())) {
                stateLabel = "→ " + stateLabel;
            }
            if (state.isFinal()) {
                stateLabel += " *";
            }

            System.out.print(stateLabel + "\t");

            List<Transition> transitions =  automaton.getTransitions();

            if (transitions.isEmpty()) {
                System.out.print("-\t");
            } else {
                StringBuilder sb = new StringBuilder();
                for (Transition trans : transitions) {
                    if (!sb.isEmpty()) sb.append(",");
                    sb.append(trans.getTarget().getId());
                }
                System.out.print(sb + "\t");
            }

            if (state.isFinal() && state.getTokenName() != null) {
                System.out.print("(" + state.getTokenName() + ")");
            }

            System.out.println();
        }

        System.out.println("=".repeat(80));
        System.out.println("Legenda: → = Estado inicial, * = Estado final");
    }
}
