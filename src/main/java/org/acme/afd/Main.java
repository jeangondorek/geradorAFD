package org.acme.afd;

import org.acme.afd.controller.AFDController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    
    public static void main(String[] args) {
        AFDController controller = new AFDController();

        try {
            String inputFile = "entrada.txt";
            
            System.out.println("Lendo arquivo: " + inputFile + "\n");
            controller.processFile(inputFile);
            
            System.out.println("\n\n");
            gerarCSVs(controller);

        } catch (IOException e) {
            System.err.println("❌ Erro: " + e.getMessage());
        }
    }

    private static void gerarCSVs(AFDController controller) {
        System.out.println("📁 GERANDO CSVs...\n");
        
        try {
            String afndCsv = converterParaCSV(controller.getAFND());
            Files.write(Paths.get("AFND.csv"), afndCsv.getBytes());
            System.out.println("✅ AFND.csv gerado com sucesso");
        } catch (IOException e) {
            System.err.println("❌ Erro ao gerar AFND.csv: " + e.getMessage());
        }

        try {
            String afdCsv = converterParaCSV(controller.getAFD());
            Files.write(Paths.get("AFD.csv"), afdCsv.getBytes());
            System.out.println("✅ AFD.csv gerado com sucesso");
        } catch (IOException e) {
            System.err.println("❌ Erro ao gerar AFD.csv: " + e.getMessage());
        }
    }

    private static String converterParaCSV(org.acme.afd.model.Automaton automaton) {
        StringBuilder csv = new StringBuilder();

        csv.append("Estado,");
//        for (String symbol : automaton.getAlphabet()) {
//            csv.append(symbol).append(",");
//        }
//        csv.append("Final,Token\n");
//
//        for (org.acme.afd.model.State state : automaton.getStates()) {
//            csv.append(state.getId()).append(",");
//
//            for (String symbol : automaton.getAlphabet()) {
//                var trans = automaton.getTransitions(state, symbol);
//                if (trans.isEmpty()) {
//                    csv.append("-,");
//                } else {
//                    for (org.acme.afd.model.Transition t : trans) {
//                        csv.append(t.getTarget().getId()).append(";");
//                    }
//                    csv.append(",");
//                }
//            }
//
//            csv.append(state.isFinal() ? "SIM" : "NAO").append(",");
//            csv.append(state.getTokenName() != null ? state.getTokenName() : "").append("\n");
//        }
        
        return csv.toString();
    }
}
