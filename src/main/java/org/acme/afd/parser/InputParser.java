package org.acme.afd.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputParser {
    private final List<String> tokens;
    private final Map<String, String> grammars;

    public InputParser() {
        this.tokens = new ArrayList<>();
        this.grammars = new LinkedHashMap<>();
    }

    public void parse(BufferedReader reader) throws IOException {
        String line;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) {
                continue;
            }
            
            if (line.contains("::=")) {
                parseGrammar(line);
            } else {
                tokens.add(line);
            }
        }
    }

    private void parseGrammar(String grammarLine) {
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
    }

    public List<String> getTokens() {
        return new ArrayList<>(tokens);
    }

    public Map<String, String> getGrammars() {
        return new LinkedHashMap<>(grammars);
    }

    public void printParsedData() {
        System.out.println("===== TOKENS =====");
        for (String token : tokens) {
            System.out.println("  " + token);
        }
        
        System.out.println("\n===== GRAMÁTICAS =====");
        for (Map.Entry<String, String> entry : grammars.entrySet()) {
            System.out.println("  <" + entry.getKey() + "> ::= " + entry.getValue());
        }
    }
}
