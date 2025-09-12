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
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.smart.impl.CorrelationSuggestionOperation.CorrelatorSuggestion;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Evaluates the suitability of correlator(s) for correlation between focus objects and resource shadows.
 * <p>
 * This class samples a set of focus (e.g., user) and resource shadow objects, computes statistics
 * on given attribute paths, and evaluates "correlator suggestions" according to their appropriateness for
 * unique, high-coverage mapping between focus and resource objects.
 */
class CorrelatorEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationServiceImpl.class);

    private static final int MAX_FOCUS_SAMPLE_SIZE = 2000;
    private static final boolean NO_FETCH_SHADOWS = true;

    private final TypeOperationContext ctx;
    private final List<CorrelatorSuggestion> suggestions;
    private final SmartIntegrationBeans b = SmartIntegrationBeans.get();
    private final Statistics focusStatistics;
    private final Statistics resourceStatistics;

    private final List<PrismObject<?>> sampledFocuses = new ArrayList<>();
    private final List<PrismObject<?>> sampledShadows = new ArrayList<>();

    /**
     * Constructs a new CorrelatorEvaluator with the given operation context and correlator suggestions.
     *
     * @param ctx         The context of the correlation operation.
     * @param suggestions The list of correlator suggestions to evaluate.
     */
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
                        .filter(Objects::nonNull)
                        .collect(PathSet::new, PathSet::add, PathSet::addAll));
    }

    /**
     * Evaluates all provided correlator suggestions by sampling focus and shadow objects, analyzing
     * attribute distribution and mapping, and computing suitability scores.
     * <p>
     * The main steps are:
     * <ul>
     *     <li>Sampling up to {@value #MAX_FOCUS_SAMPLE_SIZE} focus objects and all relevant shadow objects</li>
     *     <li>Computing statistics (uniqueness, coverage) for each suggested focus and resource path</li>
     *     <li>Scoring each suggestion by combining statistics and focus-shadow link coverage/ambiguity</li>
     * </ul>
     *
     * @param result OperationResult for operation logging/auditing.
     * @return List of scores (one per suggestion), in the same order as the input suggestions.
     */
    List<Double> evaluateSuggestions(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        LOGGER.info("Starting correlator evaluation. Focus type: {}, Shadow type: {}, Max focus sample: {}",
                ctx.getFocusClass(), ctx.getTypeIdentification(), MAX_FOCUS_SAMPLE_SIZE);

        AtomicInteger focusCounter = new AtomicInteger();
        b.modelService.searchObjectsIterative(
                ctx.getFocusClass(), null,
                (focus, lResult) -> {
                    if (focusCounter.incrementAndGet() > MAX_FOCUS_SAMPLE_SIZE) return false;
                    sampledFocuses.add(focus);
                    focusStatistics.process(focus);
                    return true;
                },
                null, ctx.task, result);

        b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.getTypeIdentification())
                        .build(),
                (shadow, lResult) -> {
                    sampledShadows.add(shadow);
                    resourceStatistics.process(shadow);
                    return true;
                },
                NO_FETCH_SHADOWS ? GetOperationOptions.noFetch() : null,
                ctx.task,
                result);

        List<Double> results = new ArrayList<>();
        for (CorrelatorSuggestion suggestion : suggestions) {
            double eval = computeScore(suggestion);
            results.add(eval);
            LOGGER.info("Suggestion: {} | Score: {}", suggestion, eval);
        }
        return results;
    }

    /**
     * Builds a mapping from focus object OIDs to sets of resource shadow OIDs that share
     * the same value on the suggested focus/resource attribute paths.
     *
     * @param suggestion The correlator suggestion specifying the focus and shadow attribute paths.
     * @return A map from focus OID to a set of matching shadow OIDs for the given correlator suggestion.
     */
    private Map<String, Set<String>> collectFocusShadowLinks(CorrelatorSuggestion suggestion) {
        Map<String, Set<String>> focusToShadows = new HashMap<>();
        ItemPath focusPath = suggestion.focusItemPath();
        ItemPath shadowPath = suggestion.resourceAttrPath();

        Map<Object, Set<String>> shadowValueToOids = new HashMap<>();
        for (PrismObject<?> shadow : sampledShadows) {
            var item = shadow.findItem(shadowPath);
            if (item != null) {
                Object val = item.getValue().getRealValue();
                if (val != null) {
                    shadowValueToOids
                            .computeIfAbsent(String.valueOf(val), k -> new HashSet<>())
                            .add(shadow.getOid());
                }
            }
        }

        for (PrismObject<?> focus : sampledFocuses) {
            var item = focus.findItem(focusPath);
            if (item != null) {
                Object val = item.getValue().getRealValue();
                if (val != null) {
                    Set<String> matchingShadows = shadowValueToOids.getOrDefault(String.valueOf(val), Collections.emptySet());
                    focusToShadows.put(focus.getOid(), new HashSet<>(matchingShadows));
                } else {
                    focusToShadows.put(focus.getOid(), Collections.emptySet());
                }
            } else {
                focusToShadows.put(focus.getOid(), Collections.emptySet());
            }
        }
        return focusToShadows;
    }

    /**
     * Determines if the property at the given item path is multivalued, in either the focus or shadow definition.
     *
     * @param path Item path to check.
     * @return True if the property is multivalued, or cannot be determined; false otherwise.
     */
    private boolean isMultiValued(ItemPath path) {
        try {
            var focusDef = ctx.getFocusTypeDefinition().findItemDefinition(path);
            if (focusDef != null && focusDef.isMultiValue()) {
                LOGGER.info("Focus path {} is multi-valued", path);
                return true;
            }
            var shadowDef = ctx.getShadowDefinition().findItemDefinition(path);
            if (shadowDef != null && shadowDef.isMultiValue()) {
                LOGGER.info("Shadow path {} is multi-valued", path);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("Unable to determine if path {} is multi-valued: {}", path, e.getMessage());
            return true;
        }
    }

    /**
     * Computes the evaluation score for a single correlator suggestion, based on:
     * <ul>
     *     <li>Whether either involved path is multivalued (score 0 if so)</li>
     *     <li>Uniqueness and coverage statistics for focus and shadow attributes</li>
     *     <li>Linkage coverage and ambiguity between sampled focuses and shadows</li>
     * </ul>
     *
     * @param suggestion The correlator suggestion to evaluate.
     * @return A double score value (0 = unusable, 1 = ideal unique/complete mapping).
     */
    private Double computeScore(CorrelatorSuggestion suggestion) {
        ItemPath focusPath = suggestion.focusItemPath();
        ItemPath resourcePath = suggestion.resourceAttrPath();

        boolean focusMulti = isMultiValued(focusPath) && focusStatistics.isPathMultiValued(focusPath);
        boolean resourceMulti = isMultiValued(resourcePath) && resourceStatistics.isPathMultiValued(resourcePath);
        if (focusMulti || resourceMulti) {
            LOGGER.info("Excluded correlator {} - {}: multi-valued path(s) found. Focus: {}, Resource: {}",
                    suggestion.focusItemPath(), suggestion.resourceAttrPath(), focusMulti, resourceMulti);
            return 0.0;
        }

        Double focusScore = focusStatistics.getScore(focusPath);
        Double resourceScore = resourceStatistics.getScore(resourcePath);
        if (focusScore == 0 || resourceScore == 0) {
            LOGGER.info("Excluded correlator {} - {}: either focus score or resource score is 0. Focus score: {}, Resource score: {}",
                    suggestion.focusItemPath(), suggestion.resourceAttrPath(), focusScore, resourceScore);
            return 0.0;
        }

        double baseScore = 2.0 * focusScore * resourceScore / (focusScore + resourceScore);

        Map<String, Set<String>> focusToShadowLinks = collectFocusShadowLinks(suggestion);
        double linkCoverage = computeLinkCoverage(focusToShadowLinks);

        double finalScore = baseScore * linkCoverage;
        LOGGER.debug("Base score: {} - Sampled focus-shadow link coverage: {} - Final score: {}",
                baseScore, linkCoverage, finalScore);

        return finalScore;
    }

    /**
     * Computes how well sampled focus objects are covered by links to shadow objects,
     * and penalizes ambiguous mappings (one focus mapping to multiple shadows).
     *
     * @param focusShadowLinks Map from focus OID to set of linked shadow OIDs.
     * @return Coverage/ambiguity score (0 = no mapping, 1 = perfect one-to-one coverage).
     */
    private static double computeLinkCoverage(Map<String, Set<String>> focusShadowLinks) {
        int focusCount = focusShadowLinks.size();
        if (focusCount == 0) return 0.0;

        int linked = 0;
        int multiLinks = 0;

        for (Set<String> shadowOids : focusShadowLinks.values()) {
            if (!shadowOids.isEmpty()) {
                linked++;
                if (shadowOids.size() > 1) {
                    multiLinks += (shadowOids.size() - 1); // ambiguity count
                }
            }
        }

        // Coverage: How many focus objects are linked to at least one shadow
        double coverage = (double) linked / focusCount;

        // Multiplicities: Penalize if a focus links to multiple shadows (ambiguous mapping)
        double avgAmbiguity = linked > 0 ? (double) multiLinks / linked : 0.0;
        double penalty = 1.0 / (1.0 + avgAmbiguity); // 1 if always 1:1, <1 if ambiguous

        return coverage * penalty;
    }

    /**
     * Collects statistics on the specified attribute paths over all sampled objects.
     * Used to measure uniqueness, value coverage, and detect multivalued attributes.
     */
    private static class Statistics {

        final PathKeyedMap<ItemStatistics> countedPathsMap;

        /**
         * Creates a new instance to gather statistics for the given set of attribute paths.
         *
         * @param countedPaths Set of attribute paths to collect statistics for.
         */
        private Statistics(PathSet countedPaths) {
            this.countedPathsMap = new PathKeyedMap<>();
            countedPaths.forEach(p -> countedPathsMap.put(p, new ItemStatistics()));
        }

        /**
         * Process the given object: for each tracked path, record whether a value is present or missing,
         * detect multivalued attributes, and track distinct values.
         *
         * @param object The object to analyze.
         */
        void process(PrismObject<?> object) {
            countedPathsMap.forEach(
                    (path, stats) -> {
                        stats.objects++;
                        var item = object.findItem(path);
                        if (item == null || item.isEmpty()) {
                            stats.missingValues++;
                        } else if (item.getValues().size() > 1) {
                            stats.skippedMultiValued = true;
                        } else {
                            Object val = item.getValue().getRealValue();
                            if (val != null) {
                                stats.distinctValues.add(val);
                            }
                        }
                    }
            );
        }

        /**
         * Returns the calculated score (F1-like) for the attribute at the given path.
         *
         * @param itemPath The path to retrieve statistics for.
         * @return The score (0 to 1).
         */
        Double getScore(ItemPath itemPath) {
            return MiscUtil.stateNonNull(
                            countedPathsMap.get(itemPath),
                            "No statistics for %s", itemPath)
                    .getScore();
        }

        /**
         * Returns whether the attribute at the given path was found to be multivalued in at least one object.
         *
         * @param itemPath The path to check.
         * @return True if at least one object had a multivalued value at this path.
         */
        Boolean isPathMultiValued(ItemPath itemPath) {
            return MiscUtil.stateNonNull(
                            countedPathsMap.get(itemPath),
                            "No statistics for %s", itemPath)
                    .skippedMultiValued;
        }
    }

    /**
     * Tracks statistics for a single attribute path, including:
     * <ul>
     *     <li>Number of objects processed</li>
     *     <li>Number of objects missing a value for this path</li>
     *     <li>Whether any object was multivalued at this path</li>
     *     <li>Set of distinct values found (for uniqueness)</li>
     * </ul>
     */
    private static class ItemStatistics {
        private int objects = 0;
        private int missingValues = 0;
        boolean skippedMultiValued = false;
        private final Set<Object> distinctValues = new HashSet<>();

        /**
         * Computes a score analogous to F1-score in classification:
         * <ul>
         *     <li>Uniqueness: Favor paths with more distinct values (close to one-to-one mapping)</li>
         *     <li>Coverage: Favor paths with fewer missing values</li>
         *     <li>Harmonic mean: Penalize paths that are low on either dimension</li>
         * </ul>
         * Returns 0 if there were any multivalued occurrences, or no objects processed.
         *
         * @return Score value between 0 and 1 (inclusive).
         */
        Double getScore() {
            if (skippedMultiValued || objects == 0) return 0.0;

            int objectsWithValue = objects - missingValues;
            if (objectsWithValue == 0) return 0.0;

            double uniqueness = ((double) distinctValues.size()) / objectsWithValue;
            double coverage = ((double) objectsWithValue) / objects;

            if (uniqueness == 0.0 || coverage == 0.0) return 0.0;

            return (2.0 * uniqueness * coverage) / (uniqueness + coverage);
        }
    }
}
