/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.util.TaskExceptionHandlingUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

/**
 * Auxiliary methods for synchronization tasks (Live Sync, Async Update, and maybe others).
 */
@Component
public class SyncTaskHelper {

    @Autowired private ProvisioningService provisioningService;
    @Autowired private PrismContext prismContext;

    @SuppressWarnings("FieldCanBeLocal")
    public static class TargetInfo {
        final ResourceShadowDiscriminator coords;
        public final ResourceType resource;
        private final RefinedResourceSchema refinedResourceSchema;
        private final ObjectClassComplexTypeDefinition objectClassDefinition;

        private TargetInfo(ResourceShadowDiscriminator coords, ResourceType resource,
                RefinedResourceSchema refinedResourceSchema, ObjectClassComplexTypeDefinition objectClassDefinition) {
            this.coords = coords;
            this.resource = resource;
            this.refinedResourceSchema = refinedResourceSchema;
            this.objectClassDefinition = objectClassDefinition;
        }

        @Override
        public String toString() {
            return "TargetInfo{" +
                    "coords=" + coords +
                    ", resource=" + resource +
                    ", refinedResourceSchema=" + refinedResourceSchema +
                    ", objectClassDefinition=" + objectClassDefinition +
                    '}';
        }

        public ResourceShadowDiscriminator getCoords() {
            return coords;
        }

        public ResourceType getResource() {
            return resource;
        }

        public RefinedResourceSchema getRefinedResourceSchema() {
            return refinedResourceSchema;
        }

        public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
            return objectClassDefinition;
        }

        public SynchronizationObjectsFilterImpl getObjectFilter(Task task) {
            return ModelImplUtils.determineSynchronizationObjectsFilter(objectClassDefinition, task);
        }

        public String getContextDescription() {
            return String.valueOf(resource); // TODO something more human friendly
        }
    }

    @NotNull
    public TargetInfo getTargetInfo(Trace logger, Task task, OperationResult opResult, String ctx) throws TaskException, MaintenanceException {
        String resourceOid = getResourceOid(task);
        ResourceType resource = getResource(resourceOid, task, opResult);
        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(resource);

        ObjectClassComplexTypeDefinition objectClass;
        try {
            objectClass = ModelImplUtils.determineObjectClass(refinedSchema, task);
        } catch (SchemaException e) {
            throw new TaskException("Schema error", FATAL_ERROR, PERMANENT_ERROR, e);
        }
        if (objectClass == null) {
            logger.debug("{}: Processing all object classes", ctx);
        }

        TargetInfo targetInfo = new TargetInfo(
                new ResourceShadowDiscriminator(resourceOid, objectClass == null ? null : objectClass.getTypeName()),
                resource, refinedSchema, objectClass);

        logger.trace("target info: {}", targetInfo);
        return targetInfo;
    }

    @NotNull
    public TargetInfo getTargetInfoForShadow(ShadowType shadow, Task task, OperationResult opResult)
            throws TaskException, MaintenanceException, SchemaException {
        String resourceOid = ShadowUtil.getResourceOid(shadow);
        ResourceType resource = getResource(resourceOid, task, opResult);
        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(resource);

        // TODO reconsider the algorithm used for deriving object class
        ObjectClassComplexTypeDefinition objectClass = java.util.Objects.requireNonNull(
                ModelImplUtils.determineObjectClass(refinedSchema, shadow.asPrismObject()),
                "No object class found for the shadow");

        return new TargetInfo(
                new ResourceShadowDiscriminator(resourceOid, objectClass.getTypeName()),
                resource, refinedSchema, objectClass);
    }

    public @NotNull String getResourceOid(Task task) throws TaskException {
        String resourceOid = task.getObjectOid();
        if (resourceOid == null) {
            throw new TaskException("No resource OID specified in the task", FATAL_ERROR, PERMANENT_ERROR);
        }
        return resourceOid;
    }

    private ResourceType getResource(String resourceOid, Task task, OperationResult opResult) throws TaskException, MaintenanceException {
        ResourceType resource;
        try {
            resource = provisioningService.getObject(ResourceType.class, resourceOid, null, task, opResult).asObjectable();
        } catch (ObjectNotFoundException ex) {
            // This is bad. The resource does not exist. Permanent problem.
            throw new TaskException("Resource " + resourceOid + " not found", FATAL_ERROR, PERMANENT_ERROR, ex);
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            throw new TaskException("Resource " + resourceOid + " has a schema problem", FATAL_ERROR, TEMPORARY_ERROR, ex);
        } catch (CommunicationException ex) {
            throw new TaskException("Communication error while getting resource " + resourceOid, FATAL_ERROR, TEMPORARY_ERROR, ex);
        } catch (RuntimeException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | Error ex) {
            // RuntimeException: Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            // Other exceptions: basically the same.
            throw new TaskException("Error while getting resource " + resourceOid, FATAL_ERROR, PERMANENT_ERROR, ex);
        }

        if (ResourceTypeUtil.isInMaintenance(resource)) {
            throw new MaintenanceException("Resource " + resource + " is in the maintenance");
        }
        return resource;
    }

    private @NotNull RefinedResourceSchema getRefinedResourceSchema(ResourceType resource) throws TaskException {
        RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            throw new TaskException("Schema error during processing account definition", FATAL_ERROR, PERMANENT_ERROR, e);
        }

        if (refinedSchema != null) {
            return refinedSchema;
        } else {
            throw new TaskException("No refined schema defined. Probably some configuration problem.", FATAL_ERROR,
                    PERMANENT_ERROR);
        }
    }

    TaskException convertException(Throwable t, TaskPartitionDefinitionType partition) {
        return TaskExceptionHandlingUtil.convertException(t, partition);
    }

    TaskRunResult processFinish(TaskRunResult runResult) {
        return TaskExceptionHandlingUtil.processFinish(runResult);
    }

    TaskRunResult processTaskException(TaskException e, Trace logger, String ctx, TaskRunResult runResult) {
        return TaskExceptionHandlingUtil.processTaskException(e, logger, ctx, runResult);
    }
}
