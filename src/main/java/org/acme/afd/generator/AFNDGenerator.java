package org.acme.afd.generator;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@ApplicationScoped
public class AFNDGenerator {
    private final Automaton afnd;
    private int stateCounter;
    private final Map<String, String> grammars;
    private final List<String> tokens;

    private static final char SKIP_LETTER = 'S';
    private static final char[] LETTERS_EXCEPT_S;

    static {
        StringBuilder sb = new StringBuilder();
        for (char c = 'A'; c <= 'Z'; c++) {
            if (c == SKIP_LETTER) continue;
            sb.append(c);
        }
        LETTERS_EXCEPT_S = sb.toString().toCharArray();
    }

    public Automaton generate() {
        State initialState = createState("S");

        return afnd;
    }

    private State createState(String label) {
        State state = new State(label);
        stateCounter++;
        return state;
    }

    public void printAFND() {
        System.out.println("\n===== AFND GERADO =====");
//        System.out.println("Estados: " + afnd.getStateCount());
//        System.out.println("Alfabeto: " + afnd.getAlphabet());
//        System.out.println("Estado inicial: " + afnd.getInitialState());
//        System.out.println("Estados finais: " + afnd.getFinalStates());
//        System.out.println("Transições: " + afnd.getTransitionCount());
    }
}
