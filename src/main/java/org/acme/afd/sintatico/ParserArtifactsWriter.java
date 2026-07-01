package org.acme.afd.sintatico;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Escritor de artefatos do parser em arquivos CSV e texto.
 * Grava producoes, conjuntos FIRST/FOLLOW, estados LR(0), transicoes,
 * tabela SLR, conflitos, resultado semantico e codigo intermediario.
 */
@ApplicationScoped
public class ParserArtifactsWriter {

    // Grava as producoes da gramatica no formato CSV (id, lado esquerdo, lado direito).
    public void writeProductions(Grammar grammar, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("id,left,right\n");
            for (Production p : grammar.productions()) {
                writer.append(Integer.toString(p.index())).append(",")
                        .append(p.left()).append(",")
                        .append(rightSideToText(p.right())).append("\n");
            }
        }
    }

    // Grava os conjuntos FIRST e FOLLOW de cada nao-terminal em CSV.
    public void writeFirstFollow(FirstFollowCalculator.FirstFollow firstFollow, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("nonterminal,first,follow\n");
            List<String> nts = new ArrayList<>(firstFollow.first().keySet());
            nts.sort(String::compareTo);
            for (String nt : nts) {
                writer.append(nt).append(",")
                        .append(String.join(" ", firstFollow.first().getOrDefault(nt, Set.of()))).append(",")
                        .append(String.join(" ", firstFollow.follow().getOrDefault(nt, Set.of()))).append("\n");
            }
        }
    }

    // Grava os estados LR(0) com seus itens (producao com posicao do ponto).
    public void writeStates(Grammar grammar, SLRTable table, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("state,item\n");
            for (int i = 0; i < table.states().size(); i++) {
                for (LR0Item item : table.states().get(i)) {
                    writer.append("I").append(Integer.toString(i)).append(",")
                            .append(itemToText(grammar, item)).append("\n");
                }
            }
        }
    }

    // Grava as transicoes entre estados (origem, simbolo, destino).
    public void writeTransitions(SLRTable table, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("from,symbol,to\n");
            List<Integer> states = new ArrayList<>(table.transitions().keySet());
            states.sort(Comparator.naturalOrder());

            for (Integer state : states) {
                Map<String, Integer> row = table.transitions().getOrDefault(state, Map.of());
                List<String> symbols = new ArrayList<>(row.keySet());
                symbols.sort(String::compareTo);

                for (String symbol : symbols) {
                    writer.append("I").append(Integer.toString(state)).append(",")
                            .append(symbol).append(",")
                            .append("I").append(Integer.toString(row.get(symbol))).append("\n");
                }
            }
        }
    }

    // Grava a tabela SLR completa com colunas ACTION (prefixo A_) e GOTO (prefixo G_).
    public void writeParsingTable(Grammar grammar, SLRTable table, String filePath) throws IOException {
        List<String> terminals = new ArrayList<>(grammar.terminals());
        terminals.sort(String::compareTo);

        List<String> nonTerminals = new ArrayList<>(grammar.nonTerminals().stream()
                .filter(nt -> !nt.equals(grammar.augmentedStartSymbol()))
                .toList());
        nonTerminals.sort(String::compareTo);

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("state");
            for (String t : terminals) {
                writer.append(",A_").append(t);
            }
            for (String nt : nonTerminals) {
                writer.append(",G_").append(nt);
            }
            writer.append("\n");

            for (int i = 0; i < table.states().size(); i++) {
                writer.append("I").append(Integer.toString(i));

                Map<String, ParsingAction> actionRow = table.action().getOrDefault(i, Map.of());
                for (String t : terminals) {
                    ParsingAction action = actionRow.get(t);
                    writer.append(",").append(action != null ? action.toString() : "");
                }

                Map<String, Integer> gotoRow = table.goTo().getOrDefault(i, Map.of());
                for (String nt : nonTerminals) {
                    Integer g = gotoRow.get(nt);
                    writer.append(",").append(g != null ? Integer.toString(g) : "");
                }

                writer.append("\n");
            }
        }
    }

    // Grava a lista de conflitos SLR detectados, ou mensagem de ausencia de conflitos.
    public void writeConflicts(List<String> conflicts, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("conflito\n");
            if (conflicts.isEmpty()) {
                writer.append("Sem conflitos SLR detectados\n");
                return;
            }

            for (String conflict : conflicts) {
                writer.append(conflict).append("\n");
            }
        }
    }

    // Grava o resultado da analise semantica (ACEITO ou REJEITADO com erros).
    public void writeSemanticResult(SemanticResult semanticResult, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            if (semanticResult.accepted()) {
                writer.append("ACEITO\n");
                return;
            }

            writer.append("REJEITADO\n");
            for (String error : semanticResult.errors()) {
                writer.append(error).append("\n");
            }
        }
    }

    // Grava linhas de codigo intermediario (ou otimizado) em arquivo texto.
    public void writeCode(List<String> code, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            if (code.isEmpty()) {
                writer.append("// codigo vazio\n");
                return;
            }

            for (String line : code) {
                writer.append(line).append("\n");
            }
        }
    }

    private String itemToText(Grammar grammar, LR0Item item) {
        Production p = grammar.productions().get(item.productionIndex());
        List<String> right = new ArrayList<>(p.right());

        if (item.dotPosition() >= 0 && item.dotPosition() <= right.size()) {
            right.add(item.dotPosition(), ".");
        }

        return p.left() + " -> " + String.join(" ", right);
    }

    private String rightSideToText(List<String> right) {
        return right.isEmpty() ? GrammarLoader.EPSILON : String.join(" ", right);
    }
}
