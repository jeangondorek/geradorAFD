package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.acme.afd.model.SymbolTableEntry;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Analisador Semantico do Compilador.
 * Esta classe realiza validacoes sobre a tabela de simbolos construida.
 * Suas funcoes principais sao garantir a corretude dos tipos, verificar 
 * se todos os lexemas possuem suas propriedades sintaticas e semanticas preenchidas,
 * e impedir duplicidades (declaracoes duplicadas de um mesmo identificador 
 * dentro de uma mesma categoria sintatica).
 */
@ApplicationScoped
public class SemanticAnalyzer {

    /**
     * Realiza a analise semantica inspecionando cada entrada da tabela de simbolos.
     * 
     * @param symbolTable A lista contendo as entradas de metadados da tabela de simbolos
     * @return Um SemanticResult contendo o status de aprovado/rejeitado e as mensagens de erro
     */
    public SemanticResult analyze(List<SymbolTableEntry> symbolTable) {
        List<String> errors = new ArrayList<>();
        
        // Rastreia chaves no formato "categoria::nome" para detectar duplicidades
        Set<String> declaredKeys = new LinkedHashSet<>();

        for (SymbolTableEntry entry : symbolTable) {
            // Regra 1: Tokens categorizados como "X" sao erros lexicos do automato e invalida o semantico
            if ("X".equals(entry.getIdentifier())) {
                errors.add("Linha " + entry.getLine() + ": token lexico invalido nao pode ser analisado semanticamente.");
                continue;
            }

            // Regra 2: Garante que todos os campos obrigatorios foram devidamente preenchidos pelo parser
            if (isBlank(entry.getSyntaxCategory())) {
                errors.add("Linha " + entry.getLine() + ": simbolo sem categoria sintatica na tabela de simbolos.");
            }

            if (isBlank(entry.getSemanticName())) {
                errors.add("Linha " + entry.getLine() + ": simbolo sem nome semantico.");
            }

            if (isBlank(entry.getSemanticType())) {
                errors.add("Linha " + entry.getLine() + ": simbolo sem tipo semantico.");
            }

            // Regra 3: Checa se há declaracoes duplicadas.
            // Cria uma chave composta combinando Categoria Sintatica e Nome Semantico (ex: "id::soma").
            String key = normalize(entry.getSyntaxCategory()) + "::" + normalize(entry.getSemanticName());
            
            // Tenta adicionar a chave ao conjunto. Se retornar false, significa que o lexema
            // ja foi definido anteriormente na mesma categoria (Erro Semantico de Duplicidade).
            if (!key.startsWith("::") && !declaredKeys.add(key)) {
                errors.add("Linha " + entry.getLine()
                        + ": lexema '" + entry.getSemanticName()
                        + "' duplicado na categoria " + entry.getSyntaxCategory() + ".");
            }
        }

        // Retorna aceito se a lista de erros estiver vazia
        return new SemanticResult(errors.isEmpty(), errors);
    }

    /**
     * Verifica se uma String e nula, vazia ou composta apenas por espacos.
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Normaliza a chave para comparacao semantica, removendo espacos
     * e convertendo para minusculo de forma insensivel a caixa (case-insensitive).
     */
    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
