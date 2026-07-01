package org.acme.afd.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.afd.model.Automaton;
import org.acme.afd.parser.InputParser;
import org.acme.afd.generator.AFNDGenerator;
import org.acme.afd.determinizer.Determinizer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador que orquestra a geracao do automato finito.
 * Realiza duas etapas: (1) leitura e parsing do arquivo de entrada para gerar o AFND,
 * e (2) determinizacao do AFND para obter o AFD.
 */
@ApplicationScoped
public class AFDController {

    @Inject
    AFNDGenerator afndGenerator;
    @Inject
    Determinizer determinizer;
    @Inject
    InputParser parser;

    /** Le o arquivo de entrada, extrai tokens e gramaticas, e gera o AFND. */
    public Automaton processFileAfnd(String filePath) throws IOException {
        System.out.println("Iniciando processamento do arquivo: " + filePath);

        // Listas para armazenar os tokens e as gramaticas extraidas do arquivo
        List<String> parserDataTokens;
        Map<String, String> parserDataGrammar = new LinkedHashMap<>();

        // Abre dois leitores: um para tokens e outro para regras gramaticais (BNF)
        try (BufferedReader tokenReader = new BufferedReader(new FileReader(filePath));
             BufferedReader grammarReader = new BufferedReader(new FileReader(filePath))) {

            // 1. Extrai os tokens do arquivo
            parserDataTokens = parser.parse(tokenReader);

            String line;
            while ((line = grammarReader.readLine()) != null) {
                line = line.trim();
                if (line.contains("::=")) {
                    parserDataGrammar.putAll(parser.parseGrammar(line));
                }
            }

            parser.printParsedData(parserDataTokens, parserDataGrammar);

            // 2. Gerar AFND
            Automaton afnd = afndGenerator.generate(parserDataTokens, parserDataGrammar);

            System.out.println("\nProcessamento concluído AFND com sucesso!");
            return afnd;
        }
    }

    /** Recebe o AFND gerado e aplica o algoritmo de determinizacao para obter o AFD. */
    public Automaton processFileAfd(Automaton afnd) {
        // 3. Determinizar
        Automaton afd = determinizer.determinize(afnd);

        System.out.println("\nProcessamento concluído AFD com sucesso!");
        return afd;
    }
}
