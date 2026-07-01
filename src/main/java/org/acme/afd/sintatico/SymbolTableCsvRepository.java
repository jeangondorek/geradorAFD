package org.acme.afd.sintatico;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.acme.afd.model.SymbolTableEntry;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Repositorio CSV para leitura e escrita da tabela de simbolos.
 * Formato de 8 colunas: Linha, Identificador, Rotulo, CategoriaSintatica,
 * ObservacaoSintatica, NomeSemantico, TipoSemantico, StatusSemantico.
 */
@ApplicationScoped
public class SymbolTableCsvRepository {

    // Le o arquivo CSV pulando o cabecalho e mapeando cada linha para
    // um SymbolTableEntry com os 8 campos.
    public List<SymbolTableEntry> read(String filePath) throws IOException {
        List<SymbolTableEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line == null) {
                return entries;
            }

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                String[] cols = line.split(",", -1);
                int lineNumber = cols.length > 0 && !cols[0].isBlank() ? Integer.parseInt(cols[0].trim()) : entries.size() + 1;
                String identifier = cols.length > 1 ? cols[1].trim() : "";
                String label = cols.length > 2 ? cols[2].trim() : "";
                String syntaxCategory = cols.length > 3 ? cols[3].trim() : "";
                String syntaxNote = cols.length > 4 ? cols[4].trim() : "";
                String semanticName = cols.length > 5 ? cols[5].trim() : "";
                String semanticType = cols.length > 6 ? cols[6].trim() : "";
                String semanticStatus = cols.length > 7 ? cols[7].trim() : "";

                entries.add(SymbolTableEntry.builder()
                        .line(lineNumber)
                        .identifier(identifier)
                        .label(label)
                        .syntaxCategory(syntaxCategory)
                        .syntaxNote(syntaxNote)
                        .semanticName(semanticName)
                        .semanticType(semanticType)
                        .semanticStatus(semanticStatus)
                        .build());
            }
        }

        return entries;
    }

    // Grava a tabela de simbolos no CSV, escrevendo o cabecalho e cada
    // entrada com virgulas escapadas para ponto-e-virgula.
    public void write(String filePath, List<SymbolTableEntry> entries) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.append("Linha,Identificador/Estado,Rotulo/Token,CategoriaSintatica,ObservacaoSintatica,NomeSemantico,TipoSemantico,StatusSemantico\n");
            for (SymbolTableEntry entry : entries) {
                writer.append(Integer.toString(entry.getLine())).append(",")
                        .append(safe(entry.getIdentifier())).append(",")
                        .append(safe(entry.getLabel())).append(",")
                        .append(safe(entry.getSyntaxCategory())).append(",")
                        .append(safe(entry.getSyntaxNote())).append(",")
                        .append(safe(entry.getSemanticName())).append(",")
                        .append(safe(entry.getSemanticType())).append(",")
                        .append(safe(entry.getSemanticStatus())).append("\n");
            }
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", ";");
    }
}
