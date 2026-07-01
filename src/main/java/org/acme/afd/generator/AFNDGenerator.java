package org.acme.afd.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Gerador do Automato Finito Nao-Deterministico (AFND).
 * Constroi o AFND a partir dos tokens (palavras reservadas/simbolos) e das
 * regras gramaticais em formato BNF lidas do arquivo de entrada.
 */
@ApplicationScoped
public class AFNDGenerator {
    private static final char SKIP_LETTER = 'S';     // Letra reservada para o estado inicial, pulada na geracao
    private static final char START_LETTER = 'A';     // Primeira letra usada para nomear estados
    private static final char END_LETTER = 'Z';       // Ultima letra do ciclo de nomeacao
    private static final char ERROR_STATE_LABEL = 'X'; // Letra reservada para o estado de erro

    private final Map<String, State> existingStates = new HashMap<>();
    private int suffix = 1;
    private char lastLetter = START_LETTER;

    /** Gera o AFND completo a partir dos tokens e das regras gramaticais. */
    public Automaton generate(List<String> tokens, Map<String, String> grammars) {
        Automaton afnd = Automaton.builder()
                .states(new LinkedHashSet<>())
                .alphabet(new LinkedHashSet<>())
                .transitions(new ArrayList<>())
                .build();

        String initialTokenName = "S";
        int countState = 0;

        Set<State> allStates = afnd.getStates();

        // Cria estados e transicoes para cada token (palavra reservada/simbolo)
        for (String token : tokens) {
            State prevState = null;
            State currentState = null;
            for (char ch : token.toCharArray()) {
                allStates = afnd.getStates();

                currentState = tokenCreate( countState, ch, afnd, initialTokenName);

                allStates.add(currentState);
                afnd.setStates(allStates);

                List<State> listState = afnd.getStates().stream().toList();

                afnd.getTransitions().add(Transition.builder()
                                .source(prevState != null ? prevState : listState.getFirst())
                                .symbol(prevState != null ? prevState.getLabel() : null)
                                .target(currentState)
                        .build());

                prevState = currentState;

                countState++;
            }

            // Cria o estado final de aceitacao para este token
            State finalState = createState("", generateStateName(), true);
            afnd.getStates().add(finalState);

            List<State> listState = afnd.getStates().stream().toList();

            afnd.getTransitions().add(Transition.builder()
                    .source(prevState != null ? prevState : listState.getFirst())
                    .symbol(prevState.getLabel())
                    .target(finalState)
                    .build());

            countState = 0;
            afnd.setStates(allStates);
        }
        
        // Mapeia nomes de nao-terminais da gramatica para nomes de estados do AFND
        Map<String, String> tokenToStateMap = new HashMap<>();
        Map<String, State> stateMap = new HashMap<>();

        // Processa cada regra gramatical BNF para criar estados e transicoes
        for (Map.Entry<String, String> entry : grammars.entrySet()) {
            String ruleName = entry.getKey();
            String grammar = entry.getValue();

            // Divide as alternativas da producao (separadas por '|')
            String[] parts = grammar.split("\\|");

            for (String part : parts) {
                part = part.trim();

                // Se a producao e epsilon, marca o estado fonte como final
                boolean isFinal = part.equals("ε");

                if (isFinal) {
                    State sourceState;
                    if (ruleName.equals("S")) {
                        sourceState = afnd.getInitialState();
                    } else {
                        String stateName = tokenToStateMap.get(ruleName);
                        sourceState = stateMap.get(stateName);
                    }
                    if (sourceState != null) {
                        sourceState.setFinal(true);
                    }
                    continue;
                }

                // Extrai o nao-terminal de destino (entre < >) e o simbolo terminal (letra minuscula)
                Matcher tokenMatcher = Pattern.compile("<(.*?)>").matcher(part);
                String tokenName = tokenMatcher.find() ? tokenMatcher.group(1) : null;

                Matcher lowerMatcher = Pattern.compile("[a-z]").matcher(part);
                String label = lowerMatcher.find() ? lowerMatcher.group() : null;
                afnd.getAlphabet().add(label);

                State sourceState;
                if (ruleName.equals("S")) {
                    sourceState = afnd.getInitialState();
                } else {
                    String sourceStateName = tokenToStateMap.get(ruleName);
                    sourceState = stateMap.get(sourceStateName);
                }

                State targetState;
                String nextRuleName;

                if (tokenName != null) {
                    if (tokenToStateMap.containsKey(tokenName)) {
                        nextRuleName = tokenToStateMap.get(tokenName);
                        targetState = stateMap.get(nextRuleName);
                    } else {
                        nextRuleName = generateStateName();
                        tokenToStateMap.put(tokenName, nextRuleName);
                        targetState = createState(label, nextRuleName, false);
                        stateMap.put(nextRuleName, targetState);
                        allStates = afnd.getStates();
                        allStates.add(targetState);
                        afnd.setStates(allStates);
                    }
                } else {
                    nextRuleName = generateStateName();
                    targetState = createState(label, nextRuleName, false);
                    allStates = afnd.getStates();
                    allStates.add(targetState);
                    afnd.setStates(allStates);
                }

                List<State> listState = afnd.getStates().stream().toList();

                afnd.getTransitions().add(Transition.builder()
                        .source(sourceState != null ? sourceState : listState.getFirst())
                        .target(targetState)
                        .symbol(label)
                        .build());
            }

        }
        List<State> listState = afnd.getStates().stream().toList();

        afnd.setInitialState(listState.getFirst());

        return afnd;
    }

    /** Cria um estado para um caractere do token; o primeiro caractere usa o estado inicial 'S'. */
    private State tokenCreate(int countState, char ch, Automaton afnd, String initialTokenName) {
        if (countState == 0){
            afnd.getAlphabet().add(String.valueOf(ch));
            return createState(String.valueOf(ch), initialTokenName, false);
        } else {
            afnd.getAlphabet().add(String.valueOf(ch));
            return createState(String.valueOf(ch), generateStateName(), false);
        }
    }

    private State createState(String label, String initialTokenName, boolean isFinal) {
        return State.builder()
                .tokenName(initialTokenName)
                .isFinal(isFinal)
                .label(label)
                .build();
    }

    /** Gera nomes unicos para estados (A, B, ..., Z, A2, B2, ...), pulando 'S' e 'X'. */
    public String generateStateName() {
        String stateName;

        while (true) {
            if (lastLetter == SKIP_LETTER) {
                lastLetter++;
            }

            if (suffix == 1) {
                stateName = String.valueOf(lastLetter);
            } else {
                stateName = lastLetter + String.valueOf(suffix);
            }

            if (!existingStates.containsKey(stateName)) {
                existingStates.put(stateName, null);
                break;
            }

            lastLetter++;
            if (lastLetter == ERROR_STATE_LABEL) {
                lastLetter++;
            }
            if (lastLetter > END_LETTER) {
                lastLetter = START_LETTER;
                suffix++;
            }
        }

        return stateName;
    }
}
