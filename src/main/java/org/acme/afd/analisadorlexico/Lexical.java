package org.acme.afd.analisadorlexico;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.SymbolTableEntry;
import org.acme.afd.model.Transition;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Analisador lexico que utiliza o AFD para classificar tokens do codigo-fonte.
 * Percorre cada token caractere a caractere pelo automato, identifica o estado final
 * e monta a fita de saida e a tabela de simbolos.
 */
@ApplicationScoped
public class Lexical {

    /** Executa a analise lexica sobre a lista de tokens usando o AFD fornecido. */
    public void analyze(Automaton afd, List<String> tokens) throws IOException {
        System.out.println("\n==== Iniciando Analisador Léxico ====");
        List<String> fita = new ArrayList<>();
        
        List<SymbolTableEntry> tabelaSimbolos = new ArrayList<>();

        int linhaAtual = 1;

        // Percorre cada token e simula o AFD caractere a caractere
        for (String token : tokens) {
            State estadoAtual = afd.getInitialState();
            
            // Avanca no automato consumindo cada simbolo do token
            for (char symbol : token.toCharArray()) {
                estadoAtual = getNextState(afd, estadoAtual, String.valueOf(symbol));
            }

            State estadoFinal = estadoAtual;

            // Se o estado alcancado nao e final, classifica como erro (estado X)
            if (!estadoFinal.isFinal()){
                estadoFinal = getErrorState(afd);
            }

            String nomeDoestado = estadoFinal != null ? estadoFinal.getTokenName() : "X";

            fita.add(nomeDoestado);


            // Registra a entrada na tabela de simbolos com linha, estado e lexema
            tabelaSimbolos.add(SymbolTableEntry.builder()
                    .line(linhaAtual)
                    .identifier(nomeDoestado)
                    .label(token)
                    .build());

            linhaAtual++;
        }

        // Adiciona o marcador de fim de fita
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

    /** Busca o proximo estado no AFD dada uma transicao pelo simbolo informado. */
    private State getNextState(Automaton afd, State estadoAtual, String symbol){
        for (Transition transition : afd.getTransitions()){
            if (transition.getSource().equals(estadoAtual) && symbol.equals(transition.getSymbol())){
                return transition.getTarget(); // retorna o estado destino
            }
        }
        
        return getErrorState(afd); 
    }

    /** Retorna o estado de erro (X) do automato, ou null se nao existir. */
    private State getErrorState(Automaton afd) {
        for (State s : afd.getStates()) {
            if ("X".equals(s.getTokenName())) {
                return s;
            }
        }
        return null;
    }

    /** Exporta a fita de saida para um arquivo CSV. */
    private void generateTapeCSV(List<String> tape, String fileName) {
        try (java.io.FileWriter writer = new java.io.FileWriter(fileName)) {
            writer.append(String.join(",", tape)).append("\n");
            System.out.println("\nArquivo da FITA gerado em: " + fileName);
        } catch (IOException e) {
            System.err.println("Erro ao escrever CSV da FITA: " + e.getMessage());
        }
    }

    /** Exporta a tabela de simbolos para um arquivo CSV. */
    private void generateSymbolTableCSV(List<SymbolTableEntry> symbolTable, String fileName) {
        try (java.io.FileWriter writer = new java.io.FileWriter(fileName)) {
            writer.append("Linha,Identificador/Estado,Rotulo/Token,CategoriaSintatica,ObservacaoSintatica\n");
            for (SymbolTableEntry entry : symbolTable) {
                writer.append(String.valueOf(entry.getLine())).append(",");
                writer.append(entry.getIdentifier() != null ? entry.getIdentifier() : "").append(",");
                writer.append(entry.getLabel() != null ? entry.getLabel() : "").append(",");
                writer.append(entry.getSyntaxCategory() != null ? entry.getSyntaxCategory() : "").append(",");
                writer.append(entry.getSyntaxNote() != null ? entry.getSyntaxNote() : "").append("\n");
            }
            System.out.println("Arquivo da TABELA DE SÍMBOLOS gerado em: " + fileName);
        } catch (IOException e) {
            System.err.println("Erro ao escrever CSV da Tabela de Símbolos: " + e.getMessage());
        }
    }
}
