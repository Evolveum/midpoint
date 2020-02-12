/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.equivalence;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.annotation.Experimental;

import java.util.HashMap;
import java.util.Map;

/**
 *  Implementation of EquivalenceStrategy that uses a parametrization of built-in equals/hashCode/diff methods.
 *
 *  These strategies are still in progress and (most probably) will be changed before 4.0 release.
 *
 *  The difference between REAL_VALUE and IGNORE_METADATA is to be established yet.
 *
 *  Basically, REAL_VALUE is oriented towards the effective content of the item or value.
 *  Contrary to IGNORE_METADATA it ignores element names and reference filters (if OID is present).
 */
@SuppressWarnings("unused")
public class ParameterizedEquivalenceStrategy implements EquivalenceStrategy {

    /**
     * The (almost) highest level of recognition. Useful e.g. for comparing values for the purpose of XML editing.
     * Still, ignores e.g. definitions, parent objects, origin, immutability flag, etc.
     *
     * Corresponds to pre-4.0 flags ignoreMetadata = false, literal = true.
     */
    public static final ParameterizedEquivalenceStrategy LITERAL;

    /**
     * As LITERAL but ignores XML namespace prefixes.
     * Also fills-in default relation name if not present (when comparing reference values).
     *
     * Currently this is the default for equals/hashCode.
     *
     * Roughly corresponds to pre-4.0 flags ignoreMetadata = false, literal = false.
     */
    public static final ParameterizedEquivalenceStrategy NOT_LITERAL;

    /**
     * Ignores metadata, typically operational items and values, container IDs, and origin information.
     * However, takes OID-ful reference filters into account.
     *
     * Corresponds to pre-4.0 flags ignoreMetadata = true, literal = false.
     */
    public static final ParameterizedEquivalenceStrategy IGNORE_METADATA;

    /**
     * Ignores metadata, typically operational items and values and origin information.
     *
     * Container IDs are taken into account only if they directly contradict each other, meaning both values being compared
     * do have them and they are different.
     *
     * Currently this is the default for diff and for delta application.
     *
     * EXPERIMENTAL
     */
    @Experimental
    public static final ParameterizedEquivalenceStrategy IGNORE_METADATA_CONSIDER_DIFFERENT_IDS;

    /**
     * As IGNORE_METADATA, but takes XML namespace prefixes into account.
     *
     * It is not clear in which situations this should be needed. But we include it here for compatibility reasons.
     * Historically it is used on a few places in midPoint.
     *
     * Corresponds to pre-4.0 flags ignoreMetadata = true, literal = true.
     */
    @SuppressWarnings("WeakerAccess")
    public static final ParameterizedEquivalenceStrategy LITERAL_IGNORE_METADATA;

    /**
     * Compares the real content if prism structures.
     * Corresponds to "equalsRealValue" method used in pre-4.0.
     *
     * It is to be seen if operational data should be considered in this mode (they are ignored now).
     * So, currently this is the most lax way of determining equivalence.
     */
    public static final ParameterizedEquivalenceStrategy REAL_VALUE;

    /**
     * As REAL_VALUE but treats values with different non-null IDs as not equivalent.
     *
     * EXPERIMENTAL
     */
    @SuppressWarnings("WeakerAccess")
    public static final ParameterizedEquivalenceStrategy REAL_VALUE_CONSIDER_DIFFERENT_IDS;

    public static final ParameterizedEquivalenceStrategy DEFAULT_FOR_EQUALS;
    public static final ParameterizedEquivalenceStrategy DEFAULT_FOR_DIFF;
    public static final ParameterizedEquivalenceStrategy DEFAULT_FOR_DELTA_APPLICATION;

    private static final Map<String, String> NICE_NAMES = new HashMap<>();

    static {
        LITERAL = new ParameterizedEquivalenceStrategy();
        LITERAL.literalDomComparison = true;
        LITERAL.consideringOperationalData = true;
        LITERAL.consideringContainerIds = true;
        LITERAL.consideringDifferentContainerIds = true;
        LITERAL.consideringValueOrigin = false;
        LITERAL.consideringReferenceFilters = true;
        LITERAL.consideringReferenceOptions = true;
        LITERAL.compareElementNames = true;
        putIntoNiceNames(LITERAL, "LITERAL");

        NOT_LITERAL = new ParameterizedEquivalenceStrategy();
        NOT_LITERAL.literalDomComparison = false;
        NOT_LITERAL.consideringOperationalData = true;
        NOT_LITERAL.consideringContainerIds = true;
        NOT_LITERAL.consideringDifferentContainerIds = true;
        NOT_LITERAL.consideringValueOrigin = false;
        NOT_LITERAL.consideringReferenceFilters = true;
        NOT_LITERAL.consideringReferenceOptions = true;         // ok?
        NOT_LITERAL.compareElementNames = true;
        putIntoNiceNames(NOT_LITERAL, "NOT_LITERAL");

        IGNORE_METADATA = new ParameterizedEquivalenceStrategy();
        IGNORE_METADATA.literalDomComparison = false;
        IGNORE_METADATA.consideringOperationalData = false;
        IGNORE_METADATA.consideringContainerIds = false;
        IGNORE_METADATA.consideringDifferentContainerIds = false;
        IGNORE_METADATA.consideringValueOrigin = false;
        IGNORE_METADATA.consideringReferenceFilters = true;
        IGNORE_METADATA.consideringReferenceOptions = true;         // ok?
        IGNORE_METADATA.compareElementNames = true; //???
        putIntoNiceNames(IGNORE_METADATA, "IGNORE_METADATA");

        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS = new ParameterizedEquivalenceStrategy();
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.literalDomComparison = false;
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.consideringOperationalData = false;
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.consideringContainerIds = false;
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.consideringDifferentContainerIds = true;
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.consideringValueOrigin = false;
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.consideringReferenceFilters = true;
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.consideringReferenceOptions = true;         // ok?
        IGNORE_METADATA_CONSIDER_DIFFERENT_IDS.compareElementNames = true; //???
        putIntoNiceNames(IGNORE_METADATA_CONSIDER_DIFFERENT_IDS, "IGNORE_METADATA_CONSIDER_DIFFERENT_IDS");

        LITERAL_IGNORE_METADATA = new ParameterizedEquivalenceStrategy();
        LITERAL_IGNORE_METADATA.literalDomComparison = true;
        LITERAL_IGNORE_METADATA.consideringOperationalData = false;
        LITERAL_IGNORE_METADATA.consideringContainerIds = false;
        LITERAL_IGNORE_METADATA.consideringDifferentContainerIds = false;
        LITERAL_IGNORE_METADATA.consideringValueOrigin = false;
        LITERAL_IGNORE_METADATA.consideringReferenceFilters = true;
        LITERAL_IGNORE_METADATA.consideringReferenceOptions = true;
        LITERAL_IGNORE_METADATA.compareElementNames = true;
        putIntoNiceNames(LITERAL_IGNORE_METADATA, "LITERAL_IGNORE_METADATA");

        REAL_VALUE = new ParameterizedEquivalenceStrategy();
        REAL_VALUE.literalDomComparison = false;
        REAL_VALUE.consideringOperationalData = false;
        REAL_VALUE.consideringContainerIds = false;
        REAL_VALUE.consideringDifferentContainerIds = false;
        REAL_VALUE.consideringValueOrigin = false;
        REAL_VALUE.consideringReferenceFilters = false;
        REAL_VALUE.consideringReferenceOptions = false;
        REAL_VALUE.compareElementNames = false;
        putIntoNiceNames(REAL_VALUE, "REAL_VALUE");

        REAL_VALUE_CONSIDER_DIFFERENT_IDS = new ParameterizedEquivalenceStrategy();
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.literalDomComparison = false;
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.consideringOperationalData = false;
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.consideringContainerIds = false;
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.consideringDifferentContainerIds = true;
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.consideringValueOrigin = false;
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.consideringReferenceFilters = false;
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.consideringReferenceOptions = false;
        REAL_VALUE_CONSIDER_DIFFERENT_IDS.compareElementNames = false;
        putIntoNiceNames(REAL_VALUE_CONSIDER_DIFFERENT_IDS, "REAL_VALUE_CONSIDER_DIFFERENT_IDS");

        DEFAULT_FOR_EQUALS = NOT_LITERAL;
        DEFAULT_FOR_DIFF = IGNORE_METADATA_CONSIDER_DIFFERENT_IDS;
        DEFAULT_FOR_DELTA_APPLICATION = IGNORE_METADATA_CONSIDER_DIFFERENT_IDS;
    }

    private static void putIntoNiceNames(ParameterizedEquivalenceStrategy strategy, String name) {
        NICE_NAMES.put(strategy.getDescription(), name);
    }

    private boolean literalDomComparison;                   // L
    private boolean consideringOperationalData;             // O
    private boolean consideringContainerIds;                // I
    private boolean consideringDifferentContainerIds;       // i
    private boolean consideringValueOrigin;                 // o
    private boolean consideringReferenceFilters;            // F
    /**
     * Whether we consider resolutionTime and referentialIntegrity.
     */
    private boolean consideringReferenceOptions;            // r
    private boolean compareElementNames;                    // E

    /**
     *  Whether we hash runtime-schema items. Setting this to "true" is dangerous because these can hold unparsed (raw) values
     *  so hashing them can break equals-hashcode contract. (See MID-5851.) Note that although statically (compile-time)
     *  defined items can hold raw values as well the probability is much lesser.
     *
     *  This excludes e.g. attributes, extension items and resource/connector configuration elements.
     *
     *  On the other hand we must be a bit careful because if we exclude too much from hashing, we can run into
     *  performance issues (see MID-5852).
     */
    private boolean hashRuntimeSchemaItems;                 // R

    public String getDescription() {
        return (literalDomComparison ? "L" : "-") +
                (consideringOperationalData ? "O" : "-") +
                (consideringContainerIds ? "I" : "-") +
                (consideringDifferentContainerIds ? "i" : "-") +
                (consideringValueOrigin ? "o" : "-") +
                (consideringReferenceFilters ? "F" : "-") +
                (consideringReferenceOptions ? "r" : "-") +
                (compareElementNames ? "E" : "-") +
                (hashRuntimeSchemaItems ? "R" : "-");
    }

    @Override
    public boolean equals(Item<?, ?> first, Item<?, ?> second) {
        return first == second || first != null && second != null && first.equals(second, this);
    }

    @Override
    public boolean equals(PrismValue first, PrismValue second) {
        return first == second || first != null && second != null && first.equals(second, this);
    }

    @Override
    public int hashCode(Item<?, ?> item) {
        return item != null ? item.hashCode(this) : 0;
    }

    @Override
    public int hashCode(PrismValue value) {
        return value != null ? value.hashCode(this) : 0;
    }

    public boolean isConsideringDefinitions() {
        return false;
    }

    public boolean isConsideringElementNames() {
        return compareElementNames;
    }

    public void setCompareElementNames(boolean compareElementNames) {
        this.compareElementNames = compareElementNames;
    }

    public boolean isConsideringValueOrigin() {
        return consideringValueOrigin;
    }

    public void setConsideringValueOrigin(boolean consideringValueOrigin) {
        this.consideringValueOrigin = consideringValueOrigin;
    }

    public boolean isLiteralDomComparison() {
        return literalDomComparison;
    }

    public void setLiteralDomComparison(boolean literalDomComparison) {
        this.literalDomComparison = literalDomComparison;
    }

    public boolean isConsideringContainerIds() {
        return consideringContainerIds;
    }

    public void setConsideringContainerIds(boolean consideringContainerIds) {
        this.consideringContainerIds = consideringContainerIds;
    }

    public boolean isConsideringDifferentContainerIds() {
        return consideringDifferentContainerIds;
    }

    public void setConsideringDifferentContainerIds(boolean consideringDifferentContainerIds) {
        this.consideringDifferentContainerIds = consideringDifferentContainerIds;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isConsideringOperationalData() {
        return consideringOperationalData;
    }

    public void setConsideringOperationalData(boolean consideringOperationalData) {
        this.consideringOperationalData = consideringOperationalData;
    }

    public boolean isConsideringReferenceFilters() {
        return consideringReferenceFilters;
    }

    public void setConsideringReferenceFilters(boolean consideringReferenceFilters) {
        this.consideringReferenceFilters = consideringReferenceFilters;
    }

    public boolean isConsideringReferenceOptions() {
        return consideringReferenceOptions;
    }

    public void setConsideringReferenceOptions(boolean consideringReferenceOptions) {
        this.consideringReferenceOptions = consideringReferenceOptions;
    }

    public boolean isHashRuntimeSchemaItems() {
        return hashRuntimeSchemaItems;
    }

    public void setHashRuntimeSchemaItems(boolean hashRuntimeSchemaItems) {
        this.hashRuntimeSchemaItems = hashRuntimeSchemaItems;
    }

    @Override
    public String toString() {
        String desc = getDescription();
        String niceName = NICE_NAMES.get(desc);
        return niceName != null ? niceName + " (" + desc + ")" : "(" + desc + ")";
    }
}
