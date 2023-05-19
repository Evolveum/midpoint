/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl.prism;

import static com.evolveum.midpoint.security.enforcer.impl.prism.PrismEntityCoverage.*;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.*;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameKeyedMap;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.impl.AuthorizationEvaluation;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationParentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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

    /** Returns `null` if the authorization is irrelevant for the current object. */
    static @Nullable PrismValueCoverageInformation forAuthorization(
            PrismObject<? extends ObjectType> object, @NotNull AuthorizationEvaluation evaluation)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        Collection<SelectorChainSegment> roots = createSelectorChains(object, evaluation);
        if (!roots.isEmpty()) {
            PrismValueCoverageInformation merged = PrismValueCoverageInformation.noCoverage();
            for (SelectorChainSegment root : roots) {
                merged.merge(
                        forSegment(object.getValue(), object, root, evaluation));
            }
            return merged;
        } else {
            return null;
        }
    }

    private static Collection<SelectorChainSegment> createSelectorChains(
            PrismObject<? extends ObjectType> object, @NotNull AuthorizationEvaluation evaluation)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        Authorization autz = evaluation.getAuthorization();
        var positives = new PathSet(autz.getItems());
        var negatives = new PathSet(autz.getExceptItems());
        List<SelectorChainSegment> roots = new ArrayList<>();
        if (autz.getObjectSelectors().isEmpty()) {
            // Quite special case (e.g. for #all autz)
            // TODO is this OK?
            return List.of(
                    SelectorChainSegment.create(
                            ItemPath.EMPTY_PATH, null, new AuthorizationObjectSelectorType(),
                            positives, negatives, evaluation));
        } else {
            for (AuthorizationObjectSelectorType objectSelector : autz.getObjectSelectors()) {
                CollectionUtils.addIgnoreNull(
                        roots,
                        createSelectorChain(
                                ItemPath.EMPTY_PATH, null, objectSelector, positives, negatives, object, evaluation));
            }
        }
        return roots;
    }

    private static SelectorChainSegment createSelectorChain(
            ItemPath path,
            SelectorChainSegment nextSegment,
            AuthorizationObjectSelectorType objectSelector,
            PathSet positives,
            PathSet negatives,
            PrismObject<? extends ObjectType> object,
            AuthorizationEvaluation evaluation)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (evaluation.isSelectorApplicable(objectSelector, object, Set.of(), "TODO")) {
            return SelectorChainSegment.create(path, nextSegment, objectSelector, positives, negatives, evaluation);
        }

        ParentSelector parentSelector = getParentSelector(objectSelector);
        if (parentSelector == null) {
            return null;
        }

        var child = SelectorChainSegment.create(path, nextSegment, objectSelector, positives, negatives, evaluation);
        return createSelectorChain(
                parentSelector.getPath(),
                child,
                parentSelector.getSelector(),
                PathSet.of(),
                PathSet.of(),
                object,
                evaluation);
    }

    private static @Nullable ParentSelector getParentSelector(
            @NotNull AuthorizationObjectSelectorType objectSelector) throws ConfigurationException {
        AuthorizationParentSelectorType explicit = objectSelector.getParent();
        if (explicit != null) {
            return ParentSelector.forBean(explicit);
        } else {
            return ParentSelector.implicit(objectSelector.getType());
        }
    }

    private static PrismValueCoverageInformation forSegment(
            @NotNull PrismValue value,
            @NotNull PrismObject<? extends ObjectType> object,
            @NotNull SelectorChainSegment segment,
            @NotNull AuthorizationEvaluation evaluation) throws ConfigurationException, SchemaException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException, ObjectNotFoundException {

        if (!evaluation.isSelectorApplicable(segment.selector, value, Set.of(), "TODO")) {
            return PrismValueCoverageInformation.noCoverage();
        }

        if (!segment.positives.isEmpty() || segment.next != null) {
            var coverage = forPositivePaths(segment.positives);
            if (segment.next != null) {
                coverage.merge(forNextSegment(segment.path, segment.next, value, object, evaluation));
            }
            return coverage;
        } else {
            return forNegativePaths(segment.negatives);
        }
    }

    private static PrismValueCoverageInformation forNextSegment(
            ItemPath path,
            SelectorChainSegment nextSegment,
            PrismValue parentValue,
            PrismObject<? extends ObjectType> object,
            AuthorizationEvaluation evaluation)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        if (!(parentValue instanceof PrismContainerValue<?>)) {
            return PrismValueCoverageInformation.noCoverage();
        }
        var pcv = (PrismContainerValue<?>) parentValue;
        var item = pcv.findItem(path);
        if (item == null) {
            // Item is not present in the PCV, the coverage needs no update.
            return PrismValueCoverageInformation.noCoverage();
        }

        var root = PrismValueCoverageInformation.noCoverage();
        var itemCoverageInformation = createItemCoverageInformationObject(root, path); // TODO evaluate lazily

        for (PrismValue itemValue : item.getValues()) {
            PrismValueCoverageInformation subValueCoverage =
                    forSegment(itemValue, object, nextSegment, evaluation);
            if (subValueCoverage.getCoverage() != NONE) {
                itemCoverageInformation.addForValue(itemValue, subValueCoverage);
            }
        }
        if (itemCoverageInformation.getCoverage() == NONE) {
            return PrismValueCoverageInformation.noCoverage(); // to avoid useless root->item chain
        } else {
            return root;
        }
    }

    // TODO explain
    private static PrismItemCoverageInformation createItemCoverageInformationObject(
            PrismValueCoverageInformation root, ItemPath path) {
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
        return last;
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

    // TODO rename
    private static class SelectorChainSegment {
        /** May be empty */
        @NotNull private final ItemPath path;
        @Nullable private final SelectorChainSegment next;
        @NotNull private final AuthorizationObjectSelectorType selector;
        @NotNull private final PathSet positives;
        @NotNull private final PathSet negatives;

        private SelectorChainSegment(
                @NotNull ItemPath path,
                @Nullable SelectorChainSegment next,
                @NotNull AuthorizationObjectSelectorType selector,
                @NotNull PathSet positives,
                @NotNull PathSet negatives) {
            this.path = path;
            this.next = next;
            this.selector = selector;
            this.positives = positives;
            this.negatives = negatives;
        }

        static SelectorChainSegment create(
                ItemPath path, SelectorChainSegment nextSegment, AuthorizationObjectSelectorType objectSelector,
                PathSet positives, PathSet negatives, AuthorizationEvaluation evaluation) throws ConfigurationException {
            configCheck(positives.isEmpty() || negatives.isEmpty(),
                    "'item' and 'exceptItem' cannot be combined: %s vs %s in %s",
                    positives, negatives, evaluation.getAuthorization());
            return new SelectorChainSegment(path, nextSegment, objectSelector, positives, negatives);
        }
    }
}
