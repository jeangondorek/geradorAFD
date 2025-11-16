package org.acme.afd.parser;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
@Getter
public class InputParser {

    public List<String> parse(BufferedReader reader) throws IOException {
        List<String> tokens = new ArrayList<>();
        String line;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                continue;
            }

            if (!line.contains("::=")) {
                tokens.add(line);
            }
        }

        return tokens;
    }

    public Map<String, String> parseGrammar(String grammarLine) {
        Map<String, String> grammars = new HashMap<>();
        Pattern symbolPattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = symbolPattern.matcher(grammarLine);
        
        if (matcher.find()) {
            String symbol = matcher.group(1);
            
            int separator = grammarLine.indexOf("::=");
            if (separator != -1) {
                String rightSide = grammarLine.substring(separator + 3).trim();
                grammars.put(symbol, rightSide);
            }
        }
        return grammars;
    }

    public void printParsedData(List<String> tokens, Map<String, String> grammars) {
        System.out.println("===== TOKENS =====");
        if (tokens == null || tokens.isEmpty()) {
            System.out.println("  Nenhum token encontrado.");
        } else {
            for (String token : tokens) {
                System.out.println("  " + token);
            }
        }

        System.out.println("\n===== GRAMÁTICAS =====");
        if (grammars == null || grammars.isEmpty()) {
            System.out.println("  Nenhuma gramática encontrada.");
        } else {
            for (Map.Entry<String, String> entry : grammars.entrySet()) {
                System.out.println("  <" + entry.getKey() + "> ::= " + entry.getValue());
            }
        }
    }
}
