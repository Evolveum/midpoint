/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade.OP_DELAYED_OPERATION;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;

/**
 * Misc utils for the `shadows` package.
 *
 * TODO sort them out
 */
class ShadowsUtil {

    /**
     * The `true` return value currently implies the operation type is {@link PendingOperationTypeType#RETRY}.
     * (Manual nor asynchronous operation have no attempt number set.)
     */
    static boolean isRetryableOperation(PendingOperationType pendingOperation) {
        return pendingOperation.getExecutionStatus() == EXECUTING
                && pendingOperation.getAttemptNumber() != null;
    }

    static boolean hasRetryableOperation(@NotNull ShadowType repoShadow) {
        return emptyIfNull(repoShadow.getPendingOperation()).stream()
                .anyMatch(ShadowsUtil::isRetryableOperation);
    }

    static void notifyAboutSuccessOperation(
            ProvisioningContext ctx,
            ShadowType shadow,
            ProvisioningOperationState<?> opState,
            ObjectDelta<ShadowType> delta,
            OperationResult result) {
        ResourceOperationDescription operationDescription = createSuccessOperationDescription(ctx, shadow, delta);
        EventDispatcher eventDispatcher = ShadowsLocalBeans.get().eventDispatcher;
        if (opState.isExecuting()) {
            eventDispatcher.notifyInProgress(operationDescription, ctx.getTask(), result);
        } else {
            eventDispatcher.notifySuccess(operationDescription, ctx.getTask(), result);
        }
    }

    static ResourceOperationDescription createSuccessOperationDescription(
            ProvisioningContext ctx, ShadowType shadowType, ObjectDelta<? extends ShadowType> delta) {
        ResourceOperationDescription operationDescription = new ResourceOperationDescription();
        operationDescription.setCurrentShadow(shadowType.asPrismObject());
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

    /**
     * This is quite an ugly hack - setting the status/message in the root {@link ProvisioningService} operation result.
     */
    static void setParentOperationStatus(
            OperationResult parentResult,
            ProvisioningOperationState<?> opState,
            OperationResultStatus finalOperationStatus) {
        parentResult.computeStatus(true); // To provide the error message from the subresults
        if (finalOperationStatus != null) {
            parentResult.setStatus(finalOperationStatus);
        } else if (!opState.isCompleted()) {
            parentResult.setInProgress();
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

    static void markOperationExecutionAsPending(
            Trace logger, String operation, ProvisioningOperationState<?> opState, OperationResult parentResult) {
        opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);

        // Create dummy subresult with IN_PROGRESS state.
        // This will force the entire result (parent) to be IN_PROGRESS rather than SUCCESS.
        OperationResult result = parentResult.createSubresult(OP_DELAYED_OPERATION);
        result.recordInProgress();
        result.close();

        logger.debug("{}: Resource operation NOT executed, execution pending", operation);
    }
}
