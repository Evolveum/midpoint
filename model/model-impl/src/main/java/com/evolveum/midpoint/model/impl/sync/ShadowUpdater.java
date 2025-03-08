/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.common.SynchronizationUtils.createSynchronizationSituationDelta;
import static com.evolveum.midpoint.common.SynchronizationUtils.createSynchronizationSituationDescriptionDelta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ShadowSimulationData;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Offloads {@link SynchronizationServiceImpl} from duties related to updating synchronization/correlation metadata.
 *
 * Accumulates deltas in {@link #deltas} list (along with applying them to the current shadow) and writes them into
 * repository - or to simulation result - when the {@link #commit(OperationResult)} method is called.
 *
 * Besides that, provides the business methods that update the operational data in the shadow.
 */
class ShadowUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    @NotNull private final SynchronizationContext<?> syncCtx;
    @NotNull private final ModelBeans beans = ModelBeans.get();
    @NotNull private final List<ItemDelta<?, ?>> deltas = new ArrayList<>();
    @NotNull private final ShadowType shadowBefore;

    ShadowUpdater(@NotNull SynchronizationContext<?> syncCtx) {
        this.syncCtx = syncCtx;
        this.shadowBefore = syncCtx.getShadowedResourceObjectBefore();
    }

    ShadowUpdater updateAllSyncMetadataRespectingMode() throws SchemaException {
        assert syncCtx.isComplete();

        XMLGregorianCalendar now = beans.clock.currentTimeXMLGregorianCalendar();

        if (shadowBefore.getSynchronizationSituation() != syncCtx.getSituation()) {
            updateSyncSituation(syncCtx.getSituation());
            updateSyncSituationDescription(syncCtx.getSituation(), now);
        }

        updateBasicSyncTimestamp(now); // needed e.g. for 3rd part of reconciliation

        return this;
    }

    private void updateSyncSituation(SynchronizationSituationType situation) throws SchemaException {
        applyShadowDelta(
                createSynchronizationSituationDelta(shadowBefore, situation));
    }

    private void updateSyncSituationDescription(SynchronizationSituationType situation, XMLGregorianCalendar now) throws SchemaException {
        applyShadowDelta(
                createSynchronizationSituationDescriptionDelta(
                        syncCtx.getShadowedResourceObject(),
                        situation,
                        now,
                        syncCtx.getChannel(),
                        syncCtx.isFullMode()));
    }

    ShadowUpdater updateFullSyncTimestamp(XMLGregorianCalendar now) throws SchemaException {
        // Not going to apply the delta to the shadow, as it's immutable now.
        addShadowDelta(
                SynchronizationUtils.createFullSynchronizationTimestampDelta(shadowBefore, now));
        return this;
    }

    private void updateBasicSyncTimestamp(XMLGregorianCalendar now) throws SchemaException {
        applyShadowDelta(
                SynchronizationUtils.createSynchronizationTimestampDelta(shadowBefore, now));
    }

    void updateBasicSyncTimestamp() throws SchemaException {
        updateBasicSyncTimestamp(
                beans.clock.currentTimeXMLGregorianCalendar());
    }

    void updateBothSyncTimestamps() throws SchemaException {
        XMLGregorianCalendar now = beans.clock.currentTimeXMLGregorianCalendar();
        updateBasicSyncTimestamp(now);
        updateFullSyncTimestamp(now);
    }

    /**
     * Updates kind/intent if some of them are null/empty/unknown.
     */
    void updateCoordinates() throws SchemaException {
        if (!syncCtx.isComplete()) {
            return;
        }
        SynchronizationContext.Complete<?> completeCtx = (SynchronizationContext.Complete<?>) syncCtx;

        ShadowType shadow = completeCtx.getShadowedResourceObject();
        ShadowKindType shadowKind = shadow.getKind();
        String shadowIntent = shadow.getIntent();
        String shadowTag = shadow.getTag();
        ShadowKindType ctxKind = completeCtx.getTypeIdentification().getKind();
        String ctxIntent = completeCtx.getTypeIdentification().getIntent();
        String ctxTag = completeCtx.getTag();

        // Are we going to update the kind/intent?
        boolean updateType =
                syncCtx.isForceClassificationUpdate() // typically when sorter is used
                        || !ShadowUtil.isClassified(shadowKind, shadowIntent);

        if (updateType) {
            // Before 4.6, the kind was updated unconditionally, only intent was driven by "force intent change" flag.
            // This is now changed to treat kind+intent as a single data item.
            deltas.addAll(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .optimizing()
                            .item(ShadowType.F_KIND).old(shadowKind).replace(ctxKind)
                            .item(ShadowType.F_INTENT).old(shadowIntent).replace(ctxIntent)
                            .asItemDeltas());
        }

        if (StringUtils.isNotBlank(ctxTag) && !ctxTag.equals(shadowTag)) {
            deltas.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_TAG).old(shadowTag).replace(ctxTag)
                            .asItemDelta());
        }

    }

    @NotNull List<ItemDelta<?, ?>> getDeltas() {
        return deltas;
    }

    void applyShadowDeltas(@NotNull Collection<? extends ItemDelta<?, ?>> deltas) throws SchemaException {
        for (ItemDelta<?, ?> delta : deltas) {
            applyShadowDelta(delta);
        }
    }

    private void applyShadowDelta(ItemDelta<?, ?> delta) throws SchemaException {
        addShadowDelta(delta);
        delta.applyTo(syncCtx.getShadowedResourceObject().asPrismObject());
    }

    private void addShadowDelta(ItemDelta<?, ?> delta) {
        deltas.add(delta);
    }

    void commit(OperationResult result) {
        if (deltas.isEmpty()) {
            return;
        }
        if (isShadowSimulation()) {
            commitToSimulation(result);
            keepOnlySynchronizationTimestampDelta();
        }
        commitToRepository(result);
        deltas.clear();
    }

    /** Even in low-level simulations, we want to record synchronization timestamp because of 3rd stage of reconciliation. */
    private void keepOnlySynchronizationTimestampDelta() {
        deltas.removeIf(
                delta -> !ShadowType.F_SYNCHRONIZATION_TIMESTAMP.equivalent(delta.getPath()));
    }

    private boolean isShadowSimulation() {
        return syncCtx.getTask().areShadowChangesSimulated();
    }

    private void commitToSimulation(OperationResult result) {
        Task task = syncCtx.getTask();
        SimulationTransaction simulationTransaction = task.getSimulationTransaction();
        if (simulationTransaction == null) {
            LOGGER.debug("Ignoring simulation data because there is no simulation transaction: {}: {}", shadowBefore, deltas);
        } else {
            simulationTransaction.writeSimulationData(
                    ShadowSimulationData.of(shadowBefore, deltas), task, result);
        }
    }

    private void commitToRepository(OperationResult result) {
        if (deltas.isEmpty()) {
            return; // Could be in the low-level simulation mode
        }
        try {
            beans.cacheRepositoryService.modifyObject(ShadowType.class, syncCtx.getShadowOid(), deltas, result);
            recordModificationExecuted(null);
        } catch (ObjectNotFoundException ex) {
            // This may happen e.g. during some recon-livesync interactions.
            // If the shadow is gone then it is gone. No point in recording the
            // situation any more.
            //
            // Also, we do not record modification executed here (see MID-9217).
            LOGGER.debug("Could not update synchronization metadata in account, because shadow {} does not "
                    + "exist any more (this may be harmless)", syncCtx.getShadowOid());
            syncCtx.setShadowExistsInRepo(false);
            result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        } catch (ObjectAlreadyExistsException | SchemaException ex) {
            recordModificationExecuted(ex);
            String message = String.format(
                    "Save of synchronization metadata failed: could not modify shadow %s: %s",
                    syncCtx.getShadowOid(), ex.getMessage());
            LoggingUtils.logException(LOGGER, "### SYNCHRONIZATION # notifyChange(..): {}", ex, message);
            result.recordFatalError(message, ex);
            throw new SystemException(message, ex);
        }
    }

    private void recordModificationExecuted(Throwable t) {
        syncCtx.getTask().recordObjectActionExecuted(
                syncCtx.getShadowedResourceObject().asPrismObject(),
                null,
                null,
                ChangeType.MODIFY,
                syncCtx.getChannel(),
                t);
    }
}
