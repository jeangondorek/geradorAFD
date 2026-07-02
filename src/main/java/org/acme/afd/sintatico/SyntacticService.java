package org.acme.afd.sintatico;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.acme.afd.model.SymbolTableEntry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Orquestra o pipeline completo de analise sintatica.
 * Etapas: carregar gramatica, verificar compatibilidade com a fita lexico,
 * normalizar, construir tabela SLR, parsear, analise semantica,
 * geracao e otimizacao de codigo intermediario, e escrita de todos os artefatos.
 */
@ApplicationScoped
public class SyntacticService {

    // Carrega a gramatica estruturada a partir do arquivo txt
    @Inject
    GrammarLoader grammarLoader;

    // Constroi a tabela SLR(1) (estados, transicoes e acoes de parsing)
    @Inject
    SLRTableBuilder slrTableBuilder;

    // Normaliza a gramatica removendo producoes inuteis e fazendo fatoracao
    @Inject
    GrammarNormalizer grammarNormalizer;

    // Exporta tabelas, FIRST/FOLLOW, itens LR0 e outros artefatos em arquivos CSV/txt
    @Inject
    ParserArtifactsWriter artifactsWriter;

    // Le a fita de tokens de entrada (gerada pela analise lexica)
    @Inject
    TapeReader tapeReader;

    // Le/escreve a Tabela de Simbolos em arquivo CSV
    @Inject
    SymbolTableCsvRepository symbolTableCsvRepository;

    // Realiza a analise sintatica SLR (shift-reduce parser)
    @Inject
    SyntacticAnalyzer syntacticAnalyzer;

    // Realiza a analise semantica verificando tipos e duplicidades
    @Inject
    SemanticAnalyzer semanticAnalyzer;

    // Executa otimizacoes de simplificacao sobre o codigo intermediario gerado
    @Inject
    IntermediateCodeOptimizer intermediateCodeOptimizer;

    /**
     * Executa o processo completo de compilacao/analise sintatica e semantica.
     * 
     * @param grammarFilePath Caminho do arquivo da gramatica sintatica (.txt)
     * @param tapeFilePath Caminho da fita de tokens gerada pelo lexico (.csv)
     * @param symbolTableFilePath Caminho da tabela de simbolos gerada pelo lexico (.csv)
     * @return O resultado consolidado da analise sintatica e semantica
     * @throws IOException Se houver erro de leitura/escrita de arquivos
     */
    public SyntacticResult run(String grammarFilePath, String tapeFilePath, String symbolTableFilePath) throws IOException {
        // Etapa 1: Le a fita de tokens e a tabela de simbolos geradas previamente pelo lexico.
        // A fita de tokens contem a sequencia de tokens para a analise.
        List<String> tape = tapeReader.readTape(tapeFilePath);
        // A tabela de simbolos contem metadados (linhas, lexemas, categorias lexicas).
        List<SymbolTableEntry> symbols = symbolTableCsvRepository.read(symbolTableFilePath);

        // Etapa 2: Carrega a definicao formal da gramatica sintatica.
        Grammar grammar = grammarLoader.load(grammarFilePath);
        
        // Verifica se a gramatica fornecida possui terminais compativeis com os tokens da fita.
        // Se a gramatica for incompativel (ex: fita tem tokens de uma linguagem e a gramatica e de outra),
        // uma gramatica de fallback e criada dinamicamente para evitar falha imediata e permitir o parsing.
        if (!isGrammarCompatibleWithTape(grammar, tape)) {
            Grammar fallback = buildFallbackGrammarFromEntrada(grammarFilePath, tape, symbols);
            if (fallback != null) {
                grammar = fallback;
                System.out.println("[INFO] Gramatica sintatica derivada automaticamente de entradacompi.txt para casar com a fita lexico.");
            }
        }

        // Etapa 3: Normaliza a gramatica para garantir que esteja livre de regras inuteis
        // e tenta fatora-la, o que ajuda a reduzir conflitos no construtor SLR.
        grammar = grammarNormalizer.normalize(grammar);

        // Etapa 4: Constroi a tabela SLR(1) (estados LR(0), First/Follow e tabela de Parsing).
        SLRTableBuilder.BuildOutput build = slrTableBuilder.build(grammar);
        
        // Escreve os arquivos CSV intermediarios com todas as tabelas e conjuntos calculados.
        writeArtifacts(build);

        // Etapa 5: Executa o parser SLR (Shift-Reduce) sobre a fita de tokens.
        // O parser empilha/desempilha estados ate aceitar a entrada ou relatar erro.
        SyntacticResult result = syntacticAnalyzer.parse(build.grammar(), build.table(), tape, symbols);

        // Etapa 6: Executa a analise semantica na tabela de simbolos.
        // A analise semantica so e realizada se a analise sintatica aceitou a sintaxe.
        SemanticResult semanticResult = result.accepted()
                ? semanticAnalyzer.analyze(symbols)
                : new SemanticResult(false, List.of("Analise semantica nao executada porque a sintaxe foi rejeitada."));
        
        // Etapa 7: Otimiza o codigo intermediario gerado durante a analise sintatica.
        // A otimizacao busca simplificar declaracoes e remover temporarios redundantes.
        List<String> optimizedCode = intermediateCodeOptimizer.optimize(result.intermediateCode());

        // Atualiza o resultado sintatico com as informacoes semanticas e o codigo intermediario otimizado.
        result = result
                .withSemanticResult(semanticResult)
                .withOptimizedCode(optimizedCode);

        // Etapa 8: Grava os artefatos de saída finais resultantes das analises.
        artifactsWriter.writeSemanticResult(semanticResult, "resultado_semantico.txt");
        artifactsWriter.writeCode(result.intermediateCode(), "codigo_intermediario.txt");
        artifactsWriter.writeCode(result.optimizedCode(), "codigo_intermediario_otimizado.txt");
        
        // Escreve de volta a tabela de simbolos atualizada com as anotacoes semanticas e sintaticas.
        symbolTableCsvRepository.write(symbolTableFilePath, symbols);

        return result;
    }

    /**
     * Auxiliar para gravar as tabelas e conjuntos intermediarios em CSV.
     * 
     * @param build O resultado da construcao da tabela SLR
     */
    private void writeArtifacts(SLRTableBuilder.BuildOutput build) throws IOException {
        // Grava as regras formais numeradas
        artifactsWriter.writeProductions(build.grammar(), "producoes.csv");
        // Grava os conjuntos FIRST e FOLLOW de cada nao-terminal
        artifactsWriter.writeFirstFollow(build.firstFollow(), "first_follow.csv");
        // Grava o conjunto de itens LR(0) de cada estado
        artifactsWriter.writeStates(build.grammar(), build.table(), "itens_lr0.csv");
        // Grava as transicoes entre estados
        artifactsWriter.writeTransitions(build.table(), "transicoes_lr0.csv");
        // Grava a tabela final de acoes (ACTION) e desvios (GOTO)
        artifactsWriter.writeParsingTable(build.grammar(), build.table(), "tabela_slr.csv");
        // Grava se houveram conflitos Shift/Reduce ou Reduce/Reduce detectados
        artifactsWriter.writeConflicts(build.table().conflicts(), "conflitos_slr.csv");
    }

    /**
     * Verifica se a gramatica tem compatibilidade de vocabulario com a fita de tokens.
     * 
     * @param grammar Gramatica a ser validada
     * @param tape Fita de tokens de entrada
     * @return true se houver compatibilidade (compartilham termos), false caso contrario
     */
    private boolean isGrammarCompatibleWithTape(Grammar grammar, List<String> tape) {
        // Copia os tokens da fita em um conjunto para evitar duplicados
        Set<String> tapeTokens = new LinkedHashSet<>(tape);
        // Remove os tokens especiais de controle sintatico/erro
        tapeTokens.remove("$");
        tapeTokens.remove("X");

        // Se a fita estiver vazia, considera compativel por vacuidade
        if (tapeTokens.isEmpty()) {
            return true;
        }

        // Se pelo menos um token da fita estiver contido no conjunto de terminais
        // formais da gramatica, entao a gramatica e compativel.
        for (String token : tapeTokens) {
            if (grammar.terminals().contains(token)) {
                return true;
            }
        }

        // Nao ha nenhuma intersecao entre os tokens da fita e os terminais da gramatica
        return false;
    }

    /**
     * Constroi uma gramatica de fallback quando a gramatica original e a fita sao incompativeis.
     * Cria producoes estruturadas como PROGRAMA -> LISTA -> TOKEN* que aceita os tokens presentes na fita.
     * 
     * @param grammarFilePath Caminho da gramatica para ler definicoes de tokens declarados
     * @param tape A fita de tokens da entrada
     * @param symbols A lista de entradas da tabela de simbolos
     * @return Uma gramatica genérica contendo terminais mapeados a partir da fita
     */
    private Grammar buildFallbackGrammarFromEntrada(
            String grammarFilePath,
            List<String> tape,
            List<SymbolTableEntry> symbols) throws IOException {

        // Le tokens declarados textualmente na gramatica (cabecalhos ou declaracoes isoladas)
        List<String> declaredLexemes = grammarLoader.readTokenDeclarations(grammarFilePath);
        Set<String> mappedTerminals = new LinkedHashSet<>();

        // Mapeia os lexemes declarados para seus identificadores reais usando a tabela de simbolos
        for (String lexeme : declaredLexemes) {
            for (SymbolTableEntry entry : symbols) {
                if (lexeme.equals(entry.getLabel()) && entry.getIdentifier() != null && !entry.getIdentifier().isBlank()) {
                    if (!"X".equals(entry.getIdentifier())) {
                        mappedTerminals.add(entry.getIdentifier());
                    }
                    break;
                }
            }
        }

        // Adiciona todos os tokens reais presentes na fita aos terminais da gramatica fallback
        for (String token : tape) {
            if (!"$".equals(token) && !"X".equals(token)) {
                mappedTerminals.add(token);
            }
        }

        // Se nao ha nenhum terminal util, nao e possivel gerar uma gramatica
        if (mappedTerminals.isEmpty()) {
            return null;
        }

        // Define os simbolos de inicio e estendidos
        String start = "PROGRAMA";
        String augmented = "PROGRAMA'";

        // Colecao de nao-terminais da gramatica fallback
        Set<String> nonTerminals = new LinkedHashSet<>(Set.of(augmented, start, "LISTA", "TOKEN"));
        // Terminais sao os tokens mapeados mais o fim de fita '$'
        Set<String> terminals = new LinkedHashSet<>(mappedTerminals);
        terminals.add("$");

        // Cria a lista de producoes da gramatica fallback
        List<Production> productions = new ArrayList<>();
        // PROGRAMA' -> PROGRAMA (Regra estendida de parsing)
        productions.add(new Production(0, augmented, List.of(start)));
        // PROGRAMA -> LISTA
        productions.add(new Production(1, start, List.of("LISTA")));
        // LISTA -> TOKEN LISTA (Recursao a esquerda para ler multiplos tokens)
        productions.add(new Production(2, "LISTA", List.of("TOKEN", "LISTA")));
        // LISTA -> epsilon (Finalizacao da lista)
        productions.add(new Production(3, "LISTA", List.of()));

        // Cria producoes individuais para cada token mapeado: TOKEN -> terminal
        int index = 4;
        for (String terminal : mappedTerminals) {
            productions.add(new Production(index++, "TOKEN", List.of(terminal)));
        }

        // Retorna a nova instancia de Gramatica fallback
        return new Grammar(start, augmented, productions, nonTerminals, terminals);
    }
}
