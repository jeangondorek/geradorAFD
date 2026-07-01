package org.acme.afd.model;

public class SymbolTableEntry {
    private int line;
    private String identifier;
    private String label;
    private String syntaxCategory;
    private String syntaxNote;
    private String semanticName;
    private String semanticType;
    private String semanticStatus;

    public SymbolTableEntry(int line, String identifier, String label, String syntaxCategory, String syntaxNote) {
        this(line, identifier, label, syntaxCategory, syntaxNote, "", "", "");
    }

    public SymbolTableEntry(
            int line,
            String identifier,
            String label,
            String syntaxCategory,
            String syntaxNote,
            String semanticName,
            String semanticType,
            String semanticStatus) {
        this.line = line;
        this.identifier = identifier;
        this.label = label;
        this.syntaxCategory = syntaxCategory;
        this.syntaxNote = syntaxNote;
        this.semanticName = semanticName;
        this.semanticType = semanticType;
        this.semanticStatus = semanticStatus;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSyntaxCategory() {
        return syntaxCategory;
    }

    public void setSyntaxCategory(String syntaxCategory) {
        this.syntaxCategory = syntaxCategory;
    }

    public String getSyntaxNote() {
        return syntaxNote;
    }

    public void setSyntaxNote(String syntaxNote) {
        this.syntaxNote = syntaxNote;
    }

    public String getSemanticName() {
        return semanticName;
    }

    public void setSemanticName(String semanticName) {
        this.semanticName = semanticName;
    }

    public String getSemanticType() {
        return semanticType;
    }

    public void setSemanticType(String semanticType) {
        this.semanticType = semanticType;
    }

    public String getSemanticStatus() {
        return semanticStatus;
    }

    public void setSemanticStatus(String semanticStatus) {
        this.semanticStatus = semanticStatus;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int line;
        private String identifier;
        private String label;
        private String syntaxCategory;
        private String syntaxNote;
        private String semanticName;
        private String semanticType;
        private String semanticStatus;

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder syntaxCategory(String syntaxCategory) {
            this.syntaxCategory = syntaxCategory;
            return this;
        }

        public Builder syntaxNote(String syntaxNote) {
            this.syntaxNote = syntaxNote;
            return this;
        }

        public Builder semanticName(String semanticName) {
            this.semanticName = semanticName;
            return this;
        }

        public Builder semanticType(String semanticType) {
            this.semanticType = semanticType;
            return this;
        }

        public Builder semanticStatus(String semanticStatus) {
            this.semanticStatus = semanticStatus;
            return this;
        }

        public SymbolTableEntry build() {
            return new SymbolTableEntry(
                    line,
                    identifier,
                    label,
                    syntaxCategory,
                    syntaxNote,
                    semanticName,
                    semanticType,
                    semanticStatus);
        }
    }
}
