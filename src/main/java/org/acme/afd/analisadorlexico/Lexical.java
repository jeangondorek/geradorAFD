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

    /*
    em resumo, lendo o afd e os tokens eu inicio a analize léxica
    com isso e preciso retornar a fita de saída e a tabela de símbolos
    */
    public void analyze(Automaton afd, List<String> tokens) throws IOException {
        System.out.println("\n==== Iniciando Analisador Léxico ====");
        List<String> fita = new ArrayList<>();
        
        List<SymbolTableEntry> tabelaSimbolos = new ArrayList<>();

        int linhaAtual = 1;

        for (String token : tokens) {
            // a iedia aqui é ler da lista de tokens, iniciando o token inicial , conmo eu ja tenho isso salvo no afd,. fica mais facil
            State estadoAtual = afd.getInitialState();
            
            // depois irei consumir os caracteres desse token e avancar o estado
            for (char symbol : token.toCharArray()) { // funcao do java q reconhece os arrays e separa eles na quebra de linha?
                estadoAtual = getNextState(afd, estadoAtual, String.valueOf(symbol));
            }

            // A palavra foi totalmente lida. Validamos se parou numa condição de sucesso.
            State estadoFinal = estadoAtual;

            // FLUXO LÓGICO: "se não final EC = X"
            // Se o estado que terminou de ler toda a palavra não for estado de "aceitação" (final=true),
            // marcamos a ocorrência como Erro e puxamos o nó representante do Lixo ('X').
            if (!estadoFinal.isFinal()){
                estadoFinal = getErrorState(afd);
            }

            // Extai o nome do estado ('B', 'I' ou o 'X' em caso de erro)
            String nomeDoestado = estadoFinal != null ? estadoFinal.getTokenName() : "X";

            // FLUXO LÓGICO: "add FITA(EC)"
            // Empilha na Fita o nome do estado validado.
            fita.add(nomeDoestado);

            // FLUXO LÓGICO: "add TS(Linha, EC, Label)"
            // Registra as propriedades na Tabela de Símbolos
            tabelaSimbolos.add(SymbolTableEntry.builder()
                    .line(linhaAtual)
                    .identifier(nomeDoestado)
                    .label(token)
                    .build());

            // Pula de linha 
            linhaAtual++;
        }

        // FLUXO LÓGICO: "add Fita ($)"
        // Encerramento formal do arquivo
        fita.add("$");

        // ---- EXIBIÇÃO NO TERMINAL ---- //
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
        // aqui eu chamo as transições
        for (Transition transition : afd.getTransitions()){
            // com isso, posso pegar o estado atual como source "origem" que esta definida nas minhas transicoes
            // dando sequencia, vejo se o simbolo é igual ao simbolo da transicao, com isso podendo pegar a traiscao destino "target"
            if (transition.getSource().equals(estadoAtual) && symbol.equals(transition.getSymbol())){
                return transition.getTarget(); // retorna o estado destino
            }
        }
        
        // Se nenhuma transição for encontrada, a letra lida quebrou a regra formando uma palavra inválida.
        // O autômato deve então ser redirecionado para o Estado "Sumidouro" / "Lixo" (Sink State).
        return getErrorState(afd); 
    }

    /**
     * Busca no autômato a instância designada formalmente como Estado de Erro (cujo tokenName é "X").
     */
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
