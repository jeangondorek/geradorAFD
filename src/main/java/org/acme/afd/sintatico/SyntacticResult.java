package org.acme.afd.sintatico;

import java.util.List;

/**
 * Encapsula o resultado completo da analise (sintatica, semantica e geracao de codigo).
 *
 * @param accepted          indica se a entrada foi aceita sintaticamente
 * @param errors            lista de erros sintaticos encontrados
 * @param semanticAccepted  indica se a entrada foi aceita semanticamente
 * @param semanticErrors    lista de erros semanticos encontrados
 * @param intermediateCode  linhas do codigo intermediario gerado
 * @param optimizedCode     linhas do codigo otimizado gerado
 */
public record SyntacticResult(
        boolean accepted,
        List<String> errors,
        boolean semanticAccepted,
        List<String> semanticErrors,
        List<String> intermediateCode,
        List<String> optimizedCode) {

    // Construtor compacto para quando so ha resultado sintatico (sem semantica/codigo)
    public SyntacticResult(boolean accepted, List<String> errors) {
        this(accepted, errors, true, List.of(), List.of(), List.of());
    }

    // Retorna nova instancia incorporando o resultado da analise semantica
    public SyntacticResult withSemanticResult(SemanticResult semanticResult) {
        return new SyntacticResult(
                accepted,
                errors,
                semanticResult.accepted(),
                semanticResult.errors(),
                intermediateCode,
                optimizedCode);
    }

    // Retorna nova instancia substituindo o codigo otimizado
    public SyntacticResult withOptimizedCode(List<String> optimizedCode) {
        return new SyntacticResult(
                accepted,
                errors,
                semanticAccepted,
                semanticErrors,
                intermediateCode,
                optimizedCode);
    }
}
