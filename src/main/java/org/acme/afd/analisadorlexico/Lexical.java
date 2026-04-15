package org.acme.afd.analisadorlexico;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.SymbolTableEntry;
import org.acme.afd.model.Transition;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Lexical {

    public void analyze(Automaton afd, List<String> tokens) throws IOException {
        System.out.println("\n==== Iniciando Analisador Léxico ====");
        List<String> fita = new ArrayList<>();
        
        List<SymbolTableEntry> tabelaSimbolos = new ArrayList<>();

        int linhaAtual = 1;

        for (String token : tokens) {
            State estadoAtual = afd.getInitialState();
            
            for (char symbol : token.toCharArray()) {
                estadoAtual = getNextState(afd, estadoAtual, String.valueOf(symbol));
            }

            State estadoFinal = estadoAtual;

            if (!estadoFinal.isFinal()){
                estadoFinal = getErrorState(afd);
            }

            String nomeDoestado = estadoFinal != null ? estadoFinal.getTokenName() : "X";

            fita.add(nomeDoestado);


            tabelaSimbolos.add(SymbolTableEntry.builder()
                    .line(linhaAtual)
                    .identifier(nomeDoestado)
                    .label(token)
                    .build());

            linhaAtual++;
        }

        fita.add("$");

        System.out.println("\n==== FITA DE SAÍDA ====");
        System.out.println(String.join(" ", fita));

        System.out.println("\n==== TABELA DE SÍMBOLOS ====");
        for (SymbolTableEntry entry : tabelaSimbolos) {
            System.out.printf("%d | %s | %s%n",
                    entry.getLine(),
                    entry.getIdentifier(),
                    entry.getLabel());
        }

        generateTapeCSV(fita, "fita.csv");
        generateSymbolTableCSV(tabelaSimbolos, "tabela_simbolos.csv");
    }

    private State getNextState(Automaton afd, State estadoAtual, String symbol){
        for (Transition transition : afd.getTransitions()){
            if (transition.getSource().equals(estadoAtual) && symbol.equals(transition.getSymbol())){
                return transition.getTarget(); // retorna o estado destino
            }
        }
        
        return getErrorState(afd); 
    }

    private State getErrorState(Automaton afd) {
        for (State s : afd.getStates()) {
            if ("X".equals(s.getTokenName())) {
                return s;
            }
        }
        return null;
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
}
