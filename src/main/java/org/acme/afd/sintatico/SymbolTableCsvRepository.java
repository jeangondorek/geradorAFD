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
 * Repositorio responsavel por ler e escrever a tabela de simbolos no formato CSV.
 * Esta tabela mantem informacoes coletadas no lexico e enriquecidas durante 
 * a analise sintatica e semantica.
 * 
 * Formato esperado de 8 colunas:
 * Linha, Identificador/Estado, Rotulo/Token, CategoriaSintatica, 
 * ObservacaoSintatica, NomeSemantico, TipoSemantico, StatusSemantico.
 */
@ApplicationScoped
public class SymbolTableCsvRepository {

    /**
     * Le o arquivo CSV da tabela de simbolos, ignorando a linha de cabecalho,
     * e mapeando cada linha para um objeto SymbolTableEntry.
     * 
     * @param filePath Caminho do arquivo CSV
     * @return Lista de entradas da tabela de simbolos
     * @throws IOException Se houver falha na leitura do arquivo
     */
    public List<SymbolTableEntry> read(String filePath) throws IOException {
        List<SymbolTableEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine(); // Le e descarta a primeira linha (cabecalho)
            if (line == null) {
                return entries;
            }

            // Le as linhas subsequentes do CSV
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                // Divide a linha pelas virgulas. O parametro -1 no split garante 
                // que colunas vazias no final da linha sejam mantidas no array.
                String[] cols = line.split(",", -1);
                
                // Le o numero da linha. Se nao houver valor valido, gera sequencial.
                int lineNumber = cols.length > 0 && !cols[0].isBlank() ? Integer.parseInt(cols[0].trim()) : entries.size() + 1;
                
                // Mapeia cada coluna para seu respectivo atributo
                String identifier = cols.length > 1 ? cols[1].trim() : "";
                String label = cols.length > 2 ? cols[2].trim() : "";
                String syntaxCategory = cols.length > 3 ? cols[3].trim() : "";
                String syntaxNote = cols.length > 4 ? cols[4].trim() : "";
                String semanticName = cols.length > 5 ? cols[5].trim() : "";
                String semanticType = cols.length > 6 ? cols[6].trim() : "";
                String semanticStatus = cols.length > 7 ? cols[7].trim() : "";

                // Instancia o objeto utilizando o builder do lombok/manual
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

    /**
     * Grava a lista atualizada de entradas da tabela de simbolos de volta no arquivo CSV.
     * 
     * @param filePath Caminho do arquivo de destino
     * @param entries Lista de SymbolTableEntry contendo os metadados finais
     * @throws IOException Se houver erro de escrita
     */
    public void write(String filePath, List<SymbolTableEntry> entries) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Escreve a linha de cabecalho do CSV
            writer.append("Linha,Identificador/Estado,Rotulo/Token,CategoriaSintatica,ObservacaoSintatica,NomeSemantico,TipoSemantico,StatusSemantico\n");
            
            // Escreve cada entrada linha por linha
            for (SymbolTableEntry entry : entries) {
                // Utiliza a funcao safe() para garantir que virgulas internas nao quebrem as colunas do CSV
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

    /**
     * Limpa o valor de um campo para a escrita no CSV. 
     * Transforma valores nulos em string vazia e substitui virgulas (,) 
     * por ponto-e-virgula (;) para nao quebrar a formatacao do arquivo CSV.
     * 
     * @param value O valor original
     * @return O valor limpo para o CSV
     */
    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", ";");
    }
}
