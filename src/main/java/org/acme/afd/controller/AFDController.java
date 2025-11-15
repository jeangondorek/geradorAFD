package org.acme.afd.controller;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import org.acme.afd.model.Automaton;
import org.acme.afd.parser.InputParser;
import org.acme.afd.generator.AFNDGenerator;
import org.acme.afd.determinizer.Determinizer;
import org.acme.afd.printer.AutomatonPrinter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

@RequiredArgsConstructor
@ApplicationScoped
public class AFDController {
    private Automaton afnd;
    private Automaton afd;

    public void processFile(String filePath) throws IOException {
        InputParser parser;
        AFNDGenerator afndGenerator;
        Determinizer determinizer;

        System.out.println("Iniciando processamento do arquivo: " + filePath);

        // 1. Parser
        parser = new InputParser();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            parser.parse(reader);
        }
        parser.printParsedData();

        // 2. Gerar AFND
//        afndGenerator = new AFNDGenerator(parser.getTokens(), parser.getGrammars());
//        afnd = afndGenerator.generate();
//        afndGenerator.printAFND();
        AutomatonPrinter.printTransitionTable(afnd, "TABELA DE TRANSIÇÕES - AFND");

        // 3. Determinizar
//        determinizer = new Determinizer(afnd);
//        afd = determinizer.determinize();
//        determinizer.printDeterminizationInfo();
        AutomatonPrinter.printTransitionTable(afd, "TABELA DE TRANSIÇÕES - AFD");

        System.out.println("\nProcessamento concluído com sucesso!");
    }

    public Automaton getAFND() { return afnd; }
    public Automaton getAFD() { return afd; }
}
