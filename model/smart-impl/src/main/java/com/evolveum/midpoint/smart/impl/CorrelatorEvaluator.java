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
 *
 * This class samples a set of focus (e.g., user) and resource shadow objects, computes statistics
 * on given attribute paths, and evaluates "correlator suggestions" according to their appropriateness for
 * unique, high-coverage mapping between focus and resource objects.
 */
class CorrelatorEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(SmartIntegrationServiceImpl.class);

    private static final int MAX_SHADOW_SAMPLE_SIZE = 2000;
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
                        .filter(Objects::nonNull)
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
     *
     * The main steps are:
     *   - Sampling up to MAX_FOCUS_SAMPLE_SIZE focus objects and all relevant shadow objects
     *   - Computing statistics (uniqueness, coverage) for each suggested focus and resource path
     *   - Scoring each suggestion by combining statistics and focus-shadow link coverage/ambiguity
     *
     * @param result OperationResult for operation logging/auditing.
     * @return List of scores (one per suggestion), in the same order as the input suggestions.
     */
    List<Double> evaluateSuggestions(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        LOGGER.info("Starting correlator evaluation. Focus type: {}, Shadow type: {}, Max shadow sample: {}",
                ctx.getFocusClass(), ctx.getTypeIdentification(), MAX_SHADOW_SAMPLE_SIZE);

        b.modelService.searchObjectsIterative(
                ctx.getFocusClass(),
                null,
                (focus, lResult) -> {
                    sampledFocuses.add(focus);
                    focusStatistics.process(focus);
                    return true;
                },
                null, ctx.task, result);

        AtomicInteger shadowCounter = new AtomicInteger();
        b.modelService.searchObjectsIterative(
                ShadowType.class,
                Resource.of(ctx.resource)
                        .queryFor(ctx.getTypeIdentification())
                        .build(),
                (shadow, lResult) -> {
                    if (shadowCounter.incrementAndGet() > MAX_SHADOW_SAMPLE_SIZE) return false;
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
            LOGGER.debug("Suggestion: {} | Score: {}", suggestion, eval);
        }
        return results;
    }

    /**
     * Builds a mapping from shadow object OIDs to sets of focus OIDs that share
     * the same value on the suggested focus/resource attribute paths.
     *
     * @param suggestion The correlator suggestion specifying the focus and shadow attribute paths.
     * @return A map from focus OID to a set of matching shadow OIDs for the given correlator suggestion.
     */
    private Map<String, Set<String>> collectShadowToFocusLinks(CorrelatorSuggestion suggestion) {
        Map<String, Set<String>> shadowToFocuses = new HashMap<>();
        ItemPath shadowPath = suggestion.resourceAttrPath();
        ItemPath focusPath = suggestion.focusItemPath();

        Map<Object, Set<String>> focusValueToOids = new HashMap<>();
        for (PrismObject<?> focus : sampledFocuses) {
            var item = focus.findItem(focusPath);
            if (item != null) {
                Object val = item.getValue().getRealValue();
                if (val != null) {
                    focusValueToOids
                            .computeIfAbsent(String.valueOf(val), k -> new HashSet<>())
                            .add(focus.getOid());
                }
            }
        }

        for (PrismObject<?> shadow : sampledShadows) {
            var item = shadow.findItem(shadowPath);
            if (item != null) {
                Object val = item.getValue().getRealValue();
                if (val != null) {
                    Set<String> matchingFocuses = focusValueToOids.getOrDefault(String.valueOf(val), Collections.emptySet());
                    shadowToFocuses.put(shadow.getOid(), new HashSet<>(matchingFocuses));
                } else {
                    shadowToFocuses.put(shadow.getOid(), Collections.emptySet());
                }
            } else {
                shadowToFocuses.put(shadow.getOid(), Collections.emptySet());
            }
        }
        return shadowToFocuses;
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
                LOGGER.debug("Focus path {} is multi-valued", path);
                return true;
            }
            var shadowDef = ctx.getShadowDefinition().findItemDefinition(path);
            if (shadowDef != null && shadowDef.isMultiValue()) {
                LOGGER.debug("Shadow path {} is multi-valued", path);
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
     *   - Whether either involved path is multivalued (score 0 if so)
     *   - Uniqueness and coverage statistics for focus and shadow attributes
     *   - Linkage coverage and ambiguity between sampled focuses and shadows
     *
     * @param suggestion The correlator suggestion to evaluate.
     * @return A double score value (0 = unusable, 1 = ideal unique/complete mapping).
     */
    private Double computeScore(CorrelatorSuggestion suggestion) {
        ItemPath focusPath = suggestion.focusItemPath();
        ItemPath resourcePath = suggestion.resourceAttrPath();

        boolean hasFocusPath = focusPath != null;
        boolean hasResourcePath = resourcePath != null;

        if (hasFocusPath && isMultiValued(focusPath) && focusStatistics.isPathMultiValued(focusPath)) {
            LOGGER.debug("Excluded correlator {}: multi-valued focus path.", focusPath);
            return 0.0;
        }
        if (hasResourcePath && isMultiValued(resourcePath) && resourceStatistics.isPathMultiValued(resourcePath)) {
            LOGGER.debug("Excluded correlator {}: multi-valued resource path.", resourcePath);
            return 0.0;
        }

        Double focusScore = hasFocusPath ? focusStatistics.getScore(focusPath) : null;
        Double resourceScore = hasResourcePath ? resourceStatistics.getScore(resourcePath) : null;

        if (focusScore == null && resourceScore == null) {
            return 0.0;
        } else if (focusScore == null) {
            return resourceScore * 0.5; // penalty for not having focus score
        } else if (resourceScore == null) {
            return focusScore * 0.5; // penalty for not having resourceScore score
        }

        if (focusScore == 0 || resourceScore == 0) {
            LOGGER.debug("Excluded correlator {} - {}: either focus score or resource score is 0. Focus score: {}, Resource score: {}",
                    suggestion.focusItemPath(), suggestion.resourceAttrPath(), focusScore, resourceScore);
            return 0.0;
        }

        Map<String, Set<String>> ShadowToFocusLinks = collectShadowToFocusLinks(suggestion);
        double linkCoverage = computeLinkCoverage(ShadowToFocusLinks);

        // Excluding base score to have unified score with "black-box" correlators
        /*
        double baseScore = 2.0 * focusScore * resourceScore / (focusScore + resourceScore);
        double finalScore = baseScore * linkCoverage;
        LOGGER.debug("Base score: {} - Sampled focus-shadow link coverage: {} - Final score: {}",
                baseScore, linkCoverage, finalScore);
         */

        return linkCoverage;
    }

    /**
     * Computes how well sampled shadow objects are covered by links to focus objects,
     * and penalizes ambiguous mappings (one focus mapping to multiple shadows).
     *
     * @param shadowToFocusesLinks Map from focus OID to set of linked shadow OIDs.
     * @return Coverage/ambiguity score (0 = no mapping, 1 = perfect one-to-one coverage).
     */
    private static double computeLinkCoverage(Map<String, Set<String>> shadowToFocusesLinks) {
        int shadowCount = shadowToFocusesLinks.size();
        if (shadowCount == 0) return 0.0;

        int linked = 0;
        int multiLinks = 0;

        for (Set<String> focusOids : shadowToFocusesLinks.values()) {
            if (!focusOids.isEmpty()) {
                linked++;
                if (focusOids.size() > 1) {
                    multiLinks += (focusOids.size() - 1); // ambiguity count
                }
            }
        }

        // Coverage: How many focus objects are linked to at least one shadow
        double coverage = (double) linked / shadowCount;

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
            if (countedPaths != null && !countedPaths.isEmpty()) {
                countedPaths.forEach(p -> {
                    if (p != null) {
                        countedPathsMap.put(p, new ItemStatistics());
                    }
                });
            }
        }

        /**
         * Process the given object: for each tracked path, record whether a value is present or missing,
         * detect multivalued attributes, and track distinct values.
         *
         * @param object The object to analyze.
         */
        void process(PrismObject<?> object) {
            if (countedPathsMap.isEmpty() || object == null) return;

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
            if (countedPathsMap.isEmpty() || itemPath == null) return null;
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
            if (countedPathsMap.isEmpty() || itemPath == null) return null;
            return MiscUtil.stateNonNull(
                            countedPathsMap.get(itemPath),
                            "No statistics for %s", itemPath)
                    .skippedMultiValued;
        }
    }

    /**
     * Tracks statistics for a single attribute path, including:
     *   - Number of objects processed
     *   - Number of objects missing a value for this path
     *   - Whether any object was multivalued at this path
     *   - Set of distinct values found (for uniqueness)
     */
    private static class ItemStatistics {
        private int objects = 0;
        private int missingValues = 0;
        boolean skippedMultiValued = false;
        private final Set<Object> distinctValues = new HashSet<>();

        /**
         * Computes a score analogous to F1-score in classification:
         *   - Uniqueness: Favor paths with more distinct values (close to one-to-one mapping)
         *   - Coverage: Favor paths with fewer missing values
         *   - Harmonic mean: Penalize paths that are low on either dimension
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
