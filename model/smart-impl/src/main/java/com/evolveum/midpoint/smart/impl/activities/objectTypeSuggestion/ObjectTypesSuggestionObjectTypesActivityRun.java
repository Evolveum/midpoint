/*
 * Copyright (c) 2020-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl.activities.objectTypeSuggestion;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunResult;
import com.evolveum.midpoint.repo.common.activity.run.LocalActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowObjectClassUtil;
import com.evolveum.midpoint.smart.impl.SmartIntegrationBeans;
import com.evolveum.midpoint.smart.impl.activities.Util;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Calls the remote service to provide object types suggestions for a given object class.
 *
 * Later, we can create a variant of this activity that computes statistics for objects already present in the repository.
 */
class ObjectTypesSuggestionObjectTypesActivityRun
        extends LocalActivityRun<
            ObjectTypesSuggestionWorkDefinition,
            ObjectTypesSuggestionActivityHandler,
            ObjectTypesSuggestionWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypesSuggestionObjectTypesActivityRun.class);

    /** Top-N value counts to retain per attribute when computing statistics for object type suggestions. */
    private static final int VALUECOUNT_TOP_N = 10;

    /** Minimum repeat count for a value to be included in Top-N value counts. */
    private static final int VALUECOUNT_MIN_REPEAT = 2;

    /** Top-N DN suffix patterns to retain per attribute. */
    private static final int DN_SUFFIX_TOP_N = 20;

    /** Top-N first/last token patterns to retain per attribute. */
    private static final int FIRST_LAST_TOP_N = 10;

    ObjectTypesSuggestionObjectTypesActivityRun(
            ActivityRunInstantiationContext<ObjectTypesSuggestionWorkDefinition, ObjectTypesSuggestionActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws ActivityRunException, CommonException {
        var task = getRunningTask();
        var parentState = Util.getParentState(this, result);
        var resourceOid = getWorkDefinition().getResourceOid();
        var objectClassName = getWorkDefinition().getObjectClassName();

        boolean useAi = getWorkDefinition().getPermissions().contains(DataAccessPermissionType.STATISTICS_ACCESS);
        ObjectTypesSuggestionType suggestedTypes;

        if (useAi) {
            var statisticsOid = MiscUtil.stateNonNull(
                    Referencable.getOid(parentState.getWorkStateReferenceRealValue(
                            ObjectTypesSuggestionWorkStateType.F_STATISTICS_REF)),
                    "Statistics object reference is not set in the work state in %s", task);
            LOGGER.debug("Going to suggest object types for resource {} and object class {}; statistics in: {}",
                    resourceOid, objectClassName, statisticsOid);

            var statistics = trimStatisticsForSuggestions(
                    ShadowObjectClassUtil.getStatisticsRequired(
                            getBeans().repositoryService.getObject(GenericObjectType.class, statisticsOid, null, result)));
            suggestedTypes = SmartIntegrationBeans.get().smartIntegrationService.suggestObjectTypes(
                    resourceOid, objectClassName, statistics, task, result);
            LOGGER.debug("AI-based suggestions to be written to the work state:\n{}",
                    suggestedTypes.debugDump(1));
        } else {
            LOGGER.debug("Cannot suggest object types for resource {} and object class {}: {} permission not granted",
                    resourceOid, objectClassName, DataAccessPermissionType.STATISTICS_ACCESS);
            suggestedTypes = new ObjectTypesSuggestionType();
        }

        parentState.setWorkStateItemRealValues(ObjectTypesSuggestionWorkStateType.F_RESULT, suggestedTypes);
        parentState.flushPendingTaskModifications(result);

        return ActivityRunResult.success();
    }

    /**
     * Trims the loaded statistics in-place, applying top-N limits and min-repeat filtering
     * before passing statistics to the AI suggestion service.
     */
    private static ShadowObjectClassStatisticsType trimStatisticsForSuggestions(ShadowObjectClassStatisticsType stats) {
        for (ShadowAttributeStatisticsType attr : stats.getAttribute()) {
            trimValueCounts(attr);
            trimPatternCounts(attr);
        }
        stats.getAttribute().removeIf(attr ->
                Objects.equals(attr.getMissingValueCount(), stats.getSize())
                || (attr.getValueCount().isEmpty() && attr.getValuePatternCount().isEmpty()));
        return stats;
    }

    private static void trimValueCounts(ShadowAttributeStatisticsType attr) {
        List<ShadowAttributeValueCountType> snapshot = new ArrayList<>(attr.getValueCount());
        int maxCount = snapshot.stream().mapToInt(ShadowAttributeValueCountType::getCount).max().orElse(0);
        attr.getValueCount().clear();
        if (maxCount < VALUECOUNT_MIN_REPEAT) {
            return;
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        snapshot.forEach(vc -> map.put(vc.getValue(), vc.getCount()));
        topN(map, VALUECOUNT_TOP_N).forEach(e ->
                attr.beginValueCount().value(e.getKey()).count(e.getValue()));
    }

    private static void trimPatternCounts(ShadowAttributeStatisticsType attr) {
        List<ShadowAttributeValuePatternCountType> snapshot = new ArrayList<>(attr.getValuePatternCount());
        attr.getValuePatternCount().clear();
        trimPatternsByType(attr, snapshot, ShadowValuePatternType.DN_SUFFIX, DN_SUFFIX_TOP_N);
        trimPatternsByType(attr, snapshot, ShadowValuePatternType.FIRST_TOKEN, FIRST_LAST_TOP_N);
        trimPatternsByType(attr, snapshot, ShadowValuePatternType.LAST_TOKEN, FIRST_LAST_TOP_N);
    }

    private static void trimPatternsByType(
            ShadowAttributeStatisticsType attr,
            List<ShadowAttributeValuePatternCountType> snapshot,
            ShadowValuePatternType type,
            int limit) {
        Map<String, Integer> map = new LinkedHashMap<>();
        snapshot.stream()
                .filter(p -> p.getType() == type)
                .forEach(p -> map.put(p.getValue(), p.getCount()));
        topN(map, limit).forEach(e ->
                attr.beginValuePatternCount().value(e.getKey()).type(type).count(e.getValue()));
    }

    /**
     * Returns top entries from the given map, sorted by count descending, capped at {@code limit}.
     */
    private static List<Map.Entry<String, Integer>> topN(Map<String, Integer> map, int limit) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
