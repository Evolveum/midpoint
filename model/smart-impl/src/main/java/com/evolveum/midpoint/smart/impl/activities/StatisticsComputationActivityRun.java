/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl.activities;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.toMillis;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.MODEL_EXTENSION_RESOURCE_OID;
import static com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil.createStatisticsObject;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypesSuggestionWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Reads data from the resource and computes statistics for the objects found.
 *
 * Skips the processing if there is already a valid statistics object in the repository.
 *
 * Later, we can create a variant of this activity that computes statistics for objects already present in the repository.
 */
public class StatisticsComputationActivityRun
        extends SearchBasedActivityRun<
            ShadowType,
            ObjectTypesSuggestionWorkDefinition,
            ObjectTypesSuggestionActivityHandler,
            ObjectTypesSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(StatisticsComputationActivityRun.class);

    /** (Resolved) resource for which the statistics are computed. Null if statistics are not being computed. */
    private ResourceType resource;

    /** Computes the statistics for the objects found. Null if statistics are not being computed. */
    private StatisticsComputer computer;

    StatisticsComputationActivityRun(
            ActivityRunInstantiationContext<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> context,
            String shortNameCapitalized) {
        super(context, shortNameCapitalized);
        setInstanceReady();
    }

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

        var workDef = getWorkDefinition();

        var presetOid = workDef.getStatisticsObjectOid();
        if (presetOid != null) {
            LOGGER.debug("Statistics object OID is pre-set to {}, will skip the execution", presetOid);
            setStatisticsObjectOidInWorkState(presetOid, result);
            return false;
        }

        var foundOid = findLatestStatisticsObjectOid(result);
        if (foundOid != null) {
            LOGGER.debug("Found existing statistics object with OID {}, will skip the execution", foundOid);
            setStatisticsObjectOidInWorkState(foundOid, result);
            return false;
        }

        LOGGER.debug("No suitable statistics data found, will compute one");
        resource = getActivityHandler().getModelBeans().modelService
                .getObject(ResourceType.class, workDef.getResourceOid(), null, getRunningTask(), result)
                .asObjectable();
        var objectClassDef = Resource.of(resource)
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(workDef.getObjectClassName());
        computer = new StatisticsComputer(objectClassDef);

        return true;
    }

    private @Nullable String findLatestStatisticsObjectOid(OperationResult result) throws SchemaException {
        var workDef = getWorkDefinition();
        var objects = beans.repositoryService.searchObjects(
                GenericObjectType.class,
                PrismContext.get().queryFor(GenericObjectType.class)
                        .item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_RESOURCE_OID)
                        .eq(workDef.getResourceOid())
                        .and().item(GenericObjectType.F_EXTENSION, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME)
                        .eq(workDef.getObjectClassName().getLocalPart())
                        .build(),
                null,
                result);
        return objects.stream()
                .max(Comparator.comparing(
                        o -> toMillis(ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(o).getTimestamp())))
                .map(PrismObject::getOid)
                .orElse(null);
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {
        super.afterRun(result);
        if (getRunningTask().canRun()) {
            computer.postProcessStatistics();
            var statistics = computer.getStatistics()
                    .coverage(1.0f) // TODO: compute coverage properly
                    .timestamp(beans.clock.currentTimeXMLGregorianCalendar());

            var statisticsObject = createStatisticsObject(
                    resource.getOid(),
                    resource.getName().getOrig(),
                    getWorkDefinition().getObjectClassName(),
                    statistics);

            LOGGER.debug("Adding statistics object:\n{}", statisticsObject.debugDump(1));

            var statisticsObjectOid =
                    getBeans().repositoryService.addObject(statisticsObject.asPrismObject(), null, result);

            setStatisticsObjectOidInWorkState(statisticsObjectOid, result);
        }
    }

    private void setStatisticsObjectOidInWorkState(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = Util.getParentState(this, result);
        parentState.setWorkStateItemRealValues(
                ObjectTypesSuggestionWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }

    @Override
    public @Nullable SearchSpecification<ShadowType> createCustomSearchSpecification(OperationResult result)
            throws SchemaException, ConfigurationException {
        return new SearchSpecification<>(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(getWorkDefinition().getObjectClassName())
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
}
