package org.acme.afd.recognizer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.SymbolTableEntry;
import org.acme.afd.model.Transition;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Recognizer {

    public void recognize(Automaton afd, String filePath) throws IOException {
        System.out.println("\n==== Iniciando Reconhecedor Léxico ====");
        System.out.println("Analisando arquivo: " + filePath);

        List<String> tape = new ArrayList<>();
        List<SymbolTableEntry> symbolTable = new ArrayList<>();

        State initialState = afd.getInitialState();
        State currentState = initialState;
        int currentLine = 1;
        StringBuilder currentToken = new java.lang.StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int c;
            while ((c = reader.read()) != -1) {
                char symbol = (char) c;

                if (symbol == '\n') {
                    processToken(currentState, currentToken, currentLine, tape, symbolTable, afd);
                    currentState = initialState;
                    currentToken.setLength(0);
                    currentLine++;
                    continue;
                }

                if (Character.isWhitespace(symbol)) {
                    processToken(currentState, currentToken, currentLine, tape, symbolTable, afd);
                    currentState = initialState;
                    currentToken.setLength(0);
                    continue;
                }

                currentToken.append(symbol);
                currentState = getNextState(afd, currentState, String.valueOf(symbol));
            }
            
            // Processa o último token se o arquivo não terminar com quebra de linha ou espaço
            if (currentToken.length() > 0) {
                processToken(currentState, currentToken, currentLine, tape, symbolTable, afd);
            }
        }

        tape.add("$");

        System.out.println("\n==== FITA DE SAÍDA ====");
        System.out.println(String.join(" ", tape));

        System.out.println("\n==== TABELA DE SÍMBOLOS ====");
        System.out.printf("%-10s | %-20s | %-20s%n", "Linha", "Identificador/Estado", "Rótulo/Token");
        System.out.println("-".repeat(56));
        for (SymbolTableEntry entry : symbolTable) {
            System.out.printf("%-10d | %-20s | %-20s%n", entry.getLine(), entry.getIdentifier(), entry.getLabel());
        }

        generateTapeCSV(tape, "fita.csv");
        generateSymbolTableCSV(symbolTable, "tabela_simbolos.csv");
    }

    private void generateTapeCSV(List<String> tape, String fileName) {
        try (java.io.FileWriter writer = new java.io.FileWriter(fileName)) {
            writer.append(String.join(",", tape)).append("\n");
            System.out.println("\nArquivo da FITA gerado em: " + fileName);
        } catch (IOException e) {
            System.err.println("Erro ao escrever CSV da FITA: " + e.getMessage());
        }
    }

    private void generateSymbolTableCSV(List<SymbolTableEntry> symbolTable, String fileName) {
        try (java.io.FileWriter writer = new java.io.FileWriter(fileName)) {
            writer.append("Linha,Identificador/Estado,Rotulo/Token\n");
            for (SymbolTableEntry entry : symbolTable) {
                writer.append(String.valueOf(entry.getLine())).append(",");
                writer.append(entry.getIdentifier() != null ? entry.getIdentifier() : "").append(",");
                writer.append(entry.getLabel() != null ? entry.getLabel() : "").append("\n");
            }
            System.out.println("Arquivo da TABELA DE SÍMBOLOS gerado em: " + fileName);
        } catch (IOException e) {
            System.err.println("Erro ao escrever CSV da Tabela de Símbolos: " + e.getMessage());
        }
    }

    private void processToken(State currentState, StringBuilder currentToken, int currentLine, 
                              List<String> tape, List<SymbolTableEntry> symbolTable, Automaton afd) {
        if (currentToken.isEmpty()) return;

        State finalState = currentState;
        if (!finalState.isFinal()) {
            finalState = getErrorState(afd);
        }

        String stateName = finalState != null ? finalState.getTokenName() : "X";
        String tokenStr = currentToken.toString();

        tape.add(stateName);
        symbolTable.add(SymbolTableEntry.builder()
                .line(currentLine)
                .identifier(stateName)
                .label(tokenStr)
                .build());
    }

    private State getNextState(Automaton afd, State currentState, String symbol) {
        for (Transition t : afd.getTransitions()) {
            if (t.getSource().equals(currentState) && symbol.equals(t.getSymbol())) {
                return t.getTarget();
            }
        }
        return getErrorState(afd); // Return Error state if no transition found
    }

    private State getErrorState(Automaton afd) {
        for (State s : afd.getStates()) {
            if ("X".equals(s.getTokenName())) {
                return s;
            }
        }
        return null;
    }
}
