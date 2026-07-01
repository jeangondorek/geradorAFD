package org.acme.afd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.acme.afd.analisadorlexico.Lexical;
import org.acme.afd.controller.AFDController;
import org.acme.afd.model.Automaton;
import org.acme.afd.model.State;
import org.acme.afd.model.Transition;
import org.acme.afd.sintatico.SyntacticResult;
import org.acme.afd.sintatico.SyntacticService;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Ponto de entrada do programa. Orquestra todo o pipeline de compilacao:
 * leitura do arquivo de entrada, geracao do AFND, determinizacao para AFD,
 * analise lexica, analise sintatica, analise semantica e geracao de codigo intermediario.
 * Os resultados sao exibidos no console e exportados em arquivos CSV.
 */
@ApplicationScoped
public class InitializeProgram {

    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "==|!=|<=|>=|&&|\\|\\||\\+\\+|--|->|=>|::|[{}()\\[\\],;.:=+\\-*/<>!%&|^~?]|[^\\s{}()\\[\\],;.:=+\\-*/<>!%&|^~?]+"
    );

    @Inject
    AFDController controller;
    @Inject
    Lexical lexical;
    @Inject
    SyntacticService syntacticService;

    private static boolean initialized = false;

    /** Metodo de inicializacao executado automaticamente pelo CDI ao subir a aplicacao. */
    @PostConstruct
    public void initialize() throws IOException {
        if (initialized) return;
        initialized = true;

        String inputFile = "entradacompi.txt";

        System.out.println("==== Inicializando programa ====");
        System.out.println("Lendo arquivo: " + inputFile + "\n");

        // Etapa 1: gera o AFND a partir do arquivo de entrada e o determiniza para AFD
        Automaton afnd = controller.processFileAfnd(inputFile);
        Automaton afd = controller.processFileAfd(afnd);

        // Etapa 2: exporta as tabelas de transicao do AFND e do AFD para arquivos CSV
        System.out.println("\nGerando CSVs...");
        converterParaCSVSomenteEstados(afnd, "afnd.csv");
        if (afd != null) {
            converterParaCSVSomenteEstados(afd, "afd.csv");
        }

        try {
            // Etapa 3: analise lexica — tokeniza o arquivo fonte e classifica os tokens pelo AFD
            lexical.analyze(afd, processFileTokens("teste.txt"));

            // Etapa 4: analise sintatica, semantica e geracao de codigo intermediario
            SyntacticResult syntacticResult = syntacticService.run(
                    inputFile,
                    "fita.csv",
                    "tabela_simbolos.csv");

            if (syntacticResult.accepted()) {
                System.out.println("\n[OK] Reconhecimento sintatico: ACEITO");
            } else {
                System.out.println("\n[ERRO] Reconhecimento sintatico: REJEITADO");
                for (String error : syntacticResult.errors()) {
                    System.out.println("  - " + error);
                }
            }

            if (syntacticResult.semanticAccepted()) {
                System.out.println("[OK] Analise semantica: ACEITO");
            } else {
                System.out.println("[ERRO] Analise semantica: REJEITADO");
                for (String error : syntacticResult.semanticErrors()) {
                    System.out.println("  - " + error);
                }
            }

            System.out.println("Codigo intermediario gerado em: codigo_intermediario.txt");
            System.out.println("Codigo intermediario otimizado gerado em: codigo_intermediario_otimizado.txt");
        } catch (IOException e) {
            System.out.println("Arquivo de código não encontrado para reconhecimento léxico (" + inputFile + "). Crie um arquivo com esse nome testar a fita.");
        } catch (Exception e) {
            System.out.println("Falha na etapa sintatica: " + e.getMessage());
        }


    }

    /**
     * Le o arquivo-fonte e o tokeniza usando expressao regular, retornando
     * a lista de tokens individuais (palavras, simbolos e operadores).
     */
    public List<String> processFileTokens(String filePath) throws IOException {
        List<String> tokens = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Substitui sequencias de escape (\n, \t, \r) por espaco
                String normalizedLine = line
                    .replaceAll("\\\\+[ntr]", " ");

                // Extrai tokens individuais usando a expressao regular
                Matcher matcher = TOKEN_PATTERN.matcher(normalizedLine);
                while (matcher.find()) {
                    String token = matcher.group().trim();
                    if (!token.isEmpty()) {
                        tokens.add(token);
                    }
                }
            }
        }

        return tokens;
    }

    /**
     * Exporta a tabela de transicao do automato para um arquivo CSV.
     * Cada linha representa um estado e cada coluna um simbolo do alfabeto.
     */
    public static void converterParaCSVSomenteEstados(Automaton automaton, String fileName) {
        if (automaton == null) {
            System.out.println("O autômato fornecido é nulo.");
            return;
        }

        System.out.println("\n=== DEBUG: Transições registradas ===");
        for (Transition t : automaton.getTransitions()) {
            String source = (t.getSource() == null) ? "<NULL>" : t.getSource().getTokenName();
            String target = (t.getTarget() == null) ? "<NULL>" : t.getTarget().getTokenName();
            String symbol = (t.getSymbol() == null || t.getSymbol().isEmpty()) ? "ε" : t.getSymbol();
            System.out.printf("%s --(%s)--> %s%n", source, symbol, target);
        }

        System.out.println("\n=== DEBUG: Estados do Autômato ===");
        for (State s : automaton.getStates()) {
            System.out.printf("Estado: %s [label=%s, final=%s]%n", s.getTokenName(), s.getLabel(), s.isFinal());
        }

        // Filtra simbolos validos do alfabeto para montar o cabecalho do CSV
        List<String> alphabet = automaton.getAlphabet().stream()
                .filter(s -> s != null && !s.isEmpty())
                .toList();

        try (FileWriter writer = new FileWriter(fileName)) {

            // Escreve o cabecalho: "Estado,simbolo1,simbolo2,..."
            writer.append("Estado");
            for (String symbol : alphabet) {
                writer.append(",").append(symbol);
            }
            writer.append("\n");

            // Para cada estado, busca os destinos de cada simbolo e preenche as celulas
            for (State state : automaton.getStates()) {
                String stateDisplay = formatStateName(state, automaton.getInitialState());

                writer.append(stateDisplay);

                for (String symbol : alphabet) {
                    Set<String> targets = new LinkedHashSet<>();

                    for (Transition t : automaton.getTransitions()) {
                        boolean isFromThisState = false;
                        if (t.getSource() != null && t.getSource().equals(state)) {
                            isFromThisState = true;
                        }

                        if (isFromThisState && 
                            t.getSymbol() != null && 
                            t.getSymbol().equals(symbol) && 
                            t.getTarget() != null) {
                            targets.add(t.getTarget().getTokenName());
                        }
                    }

                    writer.append(",");
                    if (targets.isEmpty()) {
                        writer.append("--");
                    } else {
                        writer.append(String.join("-", targets));
                    }
                }
                writer.append("\n");
            }

            System.out.println("Arquivo CSV gerado em: " + fileName);

        } catch (IOException e) {
            System.err.println("Erro ao escrever CSV: " + e.getMessage());
        }
    }

    /** Formata o nome do estado para exibicao: prefixo '->' para inicial, sufixo '*' para final. */
    private static String formatStateName(State state, State initialState) {
        String name = state.getTokenName();
        if (state.equals(initialState)) {
            name = "-> " + name;
        }
        if (state.isFinal()) {
            name += "*";
        }
        return name;
    }
}
