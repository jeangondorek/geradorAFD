package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class IntermediateCodeOptimizer {

    private static final Pattern TEMP_ASSIGNMENT = Pattern.compile("^(t\\d+) = (.+)$");

    public List<String> optimize(List<String> intermediateCode) {
        List<String> optimized = new ArrayList<>();

        for (int i = 0; i < intermediateCode.size(); i++) {
            String current = intermediateCode.get(i);
            Matcher tempMatcher = TEMP_ASSIGNMENT.matcher(current);

            if (tempMatcher.matches() && i + 1 < intermediateCode.size()) {
                // Etapa 4: propagacao de copia remove temporarios usados uma unica vez.
                String temp = tempMatcher.group(1);
                String expression = tempMatcher.group(2);
                String next = intermediateCode.get(i + 1);
                String copySuffix = " = " + temp;

                if (next.endsWith(copySuffix)) {
                    String target = next.substring(0, next.length() - copySuffix.length());
                    optimized.add(target + " = " + expression);
                    i++;
                    continue;
                }
            }

            optimized.add(current);
        }

        return optimized;
    }
}
