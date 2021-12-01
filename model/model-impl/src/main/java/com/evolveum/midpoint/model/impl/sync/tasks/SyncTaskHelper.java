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

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.APPEND;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.REPLACE;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.schema.util.*;

import com.evolveum.prism.xml.ns._public.query_3.QueryType;

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
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRunSpecifics;

import java.util.Collection;

/**
 * Auxiliary methods for synchronization tasks: Live Sync, Async Update, Import, Reconciliation,
 * and - strange but true - Shadow Cleanup).
 *
 * Deals mainly with processing resource, objectclass, kind, intent tuple, i.e. specifying resource object class to be processed.
 * The resource object class determination has the following flow in synchronization tasks:
 *
 * 1. User specifies `ResourceObjectSetType` bean with `resourceRef`, OC name, kind, intent, query (many of them optional).
 *
 * 2. It is then converted to three objects:
 *
 *   - {@link ResourceObjectClass} that holds resolved resource, object class definition, kind, and intent, see
 *     {@link #getResourceObjectClassCheckingMaintenance(ResourceObjectSetType, Task, OperationResult)}.
 *     It does _not_ contain user-specified query.
 *
 *   - {@link ResourceSearchSpecification} that contains precise query intended to obtain resource objects
 *     (plus search options). This spec is later fine-tuned by activity run to cover its specific needs
 *     (like selecting only shadows that were not updated for given time - see {@link
 *     SearchBasedActivityRunSpecifics#customizeQuery(ObjectQuery, OperationResult)}), to cater for bucketing, handling errored
 *     objects, and so on. This search specification is used only for search-based activities, e.g. not for live sync
 *     or async update.
 *
 *   - {@link SynchronizationObjectsFilter} that filters any object returned by the item source (e.g. search operation
 *     in search-based activities).
 *
 * Notes:
 *
 * * {@link SynchronizationObjectsFilter} is currently used only for import and reconciliation. In theory, it might
 *   be used also for live sync or async update. However, it is a bit questionable, because it would mean that
 *   non-compliant changes would be skipped, i.e. in fact thrown away.
 *
 * * The overall handling of object class / kind / intent triple in synchronization tasks (mainly import and reconciliation)
 *   is quite counter-intuitive: It works well if kind + intent combo is used. However, when object class is used, the
 *   provisioning module gets correct "objectclass = XYZ" style query, but it then looks up first matching refined
 *   object definition, and uses it. See {@link ProvisioningService#searchObjects(Class, ObjectQuery, Collection, Task,
 *   OperationResult)} and MID-7470 for more information.
 */
@Component
public class SyncTaskHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SyncTaskHelper.class);

    @Autowired private ProvisioningService provisioningService;
    @Autowired private PrismContext prismContext;

    @VisibleForTesting
    private static boolean skipMaintenanceCheck;

    /**
     * Returns specification of the object class against which the synchronization will be done
     * ({@link ResourceObjectClass}).
     *
     * Checks for the maintenance mode.
     */
    @NotNull
    public ResourceObjectClass getResourceObjectClassCheckingMaintenance(
            @NotNull ResourceObjectSetType resourceObjectSet, Task task, OperationResult opResult)
            throws ActivityRunException {
        ResourceObjectClass spec =
                createResourceObjectClass(resourceObjectSet, true, task, opResult);
        LOGGER.debug("Bare resource object set specification:\n{}", spec.debugDumpLazily());
        return spec;
    }

    /**
     * Creates "complete" search specification from given configuration.
     * Contains object class + user-specified query.
     *
     * Does _not_ contain tweaking (customizations) by activity run. These are applied later.
     */
    public ResourceSearchSpecification createSearchSpecification(
            @NotNull ResourceObjectSetType set, Task task, OperationResult opResult)
            throws ActivityRunException, SchemaException {

        ResourceObjectClass resourceObjectClass =
                createResourceObjectClass(set, false, task, opResult);

        @NotNull ObjectQuery bareQuery = resourceObjectClass.createBareQuery();
        @NotNull ObjectQuery resultingQuery = applyConfiguredQuery(bareQuery, set.getQuery(), set.getQueryApplication());

        return new ResourceSearchSpecification(
                resultingQuery,
                GetOperationOptionsUtil.optionsBeanToOptions(set.getSearchOptions()));
    }

    /**
     * Applies configured query to the bare query.
     */
    private @NotNull ObjectQuery applyConfiguredQuery(@NotNull ObjectQuery bareQuery,
            QueryType configuredQueryBean, ResourceObjectSetQueryApplicationModeType queryApplicationMode)
            throws SchemaException, ActivityRunException {
        if (configuredQueryBean == null) {
            return bareQuery;
        }

        ObjectQuery configuredQuery = prismContext.getQueryConverter()
                .createObjectQuery(ShadowType.class, configuredQueryBean);
        if (queryApplicationMode == REPLACE) {
            return configuredQuery;
        } else if (queryApplicationMode == APPEND) {
            return ObjectQueryUtil.addConjunctions(configuredQuery, bareQuery.getFilter());
        } else {
            throw new ActivityRunException("Unsupported query application mode: " + queryApplicationMode,
                    FATAL_ERROR, PERMANENT_ERROR);
        }
    }

    @NotNull
    private ResourceObjectClass createResourceObjectClass(@NotNull ResourceObjectSetType resourceObjectSet,
            boolean checkForMaintenance, Task task, OperationResult opResult)
            throws ActivityRunException {

        String resourceOid = getResourceOid(resourceObjectSet);
        ResourceType resource = getResource(resourceOid, task, opResult);
        if (checkForMaintenance) {
            checkNotInMaintenance(resource);
        }

        RefinedResourceSchema refinedResourceSchema = getRefinedResourceSchema(resource);

        ObjectClassComplexTypeDefinition objectClassDefinition;
        try {
            objectClassDefinition = ModelImplUtils.determineObjectClassNew(refinedResourceSchema, resourceObjectSet, task);
        } catch (SchemaException e) {
            throw new ActivityRunException("Schema error", FATAL_ERROR, PERMANENT_ERROR, e);
        }
        if (objectClassDefinition == null) {
            LOGGER.debug("Processing all object classes");
        }

        return new ResourceObjectClass(
                resource, objectClassDefinition, resourceObjectSet.getKind(), resourceObjectSet.getIntent());
    }

    /** Creates {@link ResourceObjectClass} for a single shadow. */
    @NotNull
    public ResourceObjectClass createObjectClassForShadow(ShadowType shadow, Task task, OperationResult opResult)
            throws ActivityRunException, SchemaException {
        String resourceOid = ShadowUtil.getResourceOid(shadow);
        ResourceType resource = getResource(resourceOid, task, opResult);
        checkNotInMaintenance(resource);
        RefinedResourceSchema refinedSchema = getRefinedResourceSchema(resource);

        // TODO reconsider the algorithm used for deriving object class
        ObjectClassComplexTypeDefinition objectClass = requireNonNull(
                ModelImplUtils.determineObjectClass(refinedSchema, shadow.asPrismObject()),
                "No object class found for the shadow");

        return new ResourceObjectClass(resource, objectClass, null, null);
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
