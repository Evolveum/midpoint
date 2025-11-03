/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.activity.run.state.OtherActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.schema.util.task.WallClockTimeComputer;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Computes {@link PreexistingValues} for an activity run.
 */
class PreexistingValuesComputer {

    private static final Trace LOGGER = TraceManager.getTrace(PreexistingValuesComputer.class);

    /** The activity run for which we compute the preexisting values. */
    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    /** My own path. We need to exclude ourselves when computing pre-existing values. */
    private final @NotNull ActivityPath myPath;

    /** All relevant policy rules. */
    private final @NotNull List<ActivityPolicyRule> rules;

    private Multimap<ActivityPath, OtherActivityState> allActivityStatesMap;

    private final Map<ActivityPath, Long> executionTimeMap = new HashMap<>();

    private final Map<ActivityPath, Integer> executionAttemptNumberMap = new HashMap<>();

    /**
     * Counters summarized from all relevant activities. Key should be {@link ActivityPolicyRuleIdentifier} but it's not that
     * easy, because in tasks they are stored as strings - and there's no reliable way to parse strings back to
     * {@link ActivityPath}. Hence, let's stick with strings here.
     */
    private final Map<String, Integer> ruleCounters = new HashMap<>();

    PreexistingValuesComputer(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun, @NotNull List<ActivityPolicyRule> rules) {
        this.activityRun = activityRun;
        this.myPath = activityRun.getActivityPath();
        this.rules = rules;
    }

    /**
     * Computes the preexisting values.
     *
     * For start, let's use very simple approach: read the whole task tree and obtain all needed values from it.
     * Later, we may optimize things a bit.
     */
    public void compute(@NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        allActivityStatesMap =
                CommonTaskBeans.get().activityManager.getAllActivityStates(activityRun.getRunningTask().getRootTaskOid(), result);
        determineExecutionTimeMap();
        determineExecutionAttemptNumberMap();
        summarizeCounters();
    }

    private void determineExecutionTimeMap() {
        for (ActivityPolicyRule rule : rules) {
            if (rule.doesNeedExecutionTime()) {
                var rulePath = rule.getPath();
                if (!executionTimeMap.containsKey(rulePath)) {
                    var allRuns = allActivityStatesMap.entries().stream()
                            .filter(e -> e.getKey().startsWith(rulePath) && !e.getKey().equals(myPath))
                            .map(e -> e.getValue().getRawRunRecordsClone())
                            .flatMap(Collection::stream)
                            .toList();
                    long time = WallClockTimeComputer.create(allRuns).getSummaryTime();
                    LOGGER.trace("Computed execution time for '{}' to be {} millis, using runs:\n{}",
                            rulePath, time, DebugUtil.debugDumpLazily(allRuns, 1));
                    executionTimeMap.put(rulePath, time);
                }
            }
        }
    }

    private void determineExecutionAttemptNumberMap() {
        for (ActivityPolicyRule rule : rules) {
            if (rule.doesNeedExecutionAttemptNumber()) {
                var rulePath = rule.getPath();
                var executionAttempt = allActivityStatesMap.get(rulePath).stream()
                        .filter(state -> !state.isDelegating()) // maybe not necessary
                        .mapToInt(state -> state.getExecutionAttempt())
                        .max(); // this should take care of coordinator/workers and maybe delegation case
                LOGGER.trace("Determined execution attempt # for '{}' to be {}", rulePath, executionAttempt);
                if (executionAttempt.isPresent()) { // this should always be true
                    executionAttemptNumberMap.put(rulePath, executionAttempt.getAsInt());
                }
            }
        }
    }

    /** For simplicity, let's just summarize all counters from all activities. */
    private void summarizeCounters() {
        allActivityStatesMap.entries().stream()
                .filter(e -> !e.getKey().equals(myPath))
                .forEach(e -> {
                    var state = e.getValue();
                    var localCounters = state.getCounters(activityRun.getCountersGroup());
                    for (var entry : localCounters.entrySet()) {
                        ruleCounters.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                });
    }

    @NotNull Map<ActivityPath, Long> getExecutionTimeMap() {
        return executionTimeMap;
    }

    @NotNull Map<ActivityPath, Integer> getExecutionAttemptNumberMap() {
        return executionAttemptNumberMap;
    }

    @NotNull Map<String, Integer> getRuleCounters() {
        return ruleCounters;
    }
}
