package org.acme.afd.sintatico;

/**
 * Representa uma acao do parser SLR (shift, reduce ou accept).
 *
 * @param type   tipo da acao (SHIFT, REDUCE ou ACCEPT)
 * @param target estado destino (para SHIFT) ou indice da producao (para REDUCE)
 */
public record ParsingAction(ActionType type, int target) {

    // Gera representacao textual compacta: "s3" (shift 3), "r2" (reduce 2), "acc" (aceitar)
    @Override
    public String toString() {
        return switch (type) {
            case SHIFT -> "s" + target;
            case REDUCE -> "r" + target;
            case ACCEPT -> "acc";
        };
    }
}
