package org.acme.afd.determinizer;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import java.util.*;

@RequiredArgsConstructor
@ApplicationScoped
public class Determinizer {
    private final Automaton afd;
    private final Map<Set<State>, State> stateMapping;

    public Automaton determinize(Automaton afnd) {

        return afd;
    }

    public void printDeterminizationInfo() {
        System.out.println("\n===== DETERMINIZAÇÃO =====");
//        System.out.println("Estados AFND: " + afnd.getStateCount());
//        System.out.println("Estados AFD: " + afd.getStateCount());
//        System.out.println("Alfabeto: " + afd.getAlphabet());
//        System.out.println("Transições AFD: " + afd.getTransitionCount());
    }
}