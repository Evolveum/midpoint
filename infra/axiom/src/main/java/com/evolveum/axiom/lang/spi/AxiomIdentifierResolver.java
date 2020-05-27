/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.spi;

import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomIdentifier;
import com.evolveum.axiom.api.AxiomItemDefinition;
import com.evolveum.axiom.api.AxiomTypeDefinition;
import com.evolveum.axiom.api.meta.Inheritance;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import org.jetbrains.annotations.Nullable;

public interface AxiomIdentifierResolver {

    final AxiomIdentifierResolver AXIOM_DEFAULT_NAMESPACE =  defaultNamespace(AxiomIdentifier.AXIOM_NAMESPACE);
    final Set<String> BUILTINS = ImmutableSet.of("string","boolean","uri", "int", "binary", "dateTime");
    final AxiomIdentifierResolver BUILTIN_TYPES = (prefix, localName) -> {
        if((prefix == null || prefix.isEmpty()) && BUILTINS.contains(localName)) {
            return AxiomIdentifier.axiom(localName);
        }
        return null;
    };

    AxiomIdentifier resolveIdentifier(@Nullable String prefix, @NotNull String localName);

    static AxiomIdentifierResolver defaultNamespace(String namespace) {
        return (prefix, localName) -> Strings.isNullOrEmpty(prefix) ? AxiomIdentifier.from(namespace, localName) : null;
    }

    default AxiomIdentifierResolver or(AxiomIdentifierResolver next) {
        return (prefix, localName) -> {
            AxiomIdentifier maybe = this.resolveIdentifier(prefix, localName);
            if (maybe != null) {
                return maybe;
            }
            return next.resolveIdentifier(prefix, localName);
        };
    }

    static AxiomIdentifierResolver defaultNamespaceFromType(AxiomTypeDefinition type) {
        return (prefix, localName) -> {
            if(Strings.isNullOrEmpty(prefix)) {
                AxiomIdentifier localNs = AxiomIdentifier.local(localName);
                Optional<AxiomItemDefinition> childDef = type.itemDefinition(localNs);
                if(childDef.isPresent()) {
                    return Inheritance.adapt(type.name(), childDef.get());
                }
            }
            return null;
        };
    }

}
