package org.acme.afd;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.acme.afd.controller.AFDController;
import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;

@ApplicationScoped
@AllArgsConstructor
public class InitializeProgram {

    private final AFDController controller;

    private static boolean initialized = false;

    @PostConstruct
    public void initialize() throws IOException {
        if (initialized) return;
        initialized = true;

        String inputFile = "entrada.txt";

        System.out.println("==== Inicializando programa ====");
        System.out.println("Lendo arquivo: " + inputFile + "\n");

        Automaton afnd = controller.processFileAfnd(inputFile);
        Automaton afd = controller.processFileAfd(afnd);

        System.out.println("\nGerando CSVs...");
        converterParaCSVSomenteEstados(afnd, "afnd.csv");
        if (afd != null) {
            converterParaCSVSomenteEstados(afd, "afd.csv");
        }
    }

    public static void converterParaCSVSomenteEstados(Automaton automaton, String fileName) {
        if (automaton == null) {
            System.out.println("O autômato fornecido é nulo.");
            return;
        }

        System.out.println("\n=== DEBUG: Transições registradas ===");
        for (Transition t : automaton.getTransitions()) {
            String source = (t.getSource() == null) ? "<NULL>" : t.getSource().getTokenName();
            String target = (t.getTarget() == null) ? "<NULL>" : t.getTarget().getTokenName();
            String symbol = (t.getSymbol() == null || t.getSymbol().isEmpty()) ? "ε" : t.getSymbol();
            System.out.printf("%s --(%s)--> %s%n", source, symbol, target);
        }

        System.out.println("\n=== DEBUG: Estados do Autômato ===");
        for (State s : automaton.getStates()) {
            System.out.printf("Estado: %s [label=%s, final=%s]%n", s.getTokenName(), s.getLabel(), s.isFinal());
        }

        List<String> alphabet = automaton.getAlphabet().stream()
                .filter(s -> s != null && !s.isEmpty())
                .toList();

        try (FileWriter writer = new FileWriter(fileName)) {


            writer.append("Estado");
            for (String symbol : alphabet) {
                writer.append(",").append(symbol);
            }
            writer.append("\n");

            for (State state : automaton.getStates()) {
                String stateDisplay = formatStateName(state, automaton.getInitialState());

                writer.append(stateDisplay);

                for (String symbol : alphabet) {
                    Set<String> targets = new LinkedHashSet<>();

                    // Percorrer todas as transições e filtrar as que partem deste estado
                    for (Transition t : automaton.getTransitions()) {
                        // Verificar se a transição parte deste estado
                        boolean isFromThisState = false;
                        if (t.getSource() != null && t.getSource().equals(state)) {
                            isFromThisState = true;
                        }
                        
                        // Se a transição parte deste estado e o símbolo coincide, adicionar o destino
                        if (isFromThisState && 
                            t.getSymbol() != null && 
                            t.getSymbol().equals(symbol) && 
                            t.getTarget() != null) {
                            targets.add(t.getTarget().getTokenName());
                        }
                    }

                    writer.append(",");
                    if (targets.isEmpty()) {
                        writer.append("--");
                    } else {
                        writer.append(String.join("-", targets));
                    }
                }
                writer.append("\n");
            }

            System.out.println("Arquivo CSV gerado em: " + fileName);

        } catch (IOException e) {
            System.err.println("Erro ao escrever CSV: " + e.getMessage());
        }
    }

    private static String formatStateName(State state, State initialState) {
        String name = state.getTokenName();
        if (state.equals(initialState)) {
            name = "-> " + name;
        }
        if (state.isFinal()) {
            name += "*";
        }
        return name;
    }
}