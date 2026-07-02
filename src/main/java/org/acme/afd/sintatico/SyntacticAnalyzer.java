package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.acme.afd.model.SymbolTableEntry;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * O analisador sintatico SLR (Shift-Reduce Parser).
 * Esta classe le a fita de tokens de entrada e consome os simbolos controlando 
 * duas pilhas: uma pilha de estados numericos (do DFA) e uma pilha de simbolos 
 * reconhecidos (terminais e nao-terminais).
 * 
 * Alem de validar a sintaxe do programa, ela enriquece a Tabela de Simbolos 
 * anotando os deslocamentos e reducoes em tempo de parsing, e gera o codigo
 * intermediario basico (etapa de geracao de codigo).
 */
@ApplicationScoped
public class SyntacticAnalyzer {

    /**
     * Realiza o parsing (analise sintatica) SLR sobre a fita de tokens.
     * 
     * @param grammar Gramatica sintatica formal
     * @param table Tabela de acoes e desvios SLR(1)
     * @param inputTokens Fita de tokens a ser parseada
     * @param symbolTable Tabela de simbolos a ser anotada e enriquecida
     * @return SyntacticResult indicando aceitacao ou rejeicao, e codigos gerados
     */
    public SyntacticResult parse(
            Grammar grammar,
            SLRTable table,
            List<String> inputTokens,
            List<SymbolTableEntry> symbolTable) {

        List<String> errors = new ArrayList<>();
        Stack<Integer> stateStack = new Stack<>();  // Pilha de Estados LR
        Stack<String> symbolStack = new Stack<>();  // Pilha de Simbolos Sintaticos
        List<String> intermediateCode = new ArrayList<>();
        
        // Inicializacao: empilha o estado inicial 0
        stateStack.push(0);

        int position = 0;      // Indice correspondente ao token atual sendo lido na fita
        int tempCounter = 1;   // Contador sequencial para criacao de variaveis temporarias (t1, t2, ...)

        // Varre a fita de tokens ate o fim
        while (position < inputTokens.size()) {
            String lookahead = inputTokens.get(position); // Token sob analise atual

            // Se for o token de erro "X" (gerado pelo lexico), aborta imediatamente
            if ("X".equals(lookahead)) {
                errors.add("Erro lexico antes do sintatico na posicao " + position + ". Token classificado como X.");
                return new SyntacticResult(false, errors);
            }

            // Consulta o estado no topo da pilha
            int state = stateStack.peek();
            
            // Busca a acao indicada na tabela ACTION para o par (estado, lookahead)
            ParsingAction action = table.action().getOrDefault(state, Map.of()).get(lookahead);

            // Se nao houver acao cadastrada, ocorreu um Erro Sintatico (token inesperado)
            if (action == null) {
                errors.add(buildUnexpectedTokenMessage(state, lookahead, table));
                return new SyntacticResult(false, errors);
            }

            // Processa a acao de acordo com o tipo
            switch (action.type()) {
                case SHIFT -> { // Acao de Deslocamento
                    // Empilha o token lido e o novo estado destino indicado na acao
                    symbolStack.push(lookahead);
                    stateStack.push(action.target());
                    
                    // Atualiza a tabela de simbolos na linha correspondente com metadados do shift
                    annotateShift(symbolTable, position, lookahead, action.target());
                    
                    // Avanca o ponteiro de leitura para o proximo token da fita
                    position++;
                }
                case REDUCE -> { // Acao de Reducao por uma Regra da Gramatica
                    Production production = grammar.productions().get(action.target());
                    
                    // Determina quantos elementos desempilhar. 
                    // Se for uma producao vazia (epsilon), desempilha 0. Senao desempilha o tamanho do lado direito.
                    int popSize = isEpsilonProduction(production) ? 0 : production.right().size();

                    // Desempilha os simbolos correspondentes do lado direito da pilha de simbolos e de estados
                    for (int i = 0; i < popSize; i++) {
                        if (!symbolStack.isEmpty()) {
                            symbolStack.pop();
                        }
                        if (stateStack.size() > 1) { // Garante nao desempilhar o estado inicial 0
                            stateStack.pop();
                        }
                    }

                    // Consulta o novo estado exposto no topo da pilha
                    int fromState = stateStack.peek();
                    
                    // Consulta a tabela GOTO para saber qual estado destino ir a partir da variavel reduzida
                    Integer gotoState = table.goTo().getOrDefault(fromState, Map.of()).get(production.left());
                    if (gotoState == null) {
                        errors.add("Tabela GOTO invalida para estado " + fromState + " e nao terminal " + production.left());
                        return new SyntacticResult(false, errors);
                    }

                    // Empilha o lado esquerdo da regra reduzida (variavel) e o estado GOTO correspondente
                    symbolStack.push(production.left());
                    stateStack.push(gotoState);
                    
                    // Registra na tabela de simbolos o histórico da regra de reducao aplicada
                    annotateReduce(symbolTable, position, production);
                    
                    // Se a reducao for do tipo TOKEN -> terminal, gera instrucoes de codigo intermediario de 3 enderecos
                    tempCounter = generateIntermediateCode(
                            production,
                            symbolTable,
                            position,
                            intermediateCode,
                            tempCounter);
                }
                case ACCEPT -> { // Acao de Aceitacao (Parsing concluido com sucesso!)
                    return new SyntacticResult(true, errors, true, List.of(), intermediateCode, List.of());
                }
            }
        }

        errors.add("Fim da entrada atingido sem acao ACCEPT.");
        return new SyntacticResult(false, errors);
    }

    /**
     * Verifica se a regra de producao reduzida e nula/epsilon (lado direito vazio).
     */
    private boolean isEpsilonProduction(Production production) {
        return production.right().isEmpty()
                || (production.right().size() == 1 && GrammarLoader.EPSILON.equals(production.right().get(0)));
    }

    /**
     * Constrói a mensagem informativa de erro sintatico listando quais tokens
     * seriam aceitos naquele estado especifico da tabela.
     */
    private String buildUnexpectedTokenMessage(int state, String lookahead, SLRTable table) {
        List<String> expected = new ArrayList<>(table.action().getOrDefault(state, Map.of()).keySet());
        expected.sort(String::compareTo);
        return "Erro sintatico no estado " + state + ": token inesperado '" + lookahead + "'. Esperado: " + expected;
    }

    /**
     * Anota na tabela de simbolos o deslocamento (shift) de um terminal da entrada.
     */
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

    /**
     * Anota na tabela de simbolos a reducao (reduce) de uma regra gramatical.
     * Concatena com anotacoes existentes caso o mesmo lexema passe por multiplas reducoes.
     */
    private void annotateReduce(List<SymbolTableEntry> symbolTable, int inputPosition, Production production) {
        if (symbolTable.isEmpty()) {
            return;
        }

        // Recupera o ultimo item lido associado a posicao atual
        int safeIndex = Math.max(0, Math.min(inputPosition, symbolTable.size()) - 1);
        SymbolTableEntry entry = symbolTable.get(safeIndex);

        String right = production.right().isEmpty() ? GrammarLoader.EPSILON : String.join(" ", production.right());
        String current = entry.getSyntaxNote();
        String reduction = "reduce " + production.left() + "->" + right;

        // Se ja houver outras reducoes anteriores registradas na celula, concatena com o caractere pipe '|'
        if (current == null || current.isBlank()) {
            entry.setSyntaxNote(reduction);
        } else if (!current.contains(reduction)) {
            entry.setSyntaxNote(current + " | " + reduction);
        }

        // Se reduzir a variavel TOKEN, atualiza o status semantico para reconhecido
        if ("TOKEN".equals(production.left())) {
            entry.setSemanticType("TOKEN");
            entry.setSemanticStatus("reconhecido");
        }
    }

    /**
     * Traduz uma reducao em codigo intermediario.
     * Gera duas linhas de atribuicoes usando variaveis temporarias t_x para mapear
     * cada token reconhecido:
     *   tx = TOKEN(categoria, lexema)
     *   identificador = tx
     * 
     * @return O proximo contador de temporarios a ser usado
     */
    private int generateIntermediateCode(
            Production production,
            List<SymbolTableEntry> symbolTable,
            int inputPosition,
            List<String> intermediateCode,
            int tempCounter) {

        // A geracao simplificada foca apenas quando reduzimos a variavel intermediaria TOKEN
        if (!"TOKEN".equals(production.left()) || production.right().size() != 1 || symbolTable.isEmpty()) {
            return tempCounter;
        }

        int safeIndex = Math.max(0, Math.min(inputPosition, symbolTable.size()) - 1);
        SymbolTableEntry entry = symbolTable.get(safeIndex);
        String terminal = production.right().get(0);
        String lexeme = entry.getSemanticName() == null || entry.getSemanticName().isBlank()
                ? entry.getLabel()
                : entry.getSemanticName();
        String identifier = entry.getIdentifier() == null || entry.getIdentifier().isBlank()
                ? terminal
                : entry.getIdentifier();

        // tx = TOKEN(terminal, lexema)
        String temp = "t" + tempCounter;
        intermediateCode.add(temp + " = TOKEN(" + terminal + ", " + lexeme + ")");
        // identificador_tabela = tx
        intermediateCode.add(identifier + " = " + temp);
        
        return tempCounter + 1;
    }
}
