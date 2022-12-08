/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil.getNormalizedAttributeValues;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Temporary class - this functionality has to be sorted out.
 */
public class ShadowManagerMiscUtil {

    public static <T> T determinePrimaryIdentifierValue(ProvisioningContext ctx, ShadowType shadow) throws SchemaException {
        if (ShadowUtil.isDead(shadow)) {
            return null;
        }
        ShadowLifecycleStateType state = ctx.determineShadowState(shadow);
        if (state == REAPING || state == CORPSE || state == TOMBSTONE) {
            return null;
        }

        //noinspection unchecked
        ResourceAttribute<T> primaryIdentifier = (ResourceAttribute<T>) getPrimaryIdentifier(shadow);
        if (primaryIdentifier == null) {
            return null;
        }
        //noinspection unchecked
        ResourceAttributeDefinition<T> rDef =
                (ResourceAttributeDefinition<T>)
                        ctx.getObjectDefinitionRequired()
                                .findAttributeDefinitionRequired(primaryIdentifier.getElementName());

        Collection<T> normalizedPrimaryIdentifierValues = getNormalizedAttributeValues(primaryIdentifier, rDef);
        if (normalizedPrimaryIdentifierValues.isEmpty()) {
            throw new SchemaException("No primary identifier values in " + shadow);
        }
        if (normalizedPrimaryIdentifierValues.size() > 1) {
            throw new SchemaException("Too many primary identifier values in " + shadow + ", this is not supported yet");
        }
        return normalizedPrimaryIdentifierValues.iterator().next();
    }

    private static ResourceAttribute<String> getPrimaryIdentifier(ShadowType shadow) throws SchemaException {
        Collection<? extends ResourceAttribute<?>> primaryIdentifiers = emptyIfNull(ShadowUtil.getPrimaryIdentifiers(shadow));
        // Let's make this simple. We support single-attribute, single-value, string-only primary identifiers anyway
        if (primaryIdentifiers.isEmpty()) {
            // No primary identifiers. This can happen in sme cases, e.g. for proposed shadows.
            // Therefore we should be tolerating this.
            return null;
        }
        if (primaryIdentifiers.size() > 1) {
            throw new SchemaException("Too many primary identifiers in " + shadow + ", this is not supported yet");
        }
        //noinspection unchecked
        return (ResourceAttribute<String>) primaryIdentifiers.iterator().next();
    }

    public static PendingOperationType findPendingAddOperation(ShadowType liveShadow) {
        return PendingOperationsHelper.findPendingAddOperation(liveShadow);
    }
}
