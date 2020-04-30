package com.evolveum.axiom.lang.impl;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomIdentifier;

public interface AxiomIdentifierResolver {

    final AxiomIdentifierResolver AXIOM_DEFAULT_NAMESPACE =  defaultNamespace(AxiomIdentifier.AXIOM_NAMESPACE);

    AxiomIdentifier resolveStatementIdentifier(@NotNull String prefix, @NotNull String localName);



    static AxiomIdentifierResolver defaultNamespace(String namespace) {
        return (prefix, localName) -> prefix == null ? AxiomIdentifier.from(namespace, localName) : null;
    }

}
