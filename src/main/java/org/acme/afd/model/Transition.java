package org.acme.afd.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Transition {
    private final State source;
    private final String symbol;
    private final State target;
}
