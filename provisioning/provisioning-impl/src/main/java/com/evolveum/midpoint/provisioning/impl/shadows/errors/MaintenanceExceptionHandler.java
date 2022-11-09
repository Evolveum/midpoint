/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

/*
 * @author Martin Lizner
*/

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
class MaintenanceExceptionHandler extends ErrorHandler {

    private static final String OPERATION_HANDLE_GET_ERROR = MaintenanceExceptionHandler.class.getName() + ".handleGetError";
    private static final String OPERATION_HANDLE_ADD_ERROR = MaintenanceExceptionHandler.class.getName() + ".handleAddError";
    private static final String OPERATION_HANDLE_MODIFY_ERROR = MaintenanceExceptionHandler.class.getName() + ".handleModifyError";
    private static final String OPERATION_HANDLE_DELETE_ERROR = MaintenanceExceptionHandler.class.getName() + ".handleDeleteError";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Override
    public ShadowType handleGetError(ProvisioningContext ctx,
            ShadowType repositoryShadow, GetOperationOptions rootOptions, Exception cause,
            Task task, OperationResult parentResult) throws CommunicationException {

        ResourceType resource = ctx.getResource();
        if (!ProvisioningUtil.isDoDiscovery(resource, rootOptions)) {
            throwException(cause, null, parentResult);
        }

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_GET_ERROR);
        result.addParam("exception", cause.getMessage());

        for (OperationResult subRes : parentResult.getSubresults()) {
            subRes.muteError();
        }

        result.recordSuccess();
        repositoryShadow.setFetchResult(result.createBeanReduced());

        return repositoryShadow;
    }

    @Override
    public OperationResultStatus handleAddError(ProvisioningContext ctx,
            ShadowType shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            Task task,
            OperationResult parentResult) throws SchemaException {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_ADD_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) {
                ObjectQuery query = ObjectAlreadyExistHandler.createQueryBySecondaryIdentifier(shadowToAdd, prismContext);
                SearchResultList<PrismObject<ShadowType>> conflictingShadows =
                        repositoryService.searchObjects(ShadowType.class, query, null, parentResult);

                if (!conflictingShadows.isEmpty()) {
                    opState.setRepoShadow(conflictingShadows.get(0).asObjectable()); // there is already repo shadow in mp
                    failedOperationResult.setStatus(OperationResultStatus.SUCCESS);
                    result.recordSuccess();
                    return OperationResultStatus.SUCCESS;
                }
            }

            failedOperationResult.setStatus(OperationResultStatus.IN_PROGRESS); // this influences how pending operation resultStatus is saved
            return postponeAdd(shadowToAdd, opState, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public OperationResultStatus handleModifyError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            Collection<? extends ItemDelta> modifications,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_MODIFY_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            failedOperationResult.setStatus(OperationResultStatus.IN_PROGRESS);
            return postponeModify(ctx, repoShadow, modifications, opState, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public OperationResultStatus handleDeleteError(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Exception cause,
            OperationResult failedOperationResult,
            OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_HANDLE_DELETE_ERROR);
        result.addParam("exception", cause.getMessage());
        try {
            failedOperationResult.setStatus(OperationResultStatus.IN_PROGRESS);
            return postponeDelete(ctx, repoShadow, opState, failedOperationResult, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result) throws MaintenanceException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof MaintenanceException) {
            throw (MaintenanceException)cause;
        } else {
            throw new MaintenanceException(cause.getMessage(), cause);
        }
    }
}
