package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.acme.afd.model.SymbolTableEntry;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SemanticAnalyzer {

    public SemanticResult analyze(List<SymbolTableEntry> symbolTable) {
        List<String> errors = new ArrayList<>();
        Set<String> declaredKeys = new LinkedHashSet<>();

        for (SymbolTableEntry entry : symbolTable) {
            // Caracteristica semantica: cada lexema reconhecido deve ter tipo e nome unicos por categoria.
            if ("X".equals(entry.getIdentifier())) {
                errors.add("Linha " + entry.getLine() + ": token lexico invalido nao pode ser analisado semanticamente.");
                continue;
            }

            if (isBlank(entry.getSyntaxCategory())) {
                errors.add("Linha " + entry.getLine() + ": simbolo sem categoria sintatica na tabela de simbolos.");
            }

            if (isBlank(entry.getSemanticName())) {
                errors.add("Linha " + entry.getLine() + ": simbolo sem nome semantico.");
            }

            if (isBlank(entry.getSemanticType())) {
                errors.add("Linha " + entry.getLine() + ": simbolo sem tipo semantico.");
            }

            String key = normalize(entry.getSyntaxCategory()) + "::" + normalize(entry.getSemanticName());
            if (!key.startsWith("::") && !declaredKeys.add(key)) {
                errors.add("Linha " + entry.getLine()
                        + ": lexema '" + entry.getSemanticName()
                        + "' duplicado na categoria " + entry.getSyntaxCategory() + ".");
            }
        }

        return new SemanticResult(errors.isEmpty(), errors);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
