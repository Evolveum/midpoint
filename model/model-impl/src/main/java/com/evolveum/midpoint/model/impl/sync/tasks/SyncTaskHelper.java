/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.HANDLED_ERROR;
import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.isInMaintenance;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.schema.util.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
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

    @VisibleForTesting
    private static boolean skipMaintenanceCheck;

    public ResourceSearchSpecification createSearchSpecification(
            @NotNull ResourceObjectSetType set, Task task, OperationResult opResult)
            throws ActivityRunException, SchemaException {

        ResourceObjectClassSpecification resourceObjectClassSpecification =
                createObjectClassSpecInternal(set, false, task, opResult);

        ObjectQuery basicQuery = resourceObjectClassSpecification.createBasicQuery();
        ObjectQuery query;
        if (set.getQuery() != null) {
            ObjectQuery customQuery = prismContext.getQueryConverter().createObjectQuery(ShadowType.class, set.getQuery());
            if (set.getQueryApplication() == ResourceObjectSetQueryApplicationModeType.REPLACE) {
                query = customQuery;
            } else if (set.getQueryApplication() == ResourceObjectSetQueryApplicationModeType.APPEND) {
                query = ObjectQueryUtil.addConjunctions(customQuery, basicQuery.getFilter());
            } else {
                throw new ActivityRunException("Unsupported query application mode: " + set.getQueryApplication(),
                        FATAL_ERROR, PERMANENT_ERROR);
            }
        } else {
            query = basicQuery;
        }

        return new ResourceSearchSpecification(
                resourceObjectClassSpecification,
                query,
                MiscSchemaUtil.optionsTypeToOptions(set.getSearchOptions(), prismContext));
    }

    @NotNull
    public ResourceObjectClassSpecification createObjectClassSpec(@NotNull ResourceObjectSetType resourceObjectSet,
            Task task, OperationResult opResult)
            throws ActivityRunException {
        ResourceObjectClassSpecification objectClassSpec =
                createObjectClassSpecInternal(resourceObjectSet, true, task, opResult);

        LOGGER.debug("Object class specification:\n{}", objectClassSpec.debugDumpLazily());
        return objectClassSpec;
    }

    @NotNull
    private ResourceObjectClassSpecification createObjectClassSpecInternal(@NotNull ResourceObjectSetType resourceObjectSet,
            boolean checkForMaintenance, Task task, OperationResult opResult)
            throws ActivityRunException {

        String resourceOid = getResourceOid(resourceObjectSet);
        ResourceType resource = getResource(resourceOid, task, opResult);
        if (checkForMaintenance) {
            checkNotInMaintenance(resource);
        }

        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(resource);

        ObjectClassComplexTypeDefinition objectClass;
        try {
            objectClass = ModelImplUtils.determineObjectClassNew(refinedSchema, resourceObjectSet, task); // TODO source
        } catch (SchemaException e) {
            throw new ActivityRunException("Schema error", FATAL_ERROR, PERMANENT_ERROR, e);
        }
        if (objectClass == null) {
            LOGGER.debug("Processing all object classes");
        }

        return new ResourceObjectClassSpecification(
                new ResourceShadowDiscriminator(resourceOid, objectClass == null ? null : objectClass.getTypeName()),
                resource, refinedSchema, objectClass);
    }

    @NotNull
    public ResourceObjectClassSpecification createObjectClassSpecForShadow(ShadowType shadow, Task task, OperationResult opResult)
            throws ActivityRunException, SchemaException {
        String resourceOid = ShadowUtil.getResourceOid(shadow);
        ResourceType resource = getResource(resourceOid, task, opResult);
        checkNotInMaintenance(resource);
        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(resource);

        // TODO reconsider the algorithm used for deriving object class
        ObjectClassComplexTypeDefinition objectClass = requireNonNull(
                ModelImplUtils.determineObjectClass(refinedSchema, shadow.asPrismObject()),
                "No object class found for the shadow");

        return new ResourceObjectClassSpecification(
                new ResourceShadowDiscriminator(resourceOid, objectClass.getTypeName()),
                resource, refinedSchema, objectClass);
    }

    public @NotNull String getResourceOid(ResourceObjectSetType set) throws ActivityRunException {
        String resourceOid = set.getResourceRef() != null ? set.getResourceRef().getOid() : null;
        if (resourceOid == null) {
            throw new ActivityRunException("No resource OID specified", FATAL_ERROR, PERMANENT_ERROR);
        }
        return resourceOid;
    }

    private @NotNull ResourceType getResource(String resourceOid, Task task, OperationResult opResult) throws ActivityRunException {
        try {
            return provisioningService
                    .getObject(ResourceType.class, resourceOid, createReadOnlyCollection(), task, opResult)
                    .asObjectable();
        } catch (ObjectNotFoundException ex) {
            // This is bad. The resource does not exist. Permanent problem.
            throw new ActivityRunException("Resource " + resourceOid + " not found", FATAL_ERROR, PERMANENT_ERROR, ex);
        } catch (SchemaException ex) {
            // Not sure about this. But most likely it is a misconfigured resource or connector
            // It may be worth to retry. Error is fatal, but may not be permanent.
            throw new ActivityRunException("Resource " + resourceOid + " has a schema problem", FATAL_ERROR, TEMPORARY_ERROR, ex);
        } catch (CommunicationException ex) {
            throw new ActivityRunException("Communication error while getting resource " + resourceOid, FATAL_ERROR, TEMPORARY_ERROR, ex);
        } catch (RuntimeException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException | Error ex) {
            // RuntimeException: Can be anything ... but we can't recover from that.
            // It is most likely a programming error. Does not make much sense to retry.
            // Other exceptions: basically the same.
            throw new ActivityRunException("Error while getting resource " + resourceOid, FATAL_ERROR, PERMANENT_ERROR, ex);
        }
    }

    private @NotNull RefinedResourceSchema getRefinedResourceSchema(ResourceType resource) throws ActivityRunException {
        RefinedResourceSchema refinedSchema;
        try {
            refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource, LayerType.MODEL, prismContext);
        } catch (SchemaException e) {
            throw new ActivityRunException("Schema error during processing account definition", FATAL_ERROR, PERMANENT_ERROR, e);
        }

        if (refinedSchema != null) {
            return refinedSchema;
        } else {
            throw new ActivityRunException("No refined schema defined. Probably some configuration problem.", FATAL_ERROR,
                    PERMANENT_ERROR);
        }
    }

    /**
     * This method is to be used on the activity start. So it should fail gracefully - maintenance is not a fatal error here.
     */
    static void checkNotInMaintenance(ResourceType resource) throws ActivityRunException {
        if (!skipMaintenanceCheck && isInMaintenance(resource)) {
            throw new ActivityRunException("Resource is in maintenance", HANDLED_ERROR, TEMPORARY_ERROR);
        }
    }

    @VisibleForTesting
    public static void setSkipMaintenanceCheck(boolean skipMaintenanceCheck) {
        SyncTaskHelper.skipMaintenanceCheck = skipMaintenanceCheck;
    }
}
