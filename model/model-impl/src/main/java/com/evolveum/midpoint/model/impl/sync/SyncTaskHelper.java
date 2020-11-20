/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CriticalityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    }

    TargetInfo getTargetInfo(Trace LOGGER, Task task, OperationResult opResult, TaskRunResult runResult, String ctx) {
        String resourceOid = getResourceOid(LOGGER, task, opResult, runResult, ctx);
        if (resourceOid == null) {
            return null;
        }
        ResourceType resource = getResource(LOGGER, resourceOid, task, opResult, runResult, ctx);
        if (resource == null) {
            return null;
        }
        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(LOGGER, resource, opResult, runResult, ctx);
        if (refinedSchema == null) {
            return null;
        }

        ObjectClassComplexTypeDefinition objectClass;
        try {
            objectClass = ModelImplUtils.determineObjectClass(refinedSchema, task);
        } catch (SchemaException e) {
            LOGGER.error("{}: schema error: {}", ctx, e.getMessage());
            opResult.recordFatalError(e);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }
        if (objectClass == null) {
            LOGGER.debug("{}: Processing all object classes", ctx);
        }

        return new TargetInfo(
                new ResourceShadowDiscriminator(resourceOid, objectClass == null ? null : objectClass.getTypeName()),
                resource, refinedSchema, objectClass);
    }

    public String getResourceOid(Trace logger, Task task, OperationResult opResult, TaskRunResult runResult, String ctx) {
        String resourceOid = task.getObjectOid();
        if (resourceOid == null) {
            logger.error("{}: No resource OID specified in the task", ctx);
            opResult.recordFatalError("No resource OID specified in the task");
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
        }
        return resourceOid;
    }

    private ResourceType getResource(Trace logger, String resourceOid, Task task, OperationResult opResult, TaskRunResult runResult, String ctx) {
        try {
            return provisioningService.getObject(ResourceType.class, resourceOid, null, task, opResult).asObjectable();
        } catch (ObjectNotFoundException ex) {
            logger.error("{}: Resource {} not found: {}", ctx, resourceOid, ex.getMessage(), ex);
            // This is bad. The resource does not exist. Permanent problem.
            opResult.recordFatalError("Resource not found " + resourceOid, ex);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        } catch (SchemaException ex) {
            logger.error("{}: Error dealing with schema: {}", ctx, ex.getMessage(), ex);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
            return null;
        } catch (RuntimeException ex) {
            logger.error("{}: Internal Error: {}", ctx, ex.getMessage(), ex);
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: " + ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        } catch (CommunicationException ex) {
            logger.error("{}: Error getting resource {}: {}", ctx, resourceOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting resource " + resourceOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
            return null;
        } catch (ConfigurationException | SecurityViolationException | ExpressionEvaluationException ex) {
            logger.error("{}: Error getting resource {}: {}", ctx, resourceOid, ex.getMessage(), ex);
            opResult.recordFatalError("Error getting resource " + resourceOid+": "+ex.getMessage(), ex);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }
    }

    private RefinedResourceSchema getRefinedResourceSchema(Trace LOGGER, ResourceType resource, OperationResult opResult,
            TaskRunResult runResult, String ctx) {
        RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            LOGGER.error("{}: Schema error during processing account definition: {}", ctx, e.getMessage());
            opResult.recordFatalError("Schema error during processing account definition: "+e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
            return null;
        }

        if (refinedSchema == null) {
            opResult.recordFatalError("No refined schema defined. Probably some configuration problem.");
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
            LOGGER.error("{}: No refined schema defined. Probably some configuration problem.", ctx);
        }
        return refinedSchema;
    }

    void processException(Trace LOGGER, Throwable t, OperationResult opResult, TaskRunResult runResult,
            TaskPartitionDefinitionType partition, String ctx) {
        if (t instanceof ObjectNotFoundException) {
            String oid = ((ObjectNotFoundException) t).getOid();
            LOGGER.error("{}: A required object does not exist, OID: {}", ctx, oid, t);
            // This is bad. The resource or task or something like that does not exist. Permanent problem.
            opResult.recordFatalError("A required object does not exist, OID: " + oid, t);
            setRunResultStatus(t, partition, CriticalityType.FATAL, runResult);
        } else if (t instanceof MaintenanceException) {
            opResult.recordHandledError("Maintenance exception: "+t.getMessage(), t);
            runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR); // Resource is in the maintenance, do not suspend the task
        } else if (t instanceof CommunicationException) {
            LOGGER.error("{}: Communication error: {}", ctx, t.getMessage(), t);
            // Error, but not critical. Just try later.
            opResult.recordPartialError("Communication error: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.PARTIAL, runResult);
        } else if (t instanceof SchemaException) {
            LOGGER.error("{}: Error dealing with schema: {}", ctx, t.getMessage(), t);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Error dealing with schema: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.PARTIAL, runResult);
        } else if (t instanceof PolicyViolationException) {
            LOGGER.error("{}: Policy violation: {}", ctx, t.getMessage(), t);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Policy violation: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.PARTIAL, runResult);
        } else if (t instanceof PreconditionViolationException) {
            LOGGER.error("{}: Precondition violation: {}", ctx, t.getMessage(), t);
            // Not sure about this.
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Internal error: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.PARTIAL, runResult);
        } else if (t instanceof ConfigurationException) {
            LOGGER.error("{}: Configuration error: {}", ctx, t.getMessage(), t);
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            opResult.recordFatalError("Configuration error: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.PARTIAL, runResult);
        } else if (t instanceof SecurityViolationException) {
            LOGGER.error("{}: Security violation: {}", ctx, t.getMessage(), t);
            opResult.recordFatalError("Security violation: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.FATAL, runResult);
        } else if (t instanceof ExpressionEvaluationException) {
            LOGGER.error("{}: Expression error: {}", ctx, t.getMessage(), t);
            opResult.recordFatalError("Expression error: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.FATAL, runResult);
        } else if (t instanceof SystemException) {
            LOGGER.error("{}: Unspecified error: {}", ctx, t.getMessage(), t);
            opResult.recordFatalError("Unspecified error: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.FATAL, runResult);
        } else {
            LOGGER.error("{}: Internal Error: {}", ctx, t.getMessage(), t);
            // Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            opResult.recordFatalError("Internal Error: "+t.getMessage(), t);
            setRunResultStatus(t, partition, CriticalityType.FATAL, runResult);
        }
    }

    private void setRunResultStatus(Throwable ex, TaskPartitionDefinitionType partition, CriticalityType defaultCriticality, TaskRunResult runResult) {
        CriticalityType criticality;
        if (partition == null) {
            criticality = defaultCriticality;
        } else {
            criticality = ExceptionUtil.getCriticality(partition.getErrorCriticality(), ex, defaultCriticality);
        }

        switch (criticality) {
            case PARTIAL:
                //noinspection DuplicateBranchesInSwitch
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
                break;
            case FATAL:
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR);
                break;
            default:
                runResult.setRunResultStatus(TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR);
        }
    }
}
