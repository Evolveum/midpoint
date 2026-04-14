/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */

package com.evolveum.midpoint.smart.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaMatchOneResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaMatchResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributeStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SiAttributeDefinitionType;

/**
 * Applies post-processing heuristics to a SchemaMatchResultType after the core schema matching
 * (both well-known-schema and AI-based) has produced its results.
 */
class PostSchemaMatchHeuristics {

    private static final Trace LOGGER = TraceManager.getTrace(PostSchemaMatchHeuristics.class);

    private static final ItemPath FOCUS_NAME_PATH = ItemPath.create(FocusType.F_NAME);

    /**
     * Shadow attributes whose values are at least this fraction unique (distinct / present)
     * are considered unique enough to map to focus name.
     */
    static final double UNIQUENESS_THRESHOLD = 0.9;

    private final PrismObjectDefinition<?> focusTypeDefinition;
    private final ShadowObjectClassStatisticsType objectTypeStatistics;

    PostSchemaMatchHeuristics(
            PrismObjectDefinition<?> focusTypeDefinition,
            ShadowObjectClassStatisticsType objectTypeStatistics) {
        this.focusTypeDefinition = focusTypeDefinition;
        this.objectTypeStatistics = objectTypeStatistics;
    }

    /**
     * Runs all post-schema-match heuristics
     */
    void applyAll(SchemaMatchResultType schemaMatchResult) {
        applyNameExpansionHeuristic(schemaMatchResult);
        applyUniquenessFilterHeuristic(schemaMatchResult);
    }

    /**
     * Name-expansion heuristic: every resource attribute that is already mapped to a non-name
     * correlatable focus attribute (i.e. returned by KnownCorrelator#getAllFor) is
     * additionally mapped to focus name, unless such a mapping already exists.
     *
     * Rationale: name is midPoint's primary, human-visible identifier. When a resource
     * attribute is significant enough to match a correlatable focus attribute (e.g. email, personal number, identifier),
     * it is a plausible – and often correct – additional source for constructing name.
     */
    private void applyNameExpansionHeuristic(SchemaMatchResultType schemaMatchResult) {
        List<SchemaMatchOneResultType> existing = schemaMatchResult.getSchemaMatchResult();
        List<? extends ItemPath> correlatablePaths = KnownCorrelator.getAllFor(focusTypeDefinition.getCompileTimeClass());
        List<ItemPath> shadowPathsMappedToName = new ArrayList<>();
        List<SchemaMatchOneResultType> candidates = new ArrayList<>();

        for (SchemaMatchOneResultType entry : existing) {
            if (isFocusNamePath(entry.getFocusPropertyPath())) {
                // Collect shadow attributes mapped to focus name
                try {
                    shadowPathsMappedToName.add(PrismContext.get().itemPathParser().asItemPath(entry.getShadowAttributePath()));
                } catch (Exception e) {
                    LOGGER.debug("Name-expansion heuristic: cannot parse shadow path '{}' – ignoring", entry.getShadowAttributePath());
                }
            } else if (isCorrelatableFocusPath(entry.getFocusPropertyPath(), correlatablePaths)) {
                // Collect correlatable focus attributes
                candidates.add(entry);
            }
        }

        SiAttributeDefinitionType nameFocusDef = buildNameFocusDef();
        if (nameFocusDef == null) {
            LOGGER.debug("Name-expansion heuristic: focus type has no 'name' property – skipping");
            return;
        }

        String namePathString = FOCUS_NAME_PATH.toStringStandalone();
        List<SchemaMatchOneResultType> additions = new ArrayList<>();
        List<ItemPath> coveredShadowPaths = new ArrayList<>(shadowPathsMappedToName);

        for (SchemaMatchOneResultType candidate : candidates) {
            String shadowAttrPath = candidate.getShadowAttributePath();
            if (shadowAttrPath == null) {
                continue;
            }
            ItemPath parsedPath;
            try {
                parsedPath = PrismContext.get().itemPathParser().asItemPath(shadowAttrPath);
            } catch (Exception e) {
                LOGGER.debug("Name-expansion heuristic: cannot parse shadow path '{}' – skipping", shadowAttrPath);
                continue;
            }
            if (coveredShadowPaths.stream().noneMatch(p -> p.equivalent(parsedPath))) {
                coveredShadowPaths.add(parsedPath);
                LOGGER.debug("Name-expansion heuristic: {} -> name", shadowAttrPath);
                additions.add(new SchemaMatchOneResultType()
                        .shadowAttributePath(shadowAttrPath)
                        .shadowAttribute(candidate.getShadowAttribute())
                        .focusPropertyPath(namePathString)
                        .focusProperty(nameFocusDef)
                        .isSystemProvided(true));
            }
        }

        if (!additions.isEmpty()) {
            LOGGER.info("Name-expansion heuristic: added {} shadow-attribute-to-name mapping(s)", additions.size());
            existing.addAll(additions);
        }
    }

    private SiAttributeDefinitionType buildNameFocusDef() {
        PrismPropertyDefinition<?> propDef = focusTypeDefinition.findPropertyDefinition(FOCUS_NAME_PATH);
        if (propDef == null) {
            return null;
        }
        return new SiAttributeDefinitionType()
                .name(DescriptiveItemPath.of(FOCUS_NAME_PATH, focusTypeDefinition).asString())
                .type(SchemaMatchService.getTypeName(propDef))
                .minOccurs(propDef.getMinOccurs())
                .maxOccurs(propDef.getMaxOccurs());
    }

    /**
     * Uniqueness-filter heuristic: removes focus-name mappings whose shadow attribute does not meet
     * the uniqueness threshold derived from pre-computed object type statistics.
     *
     * The uniqueness ratio is: uniqueValueCount / (totalObjects - missingValueCount).
     * Mappings with a ratio below UNIQUENESS_THRESHOLD are dropped.
     * When no statistics are available the heuristic is silently skipped.
     */
    private void applyUniquenessFilterHeuristic(SchemaMatchResultType schemaMatchResult) {
        if (objectTypeStatistics == null) {
            LOGGER.debug("Uniqueness-filter heuristic: no object type statistics available – skipping");
            return;
        }
        int totalObjects = objectTypeStatistics.getSize();
        if (totalObjects == 0) {
            LOGGER.debug("Uniqueness-filter heuristic: statistics report 0 objects – skipping");
            return;
        }

        int removed = 0;
        Iterator<SchemaMatchOneResultType> it = schemaMatchResult.getSchemaMatchResult().iterator();
        while (it.hasNext()) {
            SchemaMatchOneResultType entry = it.next();
            if (!isFocusNamePath(entry.getFocusPropertyPath())) {
                continue;
            }
            ShadowAttributeStatisticsType attrStats = findAttributeStatistics(entry.getShadowAttributePath());
            if (attrStats == null) {
                LOGGER.debug("Uniqueness-filter heuristic: no stats for {} – keeping", entry.getShadowAttributePath());
                continue;
            }
            double ratio = computeUniquenessRatio(attrStats, totalObjects);
            if (ratio < UNIQUENESS_THRESHOLD) {
                LOGGER.debug("Uniqueness-filter heuristic: removing {} -> name (ratio={}, threshold={})",
                        entry.getShadowAttributePath(), ratio, UNIQUENESS_THRESHOLD);
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            LOGGER.info("Uniqueness-filter heuristic: removed {} low-uniqueness shadow-attribute-to-name mapping(s)", removed);
        }
    }

    private ShadowAttributeStatisticsType findAttributeStatistics(String shadowAttributePathString) {
        if (shadowAttributePathString == null) {
            return null;
        }
        try {
            ItemPath parsed = PrismContext.get().itemPathParser().asItemPath(shadowAttributePathString);
            return objectTypeStatistics.getAttribute().stream()
                    .filter(a -> a.getRef() != null && parsed.equivalent(a.getRef().getItemPath()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            LOGGER.debug("Uniqueness-filter heuristic: could not parse shadow attribute path '{}': {}",
                    shadowAttributePathString, e.getMessage());
            return null;
        }
    }

    private double computeUniquenessRatio(ShadowAttributeStatisticsType attrStats, int totalObjects) {
        int presentCount = totalObjects - attrStats.getMissingValueCount();
        if (presentCount <= 0) {
            return 0.0;
        }
        int uniqueCount = attrStats.getUniqueValueCount();
        return (double) uniqueCount / presentCount;
    }

    private boolean isCorrelatableFocusPath( String focusPropertyPathString, List<? extends ItemPath> correlatablePaths) {
        if (focusPropertyPathString == null) {
            return false;
        }
        try {
            ItemPath parsed = PrismContext.get().itemPathParser().asItemPath(focusPropertyPathString);
            return correlatablePaths.stream().anyMatch(c -> c.equivalent(parsed));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isFocusNamePath(String focusPropertyPathString) {
        if (focusPropertyPathString == null) {
            return false;
        }
        try {
            ItemPath parsed = PrismContext.get().itemPathParser().asItemPath(focusPropertyPathString);
            return FOCUS_NAME_PATH.equivalent(parsed);
        } catch (Exception e) {
            LOGGER.warn("Name-expansion heuristic: could not parse focus property path '{}': {}",
                    focusPropertyPathString, e.getMessage());
            return false;
        }
    }

}
