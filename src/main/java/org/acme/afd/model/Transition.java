package org.acme.afd.model;

public class Transition {
    private final State source;
    private final String symbol;
    private final State target;

    public Transition(State source, String symbol, State target) {
        this.source = source;
        this.symbol = symbol;
        this.target = target;
    }

    public State getSource() {
        return source;
    }

    public String getSymbol() {
        return symbol;
    }

    public State getTarget() {
        return target;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private State source;
        private String symbol;
        private State target;

        public Builder source(State source) {
            this.source = source;
            return this;
        }

        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder target(State target) {
            this.target = target;
            return this;
        }

        public Transition build() {
            return new Transition(source, symbol, target);
        }
    }
}
