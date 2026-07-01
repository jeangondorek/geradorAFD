package org.acme.afd.model;

import java.util.Objects;

public class State {
    private String label;
    private boolean isFinal;
    private String tokenName;

    public State(String label, boolean isFinal, String tokenName) {
        this.label = label;
        this.isFinal = isFinal;
        this.tokenName = tokenName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        State state = (State) o;
        return Objects.equals(tokenName, state.tokenName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenName);
    }

    public static class Builder {
        private String label;
        private boolean isFinal;
        private String tokenName;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder isFinal(boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public Builder tokenName(String tokenName) {
            this.tokenName = tokenName;
            return this;
        }

        public State build() {
            return new State(label, isFinal, tokenName);
        }
    }
}
