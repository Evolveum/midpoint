/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import static com.evolveum.midpoint.security.enforcer.impl.prism.PrismEntityCoverage.*;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.enforcer.impl.ValueSelectorEvaluation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemValueSelectorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameKeyedMap;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.security.enforcer.impl.AuthorizationEvaluation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Informs whether given {@link PrismValue} and its contained sub-items are covered in the specified context.
 *
 * (The context can be e.g. items/values that are allowed or denied by a particular operation.)
 *
 * @see PrismItemCoverageInformation
 */
class PrismValueCoverageInformation implements PrismEntityCoverageInformation {

    /** Match information for specified sub-items. */
    @NotNull private final NameKeyedMap<ItemName, PrismItemCoverageInformation> itemsMap = new NameKeyedMap<>();

    /**
     * If `true`, then items that are not mentioned are considered as {@link PrismEntityCoverage#FULL}.
     * If `false` they are considered as {@link PrismEntityCoverage#NONE}.
     */
    private boolean defaultIsFullMatch;

    private PrismValueCoverageInformation(boolean defaultIsFullMatch) {
        this.defaultIsFullMatch = defaultIsFullMatch;
    }

    static PrismValueCoverageInformation fullCoverage() {
        return new PrismValueCoverageInformation(true);
    }

    static PrismValueCoverageInformation noCoverage() {
        return new PrismValueCoverageInformation(false);
    }

    public @NotNull PrismEntityCoverage getCoverage() {
        if (itemsMap.isEmpty()) {
            return defaultIsFullMatch ? FULL : NONE;
        } else {
            // We do not know if the object really contains something of interest. It may or may not.
            return PARTIAL;
        }
    }

    private boolean isPositive() {
        return !defaultIsFullMatch;
    }

    @NotNull PrismItemCoverageInformation getItemCoverageInformation(@NotNull ItemName name) {
        var forItem = itemsMap.get(name);
        if (forItem != null) {
            return forItem;
        }
        return defaultIsFullMatch ? PrismItemCoverageInformation.fullCoverage() : PrismItemCoverageInformation.noCoverage();
    }

    @NotNull static PrismValueCoverageInformation forAuthorization(
            PrismObject<? extends ObjectType> object, @NotNull AuthorizationEvaluation evaluation)
            throws ConfigurationException {
        var authorization = evaluation.getAuthorization();
        var positives = authorization.getItems();
        var negatives = authorization.getExceptItems();
        List<ItemValueSelectorType> valueSelectors = authorization.getItemValueSelectors();
        if (!positives.isEmpty() || !valueSelectors.isEmpty()) {
            configCheck(negatives.isEmpty(),
                    "'item'/'itemValue' and 'exceptItem' cannot be combined: %s (%s) vs %s in %s",
                    positives, valueSelectors, negatives, authorization);
            var coverage = forPositivePaths(new PathSet(positives));
            for (ItemValueSelectorType valueSelector : valueSelectors) {
                coverage.merge(forValueSelector(valueSelector, object, evaluation));
            }
            return coverage;
        } else {
            return forNegativePaths(new PathSet(negatives)); // may or may not be empty
        }
    }

    // FIXME temporary implementation
    private static PrismValueCoverageInformation forValueSelector(
            ItemValueSelectorType valueSelector,
            PrismObject<? extends ObjectType> object,
            AuthorizationEvaluation evaluation) throws ConfigurationException {
        var path = configNonNull(valueSelector.getPath(), () -> "no path").getItemPath(); // TODO error location
        configCheck(!path.isEmpty(), "Path cannot be empty in %s", valueSelector); // TODO error location
        var item = object.findItem(path);
        if (item == null) {
            return PrismValueCoverageInformation.noCoverage();
        }
        List<PrismValue> matching = new ArrayList<>();
        for (PrismValue value : item.getValues()) {
            ValueSelectorEvaluation selectorEvaluation = new ValueSelectorEvaluation(value, valueSelector, object, evaluation);
            if (selectorEvaluation.matches()) {
                matching.add(value);
            }
        }
        if (matching.isEmpty()) {
            return PrismValueCoverageInformation.noCoverage();
        }

        var root = PrismValueCoverageInformation.noCoverage();
        var current = root;
        PrismItemCoverageInformation last = null;
        List<?> segments = path.getSegments();
        assert !segments.isEmpty();
        for (Object segment : segments) {
            ItemName name = ItemPath.toName(segment);
            PrismValueCoverageInformation next = PrismValueCoverageInformation.noCoverage();
            last = PrismItemCoverageInformation.single(next);
            current.itemsMap.put(name, last);
            current = next;
        }
        for (PrismValue matchingValue : matching) {
            // TODO only selected items/except-for-items/values
            last.addForValue(matchingValue, PrismValueCoverageInformation.fullCoverage());
        }
        return root;
    }

    private static PrismValueCoverageInformation forPositivePaths(PathSet positives) {
        if (positives.contains(ItemPath.EMPTY_PATH)) {
            return fullCoverage();
        }
        var coverage = noCoverage();
        for (Map.Entry<ItemName, PathSet> entry : positives.factor().entrySet()) {
            ItemName first = entry.getKey();
            PathSet rests = entry.getValue();
            coverage.itemsMap.put(
                    first,
                    PrismItemCoverageInformation.single(
                            forPositivePaths(rests)));
        }
        return coverage;
    }

    private static PrismValueCoverageInformation forNegativePaths(PathSet negatives) {
        if (negatives.contains(ItemPath.EMPTY_PATH)) {
            // "except for" item means that all its sub-items are excluded
            return noCoverage();
        }
        var coverage = fullCoverage();
        for (Map.Entry<ItemName, PathSet> entry : negatives.factor().entrySet()) {
            ItemName first = entry.getKey();
            PathSet rests = entry.getValue();
            coverage.itemsMap.put(
                    first,
                    PrismItemCoverageInformation.single(
                            forNegativePaths(rests)));
        }
        return coverage;
    }

    public void merge(@NotNull PrismValueCoverageInformation increment) {
        if (isPositive()) {
            if (increment.isPositive()) {
                mergePositiveIntoPositive(increment);
            } else {
                mergeNegativeIntoPositive(increment);
            }
        } else {
            if (increment.isPositive()) {
                mergePositiveIntoNegative(increment);
            } else {
                mergeNegativeIntoNegative(increment);
            }
        }
    }

    private void mergePositiveIntoPositive(PrismValueCoverageInformation increment) {
        for (var iEntry : increment.itemsMap.entrySet()) {
            ItemName iFirst = iEntry.getKey();
            PrismItemCoverageInformation iCoverage = iEntry.getValue();
            itemsMap
                    .computeIfAbsent(iFirst, k -> PrismItemCoverageInformation.noCoverage())
                    .merge(iCoverage);
        }
    }

    private void mergePositiveIntoNegative(PrismValueCoverageInformation increment) {
        for (var iEntry : increment.itemsMap.entrySet()) {
            ItemName iFirst = iEntry.getKey();
            PrismItemCoverageInformation iCoverage = iEntry.getValue();
            PrismItemCoverageInformation eCoverage = itemsMap.get(iFirst);
            if (eCoverage == null) {
                // New item is not mentioned in existing -> it is already fully covered there (so let's ignore it)
            } else {
                eCoverage.merge(iCoverage);
            }
        }
    }

    private void mergeNegativeIntoPositive(PrismValueCoverageInformation increment) {

        // Transposing to "merge positive into negative": this -> copy (positive), increment -> this (negative)

        PrismValueCoverageInformation copy = new PrismValueCoverageInformation(this.defaultIsFullMatch);
        copy.itemsMap.putAll(this.itemsMap);
        assert copy.isPositive();

        this.defaultIsFullMatch = increment.defaultIsFullMatch;
        this.itemsMap.clear();
        this.itemsMap.putAll(increment.itemsMap);
        assert !this.isPositive();

        mergePositiveIntoNegative(copy);
    }

    private void mergeNegativeIntoNegative(PrismValueCoverageInformation increment) {
        // We preserve only items declared in both coverages.
        // (The ones mentioned on only one side are fully covered by the other side!)
        for (var eEntry : List.copyOf(itemsMap.entrySet())) {
            ItemName eFirst = eEntry.getKey();
            PrismItemCoverageInformation eCoverage = eEntry.getValue();
            PrismItemCoverageInformation iCoverage = increment.itemsMap.get(eFirst);
            if (iCoverage == null) {
                itemsMap.remove(eFirst);
            } else {
                eCoverage.merge(iCoverage);
            }
        }
    }

    @Override
    public String debugDump(int indent) {
        boolean empty = itemsMap.isEmpty();
        String label;
        if (empty) {
            label = defaultIsFullMatch ? "FULL COVERAGE" : "NO COVERAGE";
        } else {
            label = defaultIsFullMatch ? "DEFAULT: FULL COVERAGE (all-except-for)" : "DEFAULT: NO COVERAGE";
        }
        var sb = DebugUtil.createTitleStringBuilder(
                String.format("Prism value coverage information [%s]: %s\n",
                        getClass().getSimpleName(),
                        label),
                indent);
        if (!empty) {
            DebugUtil.debugDumpWithLabel(sb, "Individual sub-items", itemsMap, indent + 1);
        }
        return sb.toString();
    }
}
