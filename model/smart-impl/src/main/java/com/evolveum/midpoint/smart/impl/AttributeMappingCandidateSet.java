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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeMappingsSuggestionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Manages a collection of attribute mapping candidates, handling duplicate detection
 * and quality-based selection. Encapsulates the logic for proposing new candidates
 * and keeping only the best ones based on their target paths and quality metrics.
 *
 * Supports deduplication against existing mappings configured on the resource
 * and accepted suggestions that are held in GUI's unsaved state.
 * Target paths that already have mappings or accepted suggestions are ignored.
 */
class AttributeMappingCandidateSet {

    private final List<Candidate> candidates = new ArrayList<>();

    /** Target paths that already have mappings or are accepted as suggestions; proposals for these are skipped. */
    private final List<ItemPath> excludedMappingPaths;

    AttributeMappingCandidateSet(Collection<ItemPath> excludedMappingPaths) {
        this.excludedMappingPaths = excludedMappingPaths == null ? List.of() : List.copyOf(excludedMappingPaths);
    }

    /**
     * Proposes a new mapping candidate. If a duplicate (based on target path) already exists,
     * the candidate with better quality is kept. System-provided mappings are preferred over
     * AI-provided ones when quality is equal.
     */
    void propose(AttributeMappingsSuggestionType suggestion) {
        ItemPath targetPath = extractTargetPath(suggestion);
        if (targetPath == null) {
            throw new IllegalArgumentException("Target path must not be null for suggestion: " + suggestion);
        }

        if (excludedMappingPaths.stream().anyMatch(targetPath::equivalent)) {
            return;
        }

        float newQuality = getQuality(suggestion);
        boolean newIsSystemProvided = SmartMetadataUtil.isMarkedAsSystemProvided(suggestion.asPrismContainerValue());

        var iterator = candidates.iterator();
        while (iterator.hasNext()) {
            Candidate existing = iterator.next();
            if (targetPath.equivalent(existing.targetPath())) {
                float existingQuality = getQuality(existing.suggestion());
                boolean existingIsSystemProvided = SmartMetadataUtil.isMarkedAsSystemProvided(
                        existing.suggestion().asPrismContainerValue());

                if (shouldReplaceWith(newQuality, newIsSystemProvided, existingQuality, existingIsSystemProvided)) {
                    iterator.remove();
                    break;
                } else {
                    return;
                }
            }
        }

        candidates.add(new Candidate(targetPath, suggestion));
    }

    /**
     * Returns an immutable list of the best mapping suggestions.
     * Each suggestion contains exactly one inbound or outbound mapping.
     */
    List<AttributeMappingsSuggestionType> best() {
        return candidates.stream()
                .map(Candidate::suggestion)
                .toList();
    }

    /**
     * Extracts the target path from a mapping suggestion for duplicate detection.
     * For inbound mappings, this is the focus property (target path).
     * For outbound mappings, this is the resource attribute (ref).
     */
    private static ItemPath extractTargetPath(AttributeMappingsSuggestionType suggestion) {
        var definition = suggestion.getDefinition();
        if (definition == null) {
            return null;
        }

        List<InboundMappingType> inbounds = definition.getInbound();
        if (inbounds != null && !inbounds.isEmpty()) {
            var inbound = inbounds.get(0);
            if (inbound.getTarget() != null && inbound.getTarget().getPath() != null) {
                Object path = inbound.getTarget().getPath();
                if (path instanceof ItemPathType itemPath) {
                    return itemPath.getItemPath();
                }
            }
        }

        var outbound = definition.getOutbound();
        if (outbound != null) {
            var ref = definition.getRef();
            if (ref != null) {
                return ref.getItemPath();
            }
        }

        return null;
    }

    private static float getQuality(AttributeMappingsSuggestionType suggestion) {
        Float quality = suggestion.getExpectedQuality();
        return quality != null ? quality : 0.0f;
    }

    private static boolean shouldReplaceWith(
            float newQuality, boolean newIsSystemProvided,
            float existingQuality, boolean existingIsSystemProvided) {
        if (newQuality > existingQuality) {
            return true;
        }
        if (newQuality < existingQuality) {
            return false;
        }
        return newIsSystemProvided && !existingIsSystemProvided;
    }

    /**
     * Internal record holding a mapping candidate with its target path.
     * The target path is used for duplicate detection - mappings with the same target
     * are considered duplicates.
     */
    private record Candidate(ItemPath targetPath, AttributeMappingsSuggestionType suggestion) {
    }
}
