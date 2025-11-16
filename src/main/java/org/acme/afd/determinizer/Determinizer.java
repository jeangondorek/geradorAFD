package org.acme.afd.determinizer;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.afd.model.Automaton;

@ApplicationScoped
public class Determinizer {

    public Automaton determinize(Automaton afnd) {

        return null;
    }

    public void printDeterminizationInfo() {
        System.out.println("\n===== DETERMINIZAÇÃO =====");
    }
}