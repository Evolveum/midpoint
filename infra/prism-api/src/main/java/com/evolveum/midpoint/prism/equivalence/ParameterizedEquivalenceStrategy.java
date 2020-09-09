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
import java.util.Objects;

/**
 *  Implementation of EquivalenceStrategy that uses a parametrization of built-in equals/hashCode/diff methods.
 *
 *  These strategies are still in progress and (most probably) will be changed.
 *
 *     L = literalDomComparison
 *
 *         Compares DOM nodes literally. Currently this means that the comparison considers namespace prefixes.
 *         Also (a bit unrelated to DOM): when comparing relations, treats "null" and "org:default" relations as different.
 *
 *     E = consideringElementNames
 *
 *         Takes item names into account. So, for example, "c:user ..." is different from "x:dummy xsi:type=UserType ...".
 *         It looks like a negligible difference that is good to ignore, but beware: this also ensures that
 *         uid=jack attribute is different from name=jack attribute, when comparing attributes as prism properties.
 *         (When comparing them in their containers, element names are treated as different automatically.)
 *
 *     O = consideringOperationalData
 *
 *         Takes operational items into account (when comparing PCVs). Assumes that the definition is set,
 *         otherwise operational status cannot be determined.
 *
 *         Currently, when calling operationalItem1.equals(operationalItem2), the fact that "root" items are operational,
 *         does not play any role. This could change in the future. See TestParseDiffPath.checkComparisonOfOperationalItems.
 *
 *     I = consideringContainerIds
 *
 *         When comparing PCVs we require their IDs are equal. So (e.g.) null vs. null is OK, 123 vs. 123 is OK,
 *         null vs. 123 is not OK, 123 vs. 456 is not OK.
 *
 *     i = consideringDifferentContainerIds
 *
 *         When comparing PCVs we require their IDs are not contradicting. So (e.g.) null vs. 123 is OK, null vs. null is OK,
 *         123 vs. 456 is not OK.
 *
 *     F = consideringReferenceFilters
 *
 *         When comparing references, should we compare also reference filters? (An exception is the case when both OIDs
 *         are null: in that case we always compare the references.)
 *
 *     r = consideringReferenceOptions (resolution time, reference integrity)
 *
 *         When comparing references, should we compare also resolution options, i.e. resolution time and referential integrity?
 *
 *     M = consideringValueMetadata
 *
 *         When comparing prism values, should we take into account value metadata? If yes, we apply the same strategy to them
 *         as was used for data comparison.
 *
 *
 *  Summary of individual strategies:
 *
 *     LITERAL                                  L E O I i F r M
 *     DATA                                     - E O I i F r M
 *     IGNORE_METADATA                          - E - - - F r -
 *     REAL_VALUE_CONSIDER_DIFFERENT_IDS        - - - - i - - -
 *     REAL_VALUE                               - - - - - - - -
 *
 *
 *  Open questions
 *  ==============
 *
 *  The difference between REAL_VALUE and IGNORE_METADATA is to be established yet.
 *
 *  Basically, REAL_VALUE is oriented towards the effective content of the item or value.
 *  Contrary to IGNORE_METADATA it ignores reference filters (if OID is present) and
 *  reference resolution options (resolution time, reference integrity).
 *
 */
@SuppressWarnings({ "unused", "DuplicatedCode" })
public class ParameterizedEquivalenceStrategy implements EquivalenceStrategy, Cloneable {

    static ParameterizedEquivalenceStrategy literal() {
        ParameterizedEquivalenceStrategy literal = new ParameterizedEquivalenceStrategy();
        literal.literalDomComparison = true;
        literal.consideringElementNames = true;
        literal.consideringOperationalData = true;
        literal.consideringContainerIds = true;
        literal.consideringDifferentContainerIds = true;
        literal.consideringReferenceFilters = true;
        literal.consideringReferenceOptions = true;
        literal.consideringValueMetadata = true;
        return literal;
    }

    static ParameterizedEquivalenceStrategy data() {
        ParameterizedEquivalenceStrategy data = new ParameterizedEquivalenceStrategy();
        data.literalDomComparison = false;
        // This is necessary for correct equals/hashCode working for prism items (namely, attributes
        // and extension items).
        data.consideringElementNames = true;
        data.consideringOperationalData = true;
        data.consideringContainerIds = true;
        data.consideringDifferentContainerIds = true;
        data.consideringReferenceFilters = true;
        data.consideringReferenceOptions = true;
        data.consideringValueMetadata = true;
        return data;
    }

    static ParameterizedEquivalenceStrategy ignoreMetadata() {
        ParameterizedEquivalenceStrategy ignoreMetadata = new ParameterizedEquivalenceStrategy();
        ignoreMetadata.literalDomComparison = false;
        ignoreMetadata.consideringElementNames = true;
        ignoreMetadata.consideringOperationalData = false;
        ignoreMetadata.consideringContainerIds = false;
        ignoreMetadata.consideringDifferentContainerIds = false;
        ignoreMetadata.consideringReferenceFilters = true;
        ignoreMetadata.consideringReferenceOptions = true;         // ok?
        ignoreMetadata.consideringValueMetadata = false;
        return ignoreMetadata;
    }

    @Experimental
    static ParameterizedEquivalenceStrategy realValueConsiderDifferentIds() {
        ParameterizedEquivalenceStrategy realValueConsiderDifferentIds = new ParameterizedEquivalenceStrategy();
        realValueConsiderDifferentIds.literalDomComparison = false;
        realValueConsiderDifferentIds.consideringElementNames = false;
        realValueConsiderDifferentIds.consideringOperationalData = false;
        realValueConsiderDifferentIds.consideringContainerIds = false;
        realValueConsiderDifferentIds.consideringDifferentContainerIds = true;
        realValueConsiderDifferentIds.consideringReferenceFilters = false;
        realValueConsiderDifferentIds.consideringReferenceOptions = false;
        realValueConsiderDifferentIds.consideringValueMetadata = false;
        return realValueConsiderDifferentIds;
    }

    static ParameterizedEquivalenceStrategy realValue() {
        ParameterizedEquivalenceStrategy realValue = new ParameterizedEquivalenceStrategy();
        realValue.literalDomComparison = false;
        realValue.consideringElementNames = false;
        realValue.consideringOperationalData = false;
        realValue.consideringContainerIds = false;
        realValue.consideringDifferentContainerIds = false;
        realValue.consideringReferenceFilters = false;
        realValue.consideringReferenceOptions = false;
        realValue.consideringValueMetadata = false;
        return realValue;
    }

    public static final ParameterizedEquivalenceStrategy DEFAULT_FOR_EQUALS = data();
    public static final ParameterizedEquivalenceStrategy FOR_DELTA_ADD_APPLICATION = realValueConsiderDifferentIds();
    public static final ParameterizedEquivalenceStrategy FOR_DELTA_DELETE_APPLICATION = realValueConsiderDifferentIds();

    private static final Map<String, String> NICE_NAMES = new HashMap<>();
    static {
        putIntoNiceNames(literal(), "LITERAL");
        putIntoNiceNames(data(), "DATA");
        putIntoNiceNames(ignoreMetadata(), "IGNORE_METADATA");
        putIntoNiceNames(realValue(), "REAL_VALUE");
        putIntoNiceNames(realValueConsiderDifferentIds(), "REAL_VALUE_CONSIDER_DIFFERENT_IDS");
    }

    private static void putIntoNiceNames(ParameterizedEquivalenceStrategy strategy, String name) {
        NICE_NAMES.put(strategy.getDescription(), name);
    }

    private boolean literalDomComparison;                   // L
    private boolean consideringElementNames;                // E
    private boolean consideringOperationalData;             // O
    private boolean consideringContainerIds;                // I
    private boolean consideringDifferentContainerIds;       // i
    private boolean consideringReferenceFilters;            // F
    /**
     * Whether we consider resolutionTime and referentialIntegrity.
     */
    private boolean consideringReferenceOptions;            // r

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

    private boolean consideringValueMetadata;                   // M

    public static ParameterizedEquivalenceStrategy getLiteral() {
        return LITERAL;
    }


    public String getDescription() {
        return (literalDomComparison ? "L" : "-") +
                (consideringElementNames ? "E" : "-") +
                (consideringOperationalData ? "O" : "-") +
                (consideringContainerIds ? "I" : "-") +
                (consideringDifferentContainerIds ? "i" : "-") +
                (consideringReferenceFilters ? "F" : "-") +
                (consideringReferenceOptions ? "r" : "-") +
                (hashRuntimeSchemaItems ? "R" : "-") +
                (consideringValueMetadata ? "M" : "-");
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
        return consideringElementNames;
    }

    public void setConsideringElementNames(boolean compareElementNames) {
        this.consideringElementNames = compareElementNames;
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

    public boolean isConsideringValueMetadata() {
        return consideringValueMetadata;
    }

    public void setConsideringValueMetadata(boolean consideringValueMetadata) {
        this.consideringValueMetadata = consideringValueMetadata;
    }

    @Override
    public String toString() {
        String desc = getDescription();
        String niceName = NICE_NAMES.get(desc);
        return niceName != null ? niceName + " (" + desc + ")" : "(" + desc + ")";
    }

    public ParameterizedEquivalenceStrategy clone() {
        try {
            return (ParameterizedEquivalenceStrategy) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public ParameterizedEquivalenceStrategy exceptForValueMetadata() {
        if (consideringValueMetadata) {
            ParameterizedEquivalenceStrategy clone = clone();
            clone.consideringValueMetadata = false;
            return clone;
        } else {
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ParameterizedEquivalenceStrategy))
            return false;
        ParameterizedEquivalenceStrategy that = (ParameterizedEquivalenceStrategy) o;
        return literalDomComparison == that.literalDomComparison &&
                consideringOperationalData == that.consideringOperationalData &&
                consideringContainerIds == that.consideringContainerIds &&
                consideringDifferentContainerIds == that.consideringDifferentContainerIds &&
                consideringReferenceFilters == that.consideringReferenceFilters &&
                consideringReferenceOptions == that.consideringReferenceOptions &&
                consideringElementNames == that.consideringElementNames &&
                hashRuntimeSchemaItems == that.hashRuntimeSchemaItems &&
                consideringValueMetadata == that.consideringValueMetadata;
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(literalDomComparison, consideringOperationalData, consideringContainerIds, consideringDifferentContainerIds,
                        consideringReferenceFilters, consideringReferenceOptions, consideringElementNames, hashRuntimeSchemaItems,
                        consideringValueMetadata);
    }
}
