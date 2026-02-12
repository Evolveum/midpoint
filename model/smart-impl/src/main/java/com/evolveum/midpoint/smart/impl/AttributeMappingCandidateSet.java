/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.SmartMetadataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Nullable;

/**
 * Manages a collection of attribute mapping candidates, handling duplicate detection
 * and quality-based selection.
 * Multiple suggestions per target are allowed if they differ in source or script.
 * System-provided mappings (heuristics) are preferred over AI when quality is equal.
 */
class AttributeMappingCandidateSet {

    private static final float QUALITY_THRESHOLD = 0.4f;

    private final List<Candidate> candidates = new ArrayList<>();

    /** Target paths that already have mappings or are accepted as suggestions; proposals for these are skipped. */
    private final List<ItemPath> excludedMappingPaths;

    AttributeMappingCandidateSet(Collection<ItemPath> excludedMappingPaths) {
        this.excludedMappingPaths = excludedMappingPaths == null ? List.of() : List.copyOf(excludedMappingPaths);
    }

    /**
     * Proposes a new mapping candidate.
     * Deduplication logic:
     * - Among suggestions: Based on triple (source, target, script) where script is null for AS-IS mappings
     * - Against existing mappings: Based on target path only
     * Filtering logic:
     * - New data scenario (quality = null): Keep ALL mappings
     * - Existing data scenario (quality != null): Keep all above threshold (0.4)
     * When duplicates exist, the better quality candidate is kept.
     * System-provided mappings are preferred over AI when quality is equal.
     */
    void propose(AttributeMappingsSuggestionType suggestion) {
        var mappingContext = MappingContext.extract(suggestion);

        // Deduplicate against existing mappings by target path only
        if (excludedMappingPaths.stream().anyMatch(mappingContext.targetPath()::equivalent)) {
            return;
        }

        Float quality = suggestion.getExpectedQuality();
        boolean isSystemProvided = SmartMetadataUtil.isMarkedAsSystemProvided(suggestion.asPrismContainerValue());

        // Quality filtering: if quality is present, apply threshold
        if (quality != null && quality <= QUALITY_THRESHOLD) {
            return;
        }

        // Deduplicate among suggestions by (source, target, script) triple
        var iterator = candidates.iterator();
        while (iterator.hasNext()) {
            Candidate existing = iterator.next();

            // Check if it's a duplicate based on (source, target, script)
            if (mappingContext.isDuplicateOf(existing.mappingContext())) {
                boolean existingIsSystemProvided = SmartMetadataUtil.isMarkedAsSystemProvided(existing.suggestion().asPrismContainerValue());

                if (isSystemProvided && !existingIsSystemProvided) {
                    iterator.remove();
                    break;
                } else {
                    return;
                }
            }
        }

        candidates.add(new Candidate(mappingContext, suggestion));
    }

    /**
     * Returns an immutable list of the best mapping suggestions.
     * Multiple suggestions per target attribute are allowed if they differ in source or script.
     * Results are grouped by target path, then sorted by quality (descending) within each group,
     * with system-provided preferred over AI when quality is equal.
     */
    List<AttributeMappingsSuggestionType> best() {
        return candidates.stream()
                .sorted(this::compareByTargetThenQuality)
                .map(Candidate::suggestion)
                .toList();
    }

    private int compareByTargetThenQuality(Candidate a, Candidate b) {
        // First, group by target path
        int targetCompare = compareItemPaths(a.mappingContext().targetPath(), b.mappingContext().targetPath());
        if (targetCompare != 0) {
            return targetCompare;
        }

        // Within the same target, sort by quality and origin
        Float qualityA = a.suggestion().getExpectedQuality();
        Float qualityB = b.suggestion().getExpectedQuality();

        // Sort by quality descending (nulls last, meaning new data scenarios come last)
        if (qualityA != null && qualityB != null) {
            return Float.compare(qualityB, qualityA);
        } else if (qualityA != null) {
            return -1; // a has quality, b doesn't - a comes first
        } else if (qualityB != null) {
            return 1; // b has quality, a doesn't - b comes first
        }

        return 0;
    }

    private static int compareItemPaths(ItemPath a, ItemPath b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.toString().compareTo(b.toString());
    }

    /**
     * Identity of a mapping used for deduplication.
     * Contains the triple (source, target, script) that uniquely identifies a mapping.
     */
    private record MappingContext(ItemPath targetPath, ItemPath sourcePath, @Nullable String script) {

        boolean isDuplicateOf(MappingContext other) {
            return this.targetPath.equivalent(other.targetPath) && this.sourcePath.equivalent(other.sourcePath)
                    && (this.script != null && this.script.equals(other.script) || this.script == null && other.script == null);
        }

        static MappingContext extract(AttributeMappingsSuggestionType suggestion) {
            var definition = suggestion.getDefinition();
            if (definition == null) {
                throw new IllegalArgumentException("No definition found for suggestion: " + suggestion);
            }

            var inbounds = definition.getInbound();
            if (inbounds != null && !inbounds.isEmpty()) {
                return extractFromInbound(definition, inbounds.get(0));
            }

            var outbound = definition.getOutbound();
            if (outbound != null) {
                return extractFromOutbound(definition, outbound);
            }

            throw new IllegalArgumentException("No inbound or outbound mapping found for suggestion: " + suggestion);
        }

        private static MappingContext extractFromInbound(
                ResourceAttributeDefinitionType definition, InboundMappingType inbound) {
            // Target: focus property from inbound target
            ItemPath target = null;
            if (inbound.getTarget() != null && inbound.getTarget().getPath() != null) {
                target = toItemPath(inbound.getTarget().getPath());
            }
            // Source: resource attribute from ref
            ItemPath source = null;
            if (definition.getRef() != null) {
                source = definition.getRef().getItemPath();
            }
            if (target == null || source == null) {
                throw new IllegalArgumentException("No target or source found for inbound mapping: " + inbound);
            }
            String script = extractScriptFromExpression(inbound.getExpression());
            return new MappingContext(target, source, script);
        }

        private static MappingContext extractFromOutbound(
                ResourceAttributeDefinitionType definition, MappingType outbound) {
            // Target: resource attribute from ref
            ItemPath target = null;
            if (definition.getRef() != null) {
                target = definition.getRef().getItemPath();
            }
            // Source: focus property from outbound source (first if multiple)
            ItemPath source = null;
            var sources = outbound.getSource();
            if (sources != null && !sources.isEmpty()) {
                var firstSource = sources.get(0);
                if (firstSource != null && firstSource.getPath() != null) {
                    source = toItemPath(firstSource.getPath());
                }
            }
            if (target == null || source == null) {
                throw new IllegalArgumentException("No target or source found for outbound mapping: " + outbound);
            }
            // Script: from outbound expression
            String script = extractScriptFromExpression(outbound.getExpression());
            return new MappingContext(target, source, script);
        }

        private static @Nullable String extractScriptFromExpression(@Nullable ExpressionType expression) {
            if (expression == null) {
                return null;
            }
            var evaluators = expression.getExpressionEvaluator();
            if (evaluators != null) {
                for (var evaluator : evaluators) {
                    if (evaluator.getValue() instanceof ScriptExpressionEvaluatorType scriptEval) {
                        return scriptEval.getCode();
                    }
                }
            }
            return null;
        }

        private static ItemPath toItemPath(Object path) {
            if (path instanceof ItemPathType itemPath) {
                return itemPath.getItemPath();
            }
            return null;
        }
    }

    /**
     * Internal record holding a mapping candidate with its mappingContext.
     * Deduplication among suggestions uses (source, target, script) triple.
     * Deduplication against existing mappings uses target path only.
     */
    private record Candidate(
            MappingContext mappingContext,
            AttributeMappingsSuggestionType suggestion) {
    }
}
