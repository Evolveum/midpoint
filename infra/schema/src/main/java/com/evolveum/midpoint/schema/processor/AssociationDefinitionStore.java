/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Provides information about definitions of associations.
 */
public interface AssociationDefinitionStore {

    /**
     * Returns definitions of all associations as an unmodifiable collection.
     *
     * Note: these items are _not_ included in getDefinitions.
     * (BTW, ResourceAssociationDefinition is not a subtype of ItemDefinition, not even of Definition.)
     */
    @NotNull Collection<? extends ShadowAssociationDefinition> getAssociationDefinitions();

    default ShadowAssociationDefinition findAssociationDefinition(QName name) {
        return getAssociationDefinitions().stream()
                .filter(a -> QNameUtil.match(a.getItemName(), name))
                .findFirst().orElse(null);
    }

    /**
     * Returns true if there is an association with the given name defined.
     */
    default boolean containsAssociationDefinition(@NotNull QName associationName) {
        return findAssociationDefinition(associationName) != null;
    }

    default ShadowAssociationDefinition findAssociationDefinitionRequired(QName name) throws SchemaException {
        return findAssociationDefinitionRequired(name, () -> "");
    }

    default ShadowAssociationDefinition findAssociationDefinitionRequired(QName name, Supplier<String> contextSupplier)
            throws SchemaException {
        ShadowAssociationDefinition def = findAssociationDefinition(name);
        if (def == null) {
            throw new SchemaException("No definition of association named '" + name + "' in " + this + contextSupplier.get());
        }
        return def;
    }

    default @NotNull Collection<QName> getNamesOfAssociations() {
        return getAssociationDefinitions().stream()
                .map(ShadowAssociationDefinition::getItemName)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
