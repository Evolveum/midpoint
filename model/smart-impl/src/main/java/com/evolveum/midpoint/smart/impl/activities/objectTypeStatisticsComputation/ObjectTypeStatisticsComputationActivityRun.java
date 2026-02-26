/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.objectTypeStatisticsComputation;

import static com.evolveum.midpoint.schema.util.ShadowObjectTypeStatisticsTypeUtil.createObjectTypeStatisticsObject;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.smart.impl.activities.ObjectTypeStatisticsComputer;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.slf4j.Logger;

/**
 * Base activity run for computing statistics for a specific object class on a resource.
 *
 * <p>The activity processes all relevant {@link ShadowType} objects of the given object class
 * and computes aggregated statistics that are persisted as a statistics object.</p>
 *
 * <p>The default behavior is two-phase:
 * <ul>
 *   <li>If an existing statistics object is available, it is reused and computation is skipped.</li>
 *   <li>If no suitable statistics exist, a new statistics object is computed and stored.</li>
 * </ul>
 * Subclasses may override {@link #reuseExistingStatisticsObject()} to always force regeneration.</p>
 *
 * <p>This class provides the common execution flow; concrete subclasses define how the resulting
 * statistics object reference is stored and whether reuse is allowed.</p>
 */
public class ObjectTypeStatisticsComputationActivityRun
        extends SearchBasedActivityRun<
        ShadowType, ObjectTypeStatisticsComputationWorkDefinition,
        ObjectTypeStatisticsComputationActivityHandler, ObjectTypeStatisticsComputationWorkStateType> {

    Logger logger = TraceManager.getTrace(ObjectTypeStatisticsComputationActivityRun.class);

    /** (Resolved) resource for which the statistics are computed. */
    private ResourceType resource;

    /** Computes the statistics for the objects found. Null if statistics are not being computed. */
    private ObjectTypeStatisticsComputer computer;

    protected ObjectTypeStatisticsComputationActivityRun(
            ActivityRunInstantiationContext<ObjectTypeStatisticsComputationWorkDefinition,
                    ObjectTypeStatisticsComputationActivityHandler> context,
            String shortNameCapitalized) {
        super(context, shortNameCapitalized);
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .skipWritingOperationExecutionRecords(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }

        ensureNoDryRun();
        ensureNoParallelism();

        // Resolve resource
        resource = getActivityHandler().getModelBeans().modelService
                .getObject(ResourceType.class, getResourceOid(), null, getRunningTask(), result)
                .asObjectable();

        if (reuseExistingStatisticsObject()) {
            var presetOid = getPresetStatisticsObjectOid();
            if (presetOid != null) {
                logger.debug("Statistics object OID is pre-set to {}, will skip the execution", presetOid);
                storeStatisticsObjectOid(presetOid, result);
                return false;
            }

            var foundOid = findLatestStatisticsObjectOid(result);
            if (foundOid != null) {
                logger.debug("Found existing statistics object with OID {}, will skip the execution", foundOid);
                storeStatisticsObjectOid(foundOid, result);
                return false;
            }

            logger.debug("No suitable statistics data found, will compute one");
        }

        var res = Resource.of(resource);
        ResourceSchema completeSchemaRequired = res.getCompleteSchemaRequired();
        ResourceObjectTypeDefinition objectTypeDefinition = completeSchemaRequired.getObjectTypeDefinition(getKind(), getIntent());
        computer = new ObjectTypeStatisticsComputer(objectTypeDefinition);

        return true;
    }

    private @Nullable String findLatestStatisticsObjectOid(OperationResult result) throws SchemaException {

        var lastStatisticsObject = SmartIntegrationBeans.get().smartIntegrationService.getLatestObjectTypeStatistics(
                getResourceOid(), getKind().value(), getIntent(), result);
        return lastStatisticsObject != null ? lastStatisticsObject.getOid() : null;
    }

    @Override
    public @Nullable SearchSpecification<ShadowType> createCustomSearchSpecification(OperationResult result)
            throws SchemaException, ConfigurationException {

        return new SearchSpecification<>(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(getKind(), getIntent())
                        .build(),
                null,
                false);
    }

    @Override
    public boolean processItem(
            @NotNull ShadowType item,
            @NotNull ItemProcessingRequest<ShadowType> request,
            RunningTask workerTask,
            OperationResult result) {
        computer.process(item);
        return true;
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {
        super.afterRun(result);

        if (!getRunningTask().canRun()) {
            return;
        }

        computer.postProcessStatistics();

        var statistics = computer.getStatistics()
                .coverage(1.0f) // TODO: compute coverage properly
                .timestamp(beans.clock.currentTimeXMLGregorianCalendar());

        var statisticsObject = createObjectTypeStatisticsObject(
                resource.getOid(),
                resource.getName().getOrig(),
                getKind().value(),
                getIntent(),
                statistics);

        logger.debug("Adding statistics object:\n{}", statisticsObject.debugDump(1));

        var oid = getBeans().repositoryService.addObject(statisticsObject.asPrismObject(), null, result);
        storeStatisticsObjectOid(oid, result);
    }

    /**
     * Returns preset statistics object OID, if any. Default: none.
     * Embedded variant can override this to support workDef.getStatisticsObjectOid().
     */
    protected @Nullable String getPresetStatisticsObjectOid() {
        return getWorkDefinition().getStatisticsObjectOid();
    }

    /** Resource OID for which to compute statistics. */
    protected @NotNull String getResourceOid() {
        return getWorkDefinition().getResourceOid();
    }

    /** Stores the produced (or reused) statistics object OID into appropriate work state. */
    protected void storeStatisticsObjectOid(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = this.getActivityState();
        parentState.setWorkStateItemRealValues(
                ObjectTypeStatisticsComputationWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }

    protected boolean reuseExistingStatisticsObject() {
        return false;
    }

    private @NotNull ShadowKindType getKind() {
        return getWorkDefinition().getShadowTypeKind();
    }

    private @NotNull String getIntent() {
        return getWorkDefinition().getIntent();
    }
}
