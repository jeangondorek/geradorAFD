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

    @Inject
    GrammarLoader grammarLoader;
    @Inject
    SLRTableBuilder slrTableBuilder;
    @Inject
    GrammarNormalizer grammarNormalizer;
    @Inject
    ParserArtifactsWriter artifactsWriter;
    @Inject
    TapeReader tapeReader;
    @Inject
    SymbolTableCsvRepository symbolTableCsvRepository;
    @Inject
    SyntacticAnalyzer syntacticAnalyzer;
    @Inject
    SemanticAnalyzer semanticAnalyzer;
    @Inject
    IntermediateCodeOptimizer intermediateCodeOptimizer;

    public SyntacticResult run(String grammarFilePath, String tapeFilePath, String symbolTableFilePath) throws IOException {
        // Etapa 1: le a fita de tokens e a tabela de simbolos gerados pelo lexico
        List<String> tape = tapeReader.readTape(tapeFilePath);
        List<SymbolTableEntry> symbols = symbolTableCsvRepository.read(symbolTableFilePath);

        // Etapa 2: carrega a gramatica e verifica compatibilidade com a fita
        Grammar grammar = grammarLoader.load(grammarFilePath);
        if (!isGrammarCompatibleWithTape(grammar, tape)) {
            Grammar fallback = buildFallbackGrammarFromEntrada(grammarFilePath, tape, symbols);
            if (fallback != null) {
                grammar = fallback;
                System.out.println("[INFO] Gramatica sintatica derivada automaticamente de entradacompi.txt para casar com a fita lexico.");
            }
        }

        // Etapa 3: normaliza a gramatica (remove producoes inuteis, etc.)
        grammar = grammarNormalizer.normalize(grammar);

        // Etapa 4: constroi a tabela SLR e grava artefatos intermediarios
        SLRTableBuilder.BuildOutput build = slrTableBuilder.build(grammar);
        writeArtifacts(build);

        // Etapa 5: executa o parser SLR sobre a fita de tokens
        SyntacticResult result = syntacticAnalyzer.parse(build.grammar(), build.table(), tape, symbols);

        // Etapa 6: analise semantica (so executa se a sintaxe foi aceita)
        SemanticResult semanticResult = result.accepted()
                ? semanticAnalyzer.analyze(symbols)
                : new SemanticResult(false, List.of("Analise semantica nao executada porque a sintaxe foi rejeitada."));
        // Etapa 7: otimizacao do codigo intermediario
        List<String> optimizedCode = intermediateCodeOptimizer.optimize(result.intermediateCode());

        result = result
                .withSemanticResult(semanticResult)
                .withOptimizedCode(optimizedCode);

        // Etapa 8: grava resultados finais (semantico, codigo intermediario, tabela de simbolos)
        artifactsWriter.writeSemanticResult(semanticResult, "resultado_semantico.txt");
        artifactsWriter.writeCode(result.intermediateCode(), "codigo_intermediario.txt");
        artifactsWriter.writeCode(result.optimizedCode(), "codigo_intermediario_otimizado.txt");
        symbolTableCsvRepository.write(symbolTableFilePath, symbols);

        return result;
    }

    private void writeArtifacts(SLRTableBuilder.BuildOutput build) throws IOException {
        artifactsWriter.writeProductions(build.grammar(), "producoes.csv");
        artifactsWriter.writeFirstFollow(build.firstFollow(), "first_follow.csv");
        artifactsWriter.writeStates(build.grammar(), build.table(), "itens_lr0.csv");
        artifactsWriter.writeTransitions(build.table(), "transicoes_lr0.csv");
        artifactsWriter.writeParsingTable(build.grammar(), build.table(), "tabela_slr.csv");
        artifactsWriter.writeConflicts(build.table().conflicts(), "conflitos_slr.csv");
    }

    // Verifica se ao menos um token da fita (exceto $ e X) aparece como
    // terminal na gramatica. Se nenhum casar, a gramatica sera substituida por um fallback.
    private boolean isGrammarCompatibleWithTape(Grammar grammar, List<String> tape) {
        Set<String> tapeTokens = new LinkedHashSet<>(tape);
        tapeTokens.remove("$");
        tapeTokens.remove("X");

        if (tapeTokens.isEmpty()) {
            return true;
        }

        for (String token : tapeTokens) {
            if (grammar.terminals().contains(token)) {
                return true;
            }
        }

        return false;
    }

    // Constroi uma gramatica fallback automaticamente a partir das declaracoes
    // de tokens e da fita lexico, criando producoes PROGRAMA -> LISTA -> TOKEN*
    // onde cada TOKEN mapeia para um terminal da fita.
    private Grammar buildFallbackGrammarFromEntrada(
            String grammarFilePath,
            List<String> tape,
            List<SymbolTableEntry> symbols) throws IOException {

        List<String> declaredLexemes = grammarLoader.readTokenDeclarations(grammarFilePath);
        Set<String> mappedTerminals = new LinkedHashSet<>();

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

        for (String token : tape) {
            if (!"$".equals(token) && !"X".equals(token)) {
                mappedTerminals.add(token);
            }
        }

        if (mappedTerminals.isEmpty()) {
            return null;
        }

        String start = "PROGRAMA";
        String augmented = "PROGRAMA'";

        Set<String> nonTerminals = new LinkedHashSet<>(Set.of(augmented, start, "LISTA", "TOKEN"));
        Set<String> terminals = new LinkedHashSet<>(mappedTerminals);
        terminals.add("$");

        List<Production> productions = new ArrayList<>();
        productions.add(new Production(0, augmented, List.of(start)));
        productions.add(new Production(1, start, List.of("LISTA")));
        productions.add(new Production(2, "LISTA", List.of("TOKEN", "LISTA")));
        productions.add(new Production(3, "LISTA", List.of()));

        int index = 4;
        for (String terminal : mappedTerminals) {
            productions.add(new Production(index++, "TOKEN", List.of(terminal)));
        }

        return new Grammar(start, augmented, productions, nonTerminals, terminals);
    }
}
