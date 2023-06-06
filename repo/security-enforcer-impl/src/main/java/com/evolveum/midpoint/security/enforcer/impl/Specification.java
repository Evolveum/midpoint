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
 *
 * It is a separate object, as it has to be adjusted to match a specific use (value matching or filter creation).
 * See {@link #adjust(Class)}.
 *
 * TODO provide better class name
 */
public class Specification {

    @NotNull private final ValueSelector selector;
    @NotNull private final PathSet positives;
    @NotNull private final PathSet negatives;
    @NotNull private final String description;

    private Specification(
            @NotNull ValueSelector selector,
            @NotNull PathSet positives,
            @NotNull PathSet negatives,
            @NotNull String description) {
        this.selector = selector;
        this.positives = positives;
        this.negatives = negatives;
        this.description = description;
    }

    public static Specification of(@NotNull ValueSelector selector) {
        return new Specification(selector, PathSet.empty(), PathSet.empty(), "");
    }

    public static Specification of(
            @NotNull ValueSelector selector,
            @NotNull PathSet positives,
            @NotNull PathSet negatives,
            @NotNull String description) throws ConfigurationException {
        configCheck(positives.isEmpty() || negatives.isEmpty(),
                "'item' and 'exceptItem' cannot be combined: %s vs %s in %s",
                positives, negatives, description);
        return new Specification(selector, positives, negatives, description);
    }

    public static @NotNull Specification all() {
        return new Specification(ValueSelector.empty(), PathSet.of(), PathSet.of(), "");
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

    /** Returns `true` it this specification (at least partially) covers given item. */
    private boolean overlaps(@NotNull ItemPath path) {
        if (!positives.isEmpty()) {
            return positives.containsRelated(path);
        } else {
            // Negative scenario: we are OK unless the whole item is excluded
            return !negatives.containsSubpathOrEquivalent(path);
        }
    }

    // Later, we can take the original filter into account.
    <T> Specification adjust(@NotNull Class<T> filterType) throws SchemaException, ConfigurationException {

        Class<?> selectorType = selector.getTypeOrDefault();
        if (selectorType.isAssignableFrom(filterType)) {
            return this;
        }
        // If both types are at object level, let us continue even if there's no match (TODO reconsider!)
        if (ObjectType.class.isAssignableFrom(selectorType) && ObjectType.class.isAssignableFrom(filterType)) {
            return this; // or null?
        }

        for (Adjustment candidateAdjustment : getCandidateAdjustments(filterType)) {
            var newParentType = PrismContext.get().getSchemaRegistry()
                    .selectMoreSpecific(selector.getTypeName(), candidateAdjustment.typeName);
            if (newParentType == null) {
                continue; // No intersection -> try another candidate
            }

            // The check of overlapping is necessary to avoid conversion from non-related positive path set to an empty
            // positive path set (after computing the remainder below).
            if (!overlaps(candidateAdjustment.path)) {
                continue; // The spec does not allow (even not partial) access to the current item.
            }

            return new Specification(
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

    @Nullable TopDownSpecification asTopDown(@NotNull Class<? extends Objectable> rootType) throws ConfigurationException {

        if (ObjectTypeUtil.isObjectable(selector.getTypeOrDefault())) {
            // No parent expected here
            return asTopDown();
        }

        var builder = new TopDownSpecBuilder(this);

        // First, let us step up through explicit parent clauses.
        ValueSelector currentSelector = selector;
        for (;;) {
            ParentClause explicitParent = currentSelector.getParentClause();
            if (explicitParent == null) {
                break;
            }
            builder.addParent(explicitParent);
            currentSelector = explicitParent.getParent();
        }

        // If we are not on top yet, let us try to use implicit adjustments.
        var currentType = currentSelector.getTypeOrDefault();
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
            builtRootType = rootSelector.getTypeOrDefault();
        }

        // Now we are on top, let us finish.
        if (builtRootType.isAssignableFrom(rootType)) {
            return builder.build();
        } else {
            return null;
        }
    }

    private TopDownSpecification asTopDown() {
        return new TopDownSpecification(this.withParentClauseRemoved());
    }

    private @NotNull Specification withParentClauseRemoved() {
        return withSelectorReplaced(
                selector.withParentRemoved());
    }

    private Specification withSelectorReplaced(@NotNull ValueSelector newSelector) {
        return new Specification(newSelector, positives, negatives, description);
    }

    @Override
    public String toString() {
        return "Specification{" +
                "selector=" + selector +
                ", positives=" + positives +
                ", negatives=" + negatives +
                ", description='" + description + '\'' +
                '}';
    }

    private static class Adjustment {

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

        @NotNull private final QName typeName;
        @NotNull private final ItemPath path;

        private Adjustment(@NotNull QName typeName, @NotNull ItemPath path) {
            this.typeName = typeName;
            this.path = path;
        }

        @Override
        public String toString() {
            return typeName.getLocalPart() + ":" + path;
        }
    }

    private static class TopDownSpecBuilder {

        @NotNull private final Specification base;

        /** Reusing (misusing?) {@link ParentClause} as a path-selector pair container. Rewrite if needed. */
        @NotNull private final List<ParentClause> stepsUp = new ArrayList<>();

        private TopDownSpecBuilder(@NotNull Specification base) {
            this.base = base;
        }

        void addParent(@NotNull ParentClause stepUp) {
            stepsUp.add(stepUp);
        }

        @NotNull TopDownSpecification build() {
            TopDownSpecification current = new TopDownSpecification(base.withParentClauseRemoved());
            for (ParentClause step : stepsUp) {
                ItemPath pathToChild = step.getPath();
                List<?> segments = pathToChild.getSegments();
                stateCheck(!segments.isEmpty(), "No segments? current: %s, step: %s", current, step);
                for (int i = segments.size() - 1; i > 0; i--) {
                    current = TopDownSpecification.withChild(
                            ValueSelector.empty(),
                            ItemPath.toName(segments.get(i)),
                            current);
                }
                current = TopDownSpecification.withChild(
                        step.getParent().withParentRemoved(),
                        ItemPath.toName(segments.get(0)),
                        current);
            }
            return current;
        }
    }
}
