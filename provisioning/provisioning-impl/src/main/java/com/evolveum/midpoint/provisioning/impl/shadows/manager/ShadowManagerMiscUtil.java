/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType.*;

import java.util.Collection;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifier;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Temporary class - this functionality has to be sorted out.
 */
public class ShadowManagerMiscUtil {

    public static <T> T determinePrimaryIdentifierValue(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectShadow resourceObject) throws SchemaException {
        //noinspection unchecked
        return (T) determinePrimaryIdentifierValue(
                resourceObject,
                ctx.determineShadowState(resourceObject.getBean()));
    }

    /**
     * "Safe variant" of {@link #determinePrimaryIdentifierValue(RepoShadow)} that assumes the LC state may be out of date.
     * In theory, it should not be needed. In practice, it is.
     */
    public static <T> T determinePrimaryIdentifierValue(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow) {
        ctx.updateShadowState(repoShadow);
        //noinspection unchecked
        return (T) determinePrimaryIdentifierValue(
                repoShadow,
                repoShadow.getShadowLifecycleState());
    }

    public static <T> T determinePrimaryIdentifierValue(RepoShadow repoShadow) throws SchemaException {
        //noinspection unchecked
        return (T) determinePrimaryIdentifierValue(
                repoShadow,
                repoShadow.getShadowLifecycleState());
    }

    /**
     * Computes the value to be stored into {@link ShadowType#primaryIdentifierValue(String)}.
     * Although the shadow lifecycle state might be present in {@link AbstractShadow#getBean()} (for some subclasses),
     * this method - to be general - requires the state to be externally provided by the caller.
     *
     * Prerequisite: the shadow definition is refined.
     */
    public static Object determinePrimaryIdentifierValue(
            @NotNull AbstractShadow shadow, @NotNull ShadowLifecycleStateType lifecycleState) {

        if (lifecycleState == REAPING || lifecycleState == CORPSE || lifecycleState == TOMBSTONE) {
            return null;
        }

        ShadowSimpleAttribute<?> primaryIdentifier = shadow.getPrimaryIdentifierAttribute();
        if (primaryIdentifier == null) {
            return null;
        } else {
            return ResourceObjectIdentifier.Primary.of(primaryIdentifier)
                    .getNormValue();
        }
    }

    private static ShadowSimpleAttribute<String> getPrimaryIdentifier(ShadowType shadow) throws SchemaException {
        // Note about using ResourceObjectIdentifiers et al: We are not sure if we have the non-wildcard context here,
        // so let's go the traditional way.
        Collection<? extends ShadowSimpleAttribute<?>> primaryIdentifiers = emptyIfNull(ShadowUtil.getPrimaryIdentifiers(shadow));
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
        return (ShadowSimpleAttribute<String>) primaryIdentifiers.iterator().next();
    }
}
