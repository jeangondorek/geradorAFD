package org.acme.afd.teste;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class TesteValidator {
    /**
     * Valida cada palavra do arquivo de teste usando o AFD já construído.
     * @param afd Autômato determinístico
     * @param testFile Nome do arquivo de teste (ex: "teste.txt")
     */
    public void validarArquivoTeste(Automaton afd, String testFile) throws Exception {
        List<String> palavras = new ArrayList<>();
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
            System.out.printf("%s => %s\n", palavra, nomeEstado);
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
