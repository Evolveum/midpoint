/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Provides information about definitions of associations.
 *
 * Note that these are real
 */
public interface AssociationDefinitionStore {

    ShadowReferenceAttributeDefinition findReferenceAttributeDefinition(QName name);

    @NotNull List<? extends ShadowReferenceAttributeDefinition> getReferenceAttributeDefinitions();

    default ShadowReferenceAttributeDefinition findAssociationDefinitionRequired(QName name) throws SchemaException {
        return findAssociationDefinitionRequired(name, () -> "");
    }

    default ShadowReferenceAttributeDefinition findAssociationDefinitionRequired(QName name, Supplier<String> contextSupplier)
            throws SchemaException {
        ShadowReferenceAttributeDefinition def = findReferenceAttributeDefinition(name);
        if (def == null) {
            throw new SchemaException("No definition of association (reference attribute) named '%s' in %s%s".formatted(
                    name, this, contextSupplier.get()));
        }
        return def;
    }

    default @NotNull Collection<QName> getNamesOfReferenceAttributes() {
        return getReferenceAttributeDefinitions().stream()
                .map(ShadowReferenceAttributeDefinition::getItemName)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
