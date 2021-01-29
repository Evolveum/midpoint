/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor.Action.*;

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Executes live sync error handling strategy.
 *
 * The strategy consists of a set of ENTRIES, which are evaluated against specific error that has occurred.
 * Each entry contains a description of SITUATION(s) and appropriate REACTION.
 *
 * The main {@link #determineAction(Throwable, OperationResultStatus, String, OperationResult)} method must be thread safe.
 *
 * TODO Generalize to arbitrary tasks
 */
public class ErrorHandlingStrategyExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ErrorHandlingStrategyExecutor.class);

    // TODO deduplicate with the trigger handler class
    private static final String SHADOW_RECONCILE_TRIGGER_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/model/trigger/shadow-reconcile/handler-3";

    private static final Duration DEFAULT_INITIAL_RETRY_INTERVAL = XmlTypeConverter.createDuration("PT30M");

    @NotNull private final PrismContext prismContext;

    /**
     * Individual entries with some auxiliary information, namely how many times they were matched in the current run.
     */
    @NotNull private final List<StrategyEntryInformation> strategyEntryInformationList;
    @NotNull private final RepositoryService repositoryService;

    public enum Action {
        CONTINUE, STOP, SUSPEND
    }

    public ErrorHandlingStrategyExecutor(@NotNull Task task, @NotNull PrismContext prismContext,
            @NotNull RepositoryService repositoryService) {
        this.prismContext = prismContext;
        this.strategyEntryInformationList = getErrorHandlingStrategyEntryList(task).stream()
                .map(StrategyEntryInformation::new)
                .collect(Collectors.toList());
        this.repositoryService = repositoryService;
    }

    /**
     * Decides whether to stop or continue processing.
     * @param t Exception that occurred.
     * @param status Failure status of the operation (fatalError, partialError).
     * @param opResult Here we should report our operations.
     * @return true of we should stop
     *
     * This method must be thread safe!
     */
    public @NotNull Action determineAction(@Nullable Throwable t, @NotNull OperationResultStatus status, @Nullable String shadowOid,
            @NotNull OperationResult opResult) {
        for (StrategyEntryInformation entryInformation : strategyEntryInformationList) {
            if (matches(entryInformation.entry, t, status)) {
                return executeReaction(entryInformation, shadowOid, opResult);
            }
        }
        return STOP; // this is the default if no entry matches
    }

    private boolean matches(@NotNull LiveSyncErrorHandlingStrategyEntryType entry, @SuppressWarnings("unused") Throwable t,
            @NotNull OperationResultStatus status) {
        if (entry.getSituation() == null || entry.getSituation().getStatus().isEmpty()) {
            return true;
        } else {
            OperationResultStatusType statusBean = status.createStatusType();
            return entry.getSituation().getStatus().contains(statusBean);
        }
    }

    /**
     * @return true if the processing should stop
     */
    private @NotNull Action executeReaction(@NotNull ErrorHandlingStrategyExecutor.StrategyEntryInformation entry, @Nullable String shadowOid,
            @NotNull OperationResult opResult) {

        if (entry.registerMatchAndCheckThreshold()) {
            return SUSPEND;
        }

        LiveSyncErrorReactionType reaction = entry.entry != null ? entry.entry.getReaction() : null;
        if (reaction == null) {
            return STOP;
        }

        if (reaction.getIgnore() != null) {
            return CONTINUE;
        } else if (reaction.getStop() != null) {
            return STOP;
        } else if (reaction.getRetryLater() != null) {
            return processRetryLater(reaction, shadowOid, opResult);
        } else {
            return getDefaultAction(reaction);
        }
    }

    private Action getDefaultAction(LiveSyncErrorReactionType reaction) {
        if (reaction.getStopAfter() != null) {
            return CONTINUE;
        } else {
            return STOP;
        }
    }

    private Action processRetryLater(LiveSyncErrorReactionType reaction, @Nullable String shadowOid, @NotNull OperationResult opResult) {
        if (shadowOid == null) {
            LOGGER.warn("'retryLater' reaction was configured but there is no shadow to attach trigger to. Having to stop "
                    + "in order to avoid data loss.");
            return STOP;
        }
        try {
            createShadowSynchronizationTrigger(reaction.getRetryLater(), shadowOid, opResult);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create shadow synchronization trigger for shadow {} "
                    + "-- live synchronization must stop", t, shadowOid);
            return STOP;
        }
        return CONTINUE;
    }

    private void createShadowSynchronizationTrigger(LiveSyncRetryLaterReactionType retryReaction, String shadowOid,
            OperationResult opResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        TriggerType trigger = new TriggerType(prismContext)
                .handlerUri(SHADOW_RECONCILE_TRIGGER_HANDLER_URI)
                .timestamp(getFirstRetryTimestamp(retryReaction));
        PlannedOperationAttemptType firstAttempt = new PlannedOperationAttemptType(prismContext)
                .number(1)
                .interval(retryReaction.getNextInterval())
                .limit(retryReaction.getRetryLimit());
        ObjectTypeUtil.setExtensionContainerRealValues(prismContext, trigger.asPrismContainerValue(),
                SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT, firstAttempt);
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_TRIGGER)
                .add(trigger)
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadowOid, modifications, opResult);
    }

    private XMLGregorianCalendar getFirstRetryTimestamp(LiveSyncRetryLaterReactionType retryReaction) {
        return XmlTypeConverter.fromNow(defaultIfNull(retryReaction.getInitialInterval(), DEFAULT_INITIAL_RETRY_INTERVAL));
    }

    private List<LiveSyncErrorHandlingStrategyEntryType> getErrorHandlingStrategyEntryList(Task task) {
        LiveSyncErrorHandlingStrategyType strategy =
                task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_LIVE_SYNC_ERROR_HANDLING_STRATEGY);
        if (strategy != null) {
            LiveSyncErrorHandlingStrategyType clone = strategy.clone();
            clone.asPrismContainerValue().freeze();
            return clone.getEntry();
        }

        // legacy way
        LiveSyncErrorReactionType reaction = new LiveSyncErrorReactionType(prismContext);
        boolean retryErrors =
                isNotFalse(task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_RETRY_LIVE_SYNC_ERRORS));
        if (retryErrors) {
            reaction.setStop(new LiveSyncStopProcessingReactionType(prismContext));
        } else {
            reaction.setIgnore(new LiveSyncIgnoreErrorReactionType(prismContext));
        }
        LiveSyncErrorHandlingStrategyEntryType entry = new LiveSyncErrorHandlingStrategyEntryType(prismContext)
                .reaction(reaction);
        entry.asPrismContainerValue().freeze();
        return Collections.singletonList(entry);
    }

    private static class StrategyEntryInformation {
        private final LiveSyncErrorHandlingStrategyEntryType entry; // frozen (due to thread safety)
        private final AtomicInteger matches = new AtomicInteger();

        private StrategyEntryInformation(LiveSyncErrorHandlingStrategyEntryType entry) {
            this.entry = entry.clone();
            this.entry.asPrismContainerValue().freeze();
        }

        public boolean registerMatchAndCheckThreshold() {
            int occurred = matches.incrementAndGet();
            Integer limit = getStopAfter();
            if (limit == null || occurred < limit) {
                LOGGER.debug("Found match #{} of this error strategy entry. Limit is {}, not suspending the task",
                        occurred, limit);
                return false;
            } else {
                LOGGER.info("This error strategy entry matched {} times; limit is {}. Suspending the task.",
                        occurred, limit);
                return true;
            }
        }

        @Nullable
        private Integer getStopAfter() {
            return entry != null && entry.getReaction() != null ?
                    entry.getReaction().getStopAfter() : null;
        }
    }
}
