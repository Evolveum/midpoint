/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectPrimaryIdentifiers;

import java.util.Collection;

import com.evolveum.midpoint.schema.processor.*;

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

    void addFakePrimaryIdentifierIfNeeded(Collection<ResourceAttribute<?>> identifiers, Object primaryIdentifierRealValue,
            ResourceObjectDefinition definition) throws SchemaException {
        if (primaryIdentifierRealValue != null && definition != null &&
                selectPrimaryIdentifiers(identifiers, definition).isEmpty()) {
            identifiers.add(createFakePrimaryIdentifier(primaryIdentifierRealValue, definition));
        }
    }

    void addFakePrimaryIdentifierIfNeeded(ResourceAttributeContainer attrContainer, Object primaryIdentifierRealValue,
            @NotNull ResourceObjectDefinition objectClassDef) throws SchemaException {
        // TODO or should we use simply attrContainer.getPrimaryIdentifiers() ?
        //  It refers to the definition attached to the attrContainer.
        //  Let us be consistent with the Change-based version and use definition from the caller provisioning context.
        if (primaryIdentifierRealValue != null
                && selectPrimaryIdentifiers(attrContainer.getAllIdentifiers(), objectClassDef).isEmpty()) {
            attrContainer.add(createFakePrimaryIdentifier(primaryIdentifierRealValue, objectClassDef));
        }
    }

    private ResourceAttribute<?> createFakePrimaryIdentifier(Object primaryIdentifierRealValue,
            ResourceObjectDefinition definition) throws SchemaException {
        Collection<? extends ResourceAttributeDefinition<?>> primaryIdDefs = definition.getPrimaryIdentifiers();
        ResourceAttributeDefinition<?> primaryIdDef = MiscUtil.extractSingletonRequired(primaryIdDefs,
                () -> new SchemaException("Multiple primary identifier definitions in " + definition),
                () -> new SchemaException("No primary identifier definition in " + definition));
        ResourceAttribute<?> primaryId = primaryIdDef.instantiate();
        //noinspection unchecked
        ((ResourceAttribute<Object>) primaryId).setRealValue(primaryIdentifierRealValue);
        return primaryId;
    }
}
