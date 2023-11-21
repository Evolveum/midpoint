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

import com.evolveum.midpoint.provisioning.impl.AbstractShadow;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Temporary class - this functionality has to be sorted out.
 */
public class ShadowManagerMiscUtil {

    public static <T> T determinePrimaryIdentifierValue(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObject resourceObject) throws SchemaException {
        return determinePrimaryIdentifierValue(ctx, resourceObject.getBean());
    }

    public static <T> T determinePrimaryIdentifierValue(ProvisioningContext ctx, ShadowType shadow) throws SchemaException {
        return determinePrimaryIdentifierValueInternal(
                shadow,
                ctx.getObjectDefinitionRequired(),
                ctx.determineShadowState(shadow));
    }

    public static <T> T determinePrimaryIdentifierValue(RepoShadow repoShadow) throws SchemaException {
        return determinePrimaryIdentifierValueInternal(
                repoShadow.getBean(),
                repoShadow.getObjectDefinition(),
                repoShadow.getShadowLifecycleState());
    }

    public static <T> T determinePrimaryIdentifierValue(
            @NotNull AbstractShadow shadow, @NotNull ShadowLifecycleStateType lifecycleState) throws SchemaException {
        return determinePrimaryIdentifierValueInternal(
                shadow.getBean(),
                shadow.getObjectDefinition(),
                lifecycleState);
    }

    @Nullable
    private static <T> T determinePrimaryIdentifierValueInternal(
            @NotNull ShadowType shadow,
            @NotNull ResourceObjectDefinition objDef,
            @NotNull ShadowLifecycleStateType state)
            throws SchemaException {

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
                        objDef.findAttributeDefinitionRequired(primaryIdentifier.getElementName());

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
        // Note about using ResourceObjectIdentifiers et al: We are not sure if we have the non-wildcard context here,
        // so let's go the traditional way.
        Collection<? extends ResourceAttribute<?>> primaryIdentifiers = emptyIfNull(ShadowUtil.getPrimaryIdentifiers(shadow));
        // Let's make this simple. We support single-attribute, single-value, string-only primary identifiers anyway
        if (primaryIdentifiers.isEmpty()) {
            // No primary identifiers. This can happen in some cases, e.g. for proposed shadows.
            // Therefore we should tolerate this.
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
