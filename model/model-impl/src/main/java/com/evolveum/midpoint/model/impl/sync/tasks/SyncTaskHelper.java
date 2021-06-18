/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Auxiliary methods for synchronization tasks (Live Sync, Async Update, and maybe others).
 */
@Component
public class SyncTaskHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SyncTaskHelper.class);

    @Autowired private ProvisioningService provisioningService;
    @Autowired private PrismContext prismContext;

    public <O extends ObjectType> ResourceSearchSpecification createSearchSpecification(
            ResourceObjectSetType resourceObjectSetBean, Task task, OperationResult opResult)
            throws ActivityExecutionException {

        TargetInfo targetInfo;
        try {
            targetInfo = createTargetInfoInternal(resourceObjectSetBean, false, task, opResult);
        } catch (MaintenanceException e) {
            throw new AssertionError(e);
        }

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(
                targetInfo.getResourceOid(), targetInfo.getObjectClassName(), prismContext);

        return new ResourceSearchSpecification(
                targetInfo,
                query,
                MiscSchemaUtil.optionsTypeToOptions(resourceObjectSetBean.getSearchOptions(), prismContext));
    }

    @NotNull
    public TargetInfo createTargetInfo(@NotNull ResourceObjectSetType resourceObjectSet, Task task, OperationResult opResult)
            throws ActivityExecutionException, MaintenanceException {
        TargetInfo targetInfo = createTargetInfoInternal(resourceObjectSet, true, task, opResult);

        LOGGER.trace("target info: {}", targetInfo);
        return targetInfo;
    }

    @NotNull
    private TargetInfo createTargetInfoInternal(@NotNull ResourceObjectSetType resourceObjectSet,
            boolean checkForMaintenance, Task task, OperationResult opResult)
            throws ActivityExecutionException, MaintenanceException {

        String resourceOid = getResourceOid(resourceObjectSet);
        ResourceType resource = getResource(resourceOid, task, opResult);
        if (checkForMaintenance) {
            ResourceTypeUtil.checkNotInMaintenance(resource);
        }

        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(resource);

        ObjectClassComplexTypeDefinition objectClass;
        try {
            objectClass = ModelImplUtils.determineObjectClassNew(refinedSchema, resourceObjectSet, task); // TODO source
        } catch (SchemaException e) {
            throw new ActivityExecutionException("Schema error", FATAL_ERROR, PERMANENT_ERROR, e);
        }
        if (objectClass == null) {
            LOGGER.debug("Processing all object classes");
        }

        return new TargetInfo(
                new ResourceShadowDiscriminator(resourceOid, objectClass == null ? null : objectClass.getTypeName()),
                resource, refinedSchema, objectClass);
    }

    @NotNull
    public TargetInfo createTargetInfoForShadow(ShadowType shadow, Task task, OperationResult opResult)
            throws ActivityExecutionException, MaintenanceException, SchemaException {
        String resourceOid = ShadowUtil.getResourceOid(shadow);
        ResourceType resource = getResource(resourceOid, task, opResult);
        ResourceTypeUtil.checkNotInMaintenance(resource);
        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(resource);

        // TODO reconsider the algorithm used for deriving object class
        ObjectClassComplexTypeDefinition objectClass = requireNonNull(
                ModelImplUtils.determineObjectClass(refinedSchema, shadow.asPrismObject()),
                "No object class found for the shadow");

        return new TargetInfo(
                new ResourceShadowDiscriminator(resourceOid, objectClass.getTypeName()),
                resource, refinedSchema, objectClass);
    }

    public @NotNull String getResourceOid(ResourceObjectSetType set) throws ActivityExecutionException {
        String resourceOid = set.getResourceRef() != null ? set.getResourceRef().getOid() : null;
        if (resourceOid == null) {
            throw new ActivityExecutionException("No resource OID specified", FATAL_ERROR, PERMANENT_ERROR);
        }
        return resourceOid;
    }

    private @NotNull ResourceType getResource(String resourceOid, Task task, OperationResult opResult) throws ActivityExecutionException {
        try {
            return provisioningService.getObject(ResourceType.class, resourceOid, null, task, opResult).asObjectable();
        } catch (ObjectNotFoundException ex) {
            // This is bad. The resource does not exist. Permanent problem.
            throw new ActivityExecutionException("Resource " + resourceOid + " not found", FATAL_ERROR, PERMANENT_ERROR, ex);
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            throw new ActivityExecutionException("Resource " + resourceOid + " has a schema problem", FATAL_ERROR, TEMPORARY_ERROR, ex);
        } catch (CommunicationException ex) {
            throw new ActivityExecutionException("Communication error while getting resource " + resourceOid, FATAL_ERROR, TEMPORARY_ERROR, ex);
        } catch (RuntimeException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | Error ex) {
            // RuntimeException: Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            // Other exceptions: basically the same.
            throw new ActivityExecutionException("Error while getting resource " + resourceOid, FATAL_ERROR, PERMANENT_ERROR, ex);
        }
    }

    private @NotNull RefinedResourceSchema getRefinedResourceSchema(ResourceType resource) throws ActivityExecutionException {
        RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            throw new ActivityExecutionException("Schema error during processing account definition", FATAL_ERROR, PERMANENT_ERROR, e);
        }

        if (refinedSchema != null) {
            return refinedSchema;
        } else {
            throw new ActivityExecutionException("No refined schema defined. Probably some configuration problem.", FATAL_ERROR,
                    PERMANENT_ERROR);
        }
    }
}
