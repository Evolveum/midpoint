/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import java.io.Serializable;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Describes a correlation case, typically when it's going to be presented to the user.
 *
 * Need not be connected to actual {@link CaseType} object. The term "case" is used more figuratively here, to describe
 * a correlation situation that is going to be resolved.
 *
 * Contains the object being correlated (currently called {@link #preFocus}) and the correlation candidates ({@link #candidates}).
 *
 * The correlation data are represented as a set of {@link #correlationPropertiesDefinitions},
 * whose values on the source are to be fetched directly from {@link #preFocus}, but the valued for
 * candidates are processed into the form of {@link CorrelationPropertyValuesDescription}:
 *
 *  - sorted out into "primary" and "secondary" values (corresponding to the main identity and alternative ones),
 *  - and providing a {@link Match} value that shows the degree of match between the particular candidate and the pre-focus
 *  on this particular property.
 *
 * Optionally, there may be a {@link CorrelationExplanation} object for each correlation candidate. (If requested.)
 */
public class CorrelationCaseDescription<F extends FocusType> implements DebugDumpable, Serializable {

    /** Object being correlated, a.k.a. the source object. TODO find better name for this field */
    @NotNull private final F preFocus;

    @NotNull private final PathKeyedMap<CorrelationPropertyDefinition> correlationPropertiesDefinitions = new PathKeyedMap<>();

    @NotNull private final List<CandidateDescription<F>> candidates = new ArrayList<>();

    public CorrelationCaseDescription(@NotNull F preFocus) {
        this.preFocus = preFocus;
    }

    public @NotNull F getPreFocus() {
        return preFocus;
    }

    public @NotNull PathKeyedMap<CorrelationPropertyDefinition> getCorrelationPropertiesDefinitions() {
        return correlationPropertiesDefinitions;
    }

    /** The list is sorted according to display order (and display name, in case of ambiguity). */
    public @NotNull List<CorrelationPropertyDefinition> getCorrelationPropertiesDefinitionsList() {
        var list = new ArrayList<>(correlationPropertiesDefinitions.values());
        list.sort(
                Comparator.comparing(CorrelationPropertyDefinition::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CorrelationPropertyDefinition::getDisplayName));
        return list;
    }

    public @NotNull List<CandidateDescription<F>> getCandidates() {
        return candidates;
    }

    public boolean hasCorrelationProperty(@NotNull ItemPath path) {
        return correlationPropertiesDefinitions.containsKey(path);
    }

    public void addCorrelationPropertyDefinition(CorrelationPropertyDefinition property) {
        correlationPropertiesDefinitions.put(property.getItemPath(), property);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "correlationProperties: " + correlationPropertiesDefinitions.size() +
                "candidates: " + candidates.size() +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "preFocus", preFocus, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "correlationPropertiesDefinitions", correlationPropertiesDefinitions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "candidates", candidates, indent + 1);
        return sb.toString();
    }

    public void addCandidate(CandidateDescription<F> description) {
        candidates.add(description);
    }

    public static class CandidateDescription<F extends FocusType> implements DebugDumpable, Serializable {

        /** Contains also `identities` data. */
        @NotNull private final F object;

        private final double confidence;

        /** Values of individual correlation properties, sorted to primary/secondary, and with the match level. */
        @NotNull private final PathKeyedMap<CorrelationPropertyValuesDescription> propertiesValuesMap;

        /**
         * (Optional) explanation of the correlation process for this candidate.
         * May be missing if not requested or not available.
         */
        @Nullable private final CorrelationExplanation explanation;

        public CandidateDescription(
                @NotNull F object,
                double confidence,
                @NotNull PathKeyedMap<CorrelationPropertyValuesDescription> propertiesValuesMap,
                @Nullable CorrelationExplanation explanation) {
            this.object = object;
            this.confidence = confidence;
            this.propertiesValuesMap = propertiesValuesMap;
            this.explanation = explanation;
        }

        public @NotNull F getObject() {
            return object;
        }

        public @NotNull String getOid() {
            return MiscUtil.stateNonNull(object.getOid(), () -> "No OID in " + object);
        }

        public double getConfidence() {
            return confidence;
        }

        public @Nullable CorrelationPropertyValuesDescription getPropertyValuesDescription(
                @NotNull CorrelationPropertyDefinition propertyDef) {
            return getPropertyValuesDescription(propertyDef.getItemPath());
        }

        public @Nullable CorrelationPropertyValuesDescription getPropertyValuesDescription(@NotNull ItemPath propertyPath) {
            return propertiesValuesMap.get(propertyPath);
        }

        public @Nullable CorrelationExplanation getExplanation() {
            return explanation;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "candidate=" + object +
                    ", confidence=" + confidence +
                    ", properties: " + propertiesValuesMap.size() +
                    ", explanation: " + explanation +
                    '}';
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "object", object, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "confidence", confidence, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "values of properties", propertiesValuesMap.values(), indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "explanation", explanation, indent + 1);
            return sb.toString();
        }
    }

    public static class CorrelationPropertyValuesDescription implements Serializable {

        @NotNull private final CorrelationPropertyDefinition propertyDefinition;

        // TODO clarify
        @NotNull private final Set<PrismValue> primaryValues;

        // TODO clarify
        @NotNull private final Set<PrismValue> secondaryValues;

        @NotNull private final Match match;

        public CorrelationPropertyValuesDescription(
                @NotNull CorrelationPropertyDefinition propertyDefinition,
                @NotNull Set<PrismValue> primaryValues,
                @NotNull Set<PrismValue> secondaryValues,
                @NotNull Match match) {
            this.propertyDefinition = propertyDefinition;
            this.primaryValues = Set.copyOf(primaryValues); // to be serializable
            this.secondaryValues = Set.copyOf(secondaryValues); // to be serializable
            this.match = match;
        }

        public @NotNull Set<PrismValue> getPrimaryValues() {
            return primaryValues;
        }

        public @NotNull Set<PrismValue> getSecondaryValues() {
            return secondaryValues;
        }

        public @NotNull Match getMatch() {
            return match;
        }

        @Override
        public String toString() {
            return propertyDefinition.getItemPath()
                    + ": primary=" + dump(primaryValues)
                    + ", secondary=" + dump(secondaryValues)
                    + ", match=" + match;
        }

        private List<String> dump(Collection<PrismValue> values) {
            return values.stream()
                    .map(prismValue -> {
                        try {
                            return prismValue.getRealValue();
                        } catch (Exception e) {
                            // getRealValue may throw an exception in rare cases (for containers, maybe)
                            return prismValue.toString();
                        }
                    })
                    .map(String::valueOf)
                    .toList();
        }
    }

    /** How well the candidate matches the object being correlated on given correlation property? */
    public enum Match {

        /**
         * A full match: The default normalization of the primary value (or one of the primary values) exactly matches
         * the same normalization of the source value. Usually displayed in green.
         *
         * (Default normalization is either default "indexing", or the result of the application of the item matching rule
         * from the object template.)
         *
         * This should be adequate for the majority of cases. An exception could be if we use a correlator with more
         * strict indexing than the default one. But this may be seen as a configuration issue: one should perhaps
         * set the default correlator to be the more strict one.
         */
        FULL,

        /**
         * A partial match.
         *
         * The "baseline" meaning is that any normalization of any primary or secondary value exactly matches
         * the same normalization of the source value.
         *
         * For items mentioned by "items" correlator(s) the partial match is also if at least one filter defined
         * in the correlator matches. This may include fuzzy search filters, e.g., Levenshtein edit distance or
         * trigram similarity.
         *
         * Usually displayed in orange.
         */
        PARTIAL,

        /**
         * No match. The values are present, and should match. However, neither {@link #FULL} nor {@link #PARTIAL} case
         * applies. Usually displayed in red.
         */
        NONE,

        /**
         * The matching is not applicable, for example because the correlation property has no value
         * in object being correlated.
         */
        NOT_APPLICABLE
    }
}
