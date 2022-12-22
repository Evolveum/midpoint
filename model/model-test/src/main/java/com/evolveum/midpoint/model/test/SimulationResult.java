/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.simulation.SimulationResultManager;
import com.evolveum.midpoint.model.common.TagManager;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;
import com.evolveum.midpoint.test.DummyAuditEventListener;
import com.evolveum.midpoint.test.MidpointTestContextWithTask;
import com.evolveum.midpoint.test.TestSpringBeans;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * TODO
 */
@SuppressWarnings("WeakerAccess") // temporary
@Experimental
public class SimulationResult {

    @Nullable private final String simulationResultOid;
    private final List<ObjectDelta<?>> executedDeltas = new ArrayList<>();
    private final List<ObjectDeltaOperation<?>> auditedDeltas = new ArrayList<>();
    private final List<ProcessedObject<?>> objectsProcessedBySimulation = new ArrayList<>();
    private ModelContext<?> lastModelContext;

    SimulationResult(@Nullable String simulationResultOid) {
        this.simulationResultOid = simulationResultOid;
    }

    public static SimulationResult fromSimulationResultOid(@NotNull String simulationResultOid) {
        return new SimulationResult(simulationResultOid);
    }

    public List<ObjectDelta<?>> getExecutedDeltas() {
        return executedDeltas;
    }

    public List<ObjectDelta<?>> getSimulatedDeltas() {
        return objectsProcessedBySimulation.stream()
                .map(ProcessedObject::getDelta)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // TODO delete?
    public Collection<String> getTagsForObjectType(@NotNull Class<? extends ObjectType> type) {
        return objectsProcessedBySimulation.stream()
                .filter(o -> type.equals(o.getType()))
                .flatMap(o -> o.getEventTags().stream())
                .collect(Collectors.toSet());
    }

    public ModelContext<?> getLastModelContext() {
        return lastModelContext;
    }

    ProgressListener contextRecordingListener() {
        return new ProgressListener() {
            @Override
            public void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation) {
                lastModelContext = modelContext;
            }

            @Override
            public boolean isAbortRequested() {
                return false;
            }
        };
    }

    AggregatedObjectProcessingListener aggregatedObjectProcessingListener() {
        return this::onItemProcessed;
    }

    DummyAuditEventListener auditEventListener() {
        return record -> auditedDeltas.addAll(record.getDeltas());
    }

    private <O extends ObjectType> void onItemProcessed(
            @Nullable O stateBefore,
            @Nullable ObjectDelta<O> executedDelta,
            @Nullable ObjectDelta<O> simulatedDelta,
            @NotNull Collection<String> eventTags,
            @NotNull OperationResult ignored) throws SchemaException {
        if (executedDelta != null) {
            executedDeltas.add(executedDelta);
        }
        ProcessedObject<?> processedObject = ProcessedObject.create(stateBefore, simulatedDelta, eventTags);
        if (processedObject != null) {
            objectsProcessedBySimulation.add(processedObject);
        }
    }

    public void assertNoExecutedNorAuditedDeltas() {
        // This is a bit fake. We currently do not report executed deltas using onItemProcessed method.
        assertThat(executedDeltas).as("executed deltas").isEmpty();

        // In a similar way, auditing is currently explicitly turned off in non-persistent mode. Nevertheless, this
        // could catch situations when (e.g. embedded) clockwork is executed in persistent mode.
        assertThat(auditedDeltas).as("audited deltas").isEmpty();
    }

    public Collection<ProcessedObject<?>> getProcessedObjects(OperationResult result) throws CommonException {
        if (simulationResultOid != null) {
            return getPersistentProcessedObjects(result);
        } else {
            return getTransientProcessedObjects(result);
        }
    }

    public List<ProcessedObject<?>> getTransientProcessedObjects(OperationResult result) {
        resolveTagNames(objectsProcessedBySimulation, result);
        return objectsProcessedBySimulation;
    }

    public @NotNull List<ProcessedObject<?>> getPersistentProcessedObjects(OperationResult result) throws CommonException {
        stateCheck(
                simulationResultOid != null,
                "Asking for persistent processed objects but there is no simulation result OID");
        List<ProcessedObject<?>> objects = TestSpringBeans.getBean(SimulationResultManager.class)
                .getStoredProcessedObjects(simulationResultOid, result);
        resolveTagNames(objects, result);
        applyAttributesDefinitions(objects, result);
        return objects;
    }

    public void resolveTagNames(Collection<ProcessedObject<?>> processedObjects, OperationResult result) {
        TagManager tagManager = TestSpringBeans.getBean(TagManager.class);
        for (ProcessedObject<?> processedObject : processedObjects) {
            if (processedObject.getEventTagsMap() == null) {
                processedObject.setEventTagsMap(
                        tagManager.resolveTagNames(processedObject.getEventTags(), result));
            }
        }
    }

    /**
     * Shadow deltas stored in the repository have no definitions. These will be found and applied now.
     */
    private void applyAttributesDefinitions(List<ProcessedObject<?>> objects, OperationResult result) throws CommonException {
        for (ProcessedObject<?> object : objects) {
            if (object.getDelta() == null
                    || !ShadowType.class.equals(object.getType())) {
                continue;
            }
            ShadowType shadow = (ShadowType) object.getAfterOrBefore();
            if (shadow == null) {
                throw new IllegalStateException("No object? In: " + object);
            }
            TestSpringBeans.getBean(ProvisioningService.class)
                    .applyDefinition(object.getDelta(), MidpointTestContextWithTask.get().getTask(), result);
        }
    }

    public boolean isPersistent() {
        return simulationResultOid != null;
    }
}
