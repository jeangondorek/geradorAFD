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

@ApplicationScoped
public class AFDController {

    @Inject
    AFNDGenerator afndGenerator;
    @Inject
    Determinizer determinizer;
    @Inject
    InputParser parser;

    public Automaton processFileAfnd(String filePath) throws IOException {
        System.out.println("Iniciando processamento do arquivo: " + filePath);

        List<String> parserDataTokens;
        Map<String, String> parserDataGrammar = new LinkedHashMap<>();

        try (BufferedReader tokenReader = new BufferedReader(new FileReader(filePath));
             BufferedReader grammarReader = new BufferedReader(new FileReader(filePath))) {

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

    public Automaton processFileAfd(Automaton afnd) {
        // 3. Determinizar
        Automaton afd = determinizer.determinize(afnd);
        determinizer.printDeterminizationInfo();

        System.out.println("\nProcessamento concluído AFD com sucesso!");
        return afd;
    }
}
