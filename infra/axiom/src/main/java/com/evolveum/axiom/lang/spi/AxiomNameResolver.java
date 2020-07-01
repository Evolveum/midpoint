/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.spi;

import java.util.Optional;
import java.util.Set;

import javax.swing.plaf.basic.BasicOptionPaneUI;

import org.jetbrains.annotations.NotNull;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.AxiomPrefixedName;
import com.evolveum.axiom.api.meta.Inheritance;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.antlr.Bootstrap;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

import org.jetbrains.annotations.Nullable;

public interface AxiomNameResolver {

    final AxiomNameResolver AXIOM_DEFAULT_NAMESPACE =  defaultNamespace(AxiomName.AXIOM_NAMESPACE);
    final Set<String> BUILTINS = Bootstrap.builtInTypes();
    final AxiomNameResolver BUILTIN_TYPES = (prefix, localName) -> {
        if((prefix == null || prefix.isEmpty()) && BUILTINS.contains(localName)) {
            return AxiomName.builtIn(localName);
        }
        return null;
    };

    final AxiomNameResolver NULL_RESOLVER = (p, n) -> null;

    AxiomName resolveIdentifier(@Nullable String prefix, @NotNull String localName);

    static AxiomNameResolver defaultNamespace(String namespace) {
        return (prefix, localName) -> Strings.isNullOrEmpty(prefix) ? AxiomName.from(namespace, localName) : null;
    }

    static AxiomNameResolver nullResolver() {
        return NULL_RESOLVER;
    }

    default AxiomNameResolver orPrefix(String prefix, String namespace) {
        return or((p, localName) -> prefix.equals(p) ? AxiomName.from(namespace, localName) : null);
    }

    default AxiomNameResolver or(AxiomNameResolver next) {
        return (prefix, localName) -> {
            AxiomName maybe = this.resolveIdentifier(prefix, localName);
            if (maybe != null) {
                return maybe;
            }
            return next.resolveIdentifier(prefix, localName);
        };
    }

    static AxiomNameResolver defaultNamespaceFromType(AxiomTypeDefinition type) {
        return (prefix, localName) -> {
            if(Strings.isNullOrEmpty(prefix)) {
                AxiomName localNs = AxiomName.local(localName);
                Optional<AxiomItemDefinition> childDef = type.itemDefinition(localNs);
                if(childDef.isPresent()) {
                    return Inheritance.adapt(type.name(), childDef.get());
                }
            }
            return null;
        };
    }

    default AxiomName resolve(AxiomPrefixedName prefixedName) {
        return resolveIdentifier(prefixedName.getPrefix(), prefixedName.getLocalName());
    }

}
