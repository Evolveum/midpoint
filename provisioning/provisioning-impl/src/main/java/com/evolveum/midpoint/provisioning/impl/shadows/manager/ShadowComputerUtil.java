/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismReferenceValue;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Common functionality for {@link ShadowObjectComputer}, {@link ShadowDeltaComputerAbsolute},
 * and {@link ShadowDeltaComputerRelative}.
 */
class ShadowComputerUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowComputerUtil.class);

    static boolean shouldStoreSimpleAttributeInShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ShadowSimpleAttributeDefinition<?> attrDef) {
        if (objectDefinition.isIdentifier(attrDef.getItemName())) {
            return true;
        }
        return shouldStoreAttributeInShadow(ctx, objectDefinition, attrDef);
    }

    static boolean shouldStoreReferenceAttributeInShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ShadowReferenceAttributeDefinition attrDef) {
        return shouldStoreAttributeInShadow(ctx, objectDefinition, attrDef);
    }

    private static boolean shouldStoreAttributeInShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ShadowAttributeDefinition<?, ?, ?, ?> attrDef) {
        if (Boolean.FALSE.equals(ctx.getExplicitCachingStatus())) {
            return false;
        }
        if (ctx.isReadCachingOnlyCapabilityPresent()) {
            return true;
        }
        return attrDef.isEffectivelyCached(objectDefinition);
    }

    static @NotNull List<ObjectReferenceType> toRepoFormat(
            @NotNull ProvisioningContext ctx,
            @NotNull List<ShadowReferenceAttributeValue> refAttrValues) {
        List<ObjectReferenceType> list = new ArrayList<>();
        for (ShadowReferenceAttributeValue refAttrValue : refAttrValues) {
            CollectionUtils.addIgnoreNull(
                    list,
                    toRepoFormat(ctx, refAttrValue));
        }
        return list;
    }

    /** Returns null if the reference cannot be stored in the repository. */
    static @Nullable ObjectReferenceType toRepoFormat(
            @NotNull ProvisioningContext ctx,
            @NotNull PrismReferenceValue refAttrValue) {
        String oid = refAttrValue.getOid();
        if (oid != null) {
            return ObjectTypeUtil.createObjectRef(oid, ObjectTypes.SHADOW);
        } else {
            // This may be a reference pointing to an object that does not exist on the resource.
            // For fetched subjects, it can happen e.g. as described in TestDummy.test226WillNonsensePrivilege.
            // TODO what about objects that are being stored in the repository?
            LOGGER.warn("No-OID reference found: {} in {}", refAttrValue, ctx);
            return null;
        }
    }

    static PrismReferenceDefinition createRepoRefAttrDef(ShadowReferenceAttributeDefinition attrDef) {
        return PrismContext.get().definitionFactory().newReferenceDefinition(
                attrDef.getItemName(), ObjectReferenceType.COMPLEX_TYPE, 0, -1);
    }
}
