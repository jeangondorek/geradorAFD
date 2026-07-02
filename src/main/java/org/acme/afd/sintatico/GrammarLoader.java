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
 * Carregador de gramatica sintatica a partir de arquivos de definicao textual.
 * Le as regras de producao e os simbolos, processa diretivas como %start,
 * normaliza a grafia dos simbolos (removendo delimitadores < >), traduz definicoes
 * de epsilon/ε para producoes vazias e cria a producao estendida (augmented start)
 * exigida para a geracao do automato SLR.
 */
@ApplicationScoped
public class GrammarLoader {

    // Constante representando a palavra-chave para producoes vazias (epsilon)
    public static final String EPSILON = "epsilon";

    /**
     * Carrega e compila o arquivo de gramatica fornecido.
     * 
     * @param grammarFilePath O caminho do arquivo contendo as regras da gramatica
     * @return Um objeto Grammar estruturado contendo terminais, nao-terminais e producoes
     * @throws IOException Se houver erro de leitura
     */
    public Grammar load(String grammarFilePath) throws IOException {
        // Le apenas as linhas validas do arquivo (sem comentarios ou vazias)
        List<String> lines = readLines(grammarFilePath);

        String declaredStart = null;
        List<String[]> rawProductions = new ArrayList<>();
        Set<String> nonTerminals = new LinkedHashSet<>();

        // Passo 1: Primeiro parsing para ler definicoes de %start e separar producoes brutas
        for (String line : lines) {
            // Se a linha definir o simbolo inicial da gramatica
            if (line.startsWith("%start")) {
                declaredStart = normalizeSymbol(line.substring("%start".length()).trim());
                continue;
            }

            // Tenta dividir a linha em Lado Esquerdo e Lado Direito de producao (::= ou ->)
            String[] split = splitProduction(line);
            if (split == null) {
                // Se nao possuir o separador, ignora (pode ser declaracao de token)
                continue;
            }

            String left = normalizeSymbol(split[0]); // Lado esquerdo (Nao-Terminal)
            nonTerminals.add(left);
            rawProductions.add(new String[]{left, split[1]}); // Armazena temporariamente para o segundo passo
        }

        // Valida se a gramatica nao esta vazia
        if (rawProductions.isEmpty()) {
            throw new IllegalArgumentException("Nenhuma producao encontrada no arquivo de gramatica: " + grammarFilePath);
        }

        // Passo 2: Define o simbolo de inicio da gramatica. 
        // Se nao foi explicitado via %start, assume o Lado Esquerdo da primeira producao declarada.
        String startSymbol = declaredStart != null ? declaredStart : rawProductions.get(0)[0];
        
        // Define o simbolo de inicio estendido (augmented start symbol, ex: PROGRAMA')
        // garantindo que ele seja unico e nao exista previamente na gramatica.
        String augmentedStart = startSymbol + "'";
        while (nonTerminals.contains(augmentedStart)) {
            augmentedStart = augmentedStart + "'";
        }

        // Cria a lista oficial de producoes estruturadas
        List<Production> productions = new ArrayList<>();
        
        // Regra de Producao 0: S' -> S (Exigida pelo parser SLR para representar o estado de aceitacao)
        productions.add(new Production(0, augmentedStart, List.of(startSymbol)));

        // Passo 3: Cria as producoes numeradas, resolvendo alternativas separadas por pipeline '|'
        int index = 1;
        for (String[] raw : rawProductions) {
            String left = raw[0];
            String rightExpr = raw[1];

            // Permite producoes na mesma linha divididas por '|' (ex: A ::= B | C)
            String[] alternatives = rightExpr.split("\\|");
            for (String alternative : alternatives) {
                List<String> right = parseRightSide(alternative.trim());
                productions.add(new Production(index++, left, right));
            }
        }

        // Adiciona o simbolo estendido ao conjunto de nao-terminais
        nonTerminals.add(augmentedStart);

        // Passo 4: Coleta os simbolos Terminais.
        // Qualquer simbolo que apareca no lado direito das regras e nao esteja no conjunto 
        // de nao-terminais (e nao seja epsilon) e classificado como terminal.
        Set<String> terminals = new LinkedHashSet<>();
        for (Production p : productions) {
            for (String symbol : p.right()) {
                if (!nonTerminals.contains(symbol) && !EPSILON.equals(symbol)) {
                    terminals.add(symbol);
                }
            }
        }
        // Adiciona o token de fim de sentenca '$' como terminal obrigatorio
        terminals.add("$");

        return new Grammar(startSymbol, augmentedStart, productions, nonTerminals, terminals);
    }

    /**
     * Le declaracoes de tokens declaradas soltas no arquivo de gramatica.
     * Utilizado pelo validador de compatibilidade para identificar a lista de termos basicos.
     * 
     * @param grammarFilePath O arquivo da gramatica
     * @return Lista de strings com os tokens declarados
     * @throws IOException Se houver erro de leitura
     */
    public List<String> readTokenDeclarations(String grammarFilePath) throws IOException {
        List<String> lines = readLines(grammarFilePath);
        List<String> declaredTokens = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("%start")) {
                continue;
            }
            // Ignora linhas que sao producoes de fato
            if (line.contains("::=") || line.contains("->")) {
                continue;
            }
            declaredTokens.add(normalizeSymbol(line));
        }

        return declaredTokens;
    }

    /**
     * Le as linhas do arquivo de texto limpando espacos, ignorando linhas em branco
     * e comentarios iniciados por '#' ou '//'.
     */
    private List<String> readLines(String grammarFilePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(grammarFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String normalized = line.trim();
                // Ignora linhas vazias ou comentarios
                if (normalized.isEmpty() || normalized.startsWith("#") || normalized.startsWith("//")) {
                    continue;
                }
                lines.add(normalized);
            }
        }
        return lines;
    }

    /**
     * Divide uma linha de producao usando os delimitadores :: ou ->.
     * Retorna array de 2 elementos: [Lado Esquerdo, Lado Direito] ou null.
     */
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

    /**
     * Faz o parsing do lado direito de uma producao, separando os simbolos individuais
     * por espaco, tratando epsilon/vazio e limpando delimitadores.
     * 
     * @param right O texto correspondente ao lado direito de uma producao
     * @return Lista contendo os simbolos individuais da producao
     */
    private List<String> parseRightSide(String right) {
        // Se for vazio ou o simbolo de fim de fita, retorna lista vazia (epsilon implicitamente)
        if (right.isEmpty() || "$".equals(right)) {
            return List.of();
        }

        // Limpa brackets angulares '< >' herdados da notacao BNF antiga e tabs
        String normalizedRight = right
                .replace("<", "")
                .replace(">", "")
                .replace("\t", " ")
                .trim();

        // Se houver apenas um unico simbolo no lado direito
        if (!normalizedRight.contains(" ")) {
            String symbol = normalizeSymbol(normalizedRight);
            // Se for epsilon, retorna lista vazia, senao a lista com o unico simbolo
            return EPSILON.equals(symbol) ? List.of() : List.of(symbol);
        }

        // Se houver multiplos simbolos separados por espaco
        return Arrays.stream(normalizedRight.split("\\s+"))
                .map(this::normalizeSymbol)
                .filter(s -> !EPSILON.equals(s)) // Remove ocorrencias do termo epsilon
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * Normaliza a grafia de um simbolo individual. 
     * Remove tags < > e converte a representacao de epsilon ('ε') para a constante padrao.
     */
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
