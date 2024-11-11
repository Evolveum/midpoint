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

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * Common functionality for {@link ShadowObjectComputer}, {@link ShadowDeltaComputerAbsolute},
 * and {@link ShadowDeltaComputerRelative}.
 */
class ShadowComputerUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowComputerUtil.class);

    static boolean shouldStoreSimpleAttributeInShadow(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ShadowSimpleAttributeDefinition<?> attrDef) {
        return attrDef.isEffectivelyCached(objectDefinition);
    }

    static boolean shouldStoreReferenceAttributeInShadow(
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ShadowReferenceAttributeDefinition attrDef) {
        return attrDef.isEffectivelyCached(objectDefinition);
    }

    // MID-2585
    static boolean shouldStoreActivationItemInShadow(ProvisioningContext ctx, QName elementName) {
        return QNameUtil.match(elementName, ActivationType.F_ARCHIVE_TIMESTAMP)
                || QNameUtil.match(elementName, ActivationType.F_DISABLE_TIMESTAMP)
                || QNameUtil.match(elementName, ActivationType.F_ENABLE_TIMESTAMP)
                || QNameUtil.match(elementName, ActivationType.F_DISABLE_REASON)
                || ctx.getObjectDefinitionRequired().isActivationCached();
    }

    static void cleanupShadowActivation(ProvisioningContext ctx, ActivationType a) {
        if (a == null) {
            return;
        }

        if (!ctx.getObjectDefinitionRequired().isActivationCached()) {
            a.setAdministrativeStatus(null);
            a.setValidFrom(null);
            a.setValidTo(null);
            a.setLockoutStatus(null);
            a.setLockoutExpirationTimestamp(null);
        }
        a.setEffectiveStatus(null);
        a.setValidityStatus(null);
        a.setValidityChangeTimestamp(null);
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
            // For objects being stored in repository, this may happen when the shadow is initially created.
            // But it's updated shortly afterwards, so it's not a big deal.
            // TODO resolve eventually
            LOGGER.trace("No-OID reference found: {} in {}", refAttrValue, ctx);
            return null;
        }
    }

    static PrismReferenceDefinition createRepoRefAttrDef(ShadowReferenceAttributeDefinition attrDef) {
        return PrismContext.get().definitionFactory().newReferenceDefinition(
                attrDef.getItemName(), ObjectReferenceType.COMPLEX_TYPE, 0, -1);
    }

    static void addPasswordMetadata(PasswordType p, XMLGregorianCalendar now, ObjectReferenceType ownerRef)
            throws SchemaException {
        var valueMetadata = p.asPrismContainerValue().getValueMetadata();
        if (!valueMetadata.isEmpty()) {
            return;
        }
        // Supply some metadata if they are not present. However the normal thing is that those metadata are provided by model.
        var newMetadata = new ValueMetadataType()
                .storage(new StorageMetadataType()
                        .createTimestamp(now)
                        .creatorRef(ObjectTypeUtil.createObjectRefCopy(ownerRef)));
        valueMetadata.addMetadataValue(
                newMetadata.asPrismContainerValue());
    }
}
