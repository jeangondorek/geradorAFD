package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.acme.afd.model.SymbolTableEntry;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SyntacticAnalyzer {

    public SyntacticResult parse(
            Grammar grammar,
            SLRTable table,
            List<String> inputTokens,
            List<SymbolTableEntry> symbolTable) {

        List<String> errors = new ArrayList<>();
        Stack<Integer> stateStack = new Stack<>();
        Stack<String> symbolStack = new Stack<>();
        List<String> intermediateCode = new ArrayList<>();
        stateStack.push(0);

        int position = 0;
        int tempCounter = 1;

        while (position < inputTokens.size()) {
            String lookahead = inputTokens.get(position);

            if ("X".equals(lookahead)) {
                errors.add("Erro lexico antes do sintatico na posicao " + position + ". Token classificado como X.");
                return new SyntacticResult(false, errors);
            }

            int state = stateStack.peek();
            ParsingAction action = table.action().getOrDefault(state, Map.of()).get(lookahead);

            if (action == null) {
                errors.add(buildUnexpectedTokenMessage(state, lookahead, table));
                return new SyntacticResult(false, errors);
            }

            switch (action.type()) {
                case SHIFT -> {
                    symbolStack.push(lookahead);
                    stateStack.push(action.target());
                    // Etapa 2: acao semantica associada ao deslocamento do token.
                    annotateShift(symbolTable, position, lookahead, action.target());
                    position++;
                }
                case REDUCE -> {
                    Production production = grammar.productions().get(action.target());
                    int popSize = isEpsilonProduction(production) ? 0 : production.right().size();

                    for (int i = 0; i < popSize; i++) {
                        if (!symbolStack.isEmpty()) {
                            symbolStack.pop();
                        }
                        if (stateStack.size() > 1) {
                            stateStack.pop();
                        }
                    }

                    int fromState = stateStack.peek();
                    Integer gotoState = table.goTo().getOrDefault(fromState, Map.of()).get(production.left());
                    if (gotoState == null) {
                        errors.add("Tabela GOTO invalida para estado " + fromState + " e nao terminal " + production.left());
                        return new SyntacticResult(false, errors);
                    }

                    symbolStack.push(production.left());
                    stateStack.push(gotoState);
                    // Etapas 2 e 3: toda reducao registra a estrutura reconhecida e pode gerar codigo.
                    annotateReduce(symbolTable, position, production);
                    tempCounter = generateIntermediateCode(
                            production,
                            symbolTable,
                            position,
                            intermediateCode,
                            tempCounter);
                }
                case ACCEPT -> {
                    return new SyntacticResult(true, errors, true, List.of(), intermediateCode, List.of());
                }
            }
        }

        errors.add("Fim da entrada atingido sem acao ACCEPT.");
        return new SyntacticResult(false, errors);
    }

    private boolean isEpsilonProduction(Production production) {
        return production.right().isEmpty()
                || (production.right().size() == 1 && GrammarLoader.EPSILON.equals(production.right().get(0)));
    }

    private String buildUnexpectedTokenMessage(int state, String lookahead, SLRTable table) {
        List<String> expected = new ArrayList<>(table.action().getOrDefault(state, Map.of()).keySet());
        expected.sort(String::compareTo);
        return "Erro sintatico no estado " + state + ": token inesperado '" + lookahead + "'. Esperado: " + expected;
    }

    private void annotateShift(List<SymbolTableEntry> symbolTable, int inputPosition, String lookahead, int targetState) {
        if (inputPosition >= symbolTable.size()) {
            return;
        }

        SymbolTableEntry entry = symbolTable.get(inputPosition);
        entry.setSyntaxCategory(lookahead);
        entry.setSyntaxNote("shift->I" + targetState);
        entry.setSemanticName(entry.getLabel());
        entry.setSemanticType("terminal:" + lookahead);
        entry.setSemanticStatus("deslocado");
    }

    private void annotateReduce(List<SymbolTableEntry> symbolTable, int inputPosition, Production production) {
        if (symbolTable.isEmpty()) {
            return;
        }

        int safeIndex = Math.max(0, Math.min(inputPosition, symbolTable.size()) - 1);
        SymbolTableEntry entry = symbolTable.get(safeIndex);

        String right = production.right().isEmpty() ? GrammarLoader.EPSILON : String.join(" ", production.right());
        String current = entry.getSyntaxNote();
        String reduction = "reduce " + production.left() + "->" + right;

        if (current == null || current.isBlank()) {
            entry.setSyntaxNote(reduction);
        } else if (!current.contains(reduction)) {
            entry.setSyntaxNote(current + " | " + reduction);
        }

        if ("TOKEN".equals(production.left())) {
            entry.setSemanticType("TOKEN");
            entry.setSemanticStatus("reconhecido");
        }
    }

    private int generateIntermediateCode(
            Production production,
            List<SymbolTableEntry> symbolTable,
            int inputPosition,
            List<String> intermediateCode,
            int tempCounter) {

        if (!"TOKEN".equals(production.left()) || production.right().size() != 1 || symbolTable.isEmpty()) {
            return tempCounter;
        }

        // Demonstracao da etapa 3 para a regra TOKEN -> terminal.
        int safeIndex = Math.max(0, Math.min(inputPosition, symbolTable.size()) - 1);
        SymbolTableEntry entry = symbolTable.get(safeIndex);
        String terminal = production.right().get(0);
        String lexeme = entry.getSemanticName() == null || entry.getSemanticName().isBlank()
                ? entry.getLabel()
                : entry.getSemanticName();
        String identifier = entry.getIdentifier() == null || entry.getIdentifier().isBlank()
                ? terminal
                : entry.getIdentifier();

        String temp = "t" + tempCounter;
        intermediateCode.add(temp + " = TOKEN(" + terminal + ", " + lexeme + ")");
        intermediateCode.add(identifier + " = " + temp);
        return tempCounter + 1;
    }
}
