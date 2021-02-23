/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectPrimaryIdentifiers;

import java.util.Collection;

import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
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
            ObjectClassComplexTypeDefinition objectClassDef) throws SchemaException {
        if (primaryIdentifierRealValue != null && objectClassDef != null &&
                selectPrimaryIdentifiers(identifiers, objectClassDef).isEmpty()) {
            identifiers.add(createFakePrimaryIdentifier(primaryIdentifierRealValue, objectClassDef));
        }
    }

    void addFakePrimaryIdentifierIfNeeded(ResourceAttributeContainer attrContainer, Object primaryIdentifierRealValue,
            ObjectClassComplexTypeDefinition objectClassDef) throws SchemaException {
        // TODO or should we use simply attrContainer.getPrimaryIdentifiers() ?
        //  It refers to the definition attached to the attrContainer.
        //  Let us be consistent with the Change-based version and use definition from the caller provisioning context.
        if (primaryIdentifierRealValue != null && objectClassDef != null &&
                selectPrimaryIdentifiers(attrContainer.getAllIdentifiers(), objectClassDef).isEmpty()) {
            attrContainer.add(createFakePrimaryIdentifier(primaryIdentifierRealValue, objectClassDef));
        }
    }

    private ResourceAttribute<?> createFakePrimaryIdentifier(Object primaryIdentifierRealValue,
            ObjectClassComplexTypeDefinition objectClassDef) throws SchemaException {
        Collection<? extends ResourceAttributeDefinition<?>> primaryIdDefs = objectClassDef.getPrimaryIdentifiers();
        ResourceAttributeDefinition<?> primaryIdDef = MiscUtil.extractSingletonRequired(primaryIdDefs,
                () -> new SchemaException("Multiple primary identifier definitions in " + objectClassDef),
                () -> new SchemaException("No primary identifier definition in " + objectClassDef));
        ResourceAttribute<?> primaryId = primaryIdDef.instantiate();
        //noinspection unchecked
        ((ResourceAttribute<Object>) primaryId).setRealValue(primaryIdentifierRealValue);
        return primaryId;
    }
}
