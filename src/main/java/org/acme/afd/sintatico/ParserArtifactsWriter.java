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
 * Utilitario de gravacao de arquivos para exportar os resultados e dados intermediarios.
 * Esta classe traduz as estruturas em memoria do compilador (tabela SLR, FIRST/FOLLOW,
 * itens de estados, transicoes, resultados semanticos e codigo intermediario)
 * em arquivos CSV e TXT limpos e organizados para avaliacao ou depuracao.
 */
@ApplicationScoped
public class ParserArtifactsWriter {

    /**
     * Grava as regras de producao da gramatica indexadas no arquivo CSV.
     * 
     * @param grammar A gramatica cujas producoes serao gravadas
     * @param filePath Nome/caminho do arquivo de destino
     * @throws IOException Se houver erro de escrita
     */
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

    /**
     * Grava os conjuntos FIRST e FOLLOW ordenados por nao-terminal no CSV.
     * 
     * @param firstFollow Dados contendo os conjuntos calculados
     * @param filePath Nome/caminho do arquivo
     * @throws IOException Se houver erro de escrita
     */
    public void writeFirstFollow(FirstFollowCalculator.FirstFollow firstFollow, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("nonterminal,first,follow\n");
            
            // Ordena os nao-terminais alfabeticamente para um layout previsivel
            List<String> nts = new ArrayList<>(firstFollow.first().keySet());
            nts.sort(String::compareTo);
            
            for (String nt : nts) {
                writer.append(nt).append(",")
                        .append(String.join(" ", firstFollow.first().getOrDefault(nt, Set.of()))).append(",")
                        .append(String.join(" ", firstFollow.follow().getOrDefault(nt, Set.of()))).append("\n");
            }
        }
    }

    /**
     * Grava os estados LR(0) contendo seus respectivos itens com a posicao do ponto ('.').
     * 
     * @param grammar Gramatica de contexto
     * @param table Tabela SLR contendo a colecao de estados
     * @param filePath Nome/caminho do arquivo
     * @throws IOException Se houver erro de escrita
     */
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

    /**
     * Grava a matriz de transicoes de estados do dfa do parser (De estado I_origem por Simbolo para I_destino).
     * 
     * @param table Tabela SLR contendo as transicoes calculadas
     * @param filePath Nome/caminho do arquivo
     * @throws IOException Se houver erro de escrita
     */
    public void writeTransitions(SLRTable table, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("from,symbol,to\n");
            List<Integer> states = new ArrayList<>(table.transitions().keySet());
            states.sort(Comparator.naturalOrder()); // Ordena os IDs dos estados numericamento (0..N)

            for (Integer state : states) {
                Map<String, Integer> row = table.transitions().getOrDefault(state, Map.of());
                List<String> symbols = new ArrayList<>(row.keySet());
                symbols.sort(String::compareTo); // Ordena os simbolos de transicao alfabeticamente

                for (String symbol : symbols) {
                    writer.append("I").append(Integer.toString(state)).append(",")
                            .append(symbol).append(",")
                            .append("I").append(Integer.toString(row.get(symbol))).append("\n");
                }
            }
        }
    }

    /**
     * Grava a matriz da tabela SLR.
     * Colunas ACTION sao prefixadas por "A_" e colunas GOTO sao prefixadas por "G_".
     * Exemplo de cabecalho: state,A_if,A_id,A_$,G_PROGRAMA,G_LISTA
     * 
     * @param grammar Gramatica utilizada
     * @param table Tabela SLR contendo as acoes e desvios
     * @param filePath Nome/caminho do arquivo
     * @throws IOException Se houver erro de escrita
     */
    public void writeParsingTable(Grammar grammar, SLRTable table, String filePath) throws IOException {
        // Ordena os terminais alfabeticamente
        List<String> terminals = new ArrayList<>(grammar.terminals());
        terminals.sort(String::compareTo);

        // Filtra e ordena os nao-terminais (removendo o simbolo estendido S')
        List<String> nonTerminals = new ArrayList<>(grammar.nonTerminals().stream()
                .filter(nt -> !nt.equals(grammar.augmentedStartSymbol()))
                .toList());
        nonTerminals.sort(String::compareTo);

        try (FileWriter writer = new FileWriter(filePath)) {
            // Escreve a linha de cabecalho
            writer.append("state");
            for (String t : terminals) {
                writer.append(",A_").append(t);
            }
            for (String nt : nonTerminals) {
                writer.append(",G_").append(nt);
            }
            writer.append("\n");

            // Escreve as acoes e desvios para cada estado linha por linha
            for (int i = 0; i < table.states().size(); i++) {
                writer.append("I").append(Integer.toString(i));

                // Colunas ACTION (A_)
                Map<String, ParsingAction> actionRow = table.action().getOrDefault(i, Map.of());
                for (String t : terminals) {
                    ParsingAction action = actionRow.get(t);
                    writer.append(",").append(action != null ? action.toString() : "");
                }

                // Colunas GOTO (G_)
                Map<String, Integer> gotoRow = table.goTo().getOrDefault(i, Map.of());
                for (String nt : nonTerminals) {
                    Integer g = gotoRow.get(nt);
                    writer.append(",").append(g != null ? Integer.toString(g) : "");
                }

                writer.append("\n");
            }
        }
    }

    /**
     * Grava o relatorio de conflitos sintaticos encontrados na gramatica.
     * 
     * @param conflicts Lista de strings descrevendo os conflitos detectados
     * @param filePath Nome/caminho do arquivo
     * @throws IOException Se houver erro de escrita
     */
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

    /**
     * Grava o resultado final da analise semantica em arquivo texto.
     * Se houver erros, escreve a lista detalhada com a linha de ocorrencia de cada erro.
     * 
     * @param semanticResult O resultado contendo status e mensagens de erro
     * @param filePath Nome/caminho do arquivo
     * @throws IOException Se houver erro de escrita
     */
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

    /**
     * Grava instrucoes de codigo intermediario ou otimizado linha por linha.
     * 
     * @param code Lista de instrucoes geradas
     * @param filePath Nome/caminho do arquivo
     * @throws IOException Se houver erro de escrita
     */
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

    /**
     * Formata um item LR(0) de forma legivel, inserindo o ponto '.' na posicao correta.
     * Exemplo: de A -> B C, gera "A -> B . C" se o ponto estiver na posicao 1.
     */
    private String itemToText(Grammar grammar, LR0Item item) {
        Production p = grammar.productions().get(item.productionIndex());
        List<String> right = new ArrayList<>(p.right());

        // Insere o ponto na lista do lado direito baseado no atributo dotPosition
        if (item.dotPosition() >= 0 && item.dotPosition() <= right.size()) {
            right.add(item.dotPosition(), ".");
        }

        return p.left() + " -> " + String.join(" ", right);
    }

    /**
     * Formata a lista de simbolos do lado direito de uma producao para escrita.
     * Se a lista estiver vazia, retorna a string "epsilon".
     */
    private String rightSideToText(List<String> right) {
        return right.isEmpty() ? GrammarLoader.EPSILON : String.join(" ", right);
    }
}
