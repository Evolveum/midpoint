/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collects synchronization statistics related e.g. to the processing within given task.
 */
public interface SynchronizationInformationCollector {

    /**
     * Records synchronization operation in "legacy way" - i.e. in the set of "before" and "after" counters.
     * This is in contrast with the "new way", where we record transitions between states instead.
     */
    void recordSynchronizationOperationLegacy(SynchronizationInformation.LegacyCounters originalStateIncrement,
            SynchronizationInformation.LegacyCounters newStateIncrement);

    /**
     * Support method for recording sync operation in the new way.
     *
     * 1. Informs the collector that synchronization-sensitive item is going to be processed.
     * 2. Establishes a filter that rejects any events having processing identifier different from this one.
     */
    void onSyncItemProcessingStart(@NotNull String processingIdentifier, @Nullable SynchronizationSituationType situationBefore);

    /**
     * Called when a situation was determined right before a synchronization takes place.
     * We assume that we have a shadow with OID by that time.
     * (If the OID is null we ignore further synchronization situation updates.)
     */
    void onSynchronizationStart(@Nullable String processingIdentifier, @Nullable String shadowOid,
            @Nullable SynchronizationSituationType situation);

    /**
     * Informs the task that no synchronization will take place.
     * Note that in theory it is possible that {@link #onSynchronizationStart(String, String, SynchronizationSituationType)} is called first.
     */
    void onSynchronizationExclusion(@Nullable String processingIdentifier,
            @NotNull SynchronizationExclusionReasonType exclusionReason);

    /**
     * Informs the task that sync situation has changed for given shadow OID.
     * There could be more such changes. But we are interested in the last one.
     * If the shadow OID is null, we ignore such updates.
     */
    void onSynchronizationSituationChange(@Nullable String processingIdentifier, @Nullable String shadowOid,
            @Nullable SynchronizationSituationType situation);

    /**
     * Records the synchronization-related information into the statistics. Stops the watching.
     */
    void onSyncItemProcessingEnd(@NotNull String processingIdentifier, @NotNull SynchronizationInformation.Status status);

}
