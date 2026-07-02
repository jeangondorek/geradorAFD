package org.acme.afd.sintatico;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Leitor da fita de tokens de saida do analisador lexico.
 * Esta classe le um arquivo contendo tokens separados por virgula e 
 * prepara a fita no formato adequado para o analisador sintatico (SLR).
 * A fita gerada sempre garante ter o marcador de fim '$' no final.
 */
@ApplicationScoped
public class TapeReader {

    /**
     * Le a primeira linha do arquivo CSV de fita, separa os tokens por virgula,
     * remove espacos extras em branco e garante o caractere de parada '$' no fim.
     * 
     * @param filePath O caminho do arquivo contendo a fita de tokens
     * @return Uma lista de Strings contendo a fita de tokens normalizada
     * @throws IOException Se houver erro de leitura do arquivo
     */
    public List<String> readTape(String filePath) throws IOException {
        // Abre o arquivo para leitura linha por linha
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            
            // Se o arquivo estiver vazio, retorna apenas o delimitador de fim de fita '$'
            if (line == null || line.isBlank()) {
                return new ArrayList<>(List.of("$"));
            }

            // Divide a linha pelas virgulas, limpa espacos extras e ignora itens vazios
            List<String> tokens = new ArrayList<>(Arrays.stream(line.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList());

            // Garante que o ultimo caractere da fita seja o cifrao '$',
            // que indica o fim de sentenca na analise sintatica
            if (tokens.isEmpty() || !"$".equals(tokens.get(tokens.size() - 1))) {
                tokens.add("$");
            }

            return tokens;
        }
    }
}
