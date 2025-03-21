/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.*;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * @author Martin Lizner
 */
@Component
class MaintenanceExceptionHandler extends ErrorHandler {

    private static final Trace LOGGER = TraceManager.getTrace(MaintenanceExceptionHandler.class);

    private static final String OPERATION_HANDLE_ADD_ERROR = MaintenanceExceptionHandler.class.getName() + ".handleAddError";
    private static final String OPERATION_HANDLE_MODIFY_ERROR = MaintenanceExceptionHandler.class.getName() + ".handleModifyError";
    private static final String OPERATION_HANDLE_DELETE_ERROR = MaintenanceExceptionHandler.class.getName() + ".handleDeleteError";

    @Autowired private ShadowFinder shadowFinder;

    @Override
    public RepoShadow handleGetError(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repositoryShadow,
            @NotNull Exception cause,
            @NotNull OperationResult failedOperationResult,
            @NotNull OperationResult result) {
        throw new UnsupportedOperationException("MaintenanceException cannot occur during GET operation.");
    }

    @Override
    public OperationResultStatus handleAddError(
            @NotNull ShadowAddOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            OperationResult parentResult) throws SchemaException, ConfigurationException {

        var opState = operation.getOpState();
        var ctx = operation.getCtx();

        var result = parentResult.createSubresult(OPERATION_HANDLE_ADD_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            OperationResultStatus status;

            // TODO why querying by secondary identifiers? Maybe because the primary identifier is usually generated by the
            //  resource ... but is it always the case?

            // TODO shouldn't we have similar code for CommunicationException handling?
            //  For operation grouping, etc?

            // Think again if this is the best place for this functionality.

            ObjectQuery query = ObjectAlreadyExistHandler.createQueryBySecondaryIdentifier(operation.getResourceObjectToAdd());
            LOGGER.trace("Going to find matching shadows using the query:\n{}", query.debugDumpLazily(1));
            List<PrismObject<ShadowType>> matchingShadows = shadowFinder.searchShadows(ctx, query, null, result);
            LOGGER.trace("Found {}: {}", matchingShadows.size(), matchingShadows);
            RawRepoShadow rawLiveShadow =
                    RawRepoShadow.selectLiveShadow(
                            matchingShadows,
                            DebugUtil.lazy(() -> "when looking by secondary identifier: " + query));
            LOGGER.trace("Live shadow found: {}", rawLiveShadow);

            if (rawLiveShadow != null) {
                var liveShadow = ctx.adoptRawRepoShadow(rawLiveShadow);
                if (liveShadow.doesExist()) {
                    LOGGER.trace("Found a live shadow that seems to exist on the resource: {}", liveShadow);
                    status = OperationResultStatus.SUCCESS;
                } else {
                    LOGGER.trace("Found a live shadow that was probably not yet created on the resource: {}", liveShadow);
                    status = OperationResultStatus.IN_PROGRESS;
                }
                opState.setRepoShadow(liveShadow);
                // TODO shouldn't we do something similar for other cases like this?
                if (!opState.hasCurrentPendingOperation()) {
                    opState.setCurrentPendingOperation(
                            stateNonNull(
                                    liveShadow.findPendingAddOperation(),
                                    "No pending ADD operation in %s", liveShadow));
                }
            } else {
                status = OperationResultStatus.IN_PROGRESS;
            }

            failedOperationResult.setStatus(status);
            result.setStatus(status); // TODO
            if (status == OperationResultStatus.IN_PROGRESS) {
                opState.markAsPostponed(status);
            }
            return status;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public OperationResultStatus handleModifyError(
            @NotNull ShadowModifyOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_MODIFY_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            failedOperationResult.setStatus(OperationResultStatus.IN_PROGRESS);
            result.setInProgress();
            operation.getOpState().markAsPostponed(OperationResultStatus.IN_PROGRESS);
            return OperationResultStatus.IN_PROGRESS;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public OperationResultStatus handleDeleteError(
            @NotNull ShadowDeleteOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_DELETE_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            failedOperationResult.setStatus(OperationResultStatus.IN_PROGRESS);
            result.setInProgress();
            operation.getOpState().markAsPostponed(OperationResultStatus.IN_PROGRESS);
            return OperationResultStatus.IN_PROGRESS;
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    protected void throwException(@Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws MaintenanceException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof MaintenanceException maintenanceException) {
            throw maintenanceException;
        } else {
            throw new MaintenanceException(cause.getMessage(), cause);
        }
    }
}
