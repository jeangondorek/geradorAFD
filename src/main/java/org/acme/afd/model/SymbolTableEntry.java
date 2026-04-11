package org.acme.afd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SymbolTableEntry {
    private int line;
    private String identifier;
    private String label;
}
