package org.acme.afd.teste;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.SymbolTableEntry;
import org.acme.afd.model.Transition;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TesteValidator {

    public void validarArquivoTeste(Automaton afd, String testFile) throws Exception {
        List<String> palavras = new ArrayList<>();
        List<String> fitaTeste = new ArrayList<>();
        List<SymbolTableEntry> tabelaSimbolosTeste = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(testFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    palavras.add(trimmed);
                }
            }
        }

        System.out.println("==== Validação de " + testFile + " ====");
        int linha = 1;
        for (String palavra : palavras) {
            State estadoAtual = afd.getInitialState();
            
            for (char c : palavra.toCharArray()) {
                estadoAtual = getNextState(afd, estadoAtual, String.valueOf(c));
            }

            State estadoFinal = estadoAtual;
            if (!estadoFinal.isFinal()) {
                estadoFinal = getErrorState(afd);
            }

            String nomeEstado = estadoFinal != null ? estadoFinal.getTokenName() : "X";
            
            fitaTeste.add(nomeEstado);

            tabelaSimbolosTeste.add(SymbolTableEntry.builder()
                    .line(linha)
                    .identifier(nomeEstado)
                    .label(palavra)
                    .build());

            System.out.printf("%s => %s\n", palavra, nomeEstado);
            linha++;
        }
        
        fitaTeste.add("$");

        gerarArquivosSaida(fitaTeste, tabelaSimbolosTeste);
    }

    private void gerarArquivosSaida(List<String> fita, List<SymbolTableEntry> tabela) {
        try (FileWriter writer = new FileWriter("fita_teste.csv")) {
            writer.append(String.join(",", fita)).append("\n");
            System.out.println("\n[OK] fita_teste.csv gerado.");
        } catch (IOException e) {
            System.err.println("Erro fita_teste: " + e.getMessage());
        }

        try (FileWriter writer = new FileWriter("tabela_simbolos_teste.csv")) {
            writer.append("Linha,Identificador/Estado,Rotulo/Token\n");
            for (SymbolTableEntry entry : tabela) {
                writer.append(String.format("%d,%s,%s\n", 
                    entry.getLine(), entry.getIdentifier(), entry.getLabel()));
            }
            System.out.println("[OK] tabela_simbolos_teste.csv gerado.");
        } catch (IOException e) {
            System.err.println("Erro tabela_simbolos_teste: " + e.getMessage());
        }
    }

    private State getNextState(Automaton afd, State estadoAtual, String symbol) {
        for (Transition t : afd.getTransitions()) {
            if (t.getSource().equals(estadoAtual) && symbol.equals(t.getSymbol())) {
                return t.getTarget();
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
}