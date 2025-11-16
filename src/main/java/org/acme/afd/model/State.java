package org.acme.afd.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class State {
    private String label;
    private boolean isFinal;
    private String tokenName;
}
