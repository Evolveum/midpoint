/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.common.SynchronizationUtils.*;

/**
 * Offloads {@link SynchronizationServiceImpl} from duties related to updating synchronization/correlation metadata.
 *
 * Accumulates deltas in {@link #deltas} list (along with applying them to the current shadow) and writes them into
 * repository when the {@link #commit(OperationResult)} method is called.
 *
 * Besides that, provides the business methods that update the operational data in the shadow.
 */
class ShadowUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    @NotNull private final SynchronizationContext<?> syncCtx;
    @NotNull private final ModelBeans beans;
    @NotNull private final List<ItemDelta<?, ?>> deltas = new ArrayList<>();

    ShadowUpdater(@NotNull SynchronizationContext<?> syncCtx, @NotNull ModelBeans beans) {
        this.syncCtx = syncCtx;
        this.beans = beans;
    }

    ShadowUpdater updateAllSyncMetadata() throws SchemaException {
        assert syncCtx.isComplete();

        XMLGregorianCalendar now = beans.clock.currentTimeXMLGregorianCalendar();

        updateSyncSituation();
        updateSyncSituationDescription(now);
        updateSyncTimestamps(now, syncCtx.isFullMode());

        updateCoordinatesIfUnknown();

        return this;
    }

    private void updateSyncSituation() throws SchemaException {
        applyShadowDelta(
                createSynchronizationSituationDelta(
                        syncCtx.getShadowedResourceObject().asPrismObject(),
                        syncCtx.getSituation()));
    }

    private void updateSyncSituationDescription(XMLGregorianCalendar now) throws SchemaException {
        applyShadowDelta(
                createSynchronizationSituationDescriptionDelta(
                        syncCtx.getShadowedResourceObject().asPrismObject(),
                        syncCtx.getSituation(),
                        now,
                        syncCtx.getChannel(),
                        syncCtx.isFullMode()));
    }

    private ShadowUpdater updateSyncTimestamps(XMLGregorianCalendar now, boolean full) throws SchemaException {
        applyShadowDeltas(
                SynchronizationUtils.createSynchronizationTimestampsDeltas(
                        syncCtx.getShadowedResourceObject().asPrismObject(),
                        now,
                        full));
        return this;
    }

    ShadowUpdater updateBothSyncTimestamps() throws SchemaException {
        return updateSyncTimestamps(
                beans.clock.currentTimeXMLGregorianCalendar(),
                true);
    }

    /**
     * Updates kind/intent if some of them are null/empty. This is used if synchronization is skipped.
     */
    ShadowUpdater updateCoordinatesIfMissing() throws SchemaException {
        assert syncCtx.isComplete();
        return updateCoordinates(false);
    }

    /**
     * Updates kind/intent if some of them are null/empty/unknown. This is used if synchronization is NOT skipped.
     *
     * TODO why the difference from {@link #updateCoordinatesIfMissing()}? Is there any reason for it?
     * TODO this behavior should be made configurable
     */
    private void updateCoordinatesIfUnknown() throws SchemaException {
        assert syncCtx.isComplete();
        updateCoordinates(true);
    }

    private ShadowUpdater updateCoordinates(boolean overwriteUnknownValues) throws SchemaException {
        assert syncCtx.isComplete();
        SynchronizationContext.Complete<?> completeCtx = (SynchronizationContext.Complete<?>) syncCtx;

        ShadowType shadow = completeCtx.getShadowedResourceObject();
        ShadowKindType shadowKind = shadow.getKind();
        String shadowIntent = shadow.getIntent();
        String shadowTag = shadow.getTag();
        ShadowKindType ctxKind = completeCtx.getTypeIdentification().getKind();
        String ctxIntent = completeCtx.getTypeIdentification().getIntent();
        String ctxTag = completeCtx.getTag();

        boolean typeEmpty = shadowKind == null || StringUtils.isBlank(shadowIntent);
        boolean typeNotKnown = ShadowUtil.isNotKnown(shadowKind) || ShadowUtil.isNotKnown(shadowIntent);

        // Are we going to update the kind/intent?
        boolean updateType =
                syncCtx.isForceClassificationUpdate() // typically when sorter is used
                        || typeEmpty
                        || typeNotKnown && overwriteUnknownValues;

        if (updateType) {
            // Before 4.6, the kind was updated unconditionally, only intent was driven by "force intent change" flag.
            // This is now changed to treat kind+intent as a single data item.
            if (ctxKind != shadowKind) {
                deltas.add(
                        PrismContext.get().deltaFor(ShadowType.class)
                                .item(ShadowType.F_KIND).replace(ctxKind)
                                .asItemDelta());
            }
            if (!ctxIntent.equals(shadowIntent)) {
                deltas.add(
                        PrismContext.get().deltaFor(ShadowType.class)
                                .item(ShadowType.F_INTENT).replace(ctxIntent)
                                .asItemDelta());
            }
        }

        if (StringUtils.isNotBlank(ctxTag) && !ctxTag.equals(shadowTag)) {
            deltas.add(
                    PrismContext.get().deltaFor(ShadowType.class)
                            .item(ShadowType.F_TAG).replace(ctxTag)
                            .asItemDelta());
        }

        return this;
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
        deltas.add(delta);
        delta.applyTo(syncCtx.getShadowedResourceObject().asPrismObject());
    }

    void commit(OperationResult result) {
        ShadowType shadow = syncCtx.getShadowedResourceObject();
        try {
            beans.cacheRepositoryService.modifyObject(ShadowType.class, shadow.getOid(), deltas, result);
            deltas.clear();
            recordModificationExecuted(null);
        } catch (ObjectNotFoundException ex) {
            recordModificationExecuted(ex);
            // This may happen e.g. during some recon-livesync interactions.
            // If the shadow is gone then it is gone. No point in recording the
            // situation any more.
            LOGGER.debug("Could not update synchronization metadata in account, because shadow {} does not "
                            + "exist any more (this may be harmless)", shadow.getOid());
            syncCtx.setShadowExistsInRepo(false);
            result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        } catch (ObjectAlreadyExistsException | SchemaException ex) {
            recordModificationExecuted(ex);
            LoggingUtils.logException(LOGGER,
                    "### SYNCHRONIZATION # notifyChange(..): Save of synchronization metadata failed: could not modify shadow "
                            + shadow.getOid() + ": " + ex.getMessage(),
                    ex);
            result.recordFatalError("Save of synchronization metadata failed: could not modify shadow "
                    + shadow.getOid() + ": " + ex.getMessage(), ex);
            throw new SystemException("Save of synchronization metadata failed: could not modify shadow "
                    + shadow.getOid() + ": " + ex.getMessage(), ex);
        } catch (Throwable t) {
            recordModificationExecuted(t);
            throw t;
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
