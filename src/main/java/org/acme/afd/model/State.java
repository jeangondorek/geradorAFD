package org.acme.afd.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@RequiredArgsConstructor
public class State {
    private final String id;
    private boolean isFinal;
    private String tokenName;
}
