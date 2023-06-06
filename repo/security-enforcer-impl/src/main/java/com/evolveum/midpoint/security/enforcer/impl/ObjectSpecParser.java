/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates {@link TopDownSpecification} instances for the following scenarios:
 *
 * - authorization + object: to determine access (at the object or sub-object level)
 * - authorization + type + position: to determine filter
 *
 * TODO move somewhere else
 */
public class ObjectSpecParser {

    /**
     * Creates specifications relevant to given `value`, derived from the currently-processed authorization.
     */
    public static Collection<TopDownSpecification> forAutzAndValue(
            @NotNull PrismObjectValue<?> value, @NotNull AuthorizationEvaluation evaluation)
            throws ConfigurationException {
        Authorization autz = evaluation.getAuthorization();
        List<TopDownSpecification> specifications = new ArrayList<>();
        var autzSelectors = autz.getParsedObjectSelectors();
        // Autz selectors can be empty e.g. for #all autz or for weird ones like role-prop-read-some-modify-some.xml.
        List<ValueSelector> objectSelectors = !autzSelectors.isEmpty() ? autzSelectors : List.of(ValueSelector.empty());
        for (ValueSelector objectSelector : objectSelectors) {
            Specification base = Specification.of(objectSelector, autz.getItems(), autz.getExceptItems(), evaluation.getDesc());
            TopDownSpecification topDown = base.asTopDown(value.asObjectable().getClass());
            if (topDown != null) {
                specifications.add(topDown);
            }
        }
        return specifications;
    }

//    /**
//     * Tries to match given `objectSelector` to `value`.
//     * If not matching directly, looks for parent selectors (explicit or implicit) of the selector provided.
//     *
//     * Returns the parsed selector at the value level, or `null` if the original selector cannot be applied to the value
//     * in any way.
//     *
//     * An example:
//     *
//     * - value is an instance of {@link AccessCertificationCaseType}
//     * - selector is for `type` = {@link AccessCertificationWorkItemType} (with or without `parent` specification)
//     *
//     * If the selector matches the value (usually does, unless it has a `parent` spec that excludes the value), then the
//     * returned structure looks like this:
//     *
//     * - spec for {@link AccessCertificationCaseType} (returned)
//     * - linked (via `workItem`) to a spec for {@link AccessCertificationWorkItemType}, derived from the selector
//     */
//    private static @Nullable TopDownSpecification forSelectorAndValueUpwards(
//            String baseId,
//            @NotNull ValueSelector objectSelector,
//            @NotNull PathSet positives,
//            @NotNull PathSet negatives,
//            @NotNull PrismValue value,
//            @NotNull AuthorizationEvaluation evaluation)
//            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
//            ConfigurationException, ObjectNotFoundException {
//
//        if (evaluation.isSelectorApplicable(baseId, objectSelector, value, Set.of(), "TODO")) {
//            return TopDownSpecification.create(objectSelector, positives, negatives, evaluation);
//        }
//
//        ParentSelector parentSelector = getParentSelector(objectSelector);
//        if (parentSelector == null) {
//            return null;
//        }
//
//        // Let's try to match the parent.
//        var root = forSelectorAndValueUpwards(
//                baseId + "^", parentSelector.getSelector(), PathSet.of(), PathSet.of(), value, evaluation);
//        if (root != null) {
//            var child = TopDownSpecification.create(objectSelector, positives, negatives, evaluation);
//            link(root.getLastDescendant(), child, parentSelector.getPath());
//            return root;
//        } else {
//            return null;
//        }
//    }

//    private static @Nullable ParentSelector getParentSelector(
//            @NotNull ValueSelector objectSelector) throws ConfigurationException {
//        ParentClause explicit = objectSelector.getParentClause();
//        if (explicit != null) {
//            return ParentSelector.explicit(explicit);
//        } else {
//            return ParentSelector.implicit(objectSelector.getTypeName());
//        }
//    }

//    private static void link(
//            @NotNull TopDownSpecification parent, @NotNull TopDownSpecification child, @NotNull ItemPath path) {
//        argCheck(!path.isEmpty(), "Empty path between %s and %s?", parent, child);
//        Object first = path.first();
//        ItemPath rest = path.rest();
//        var name = ItemPath.toName(first);
//
//        if (rest.isEmpty()) {
//            parent.addChild(name, child);
//        } else {
//            var intermediate = TopDownSpecification.empty();
//            parent.addChild(name, intermediate);
//            link(intermediate, child, rest);
//        }
//    }

    /**
     * Tries to match given `objectSelector` to `value`.
     * If not matching directly, looks for selectors related to the value's parent (recursively).
     *
     * Returns the parsed selector at the value level, or `null` if the original selector cannot be applied to the value
     * in any way.
     *
     * An example:
     *
     * - value is an instance of {@link AccessCertificationCaseType}
     * - selector is for `type` = {@link AccessCertificationCampaignType} that includes the `case` item
     *
     * The returned structure then looks like this:
     *
     * - spec for {@link AccessCertificationCaseType}, with item paths derived from the original selector
     *
     * Note that we do not need to care for parent selectors (yet). The structure will be used for determine the access
     * to the value itself and _not_ for the parents; and assumes that the selector applicability to the parent was checked
     * by this method.
     */
//    private static @Nullable TopDownSpecification forSelectorAndValueDownwards(
//            String baseId,
//            @NotNull ValueSelector objectSelector,
//            @NotNull PathSet positives,
//            @NotNull PathSet negatives,
//            @NotNull PrismValue value,
//            @NotNull AuthorizationEvaluation evaluation)
//            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
//            ConfigurationException, ObjectNotFoundException {
//
//        // FIXME WRONG!!! We should return PrismValueCoverageInformation instead - the current approach is pure hack
//
//        //TopDownSpecification.checkNotBoth(positives, negatives, evaluation);
//
//        ItemPath path = ItemPath.EMPTY_PATH;
//
//        String id = baseId;
//
//        for (;;) {
//            if (evaluation.isSelectorApplicable(id, objectSelector, value, Set.of(), "TODO")) {
//                var adjusted = AdjustedPaths.compute(positives, negatives, path);
//                return TopDownSpecification.create(
//                        ValueSelector.empty(), // i.e. matching without further checks
//                        adjusted.newPositives,
//                        adjusted.newNegatives,
//                        evaluation);
//            }
//
//            Itemable parent = value.getParent();
//            if (!(parent instanceof Item<?, ?>)) {
//                return null;
//            }
//            var item = (Item<?, ?>) parent;
//            var itemName = item.getElementName();
//            value = item.getParent();
//            if (value == null) {
//                return null;
//            }
//            path = itemName.append(path);
//            id = id + "v";
//        }
//    }

//    /**
//     * Strips down the `path` prefix from all paths in `positives` and `negatives`.
//     */
//    private static class AdjustedPaths {
//
//        @NotNull private final PathSet newPositives;
//        @NotNull private final PathSet newNegatives;
//
//        private AdjustedPaths(@NotNull PathSet newPositives, @NotNull PathSet newNegatives) {
//            this.newPositives = newPositives;
//            this.newNegatives = newNegatives;
//        }
//
//        static AdjustedPaths compute(PathSet positives, PathSet negatives, ItemPath path) {
//            assert positives.isEmpty() || negatives.isEmpty();
//            if (path.isEmpty()) {
//                return new AdjustedPaths(positives, negatives);
//            } else if (!positives.isEmpty()) {
//                PathSet positivesAdjusted = positives.remainder(path);
//                if (!positivesAdjusted.isEmpty()) {
//                    return new AdjustedPaths(positivesAdjusted, PathSet.empty());
//                } else {
//                    // We have to return "exclude all" spec, because original spec was positive but nothing remained.
//                    return new AdjustedPaths(PathSet.empty(), PathSet.of(ItemPath.EMPTY_PATH));
//                }
//            } else {
//                // We don't care if the negatives or their remainder is empty. If it is, we want to allow all.
//                return new AdjustedPaths(PathSet.empty(), negatives.remainder(path));
//            }
//        }
//    }
}
