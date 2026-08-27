/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.mappingSuggestion;

import static com.evolveum.midpoint.schema.util.ShadowObjectTypeUtil.createObjectTypeStatisticsObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.impl.activities.ObjectTypeStatisticsComputer;
import com.evolveum.midpoint.smart.impl.activities.Util;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity run responsible for computing object type statistics before the mapping suggestion step.
 *
 * Skipped when RAW_DATA_ACCESS is not granted, since
 * the statistics are only used for categorical mapping suggestions which require that permission.
 *
 * If suitable statistics already exist they are reused; otherwise new ones are computed.
 */
public class MappingsSuggestionStatisticsComputationActivityRun
        extends SearchBasedActivityRun<
        ShadowType,
        MappingsSuggestionWorkDefinition,
        MappingsSuggestionActivityHandler,
        MappingsSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingsSuggestionStatisticsComputationActivityRun.class);

    private final ModelService modelService;
    private final RepositoryService repositoryService;
    private final SmartIntegrationService smartIntegrationService;
    private final Clock clock;

    private ResourceType resource;
    private ObjectTypeStatisticsComputer computer;

    MappingsSuggestionStatisticsComputationActivityRun(
            @NotNull ActivityRunInstantiationContext<MappingsSuggestionWorkDefinition, MappingsSuggestionActivityHandler> context,
            ModelService modelService,
            RepositoryService repositoryService,
            SmartIntegrationService smartIntegrationService,
            Clock clock) {
        super(context, "Statistics computation");
        this.modelService = modelService;
        this.repositoryService = repositoryService;
        this.smartIntegrationService = smartIntegrationService;
        this.clock = clock;
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .skipWritingOperationExecutionRecords(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!getWorkDefinition().getPermissions().contains(DataAccessPermissionType.RAW_DATA_ACCESS)) {
            LOGGER.debug("Skipping statistics computation: {} permission not granted",
                    DataAccessPermissionType.RAW_DATA_ACCESS);
            return false;
        }

        if (!super.beforeRun(result)) {
            return false;
        }

        ensureNoDryRun();
        ensureNoParallelism();

        resource = modelService
                .getObject(ResourceType.class, getWorkDefinition().getResourceOid(), null, getRunningTask(), result)
                .asObjectable();

        var foundOid = findLatestStatisticsObjectOid(result);
        if (foundOid != null) {
            LOGGER.debug("Found existing object type statistics object with OID {}, will skip the computation", foundOid);
            storeStatisticsObjectOid(foundOid, result);
            return false;
        }

        LOGGER.debug("No suitable object type statistics found, will compute one");
        ResourceObjectTypeDefinition objectTypeDefinition =
                Resource.of(resource)
                        .getCompleteSchemaRequired()
                        .getObjectTypeDefinitionRequired(getWorkDefinition().getTypeIdentification());
        computer = new ObjectTypeStatisticsComputer(objectTypeDefinition);
        return true;
    }

    private @Nullable String findLatestStatisticsObjectOid(OperationResult result) throws SchemaException {
        var def = getWorkDefinition();
        var lastStatisticsObject = smartIntegrationService
                .getLatestObjectTypeStatistics(def.getResourceOid(), def.getKind(), def.getIntent(), result);
        return lastStatisticsObject != null ? lastStatisticsObject.getOid() : null;
    }

    @Override
    public @Nullable SearchSpecification<ShadowType> createCustomSearchSpecification(OperationResult result)
            throws SchemaException, ConfigurationException {
        var def = getWorkDefinition();
        return new SearchSpecification<>(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(def.getTypeIdentification())
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
                .coverage(1.0f)
                .timestamp(clock.currentTimeXMLGregorianCalendar());

        var def = getWorkDefinition();
        var statisticsObject = createObjectTypeStatisticsObject(
                resource.getOid(),
                resource.getName().getOrig(),
                def.getKind(),
                def.getIntent(),
                statistics);

        LOGGER.debug("Adding object type statistics object:\n{}", statisticsObject.debugDump(1));

        var oid = repositoryService.addObject(statisticsObject.asPrismObject(), null, result);
        storeStatisticsObjectOid(oid, result);
    }

    private void storeStatisticsObjectOid(String oid, OperationResult result)
            throws SchemaException, ActivityRunException, ObjectNotFoundException {
        var parentState = Util.getParentState(this, result);
        parentState.setWorkStateItemRealValues(
                MappingsSuggestionWorkStateType.F_STATISTICS_REF,
                ObjectTypeUtil.createObjectRef(oid, ObjectTypes.GENERIC_OBJECT));
        parentState.flushPendingTaskModificationsChecked(result);
    }
}
