/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Misc utils for the `shadows` package.
 *
 * TODO sort them out
 */
class ShadowsUtil {

    static ResourceOperationDescription createSuccessOperationDescription(
            ProvisioningContext ctx, RepoShadow repoShadow, ObjectDelta<? extends ShadowType> delta) {
        ResourceOperationDescription operationDescription = new ResourceOperationDescription();
        operationDescription.setCurrentShadow(repoShadow.getPrismObject());
        operationDescription.setResource(ctx.getResource().asPrismObject());
        operationDescription.setSourceChannel(ctx.getChannel());
        operationDescription.setObjectDelta(delta);
        return operationDescription;
    }

    static ResourceOperationDescription createResourceFailureDescription(
            ShadowType shadow, ResourceType resource, ObjectDelta<ShadowType> delta, String message) {
        ResourceOperationDescription failureDesc = new ResourceOperationDescription();
        failureDesc.setCurrentShadow(asPrismObject(shadow));
        failureDesc.setObjectDelta(delta);
        failureDesc.setResource(resource.asPrismObject());
        failureDesc.setMessage(message);
        failureDesc.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_DISCOVERY)); // ???
        return failureDesc;
    }

    static String getAdditionalOperationDesc(
            OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options) {
        if (scripts == null && options == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(" (");
        if (options != null) {
            sb.append("options:");
            options.shortDump(sb);
            if (scripts != null) {
                sb.append("; ");
            }
        }
        if (scripts != null) {
            sb.append("scripts");
        }
        sb.append(")");
        return sb.toString();
    }

    static void checkReturnedShadowValidity(ShadowType shadow) {
        checkReturnedShadowValidityDeeply(shadow);
    }

    static void checkReturnedShadowsValidity(Collection<PrismObject<ShadowType>> shadows) {
        shadows.forEach(shadow -> checkReturnedShadowValidity(shadow.asObjectable()));
    }

    static @NotNull ResultHandler<ShadowType> createCheckingHandler(@NotNull ResultHandler<ShadowType> handler) {
        return (object, parentResult) -> {
            checkReturnedShadowValidity(object.asObjectable());
            return handler.handle(object, parentResult);
        };
    }

    private static void checkReturnedShadowValidityDeeply(@NotNull ShadowType shadow) {

        if (true) return; // FIXME temporarily disabled

        // Here are the checks
        stateNonNull(shadow.getEffectiveOperationPolicy(), "No effective operation policy in %s", shadow);

        // And this is the recursion
        for (var refAttr : ShadowUtil.getReferenceAttributes(shadow)) {
            for (var refAttrVal : refAttr.getReferenceValues()) {
                try {
                    checkReturnedShadowValidityDeeply(refAttrVal.getShadowBean());
                } catch (IllegalStateException e) {
                    throw in(refAttr.getElementName() + " value " + refAttrVal, e);
                }
            }
        }
        for (var assoc : ShadowUtil.getAssociations(shadow)) {
            for (var assocVal : assoc.getAssociationValues()) {
                try {
                    for (var ref : assocVal.getObjectReferences()) {
                        for (var refAttrVal : ref.getReferenceValues()) {
                            try {
                                checkReturnedShadowValidityDeeply(refAttrVal.getShadowBean());
                            } catch (IllegalStateException e) {
                                throw in(ref.getElementName() + " value " + refAttrVal, e);
                            }
                        }
                    }
                    if (assoc.getDefinitionRequired().isComplex()) {
                        try {
                            checkReturnedShadowValidityDeeply(assocVal.getAssociationDataObject().getBean());
                        } catch (IllegalStateException e) {
                            throw in("association object", e);
                        }
                    }
                } catch (IllegalStateException e) {
                    throw in(assoc.getElementName() + " value " + assocVal, e);
                }
            }
        }
    }

    private static @NotNull IllegalStateException in(String where, IllegalStateException innerException) {
        return new IllegalStateException(innerException.getMessage() + " in " + where, innerException);
    }
}
