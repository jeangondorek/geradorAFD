package org.acme.afd.sintatico;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Carregador de gramatica a partir de arquivo de texto.
 * Le as linhas do arquivo, identifica producoes (separadas por ::= ou ->),
 * normaliza simbolos (removendo delimitadores < >), trata epsilon/ε como
 * producao vazia e cria o simbolo inicial aumentado para a analise SLR.
 */
@ApplicationScoped
public class GrammarLoader {

    public static final String EPSILON = "epsilon";

    // Carrega a gramatica: le as linhas, extrai producoes, determina o simbolo
    // inicial (declarado via %start ou o primeiro nao-terminal), cria o simbolo
    // aumentado e coleta terminais e nao-terminais.
    public Grammar load(String grammarFilePath) throws IOException {
        List<String> lines = readLines(grammarFilePath);

        String declaredStart = null;
        List<String[]> rawProductions = new ArrayList<>();
        Set<String> nonTerminals = new LinkedHashSet<>();

        for (String line : lines) {
            if (line.startsWith("%start")) {
                declaredStart = normalizeSymbol(line.substring("%start".length()).trim());
                continue;
            }

            String[] split = splitProduction(line);
            if (split == null) {
                continue;
            }

            String left = normalizeSymbol(split[0]);
            nonTerminals.add(left);
            rawProductions.add(new String[]{left, split[1]});
        }

        if (rawProductions.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma producao encontrada no arquivo de gramatica: " + grammarFilePath);
        }

        String startSymbol = declaredStart != null ? declaredStart : rawProductions.get(0)[0];
        String augmentedStart = startSymbol + "'";
        while (nonTerminals.contains(augmentedStart)) {
            augmentedStart = augmentedStart + "'";
        }

        List<Production> productions = new ArrayList<>();
        productions.add(new Production(0, augmentedStart, List.of(startSymbol)));

        int index = 1;
        for (String[] raw : rawProductions) {
            String left = raw[0];
            String rightExpr = raw[1];

            String[] alternatives = rightExpr.split("\\|");
            for (String alternative : alternatives) {
                List<String> right = parseRightSide(alternative.trim());
                productions.add(new Production(index++, left, right));
            }
        }

        nonTerminals.add(augmentedStart);

        Set<String> terminals = new LinkedHashSet<>();
        for (Production p : productions) {
            for (String symbol : p.right()) {
                if (!nonTerminals.contains(symbol) && !EPSILON.equals(symbol)) {
                    terminals.add(symbol);
                }
            }
        }
        terminals.add("$");

        return new Grammar(startSymbol, augmentedStart, productions, nonTerminals, terminals);
    }

    // Le declaracoes de tokens do arquivo de gramatica (linhas que nao sao
    // producoes nem %start), usadas como fallback para compatibilidade com a fita.
    public List<String> readTokenDeclarations(String grammarFilePath) throws IOException {
        List<String> lines = readLines(grammarFilePath);
        List<String> declaredTokens = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("%start")) {
                continue;
            }
            if (line.contains("::=") || line.contains("->")) {
                continue;
            }
            declaredTokens.add(normalizeSymbol(line));
        }

        return declaredTokens;
    }

    private List<String> readLines(String grammarFilePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(grammarFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = line.trim();
                if (normalized.isEmpty() || normalized.startsWith("#") || normalized.startsWith("//")) {
                    continue;
                }
                lines.add(normalized);
            }
        }
        return lines;
    }

    // Detecta o separador da producao (::= ou ->) e divide a linha em
    // lado esquerdo e lado direito. Retorna null se nao for uma producao.
    private String[] splitProduction(String line) {
        String separator = null;
        if (line.contains("::=")) {
            separator = "::=";
        } else if (line.contains("->")) {
            separator = "->";
        }

        if (separator == null) {
            return null;
        }

        String[] parts = line.split(separator, 2);
        if (parts.length != 2) {
            return null;
        }

        return new String[]{parts[0].trim(), parts[1].trim()};
    }

    // Analisa o lado direito de uma producao, tratando epsilon e $ como
    // producao vazia (lista vazia) e separando simbolos por espacos.
    private List<String> parseRightSide(String right) {
        if (right.isEmpty() || "$".equals(right)) {
            return List.of();
        }

        String normalizedRight = right
                .replace("<", "")
                .replace(">", "")
                .replace("\t", " ")
                .trim();

        if (!normalizedRight.contains(" ")) {
            String symbol = normalizeSymbol(normalizedRight);
            return EPSILON.equals(symbol) ? List.of() : List.of(symbol);
        }

        return Arrays.stream(normalizedRight.split("\\s+"))
                .map(this::normalizeSymbol)
                .filter(s -> !EPSILON.equals(s))
                .filter(s -> !s.isBlank())
                .toList();
    }

    // Remove delimitadores < > do simbolo e converte ε/epsilon para a
    // constante interna EPSILON.
    private String normalizeSymbol(String symbol) {
        String s = symbol.trim();
        if (s.startsWith("<") && s.endsWith(">") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        if ("ε".equals(s) || "epsilon".equalsIgnoreCase(s)) {
            return EPSILON;
        }
        return s;
    }
}
