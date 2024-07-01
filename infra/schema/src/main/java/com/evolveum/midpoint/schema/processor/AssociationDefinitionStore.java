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
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/** Provides {@link ShadowAssociationDefinition}s. */
public interface AssociationDefinitionStore {

    ShadowAssociationDefinition findAssociationDefinition(QName name);

    @NotNull List<? extends ShadowAssociationDefinition> getAssociationDefinitions();

    default ShadowAssociationDefinition findAssociationDefinitionRequired(QName name) throws SchemaException {
        return findAssociationDefinitionRequired(name, "");
    }

    default ShadowAssociationDefinition findAssociationDefinitionRequired(QName name, Object context)
            throws SchemaException {
        return MiscUtil.requireNonNull(
                findAssociationDefinition(name),
                "No definition of association named '%s' in %s%s", name, this, context);
    }

    default @NotNull Collection<QName> getNamesOfAssociations() {
        return getAssociationDefinitions().stream()
                .map(ShadowAssociationDefinition::getItemName)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
