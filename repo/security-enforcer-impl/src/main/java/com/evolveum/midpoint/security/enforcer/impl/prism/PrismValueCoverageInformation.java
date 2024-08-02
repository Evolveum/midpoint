/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import static com.evolveum.midpoint.security.enforcer.impl.prism.PrismEntityCoverage.*;

import java.util.*;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.security.enforcer.impl.TieredSelectorWithItems;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.security.enforcer.impl.AuthorizationEvaluation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;

/**
 * Informs whether given {@link PrismValue} and its contained sub-items are covered in the specified context.
 *
 * (The context can be e.g. items/values that are allowed or denied by a particular operation, e.g. `#get`.)
 *
 * @see PrismItemCoverageInformation
 */
class PrismValueCoverageInformation implements PrismEntityCoverageInformation {

    /** Coverage information for specified sub-items (applies to container values only). */
    @NotNull private final NameKeyedMap<ItemName, PrismItemCoverageInformation> itemsMap = new NameKeyedMap<>();

    /**
     * - If `true`, then items that are not mentioned are considered as {@link PrismEntityCoverage#NONE}
     * (i.e., what is not mentioned, is not covered).
     * - If `false` they are considered as {@link PrismEntityCoverage#FULL}
     * (i.e., what is not mentioned, is covered in full).
     */
    private boolean positive;

    private boolean exceptMetadata;

    private PrismValueCoverageInformation(boolean positive, boolean exceptMetadata) {
        this.positive = positive;
        this.exceptMetadata = exceptMetadata;
    }

    static PrismValueCoverageInformation fullCoverage(boolean exceptMetadata) {
        return new PrismValueCoverageInformation(false, exceptMetadata);
    }

    static PrismValueCoverageInformation noCoverage(boolean exceptMetadata) {
        return new PrismValueCoverageInformation(true, exceptMetadata);
    }

    public @NotNull PrismEntityCoverage getCoverage() {
        if (itemsMap.isEmpty()) {
            if (positive) {
                return NONE;
            }
            return exceptMetadata ? PARTIAL : FULL;
        } else {
            // We do not know if the object really contains something of interest. It may or may not.
            return PARTIAL;
        }
    }

    private boolean isPositive() {
        return positive;
    }

    @Override
    public boolean isExceptMetadata() {
        return exceptMetadata;
    }

    @NotNull PrismItemCoverageInformation getItemCoverageInformation(@NotNull ItemName name) {
        var forItem = itemsMap.get(name);
        if (forItem != null) {
            return forItem;
        }
        return positive ? PrismItemCoverageInformation.noCoverage(exceptMetadata) : PrismItemCoverageInformation.fullCoverage(exceptMetadata);
    }

    @NotNull PrismValueCoverageInformation getValueCoverageInformation(@NotNull ItemPath nameOnlyPath) {
        if (nameOnlyPath.isEmpty()) {
            return this;
        } else {
            var itemInfo = getItemCoverageInformation(nameOnlyPath.firstNameOrFail());
            return itemInfo.getOtherValueCoverageInformation().getValueCoverageInformation(nameOnlyPath.rest());
        }
    }

    /**
     * Computes the coverage information for given `value` and authorization.
     * (Typically, to be merged with coverages of the same value from other authorizations.)
     *
     * Returns `null` if the authorization is irrelevant for the current value.
     */
    static @Nullable PrismValueCoverageInformation forAuthorization(
            @NotNull PrismObjectValue<?> value, @NotNull AuthorizationEvaluation evaluation)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        Collection<TieredSelectorWithItems> tieredSelectors = TieredSelectorWithItems.forAutzAndValue(value, evaluation);
        if (!tieredSelectors.isEmpty()) {
            PrismValueCoverageInformation merged = PrismValueCoverageInformation.noCoverage(false);
            int i = 0;
            for (TieredSelectorWithItems tieredSelector : tieredSelectors) {
                merged.merge(
                        forTieredSelector(evaluation.selectorId(i++), value, value, tieredSelector, evaluation));
            }
            return merged;
        } else {
            return null;
        }
    }

    private static PrismValueCoverageInformation forTieredSelector(
            @NotNull String id,
            @NotNull PrismValue value,
            @NotNull PrismValue rootValue,
            @NotNull TieredSelectorWithItems tieredSelector,
            @NotNull AuthorizationEvaluation evaluation) throws ConfigurationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException, ObjectNotFoundException {

        ValueSelector valueSelector = tieredSelector.getSelector();
        assert valueSelector.getParentClause() == null;

        if (!evaluation.isSelectorApplicable(id, valueSelector, value, "TODO")) {
            return PrismValueCoverageInformation.noCoverage(false);
        }

        PathSet positives = tieredSelector.getPositives();
        PathSet negatives = tieredSelector.getNegatives();
        boolean exceptMetadata = tieredSelector.isExceptMetadata();
        var linkToChild = tieredSelector.getLinkToChild();
        if (!positives.isEmpty() || linkToChild != null) {
            var coverage = forPositivePaths(positives, exceptMetadata);
            if (linkToChild != null) {
                coverage.merge(forChildTieredSelector(id + "v", linkToChild, value, rootValue, evaluation));
            }
            return coverage;
        } else {
            return forNegativePaths(negatives, exceptMetadata);
        }
    }

    private static PrismValueCoverageInformation forChildTieredSelector(
            String id,
            TieredSelectorWithItems.Link linkToChild,
            PrismValue parentValue,
            PrismValue rootValue,
            AuthorizationEvaluation evaluation)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        if (!(parentValue instanceof PrismContainerValue<?> pcv)) {
            return PrismValueCoverageInformation.noCoverage(false);
        }
        ItemPath childPath = linkToChild.getItemPath();
        Item<?, ?> item = pcv.findItem(childPath);
        if (item == null) {
            // Item is not present in the PCV, the coverage needs no update.
            return PrismValueCoverageInformation.noCoverage(false);
        }

        var parentCoverage = PrismValueCoverageInformation.noCoverage(false);
        var itemCoverageInformation = createItemCoverageInformationObject(parentCoverage, childPath); // TODO evaluate lazily

        int valId = 0;
        for (PrismValue itemValue : item.getValues()) {
            PrismValueCoverageInformation subValueCoverage =
                    forTieredSelector(id + ".val" + (valId++), itemValue, rootValue, linkToChild.getChild(), evaluation);
            if (subValueCoverage.getCoverage() != NONE) {
                itemCoverageInformation.addForValue(itemValue, subValueCoverage);
            }
        }
        if (itemCoverageInformation.getCoverage() == NONE) {
            return PrismValueCoverageInformation.noCoverage(false); // to avoid useless root->item chain
        } else {
            return parentCoverage;
        }
    }

    private static PrismItemCoverageInformation createItemCoverageInformationObject(
            PrismValueCoverageInformation root, ItemPath path) {
        var current = root;
        PrismItemCoverageInformation last = null;
        List<?> segments = path.getSegments();
        assert !segments.isEmpty();
        for (Object segment : segments) {
            ItemName name = ItemPath.toName(segment);
            PrismValueCoverageInformation next = PrismValueCoverageInformation.noCoverage(root.exceptMetadata);
            last = PrismItemCoverageInformation.single(next);
            current.itemsMap.put(name, last);
            current = next;
        }
        return last;
    }

    private static PrismValueCoverageInformation forPositivePaths(PathSet positives, boolean exceptMetadata) {
        if (positives.contains(ItemPath.EMPTY_PATH)) {
            return fullCoverage(exceptMetadata);
        }
        var coverage = noCoverage(exceptMetadata);
        for (Map.Entry<ItemName, PathSet> entry : positives.factor().entrySet()) {
            ItemName first = entry.getKey();
            PathSet rests = entry.getValue();
            coverage.itemsMap.put(
                    first,
                    PrismItemCoverageInformation.single(
                            forPositivePaths(rests, exceptMetadata)));
        }
        return coverage;
    }

    private static PrismValueCoverageInformation forNegativePaths(PathSet negatives, boolean exceptMetadata) {
        if (negatives.contains(ItemPath.EMPTY_PATH)) {
            // "except for" item means that all its sub-items are excluded
            return noCoverage(exceptMetadata);
        }
        var coverage = fullCoverage(exceptMetadata);
        for (Map.Entry<ItemName, PathSet> entry : negatives.factor().entrySet()) {
            ItemName first = entry.getKey();
            PathSet rests = entry.getValue();
            coverage.itemsMap.put(
                    first,
                    PrismItemCoverageInformation.single(
                            forNegativePaths(rests, exceptMetadata)));
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
        exceptMetadata = increment.exceptMetadata;
        for (var iEntry : increment.itemsMap.entrySet()) {
            ItemName iFirst = iEntry.getKey();
            PrismItemCoverageInformation iCoverage = iEntry.getValue();
            itemsMap
                    .computeIfAbsent(iFirst, k -> PrismItemCoverageInformation.noCoverage(exceptMetadata))
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

        PrismValueCoverageInformation copy = new PrismValueCoverageInformation(this.positive, this.exceptMetadata);
        copy.itemsMap.putAll(this.itemsMap);
        assert copy.isPositive();

        this.positive = increment.positive;
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
            label = positive ? "NO COVERAGE" : "FULL COVERAGE";
        } else {
            label = positive ? "DEFAULT: NO COVERAGE" : "DEFAULT: FULL COVERAGE (all-except-for)";
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

    public boolean hasItemCoverage(ItemName name) {
        return itemsMap.containsKey(name);
    }
}
