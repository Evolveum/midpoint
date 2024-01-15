/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlation;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Describes a correlation case, typically when it's going to be presented to the user.
 *
 * Need not be connected to actual {@link CaseType} object. The term "case" is used more figuratively here, to describe
 * a correlation situation that is going to be resolved.
 *
 * Contains the object being correlated (currently called {@link #preFocus}) and the correlation candidates ({@link #candidates}).
 *
 * The correlation data are represented as a set of {@link #correlationProperties}, whose values on the source are to be fetched
 * directly from {@link #preFocus}, but the valued for candidates are processed into the form of
 * {@link CorrelationPropertyValuesDescription}:
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

    @NotNull private final PathKeyedMap<CorrelationProperty> correlationProperties = new PathKeyedMap<>();

    @NotNull private final List<CandidateDescription<F>> candidates = new ArrayList<>();

    public CorrelationCaseDescription(@NotNull F preFocus) {
        this.preFocus = preFocus;
    }

    public @NotNull F getPreFocus() {
        return preFocus;
    }

    public @NotNull PathKeyedMap<CorrelationProperty> getCorrelationProperties() {
        return correlationProperties;
    }

    /** The list is sorted according to display order (and display name, in case of ambiguity). */
    public @NotNull List<CorrelationProperty> getCorrelationPropertiesList() {
        var list = new ArrayList<>(correlationProperties.values());
        list.sort(
                Comparator.comparing(CorrelationProperty::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CorrelationProperty::getDisplayName));
        return list;
    }

    public @NotNull List<CandidateDescription<F>> getCandidates() {
        return candidates;
    }

    public void addCorrelationProperty(CorrelationProperty property) {
        correlationProperties.put(property.getItemPath(), property);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "correlationProperties: " + correlationProperties.size() +
                "candidates: " + candidates.size() +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "preFocus", preFocus, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "correlationProperties", correlationProperties, indent + 1);
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

        public @Nullable CorrelationPropertyValuesDescription getPropertyValuesDescription(@NotNull CorrelationProperty property) {
            return getPropertyValuesDescription(property.getItemPath());
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

    /**
     * Contains information about a correlation property that is to be (e.g.) displayed in the correlation case view.
     *
     * TEMPORARY
     */
    public static class CorrelationProperty implements Serializable, DebugDumpable {

        public static final String F_DISPLAY_NAME = "displayName";

        /** The "technical" name. */
        @NotNull private final String name;

        /** Path within the focus object. */
        @NotNull private final ItemPath itemPath;

        /** Definition in the focus object. */
        @Nullable private final ItemDefinition<?> definition;

        private CorrelationProperty(
                @NotNull String name,
                @NotNull ItemPath itemPath,
                @Nullable ItemDefinition<?> definition) {
            this.name = name;
            this.itemPath = itemPath;
            this.definition = definition;
        }

        public static CorrelationProperty createSimple(
                @NotNull ItemPath path,
                @Nullable PrismPropertyDefinition<?> definition) {
            ItemName lastName =
                    MiscUtil.requireNonNull(path.lastName(), () -> new IllegalArgumentException("Path has no last name: " + path));
            return new CorrelationProperty(lastName.getLocalPart(), path, definition);
        }

        public @NotNull ItemPath getItemPath() {
            return itemPath;
        }

        public @NotNull ItemPath getSecondaryPath() {
            return SchemaConstants.PATH_FOCUS_IDENTITY.append(FocusIdentityType.F_DATA, itemPath);
        }

        public @Nullable ItemDefinition<?> getDefinition() {
            return definition;
        }

        public @NotNull String getDisplayName() {
            if (definition != null) {
                if (definition.getDisplayName() != null) {
                    return definition.getDisplayName();
                } else {
                    return definition.getItemName().getLocalPart();
                }
            } else {
                return name;
            }
        }

        public Integer getDisplayOrder() {
            return definition != null ? definition.getDisplayOrder() : null;
        }

        public @NotNull String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "itemPath=" + itemPath +
                    '}';
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            DebugUtil.debugDumpWithLabelLn(sb, "name", name, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "itemPath", String.valueOf(itemPath), indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "definition", String.valueOf(definition), indent + 1);
            return sb.toString();
        }
    }

    public static class CorrelationPropertyValuesDescription implements Serializable {

        @NotNull private final CorrelationProperty correlationProperty;

        // TODO clarify
        @NotNull private final Set<PrismValue> primaryValues;

        // TODO clarify
        @NotNull private final Set<PrismValue> secondaryValues;

        @NotNull private final Match match;

        public CorrelationPropertyValuesDescription(
                @NotNull CorrelationProperty correlationProperty,
                @NotNull Set<PrismValue> primaryValues,
                @NotNull Set<PrismValue> secondaryValues,
                @NotNull Match match) {
            this.correlationProperty = correlationProperty;
            this.primaryValues = primaryValues;
            this.secondaryValues = secondaryValues;
            this.match = match;
        }

        public @NotNull CorrelationProperty getCorrelationProperty() {
            return correlationProperty;
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
            return correlationProperty.getItemPath()
                    + ": primary=" + dump(primaryValues)
                    + ", secondary=" + dump(secondaryValues)
                    + ", match=" + match;
        }

        private List<String> dump(Collection<PrismValue> values) {
            return values.stream()
                    .map(PrismValue::getRealValue)
                    .map(String::valueOf)
                    .collect(Collectors.toList());
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
