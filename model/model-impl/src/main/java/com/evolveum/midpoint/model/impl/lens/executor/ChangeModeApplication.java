/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.path.PathSet;

import com.evolveum.midpoint.util.Holder;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.simulation.SingleDeltaSimulationDataImpl;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemChangeApplicationModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Applies {@link ItemChangeApplicationModeType} to object deltas.
 *
 * Returns "reduced" execution delta + sends "delta to report" to the simulation result transaction.
 */
class ChangeModeApplication<E extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeModeApplication.class);

    @NotNull private final LensElementContext<E> elementContext;
    @NotNull private final ItemChangeApplicationModeConfiguration configuration;
    @NotNull private final ObjectDelta<E> fullDelta;
    private ObjectDelta<E> deltaForExecution;
    private ObjectDelta<E> deltaForReporting;
    @NotNull private final Task task;

    ChangeModeApplication(
            @NotNull LensElementContext<E> elementContext,
            @NotNull ObjectDelta<E> fullDelta,
            @NotNull Task task) throws SchemaException, ConfigurationException {
        this.elementContext = elementContext;
        this.configuration = elementContext.getItemChangeApplicationModeConfiguration();
        this.fullDelta = fullDelta;
        this.task = task;
    }

    public ObjectDelta<E> execute(OperationResult result) throws SchemaException, ConfigurationException {
        if (fullDelta.isAdd()) {
            processObjectAdd();
        } else if (fullDelta.isModify()) {
            processObjectModify();
        } else {
            throw new AssertionError("Only ADD/MODIFY deltas are supported: " + fullDelta);
        }

        SimulationTransaction simulationTransaction = task.getSimulationTransaction();
        if (simulationTransaction != null && deltaForReporting != null) {
            simulationTransaction.writeSimulationData(
                    SingleDeltaSimulationDataImpl.of(elementContext, deltaForReporting), task, result);
        }
        return deltaForExecution;
    }

    private void processObjectAdd() {
        PrismObject<E> objectToAdd = fullDelta.getObjectToAdd();

        Holder<Boolean> ignoredSeen = new Holder<>(false);
        Holder<Boolean> reportOnlySeen = new Holder<>(false);
        PrismObject<E> reduced1 = reduce(objectToAdd, configuration.getIgnoredItems(), ignoredSeen);
        PrismObject<E> reduced2 =
                task.isPersistentExecution() ?
                        reduce(reduced1, configuration.getReportOnlyItems(), reportOnlySeen) :
                        reduced1; // no need to skip "report" items, as they will be provided in the simulation report

        deltaForExecution =
                !ignoredSeen.getValue() && !reportOnlySeen.getValue() ? fullDelta : reduced2.createAddDelta();

        if (reportOnlySeen.getValue()) {
            // We want to report on items; but we have no possibility to mark then in ADD delta, so we'll provide the whole delta
            deltaForReporting = fullDelta;
        } else {
            deltaForReporting = null;
        }
    }

    private PrismObject<E> reduce(PrismObject<E> original, PathSet pathsToRemove, Holder<Boolean> seen) {
        PrismObject<E> reduced = null;
        for (ItemPath pathToRemove : pathsToRemove) {
            Item<?, ?> item = original.findItem(pathToRemove);
            if (item != null && item.hasAnyValue()) {
                if (reduced == null) {
                    reduced = original.clone();
                    seen.setValue(true);
                }
                //noinspection unchecked
                reduced.removeItem(pathToRemove, Item.class);
            }
        }
        return Objects.requireNonNullElse(reduced, original);
    }

    private void processObjectModify() {
        List<ItemDelta<?, ?>> toApply = new ArrayList<>();
        List<ItemDelta<?, ?>> toReport = new ArrayList<>();
        if (configuration.isAllToApply()) {
            deltaForExecution = fullDelta;
            LOGGER.trace("All items are to be applied");
            return;
        }
        for (ItemDelta<?, ?> modification : fullDelta.getModifications()) {
            ItemChangeApplicationModeType mode = configuration.getMode(modification.getPath());
            switch (mode) {
                case APPLY:
                    toApply.add(modification);
                    break;
                case REPORT:
                    if (task.isSimulatedExecution()) {
                        // "report" items will be provided in the simulation report
                        toApply.add(modification);
                    } else {
                        toReport.add(modification);
                    }
                    break;
                case IGNORE:
                    break;
                default:
                    throw new AssertionError(mode);
            }
        }
        if (!toReport.isEmpty()) {
            deltaForReporting = createEmptyDelta();
            deltaForReporting.addModifications(toReport);
        }
        deltaForExecution = createEmptyDelta();
        deltaForExecution.addModifications(toApply);
        LOGGER.trace("Delta for execution:\n{}\nDelta for reporting:\n{}",
                DebugUtil.debugDumpLazily(deltaForExecution, 1), DebugUtil.debugDumpLazily(deltaForReporting, 1));
    }

    private ObjectDelta<E> createEmptyDelta() {
        return PrismContext.get().deltaFactory().object().createEmptyModifyDelta(
                fullDelta.getObjectTypeClass(), fullDelta.getOid());
    }
}
