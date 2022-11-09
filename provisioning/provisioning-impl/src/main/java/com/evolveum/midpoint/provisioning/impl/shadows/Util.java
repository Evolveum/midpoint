/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Misc utils for the shadow cache.
 *
 * TODO sort them out
 */
class Util {

    static boolean needsRetry(PendingOperationType pendingOperation) {
        return PendingOperationExecutionStatusType.EXECUTING.equals(pendingOperation.getExecutionStatus()) &&
                pendingOperation.getAttemptNumber() != null;
    }

    static boolean shouldRefresh(ShadowType repoShadow) {
        if (repoShadow == null) {
            return false;
        }

        List<PendingOperationType> pendingOperations = repoShadow.getPendingOperation();
        if (pendingOperations == null || pendingOperations.isEmpty()) {
            return false;
        }

        for (PendingOperationType pendingOperationType : pendingOperations) {
            if (needsRetry(pendingOperationType)) {
                return true;
            }
        }

        return false;
    }

    static ResourceOperationDescription createSuccessOperationDescription(ProvisioningContext ctx,
            ShadowType shadowType, ObjectDelta<? extends ShadowType> delta, String message) {
        ResourceOperationDescription operationDescription = new ResourceOperationDescription();
        operationDescription.setCurrentShadow(shadowType.asPrismObject());
        operationDescription.setResource(ctx.getResource().asPrismObject());
        operationDescription.setSourceChannel(ctx.getChannel());
        operationDescription.setObjectDelta(delta);
        operationDescription.setMessage(message);
        return operationDescription;
    }

    static void setParentOperationStatus(OperationResult parentResult,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            OperationResultStatus finalOperationStatus) {
        if (finalOperationStatus != null) {
            parentResult.setStatus(finalOperationStatus);
        } else {
            if (opState.isCompleted()) {
                parentResult.computeStatus(true);
            } else {
                parentResult.setInProgress();
            }
        }
        parentResult.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
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

    static ShadowType minimize(ShadowType resourceObject, ResourceObjectDefinition objDef) {
        if (resourceObject == null) {
            return null;
        }
        ShadowType minimized = resourceObject.clone();
        ShadowUtil.removeAllAttributesExceptPrimaryIdentifier(minimized, objDef);
        if (ShadowUtil.hasPrimaryIdentifier(minimized, objDef)) {
            return minimized;
        } else {
            return null;
        }
    }
}
