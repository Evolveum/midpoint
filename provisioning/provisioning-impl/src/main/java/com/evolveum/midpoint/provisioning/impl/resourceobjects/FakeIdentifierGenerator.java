/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Generates fake primary identifier for malformed fetched objects and changes.
 *
 * It is used if such objects are broken so severely that they do not have a primary identifier.
 */
@Component
class FakeIdentifierGenerator {

    void addFakePrimaryIdentifierIfNeeded(Collection<ShadowSimpleAttribute<?>> identifiers, Object primaryIdentifierRealValue,
            ResourceObjectDefinition definition) throws SchemaException {
        if (primaryIdentifierRealValue != null && definition != null &&
                selectPrimaryIdentifiers(identifiers, definition).isEmpty()) {
            identifiers.add(createFakePrimaryIdentifier(primaryIdentifierRealValue, definition));
        }
    }

    void addFakePrimaryIdentifierIfNeeded(
            ShadowAttributesContainer attrContainer, Object primaryIdentifierRealValue,
            @NotNull ResourceObjectDefinition objectClassDef) throws SchemaException {
        // TODO or should we use simply attrContainer.getPrimaryIdentifiers() ?
        //  It refers to the definition attached to the attrContainer.
        //  Let us be consistent with the Change-based version and use definition from the caller provisioning context.
        if (primaryIdentifierRealValue != null
                && selectPrimaryIdentifiers(attrContainer.getAllIdentifiers(), objectClassDef).isEmpty()) {
            attrContainer.addAttribute(
                    createFakePrimaryIdentifier(primaryIdentifierRealValue, objectClassDef));
        }
    }

    private static Collection<ShadowSimpleAttribute<?>> selectPrimaryIdentifiers(
            Collection<ShadowSimpleAttribute<?>> identifiers, ResourceObjectDefinition def) {

        Collection<ItemName> primaryIdentifiers = def.getPrimaryIdentifiers().stream()
                .map(ItemDefinition::getItemName)
                .collect(Collectors.toSet());

        return identifiers.stream()
                .filter(attr -> QNameUtil.matchAny(attr.getElementName(), primaryIdentifiers))
                .collect(Collectors.toList());
    }

    private ShadowSimpleAttribute<?> createFakePrimaryIdentifier(Object primaryIdentifierRealValue,
            ResourceObjectDefinition definition) throws SchemaException {
        Collection<? extends ShadowSimpleAttributeDefinition<?>> primaryIdDefs = definition.getPrimaryIdentifiers();
        ShadowSimpleAttributeDefinition<?> primaryIdDef = MiscUtil.extractSingletonRequired(primaryIdDefs,
                () -> new SchemaException("Multiple primary identifier definitions in " + definition),
                () -> new SchemaException("No primary identifier definition in " + definition));
        ShadowSimpleAttribute<?> primaryId = primaryIdDef.instantiate();
        //noinspection unchecked
        ((ShadowSimpleAttribute<Object>) primaryId).setRealValue(primaryIdentifierRealValue);
        return primaryId;
    }
}
