/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.repo.common.task.ErrorHandlingStrategyExecutor.Action.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

/**
 * Executes iterative task error handling strategy.
 *
 * The strategy consists of a set of _entries_, which are evaluated against specific error that has occurred.
 * Each entry contains a description of _situation(s)_ and appropriate _reaction_.
 *
 * The main {@link #determineAction(OperationResultStatus, Throwable, String, OperationResult)} method must be thread safe.
 *
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

    @NotNull private final Action defaultAction;

    public enum Action {
        CONTINUE, STOP, SUSPEND
    }

    ErrorHandlingStrategyExecutor(@NotNull Task task, @NotNull PrismContext prismContext,
            @NotNull RepositoryService repositoryService, @NotNull Action defaultAction) {
        this.prismContext = prismContext;
        this.strategyEntryInformationList = getErrorHandlingStrategyEntryList(task).stream()
                .map(StrategyEntryInformation::new)
                .collect(Collectors.toList());
        this.repositoryService = repositoryService;
        this.defaultAction = defaultAction;
    }

    /**
     * Decides whether to stop or continue processing.
     * @param status Failure status of the operation (fatalError, partialError).
     * @param exception Exception that occurred.
     * @param opResult Here we should report our operations.
     * @return true of we should stop
     *
     * This method must be thread safe!
     */
    @NotNull Action determineAction(@NotNull OperationResultStatus status, @NotNull Throwable exception,
            @Nullable String triggerHolderOid, @NotNull OperationResult opResult) {
        for (StrategyEntryInformation entryInformation : strategyEntryInformationList) {
            if (matches(entryInformation.entry, status, exception)) {
                return executeReaction(entryInformation, triggerHolderOid, opResult);
            }
        }
        return defaultAction;
    }

    private boolean matches(@NotNull TaskErrorHandlingStrategyEntryType entry, @NotNull OperationResultStatus status,
            @SuppressWarnings("unused") Throwable exception) {
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
    private @NotNull Action executeReaction(@NotNull ErrorHandlingStrategyExecutor.StrategyEntryInformation entry,
            @Nullable String triggerHolderOid, @NotNull OperationResult opResult) {

        if (entry.registerMatchAndCheckThreshold()) {
            return SUSPEND;
        }

        TaskErrorReactionType reaction = entry.entry != null ? entry.entry.getReaction() : null;
        if (reaction == null) {
            return STOP;
        }

        if (reaction.getIgnore() != null) {
            return CONTINUE;
        } else if (reaction.getStop() != null) {
            return STOP;
        } else if (reaction.getRetryLater() != null) {
            return processRetryLater(reaction, triggerHolderOid, opResult);
        } else {
            return getDefaultAction(reaction);
        }
    }

    private Action getDefaultAction(TaskErrorReactionType reaction) {
        if (reaction.getStopAfter() != null) {
            return CONTINUE;
        } else {
            return STOP;
        }
    }

    private Action processRetryLater(TaskErrorReactionType reaction, @Nullable String shadowOid,
            @NotNull OperationResult opResult) {
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

    private void createShadowSynchronizationTrigger(RetryLaterReactionType retryReaction, String shadowOid,
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

    private XMLGregorianCalendar getFirstRetryTimestamp(RetryLaterReactionType retryReaction) {
        return XmlTypeConverter.fromNow(defaultIfNull(retryReaction.getInitialInterval(), DEFAULT_INITIAL_RETRY_INTERVAL));
    }

    private List<TaskErrorHandlingStrategyEntryType> getErrorHandlingStrategyEntryList(Task task) {

        // The current way
        TaskErrorHandlingStrategyType strategy = task.getErrorHandlingStrategy();
        if (strategy != null) {
            TaskErrorHandlingStrategyType clone = strategy.clone();
            clone.asPrismContainerValue().freeze();
            return clone.getEntry();
        }

        // The 4.2.x way
        TaskErrorHandlingStrategyType strategyFromExtension =
                task.getExtensionContainerRealValueOrClone(SchemaConstants.MODEL_EXTENSION_LIVE_SYNC_ERROR_HANDLING_STRATEGY);
        if (strategyFromExtension != null) {
            TaskErrorHandlingStrategyType clone = strategyFromExtension.clone();
            clone.asPrismContainerValue().freeze();
            return clone.getEntry();
        }

        return emptyList();
    }

    private static class StrategyEntryInformation {
        private final TaskErrorHandlingStrategyEntryType entry; // frozen (due to thread safety)
        private final AtomicInteger matches = new AtomicInteger();

        private StrategyEntryInformation(TaskErrorHandlingStrategyEntryType entry) {
            this.entry = entry.clone();
            this.entry.asPrismContainerValue().freeze();
        }

        private boolean registerMatchAndCheckThreshold() {
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
