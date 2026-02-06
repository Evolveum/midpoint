/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.smart.impl.activities;

import static com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil.createStatisticsObject;

import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

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
public abstract class AbstractStatisticsComputationActivityRun<
        WD extends WorkDefinition,
        H extends ModelActivityHandler<WD, H>,
        WS extends AbstractActivityWorkStateType>
        extends SearchBasedActivityRun<ShadowType, WD, H, WS> {

    private final Trace logger;

    /** (Resolved) resource for which the statistics are computed. */
    private ResourceType resource;

    /** Computes the statistics for the objects found. Null if statistics are not being computed. */
    private ObjectClassStatisticsComputer computer;

    protected AbstractStatisticsComputationActivityRun(
            ActivityRunInstantiationContext<WD, H> context,
            String shortNameCapitalized,
            Trace logger) {
        super(context, shortNameCapitalized);
        this.logger = logger;
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

        var objectClassDef = Resource.of(resource)
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(getObjectClassName());

        computer = new ObjectClassStatisticsComputer(objectClassDef);
        return true;
    }

    private @Nullable String findLatestStatisticsObjectOid(OperationResult result) throws SchemaException {
        var lastStatisticsObject = SmartIntegrationBeans.get().smartIntegrationService.getLatestStatistics(
                getResourceOid(), getObjectClassName(), result);
        return lastStatisticsObject != null ? lastStatisticsObject.getOid() : null;
    }

    @Override
    public @Nullable SearchSpecification<ShadowType> createCustomSearchSpecification(OperationResult result)
            throws SchemaException, ConfigurationException {

        return new SearchSpecification<>(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(getObjectClassName())
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

        var statisticsObject = createStatisticsObject(
                resource.getOid(),
                resource.getName().getOrig(),
                getObjectClassName(),
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
        return null;
    }

    /** Resource OID for which to compute statistics. */
    protected abstract @NotNull String getResourceOid();

    /** Object class name for which to compute statistics. */
    protected abstract @NotNull QName getObjectClassName();

    /** Stores the produced (or reused) statistics object OID into appropriate work state. */
    protected abstract void storeStatisticsObjectOid(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException;

    protected boolean reuseExistingStatisticsObject() { return true; }
}
