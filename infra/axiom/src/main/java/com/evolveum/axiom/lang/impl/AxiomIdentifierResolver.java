/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
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
