package org.acme.afd.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class Transition {
    private final State source;
    private final String symbol;
    private final State target;
}
