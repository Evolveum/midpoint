/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.operations;

import java.util.Collection;

import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Handles `getObject` operation for resources, shadows, and other kinds of objects.
 */
public class ProvisioningGetOperation<T extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningGetOperation.class);

    @NotNull private final Class<T> type;
    @NotNull private final String oid;
    @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;
    @NotNull private final ProvisioningOperationContext context;
    @Nullable private final GetOperationOptions rootOptions;
    @NotNull private final Task task;
    @NotNull private final CommonBeans beans;
    @NotNull private final OperationsHelper operationsHelper;

    public ProvisioningGetOperation(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull CommonBeans beans,
            @NotNull OperationsHelper operationsHelper) {
        this.type = type;
        this.oid = oid;
        this.options = options;
        this.context = context;
        this.rootOptions = SelectorOptions.findRootOptions(options);
        this.task = task;
        this.beans = beans;
        this.operationsHelper = operationsHelper;
    }

    public @NotNull T execute(@NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException {

        if (ResourceType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            return (T) (isRawMode() ? getResourceRaw(result) : getResourceNonRaw(result));
        } else if (ShadowType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            return (T) (isRawMode() ? getShadowRaw(result) : getShadowNonRaw(result));
        } else {
            return operationsHelper.getRepoObject(type, oid, options, result);
        }
    }

    private @NotNull ResourceType getResourceRaw(@NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        ResourceType resource = operationsHelper.getRepoObject(ResourceType.class, oid, options, result);
        tryApplyingDefinition(resource, result);
        return resource;
    }

    private void tryApplyingDefinition(ObjectType object, @NotNull OperationResult result) {
        try {
            beans.provisioningService.applyDefinition(object.asPrismObject(), task, result);
        } catch (CommonException ex) {
            ProvisioningUtil.recordWarningNotRethrowing(
                    LOGGER, result, "Problem when applying definitions to " + object + " fetched in raw mode", ex);
            // Intentionally not propagating the (sometimes expected) exception.
            // Runtime exceptions are propagated, though. This could be reconsidered in the future.
        }
    }

    private @NotNull ResourceType getResourceNonRaw(@NotNull OperationResult result)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
        try {
            return beans.resourceManager.getCompletedResource(oid, rootOptions, task, result);
        } catch (CommonException ex) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Problem getting resource " + oid, ex);
            throw ex;
        }
    }

    private @NotNull ShadowType getShadowRaw(@NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        ShadowType shadow = operationsHelper.getRepoObject(ShadowType.class, oid, options, result);
        tryApplyingDefinition(shadow, result);
        return shadow;
    }

    private @NotNull ShadowType getShadowNonRaw(@NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException {
        try {
            return beans.shadowsFacade
                    .getShadow(oid, null, null, options, context, task, result)
                    .getBean();
        } catch (MaintenanceException e) {
            throw new AssertionError(
                    "Unexpected MaintenanceException. The called method should have returned cached shadow instead.", e);
        } catch (EncryptionException e) {
            ProvisioningUtil.recordExceptionWhileRethrowing(
                    LOGGER, result, "Error getting shadow with OID=" + oid + ": " + e.getMessage(), e);
            throw new SystemException(e);
        } catch (Throwable t) {
            ProvisioningUtil.recordExceptionWhileRethrowing(
                    LOGGER, result, "Error getting shadow with OID=" + oid + ": " + t.getMessage(), t);
            throw t;
        }
    }

    private boolean isRawMode() {
        return GetOperationOptions.isRaw(rootOptions);
    }
}
