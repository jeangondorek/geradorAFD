package org.acme.afd.generator;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class AFNDGenerator {
    private static final char SKIP_LETTER = 'S';
    private static final char START_LETTER = 'A';
    private static final char END_LETTER = 'Z';

    private final Map<String, State> existingStates = new HashMap<>();
    private int suffix = 1;
    private char lastLetter = START_LETTER;

    public Automaton generate(List<String> tokens, Map<String, String> grammars) {
        Automaton afnd = Automaton.builder()
                .states(new LinkedHashSet<>())
                .alphabet(new LinkedHashSet<>())
                .transitions(new ArrayList<>())
                .build();

        String initialTokenName = "S";
        int countState = 0;

        Set<State> allStates = afnd.getStates();

        for (String token : tokens) {
            State prevState = null;
            State currentState = null;
            for (char ch : token.toCharArray()) {
                allStates = afnd.getStates();

                currentState = tokenCreate( countState, ch, afnd, initialTokenName);

                allStates.add(currentState);
                afnd.setStates(allStates);

                afnd.getTransitions().add(Transition.builder()
                                .source(prevState != null ? prevState : null)
                                .symbol(prevState != null ? prevState.getLabel() : null)
                                .target(currentState)
                        .build());

                prevState = currentState;

                countState++;
            }

            State finalState = createState("", generateStateName(), true);
            afnd.getStates().add(finalState);

            afnd.getTransitions().add(Transition.builder()
                    .source(prevState)
                    .symbol(prevState.getLabel())
                    .target(finalState)
                    .build());

            countState = 0;
            afnd.setStates(allStates);
        }

        Map<String, String> tokenToStateMap = new HashMap<>();

        State prevState = null;
        State currentState = null;

        for (Map.Entry<String, String> entry : grammars.entrySet()) {
            String ruleName = entry.getKey();
            String grammar = entry.getValue();

            String[] parts = grammar.split("\\|");

            for (String part : parts) {
                part = part.trim();

                boolean isFinal = part.equals("ε");

                Matcher tokenMatcher = Pattern.compile("<(.*?)>").matcher(part);
                String tokenName = tokenMatcher.find() ? tokenMatcher.group(1) : null;

                Matcher lowerMatcher = Pattern.compile("[a-z]").matcher(part);
                String label = lowerMatcher.find() ? lowerMatcher.group() : null;
                afnd.getAlphabet().add(label);

                String nextRuleName;

                if (tokenName != null) {
                    if (tokenToStateMap.containsKey(tokenName)) {
                        nextRuleName = tokenToStateMap.get(tokenName);
                    } else {
                        nextRuleName = generateStateName();
                        tokenToStateMap.put(tokenName, nextRuleName);
                    }
                } else {
                    if (tokenToStateMap.containsKey(ruleName)) {
                        nextRuleName = tokenToStateMap.get(ruleName);
                    } else {
                        nextRuleName = generateStateName();
                        tokenToStateMap.put(ruleName, nextRuleName);
                    }
                }
                allStates = afnd.getStates();
                List<State> statesList = new ArrayList<>(allStates);
                prevState = statesList.getLast();
                currentState = createState(label, nextRuleName, isFinal);
                afnd.getTransitions().add(Transition.builder()
                                .source(prevState)
                                .target(currentState)
                                .symbol(prevState.getLabel())
                        .build());
                allStates = afnd.getStates();
                allStates.add(currentState);
                afnd.setStates(allStates);
            }
          
        }

        return afnd;
    }

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
            if (lastLetter > END_LETTER) {
                lastLetter = START_LETTER;
                suffix++;
            }
        }

        return stateName;
    }
}
