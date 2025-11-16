package org.acme.afd.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class State {
    private String label;
    private boolean isFinal;
    @EqualsAndHashCode.Include
    private String tokenName;
}
