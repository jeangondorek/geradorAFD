package org.acme.afd;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import org.acme.afd.controller.AFDController;
import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@AllArgsConstructor
public class InitializeProgram {

    private final AFDController controller;

    @PostConstruct
    public void initialize() throws IOException {
        String inputFile = "entrada.txt";

        System.out.println("Lendo arquivo: " + inputFile + "\n");
        Automaton afnd = controller.processFileAfnd(inputFile);
        Automaton afd = controller.processFileAfd(afnd);

        System.out.println("\n\n");
        converterParaCSV(afnd);
        //converterParaCSV(afd);
    }

    private void converterParaCSV(Automaton automaton) {
        StringBuilder csv = new StringBuilder();

        // Ordem fixa do alfabeto
        List<String> alphabetOrder = List.of("s","e","n","t","a","o","i","u");

        // Cabeçalho
        csv.append("Estado,");
        for (String symbol : alphabetOrder) {
            csv.append(symbol).append(",");
        }
        csv.append("Final,Token\n");

        // Percorre os estados
        for (State state : automaton.getStates()) {
            csv.append(state.getTokenName()).append(",");

            for (String symbol : alphabetOrder) {
                Set<Transition> transitions = automaton.getTransitionTable()
                        .getOrDefault(state.getTokenName(), Collections.emptySet());

                List<String> targets = new ArrayList<>();
                for (Transition t : transitions) {
                    if (symbol.equals(t.getSymbol())) {
                        targets.add(t.getTarget().getTokenName());
                    }
                }

                if (targets.isEmpty()) {
                    csv.append("--,");
                } else {
                    // Se houver múltiplos estados-alvo, separa por |
                    csv.append(String.join("|", targets)).append(",");
                }
            }

            csv.append(state.isFinal() ? "SIM" : "NAO").append(",");
            csv.append(state.getTokenName() != null ? state.getTokenName() : "").append("\n");
        }

        // Exibir ou salvar
        System.out.println(csv.toString());
    }
}
