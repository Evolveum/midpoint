/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state.sync;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.statistics.ActivitySynchronizationStatisticsUtil;
import com.evolveum.midpoint.schema.statistics.SynchronizationStatisticsCollector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Tracks synchronization situation changes during a single item processing.
 *
 * Assumptions:
 *
 * 1. Exists during a single item processing only.
 * 2. Executed from a single thread only (the worker task).
 */
public class SynchronizationStatisticsCollectorImpl implements SynchronizationStatisticsCollector {

    private static final Trace LOGGER = TraceManager.getTrace(ActivitySynchronizationStatisticsUtil.class);

    /**
     * Place where the summarized information is sent at the end.
     */
    @NotNull private final ActivitySynchronizationStatistics activitySynchronizationStatistics;

    /**
     * Item processing for which we collect the synchronization situation changes.
     * We assume we are in the _worker_ task i.e. that it is not possible to call us with any other non-null
     * value. However, nulls are OK for embedded clockwork runs like discovery, etc.
     */
    @NotNull private final String processingIdentifier;

    /**
     * OID of the shadow that we use to filter out situation update messages.
     * It is quite questionable whether to use this for statistics keeping.
     */
    private String shadowOid;

    /**
     * Situation of the shadow at the beginning of item processing.
     */
    private final SynchronizationSituationType onProcessingStart;

    /**
     * Situation at the beginning of the synchronization operation - i.e. after initial situation determination.
     */
    private SynchronizationSituationType onSynchronizationStart;

    /**
     * Reason for which the synchronization is skipped for this item.
     */
    private SynchronizationExclusionReasonType exclusionReason;

    /**
     * Situation at the end of the synchronization operation - i.e. either after the clockwork was executed, or just after
     * synchronization service finished.
     */
    private SynchronizationSituationType onSynchronizationEnd;

    public SynchronizationStatisticsCollectorImpl(@NotNull ActivitySynchronizationStatistics activitySynchronizationStatistics,
            @NotNull String processingIdentifier, SynchronizationSituationType situationOnStart) {
        this.activitySynchronizationStatistics = activitySynchronizationStatistics;
        this.processingIdentifier = processingIdentifier;
        this.onProcessingStart = situationOnStart;
    }

    public void onSynchronizationStart(@Nullable String processingIdentifier, @Nullable String shadowOid,
            @Nullable SynchronizationSituationType situation) {
        if (identifierMatches(processingIdentifier)) {
            this.onSynchronizationStart = situation;
            this.onSynchronizationEnd = situation; // This is an assumption. Valid until situation changes.
            this.shadowOid = shadowOid;
        }
    }

    public void onSynchronizationExclusion(@Nullable String processingIdentifier,
            @NotNull SynchronizationExclusionReasonType exclusionReason) {
        if (identifierMatches(processingIdentifier)) {
            this.exclusionReason = exclusionReason;
        }
    }

    public void onSynchronizationSituationChange(@Nullable String processingIdentifier,
            @Nullable String shadowOid, @Nullable SynchronizationSituationType situation) {
        if (identifierMatches(processingIdentifier) && shadowMatches(shadowOid)) {
            this.onSynchronizationEnd = situation;
        }
    }

    private boolean identifierMatches(String processingIdentifier) {
        if (processingIdentifier == null) {
            return false; // This is OK: null identifier means we do not want to record this event.
        } else {
            stateCheck(processingIdentifier.equals(this.processingIdentifier), "Trying to record sync event "
                    + "for processing item (%s) other than current (%s).", processingIdentifier, this.processingIdentifier);
            return true;
        }
    }

    private boolean shadowMatches(String shadowOid) {
        if (shadowOid != null && shadowOid.equals(this.shadowOid)) {
            return true;
        } else {
            LOGGER.debug("Shadow does not match in {}: got shadow OID: {}", this, shadowOid);
            return false;
        }
    }

    public void stop(@NotNull QualifiedItemProcessingOutcomeType outcome) {

        // Poor man's solution: We create artificial delta and reuse the summarization code.
        ActivitySynchronizationStatisticsType delta = new ActivitySynchronizationStatisticsType(PrismContext.get())
                .beginTransition()
                    .onProcessingStart(onProcessingStart)
                    .onSynchronizationStart(onSynchronizationStart)
                    .onSynchronizationEnd(onSynchronizationEnd)
                    .exclusionReason(exclusionReason)
                    .beginCounter()
                        .outcome(outcome.cloneWithoutId())
                        .count(1)
                    .<SynchronizationSituationTransitionType>end()
                .end();

        activitySynchronizationStatistics.add(delta);
    }

    @Override
    public String toString() {
        return "SynchronizationStatisticsCollectorImpl{" +
                "processingIdentifier='" + processingIdentifier + '\'' +
                ", shadowOid='" + shadowOid + '\'' +
                '}';
    }
}
