package org.acme.afd.sintatico;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Leitor da fita de saida lexico (arquivo CSV).
 * Le os tokens separados por virgula, remove espacos e garante
 * que a fita sempre termine com o marcador de fim $.
 */
@ApplicationScoped
public class TapeReader {

    // Le a primeira linha do arquivo CSV, separa tokens por virgula
    // e adiciona $ ao final se necessario.
    public List<String> readTape(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return new ArrayList<>(List.of("$"));
            }

            List<String> tokens = new ArrayList<>(Arrays.stream(line.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());

            if (tokens.isEmpty() || !"$".equals(tokens.get(tokens.size() - 1))) {
                tokens.add("$");
            }

            return tokens;
        }
    }
}
