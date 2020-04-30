package com.evolveum.axiom.lang.api;

import com.evolveum.axiom.api.AxiomIdentifier;

public enum AxiomBuiltInSimpleType implements AxiomTypeDefinition {

    IDENTIFIER("identifier"),
    STRING("string"),
    SEMANTIC_VERSION("SemanticVersion");

    private final AxiomIdentifier identifier;

    AxiomBuiltInSimpleType(String identifier) {
        this.identifier = AxiomIdentifier.axiom(identifier);
    }

    public AxiomIdentifier getIdentifier() {
        return identifier;
    }
}
