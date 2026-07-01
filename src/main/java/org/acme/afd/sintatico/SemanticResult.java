package org.acme.afd.sintatico;

import java.util.List;

/**
 * Resultado da analise semantica.
 *
 * @param accepted indica se a entrada foi aceita semanticamente (sem erros)
 * @param errors   lista de mensagens de erro semantico encontradas
 */
public record SemanticResult(boolean accepted, List<String> errors) {
}
