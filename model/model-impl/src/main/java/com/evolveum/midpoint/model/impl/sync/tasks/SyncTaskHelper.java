/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.WARNING;
import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.isInMaintenance;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.APPEND;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.REPLACE;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRunSpecifics;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * Auxiliary methods for synchronization tasks: Live Sync, Async Update, Import, Reconciliation,
 * and - strange but true - Shadow Cleanup).
 *
 * Deals mainly with processing resource, objectclass, kind, intent tuple, i.e. specifying resource object class to be processed.
 * The resource object class determination has the following flow in synchronization tasks:
 *
 * 1. User specifies {@link ResourceObjectSetType} bean with `resourceRef`, OC name, kind, intent, query (many of them optional).
 *
 * 2. It is then converted to three objects:
 *
 *   - {@link ProcessingScope} that holds resolved resource, object class definition, kind, and intent, see
 *     {@link #getProcessingScopeCheckingMaintenance(ResourceObjectSetType, Task, OperationResult)}.
 *     It does _not_ contain user-specified query.
 *
 *   - {@link ResourceSearchSpecification} that contains precise query intended to obtain resource objects
 *     (plus search options). This spec is later fine-tuned by activity run to cover its specific needs
 *     (like selecting only shadows that were not updated for given time - see {@link
 *     SearchBasedActivityRunSpecifics#customizeQuery(ObjectQuery, OperationResult)}), to cater for bucketing, handling errored
 *     objects, and so on. This search specification is used only for search-based activities, e.g. not for live sync
 *     or async update.
 *
 *   - {@link PostSearchFilter} that filters any object returned by the item source (e.g. search operation
 *     in search-based activities).
 *
 * Note:
 *
 * * {@link PostSearchFilter} is currently used only for import and reconciliation. In theory, it might
 *   be used also for live sync or async update. However, it is a bit questionable, because it would mean that
 *   non-compliant changes would be skipped, i.e. in fact thrown away.
 */
@Component
public class SyncTaskHelper {

    private static final Trace LOGGER = TraceManager.getTrace(SyncTaskHelper.class);

    @Autowired private ProvisioningService provisioningService;
    @Autowired private PrismContext prismContext;

    @VisibleForTesting
    private static boolean skipMaintenanceCheck;

    /**
     * Returns the specification of objects against which the synchronization will be done ({@link ProcessingScope}).
     *
     * Also checks for the maintenance mode.
     */
    @NotNull
    public ProcessingScope getProcessingScopeCheckingMaintenance(
            @NotNull ResourceObjectSetType resourceObjectSet, Task task, OperationResult opResult)
            throws ActivityRunException {
        ProcessingScope spec = createProcessingScope(resourceObjectSet, true, task, opResult);
        LOGGER.debug("Bare processing scope specification:\n{}", spec.debugDumpLazily());
        return spec;
    }

    /**
     * Creates "complete" search specification from given configuration.
     * Contains processing scope + user-specified query.
     *
     * Does _not_ contain tweaking (customizations) by activity run. These are applied later.
     */
    public ResourceSearchSpecification createSearchSpecification(
            @NotNull ResourceObjectSetType set, Task task, OperationResult opResult)
            throws ActivityRunException, SchemaException {

        ProcessingScope processingScope =
                createProcessingScope(set, false, task, opResult);

        @NotNull ObjectQuery bareQuery = processingScope.createBareQuery();
        @NotNull ObjectQuery resultingQuery = applyConfiguredQuery(bareQuery, set.getQuery(), set.getQueryApplication());

        return new ResourceSearchSpecification(
                resultingQuery,
                GetOperationOptionsUtil.optionsBeanToOptions(set.getSearchOptions()));
    }

    /**
     * Applies configured query to the bare query.
     */
    private @NotNull ObjectQuery applyConfiguredQuery(
            @NotNull ObjectQuery bareQuery,
            QueryType configuredQueryBean,
            ResourceObjectSetQueryApplicationModeType queryApplicationMode)
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
    private ProcessingScope createProcessingScope(@NotNull ResourceObjectSetType resourceObjectSet,
            boolean checkForMaintenance, Task task, OperationResult opResult)
            throws ActivityRunException {

        String resourceOid = getResourceOid(resourceObjectSet);
        ResourceType resource = getResource(resourceOid, task, opResult);
        if (checkForMaintenance) {
            checkNotInMaintenance(resource);
        }
        return ProcessingScope.of(resource, resourceObjectSet);
    }

    /** Creates {@link ProcessingScope} for a single shadow. */
    @NotNull
    public ProcessingScope createProcessingScopeForShadow(
            @NotNull ShadowType shadow, Task task, OperationResult opResult)
            throws ActivityRunException, SchemaException {
        String resourceOid = ShadowUtil.getResourceOid(shadow);
        ResourceType resource = getResource(resourceOid, task, opResult);
        checkNotInMaintenance(resource);
        return ProcessingScope.of(resource, shadow);
    }

    public @NotNull String getResourceOid(ResourceObjectSetType set) throws ActivityRunException {
        String resourceOid = set.getResourceRef() != null ? set.getResourceRef().getOid() : null;
        if (resourceOid == null) {
            throw new ActivityRunException("No resource OID specified", FATAL_ERROR, PERMANENT_ERROR);
        }
        return resourceOid;
    }

    private @NotNull ResourceType getResource(String resourceOid, Task task, OperationResult opResult)
            throws ActivityRunException {
        try {
            return provisioningService
                    .getObject(ResourceType.class, resourceOid, readOnly(), task, opResult)
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

    /**
     * This method is to be used on the activity start. So it should fail gracefully - maintenance is not a fatal error here.
     */
    private static void checkNotInMaintenance(ResourceType resource) throws ActivityRunException {
        if (!skipMaintenanceCheck && isInMaintenance(resource)) {
            // Temporary error means that the recurring task should continue (until the maintenance mode is off).
            throw new ActivityRunException("Couldn't synchronize resource '" + resource.getName()
                    + "', because it is in maintenance", WARNING, TEMPORARY_ERROR);
        }
    }

    @VisibleForTesting
    public static void setSkipMaintenanceCheck(boolean skipMaintenanceCheck) {
        SyncTaskHelper.skipMaintenanceCheck = skipMaintenanceCheck;
    }
}
