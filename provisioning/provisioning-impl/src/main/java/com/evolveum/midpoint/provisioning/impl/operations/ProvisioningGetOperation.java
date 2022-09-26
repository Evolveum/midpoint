/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.operations;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.CommonBeans;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Handles `getObject` operation for resources, shadows, and other kinds of objects.
 */
public class ProvisioningGetOperation<T extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningGetOperation.class);

    @NotNull private final Class<T> type;
    @NotNull private final String oid;
    @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;
    @Nullable private final GetOperationOptions rootOptions;
    @NotNull private final Task task;
    @NotNull private final CommonBeans beans;
    @NotNull private final OperationsHelper operationsHelper;

    /**
     * See the comment in error handling code in {@link ProvisioningServiceImpl#getObject(Class, String, Collection,
     * Task, OperationResult)} where this flag is used.
     */
    private boolean exceptionRecorded;

    public ProvisioningGetOperation(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull CommonBeans beans,
            @NotNull OperationsHelper operationsHelper) {
        this.type = type;
        this.oid = oid;
        this.options = options;
        this.rootOptions = SelectorOptions.findRootOptions(options);
        this.task = task;
        this.beans = beans;
        this.operationsHelper = operationsHelper;
    }

    public @NotNull PrismObject<T> execute(@NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException {

        if (ResourceType.class.isAssignableFrom(type)) {
            if (isRawMode()) {
                //noinspection unchecked
                return (PrismObject<T>) getResourceRaw(result);
            } else {
                //noinspection unchecked
                return (PrismObject<T>) getResourceNonRaw(result);
            }
        } else if (ShadowType.class.isAssignableFrom(type)) {
            //noinspection unchecked
            return (PrismObject<T>) getShadow(result);
        } else {
            return operationsHelper.getRepoObject(type, oid, options, result);
        }
    }

    private @NotNull PrismObject<ResourceType> getResourceRaw(@NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        PrismObject<ResourceType> resource = beans.cacheRepositoryService.getObject(ResourceType.class, oid, options, result);
        try {
            beans.provisioningService.applyDefinition(resource, task, result);
        } catch (CommonException ex) {
            ProvisioningUtil.recordWarningNotRethrowing(
                    LOGGER, result, "Problem when applying definitions to " + resource + " fetched in raw mode", ex);
            // Intentionally not propagating the (sometimes expected) exception.
            // Runtime exceptions are propagated, though. This could be reconsidered in the future.
        }
        return resource;
    }

    private @NotNull PrismObject<ResourceType> getResourceNonRaw(@NotNull OperationResult result)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException {
        try {
            return beans.resourceManager.getResource(oid, rootOptions, task, result);
        } catch (CommonException ex) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(LOGGER, result, "Problem getting resource " + oid, ex);
            exceptionRecorded = true;
            throw ex;
        }
    }

    private @NotNull PrismObject<ShadowType> getShadow(@NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException {
        try {
            return beans.shadowsFacade.getShadow(
                    oid,
                    null,
                    null,
                    options,
                    task,
                    result);
        } catch (ObjectNotFoundException e) {
            if (GetOperationOptions.isAllowNotFound(rootOptions)) {
                // TODO check if this is really needed (lower layers shouldn't produce FATAL_ERROR if allow not found is true)
                result.muteLastSubresultError();
            } else {
                ProvisioningUtil.recordFatalErrorWhileRethrowing(
                        LOGGER, result, "Error getting shadow with OID=" + oid + ": " + e.getMessage(), e);
            }
            exceptionRecorded = true;
            throw e;
        } catch (MaintenanceException e) {
            LOGGER.trace(e.getMessage(), e);
            exceptionRecorded = true;
            throw e;
        } catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException | RuntimeException | Error e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(
                    LOGGER, result, "Error getting shadow with OID=" + oid + ": " + e.getMessage(), e);
            exceptionRecorded = true;
            throw e;
        } catch (EncryptionException e) {
            ProvisioningUtil.recordFatalErrorWhileRethrowing(
                    LOGGER, result, "Error getting shadow with OID=" + oid + ": " + e.getMessage(), e);
            exceptionRecorded = true;
            throw new SystemException(e);
        }
    }

    public boolean isExceptionRecorded() {
        return exceptionRecorded;
    }

    public boolean isRawMode() {
        return GetOperationOptions.isRaw(rootOptions);
    }
}
