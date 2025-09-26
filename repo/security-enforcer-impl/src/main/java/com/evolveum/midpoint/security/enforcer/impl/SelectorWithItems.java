/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.security.api.Authorization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.selector.spec.ParentClause;
import com.evolveum.midpoint.schema.selector.spec.TypeClause;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * This is a selector + items (positive/negative) specification.
 * Sometimes we call it also an "extended selector"; until better term is found.
 *
 * Its meaning is to select some values (object or sub-object), and all or just some of their sub-items.
 *
 * It is directly derived from an {@link Authorization}.
 *
 * It is a separate Java object, because it sometimes has to be adjusted to match a specific use
 * (value matching or filter creation). See {@link #adjustToSubObjectFilter(Class)} and {@link #asTieredSelectors(Class)} methods.
 *
 * @see TieredSelectorWithItems
 */
public class SelectorWithItems {

    @NotNull private final ValueSelector selector;
    @NotNull private final PathSet positives;
    @NotNull private final PathSet negatives;

    /** TODO what kind of description is here? */
    @NotNull private final String description;

    private SelectorWithItems(
            @NotNull ValueSelector selector,
            @NotNull PathSet positives,
            @NotNull PathSet negatives,
            @NotNull String description) {
        this.selector = selector;
        this.positives = positives;
        this.negatives = negatives;
        this.description = description;
    }

    /** TODO explain the meaning of this method i.e. why there are no paths? */
    public static SelectorWithItems of(@NotNull ValueSelector selector) {
        return new SelectorWithItems(selector, PathSet.of(), PathSet.of(), "");
    }

    public static SelectorWithItems of(
            @NotNull ValueSelector selector,
            @NotNull PathSet positives,
            @NotNull PathSet negatives,
            @NotNull String description) throws ConfigurationException {
        configCheck(positives.isEmpty() || negatives.isEmpty(),
                "'item' and 'exceptItem' cannot be combined: %s vs %s in %s",
                positives, negatives, description);
        return new SelectorWithItems(selector, positives, negatives, description);
    }

    public static @NotNull SelectorWithItems all() {
        return new SelectorWithItems(ValueSelector.empty(), PathSet.of(), PathSet.of(), "");
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull ValueSelector getSelector() {
        return selector;
    }

    public @NotNull PathSet getPositives() {
        return positives;
    }

    public @NotNull PathSet getNegatives() {
        return negatives;
    }

    /**
     * Adjusts this selector to match given (presumably sub-object) filter type.
     *
     * For example, if the original selector-with-items is targeted at {@link UserType} (maybe allowing some parts of the users'
     * assignments), and we are constructing a filter for assignments ({@link AssignmentType}), we may try to adapt the selector
     * by stepping down: creating a new selector for {@link AssignmentType} with a parent clause pointing to the original
     * selector.
     */
    <T> SelectorWithItems adjustToSubObjectFilter(@NotNull Class<T> filterType) throws SchemaException, ConfigurationException {

        Class<?> selectorType = selector.getEffectiveType();
        if (selectorType.isAssignableFrom(filterType) || filterType.isAssignableFrom(selectorType)) {
            return this; // There is an overlap
        }

        // We could check whether filter is already at the root level. But the getCandidateAdjustments will handle that case.

        // We are guessing what object/path we are selecting. For more modern searches (e.g. assignments) this should
        // be part of the original filter, so we ideally should check that. TODO implement this
        for (Adjustment candidateAdjustment : getCandidateAdjustments(filterType)) {
            var newParentType = PrismContext.get().getSchemaRegistry()
                    .selectMoreSpecific(selector.getTypeName(), candidateAdjustment.typeName);
            if (newParentType == null) {
                continue; // No intersection -> try another candidate
            }

            // It may happen that the selector is completely unrelated. For example,
            //
            // 1. filterType=AssignmentType,
            // 2. the enhanced selector pointing to UserType (so far so good)
            // but 3. with the paths of (e.g.) givenName and familyName.
            //
            // If we'd not exclude it here, the resulting selector would have no paths ("remainder" computation would provide
            // empty sets), and that would be wrong - as that indicates that the whole content is covered.
            if (!overlapsItem(candidateAdjustment.path)) {
                continue;
            }

            return new SelectorWithItems(
                    ValueSelector.of(
                            TypeClause.of(filterType),
                            ParentClause.of(
                                    selector.sameWithType(newParentType),
                                    candidateAdjustment.path)),
                    positives.remainder(candidateAdjustment.path),
                    negatives.remainder(candidateAdjustment.path),
                    "adjusted " + description);
        }

        return null;
    }

    /** Returns `true` it this specification (at least partially) covers given item. */
    private boolean overlapsItem(@NotNull ItemPath path) {
        if (!positives.isEmpty()) {
            return positives.containsRelated(path);
        } else {
            // Negative scenario: we are OK unless the whole item is excluded
            return !negatives.containsSubpathOrEquivalent(path);
        }
    }

    /**
     * In the future, we should take also the original filter (containing probably `ownedBy` clause)
     * into account when guessing the selector use.
     */
    private static List<Adjustment> getCandidateAdjustments(@NotNull Class<?> currentType) {
        if (AssignmentType.class.equals(currentType)) {
            return Adjustment.ADJUSTMENTS_FOR_ASSIGNMENT;
        } else if (CaseWorkItemType.class.equals(currentType)) {
            return Adjustment.ADJUSTMENTS_FOR_CASE_WORK_ITEM;
        } else if (AccessCertificationCaseType.class.equals(currentType)) {
            return Adjustment.ADJUSTMENTS_FOR_CERT_CASE;
        } else if (AccessCertificationWorkItemType.class.equals(currentType)) {
            return Adjustment.ADJUSTMENTS_FOR_CERT_WORK_ITEM;
        } else {
            return Adjustment.NO_ADJUSTMENTS;
        }
    }

    private static @Nullable Adjustment getAdjustmentToRoot(@NotNull Class<?> currentType) {
        var candidates = getCandidateAdjustments(currentType);
        return candidates.isEmpty() ? null : candidates.get(candidates.size() - 1);
    }

    /**
     * Decomposes the current selector into a matching tiers of {@link TieredSelectorWithItems}, starting at the specified
     * root type.
     */
    @Nullable TieredSelectorWithItems asTieredSelectors(@NotNull Class<? extends Objectable> rootType)
            throws ConfigurationException {

        if (!selector.isSubObject()) {
            // No parent expected here; we remove the parent clause just to be sure.
            return TieredSelectorWithItems.withNoChild(
                    this.withParentClauseRemoved());
        }

        var builder = new TieredSelectorBuilder(this);

        // First, let us step up through explicit parent clauses.
        ValueSelector currentSelector = selector;
        for (;;) {
            ParentClause explicitParent = currentSelector.getParentClause();
            if (explicitParent == null) {
                break;
            }
            builder.addParent(explicitParent);
            currentSelector = explicitParent.getParentSelector();
        }

        // If we are not on top yet, let us try to use implicit adjustments.
        // FIXME this will be probably removed, as we will require explicit parent clauses
        var currentType = currentSelector.getEffectiveType();
        Class<?> builtRootType;
        if (ObjectTypeUtil.isObjectable(currentType)) {
            builtRootType = currentType;
        } else {
            var adjustmentToRoot = getAdjustmentToRoot(currentType);
            if (adjustmentToRoot == null) {
                return null;
            }
            ValueSelector rootSelector = ValueSelector.forType(adjustmentToRoot.typeName);
            builder.addParent(
                    ParentClause.of(rootSelector, adjustmentToRoot.path));
            builtRootType = rootSelector.getEffectiveType();
        }

        // Now we are on top, let us finish.
        if (builtRootType.isAssignableFrom(rootType)) {
            return builder.build();
        } else {
            return null;
        }
    }

    private @NotNull SelectorWithItems withParentClauseRemoved() {
        return withSelectorReplaced(
                selector.withParentRemoved());
    }

    private SelectorWithItems withSelectorReplaced(@NotNull ValueSelector newSelector) {
        return new SelectorWithItems(newSelector, positives, negatives, description);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "selector=" + selector +
                ", positives=" + positives +
                ", negatives=" + negatives +
                ", description='" + description + '\'' +
                '}';
    }

    boolean isParentLess() {
        return selector.isParentLess();
    }

    boolean hasOverlapWith(@NotNull Class<?> requiredType) {
        return selector.getEffectiveType().isAssignableFrom(requiredType)
                || requiredType.isAssignableFrom(selector.getEffectiveType());
    }

    private record Adjustment(@NotNull QName typeName, @NotNull ItemPath path) {

            // Adjustment to root should be the last one

            static final List<Adjustment> ADJUSTMENTS_FOR_ASSIGNMENT =
                    List.of(new Adjustment(
                            AssignmentHolderType.COMPLEX_TYPE,
                            AssignmentHolderType.F_ASSIGNMENT));

            static final List<Adjustment> ADJUSTMENTS_FOR_CASE_WORK_ITEM =
                    List.of(new Adjustment(
                            CaseType.COMPLEX_TYPE,
                            CaseType.F_WORK_ITEM));

            static final List<Adjustment> ADJUSTMENTS_FOR_CERT_CASE =
                    List.of(new Adjustment(
                            AccessCertificationCampaignType.COMPLEX_TYPE,
                            AccessCertificationCampaignType.F_CASE));

            static final List<Adjustment> ADJUSTMENTS_FOR_CERT_WORK_ITEM =
                    List.of(
                            new Adjustment(
                                    AccessCertificationCaseType.COMPLEX_TYPE,
                                    AccessCertificationCaseType.F_WORK_ITEM),
                            new Adjustment(
                                    AccessCertificationCampaignType.COMPLEX_TYPE,
                                    AccessCertificationCampaignType.F_CASE.append(AccessCertificationCaseType.F_WORK_ITEM)));

            static final List<Adjustment> NO_ADJUSTMENTS = List.of();

        @Override
        public String toString() {
            return typeName.getLocalPart() + ":" + path;
        }
    }

    /** The {@link TieredSelectorWithItems} has everything final (by design), so we need to build it in a separate class. */
    private static class TieredSelectorBuilder {

        @NotNull private final SelectorWithItems base;

        /** Reusing (misusing?) {@link ParentClause} as a path-selector pair container. Rewrite if needed. */
        @NotNull private final List<ParentClause> stepsUp = new ArrayList<>();

        private TieredSelectorBuilder(@NotNull SelectorWithItems base) {
            this.base = base;
        }

        void addParent(@NotNull ParentClause stepUp) {
            stepsUp.add(stepUp);
        }

        @NotNull TieredSelectorWithItems build() {
            TieredSelectorWithItems current = TieredSelectorWithItems.withNoChild(base.withParentClauseRemoved());
            for (ParentClause step : stepsUp) {
                ItemPath pathToChild = step.getPath();
                List<?> segments = pathToChild.getSegments();
                stateCheck(!segments.isEmpty(), "No segments? current: %s, step: %s", current, step);
                for (int i = segments.size() - 1; i > 0; i--) {
                    current = TieredSelectorWithItems.withChild(
                            ValueSelector.empty(),
                            ItemPath.toName(segments.get(i)),
                            current);
                }
                current = TieredSelectorWithItems.withChild(
                        step.getParentSelector().withParentRemoved(),
                        ItemPath.toName(segments.get(0)),
                        current);
            }
            return current;
        }
    }
}
