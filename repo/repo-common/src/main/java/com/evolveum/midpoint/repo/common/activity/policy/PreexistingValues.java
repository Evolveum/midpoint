/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import java.util.List;
import java.util.Map;

/**
 * Contains values for policy constraints that existed before the activity run started.
 *
 * Evaluated at the beginning of the activity run. We can do it that we, as we assume that nothing in the tree changes
 * during the run of the current activity.
 */
public class PreexistingValues implements DebugDumpable {

    private final @NotNull Map<ActivityPath, Long> executionTimeMap;

    private final @NotNull Map<ActivityPath, Integer> executionAttemptNumberMap;

    /** Key is {@link ActivityPolicyRuleIdentifier#toString()}. Explained in {@link PreexistingValuesComputer#ruleCounters}. */
    private final @NotNull Map<String, Integer> ruleCounters;

    private PreexistingValues(
            @NotNull Map<ActivityPath, Long> executionTimeMap,
            @NotNull Map<ActivityPath, Integer> executionAttemptNumberMap,
            @NotNull Map<String, Integer> ruleCounters) {
        this.executionTimeMap = executionTimeMap;
        this.executionAttemptNumberMap = executionAttemptNumberMap;
        this.ruleCounters = ruleCounters;
    }

    /**
     * Determines preexisting values for the given activity run and evaluated rules. We climb up and down the activity run tree
     * to find out the values distributed throughout it.
     */
    public static PreexistingValues determine(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull List<ActivityPolicyRule> rules,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        var computer = new PreexistingValuesComputer(activityRun, rules);
        computer.compute(result);
        return new PreexistingValues(
                computer.getExecutionTimeMap(),
                computer.getExecutionAttemptNumberMap(),
                computer.getRuleCounters());
    }

    @Nullable Duration getExecutionTime(@NotNull ActivityPath path) {
        Long millis = executionTimeMap.get(path);
        return millis != null ? XmlTypeConverter.createDuration(millis) : null;
    }

    @Nullable Integer getExecutionAttemptNumber(@NotNull ActivityPath path) {
        return executionAttemptNumberMap.get(path);
    }

    Map<String, Integer> getPreexistingCounters() {
        return ruleCounters;
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "executionTimeMap", executionTimeMap, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "executionAttemptNumberMap", executionAttemptNumberMap, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "ruleCounters", ruleCounters, indent + 1);
        return sb.toString();
    }
}
