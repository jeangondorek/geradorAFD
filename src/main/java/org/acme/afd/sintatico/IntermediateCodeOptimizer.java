package org.acme.afd.sintatico;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Otimizador de Codigo Intermediario.
 * Esta classe aplica otimizacoes basicas sobre o codigo de tres enderecos gerado.
 * Atualmente implementa a tecnica de "Propagacao de Copias", eliminando variaveis
 * temporarias intermediarias inuteis (ex: transforma "t1 = exp; a = t1;" em "a = exp;").
 */
@ApplicationScoped
public class IntermediateCodeOptimizer {

    // Expressao regular para casar com atribuicoes a variaveis temporarias (ex: "t1 = TOKEN(...)")
    // Grupo 1 captura o temporario (ex: "t1"), Grupo 2 captura a expressao (ex: "TOKEN(...)")
    private static final Pattern TEMP_ASSIGNMENT = Pattern.compile("^(t\\d+) = (.+)$");

    /**
     * Otimiza a lista de instrucoes de codigo intermediario.
     * 
     * @param intermediateCode A lista original de instrucoes brutas
     * @return Uma nova lista de instrucoes otimizada
     */
    public List<String> optimize(List<String> intermediateCode) {
        List<String> optimized = new ArrayList<>();

        for (int i = 0; i < intermediateCode.size(); i++) {
            String current = intermediateCode.get(i);
            Matcher tempMatcher = TEMP_ASSIGNMENT.matcher(current);

            // Se a linha atual e uma atribuicao temporaria e existe uma linha subsequente
            if (tempMatcher.matches() && i + 1 < intermediateCode.size()) {
                String temp = tempMatcher.group(1);        // ex: "t1"
                String expression = tempMatcher.group(2);  // ex: "TOKEN(IF, se)"
                String next = intermediateCode.get(i + 1); // ex: "IF = t1"
                
                String copySuffix = " = " + temp; // ex: " = t1"

                // Se a proxima linha for a atribuicao de copia do temporario para a variavel real
                if (next.endsWith(copySuffix)) {
                    // Extrai o nome da variavel real de destino (ex: "IF")
                    String target = next.substring(0, next.length() - copySuffix.length());
                    
                    // Une as duas linhas em uma unica instrucao direta (ex: "IF = TOKEN(IF, se)")
                    optimized.add(target + " = " + expression);
                    
                    // Avanca o indice para pular a proxima linha ja otimizada/consumida
                    i++;
                    continue;
                }
            }

            // Se nao couber na regra de otimizacao, mantem a instrucao original intacta
            optimized.add(current);
        }

        return optimized;
    }
}
