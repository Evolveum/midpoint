/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.impl;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.CorrelationSuggestionOperation.CorrelatorSuggestion;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Evaluates the suitability of correlator(s) for correlation.
 *
 * Currently very primitive implementation.
 *
 * The overall score is computed as
 *
 *          average(focusScore, resourceScore)
 *
 * (see {@link #computeScore(CorrelatorSuggestion)}),
 * where each of the scores is computed as
 *
 *          0.8 * uniqueness + 0.2 * coverage
 *
 * where uniqueness is the ratio of distinct values to number of objects with value,
 * and coverage is the ratio of number of objects with value to total number of objects
 * (see {@link ItemStatistics#getScore()}.
 *
 * Ideas for improvements:
 *
 * - reconsider the formula for evaluation
 * - exclude multivalued properties
 * - consider focus-shadow links
 * - consider matching rules (generally: consider using real correlators instead of simulating them by just looking at raw values)
 * - performance aspects: consider using only a sample of objects and/or use noFetch option for shadows
 * - some diagnostic logging here (to know how we got to the final score)
 *
 * Also, tests need to be written.
 */
class CorrelatorEvaluator {

    private final TypeOperationContext ctx;
    private final List<CorrelatorSuggestion> suggestions;
    private final SmartIntegrationBeans b = SmartIntegrationBeans.get();
    private final Statistics focusStatistics;
    private final Statistics resourceStatistics;

    CorrelatorEvaluator(TypeOperationContext ctx, List<CorrelatorSuggestion> suggestions) {
        this.ctx = ctx;
        this.suggestions = suggestions;
        this.focusStatistics = new Statistics(
                suggestions.stream()
                        .map(s -> s.focusItemPath())
                        .collect(PathSet::new, PathSet::add, PathSet::addAll));
        this.resourceStatistics = new Statistics(
                suggestions.stream()
                        .map(s -> s.resourceAttrPath())
                        .collect(PathSet::new, PathSet::add, PathSet::addAll));
    }

    /**
     * Evaluates individual suggestions.
     *
     * Only very primitive implementation for now.
     *
     * Returns a list of scores (0.0 .. 1.0), one for each suggestion - in the order of suggestions.
     */
    List<Double> evaluateSuggestions(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        // Counting values for focus objects
        b.modelService.searchObjectsIterative(
                ctx.getFocusClass(), null, (focus, lResult) -> processFocus(focus), null, ctx.task, result);

        // Counting values for shadows
        b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.getTypeIdentification())
                        .build(),
                (shadow, lResult) -> processShadow(shadow),
                null,
                ctx.task,
                result);

        // TODO think about evaluating focus-shadow links
        //  Beware of type compatibility issues, e.g. user.name (PolyString) vs account.name (String)

        return computeScores();
    }

    private List<Double> computeScores() {
        return suggestions.stream()
                .map(suggestion -> computeScore(suggestion))
                .toList();
    }

    private Double computeScore(CorrelatorSuggestion suggestion) {
        var focusScore = focusStatistics.getScore(suggestion.focusItemPath());
        var resourceScore = resourceStatistics.getScore(suggestion.resourceAttrPath());
        if (focusScore == null) {
            return resourceScore;
        } else if (resourceScore == null) {
            return focusScore;
        } else {
            return (focusScore + resourceScore) / 2;
        }
    }

    private boolean processFocus(PrismObject<?> focus) {
        focusStatistics.process(focus);
        return true;
    }

    private boolean processShadow(PrismObject<?> shadow) {
        resourceStatistics.process(shadow);
        return true;
    }

    /**
     * Statistics on the selected items of objects seen (focuses/shadows).
     */
    private static class Statistics {

        final PathKeyedMap<ItemStatistics> countedPathsMap;

        private Statistics(PathSet countedPaths) {
            this.countedPathsMap = new PathKeyedMap<>();
            countedPaths.forEach(p -> countedPathsMap.put(p, new ItemStatistics()));
        }

        void process(PrismObject<?> object) {
            countedPathsMap.forEach(
                    (path, stats) -> {
                        stats.objects++;
                        var item = object.findItem(path);
                        if (item == null || item.isEmpty()) {
                            stats.missingValues++;
                        } else {
                            // Ignoring other values; we should skip multivalued items entirely anyway
                            stats.distinctValues.add(
                                    item.getValue().getRealValue());
                        }
                    }
            );
        }

        Double getScore(ItemPath itemPath) {
            return MiscUtil.stateNonNull(
                            countedPathsMap.get(itemPath),
                            "No statistics for %s", itemPath)
                    .getScore();
        }
    }

    private static class ItemStatistics {
        private int objects = 0;
        private int missingValues = 0;
        private final Set<Object> distinctValues = new HashSet<>();

        Double getScore() {
            if (objects == 0) {
                return null; // no data, neutral
            }
            // TODO think about a smart formula here; now let's keep it simple: 80% is uniqueness, 20% is coverage
            int objectsWithValue = objects - missingValues;
            double uniqueness = objectsWithValue > 0 ? (double) distinctValues.size() / objectsWithValue : 0.0;
            double coverage = (double) objectsWithValue / objects;
            return 0.8 * uniqueness + 0.2 * coverage;
        }
    }
}
