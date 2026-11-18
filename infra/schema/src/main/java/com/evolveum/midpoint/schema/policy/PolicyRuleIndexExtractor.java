/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.schema.policy;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class PolicyRuleIndexExtractor {

    private static final Set<QName> COMPOSITES = Set.of(
            PolicyConstraintsType.F_AND, PolicyConstraintsType.F_OR, PolicyConstraintsType.F_NOT);

    private PolicyRuleIndexExtractor() {
    }

    public static @NotNull PolicyRuleIndexData extract(@NotNull PolicyRuleType rule) {

        Set<QName> constraints = new LinkedHashSet<>();
        Set<IndexedRef> refs = new LinkedHashSet<>();
        PolicyRuleTypeUtil.accept(rule.getPolicyConstraints(),
                (name, constraint) -> {
                    if (!COMPOSITES.contains(name)) {
                        constraints.add(name);
                    }

                    if (constraint instanceof ExclusionPolicyConstraintType exclusion) {
                        addRef(refs, name, ExclusionPolicyConstraintType.F_TARGET_REF, exclusion.getTargetRef());
                        if (exclusion.getTargetRef() == null) {
                            addRef(refs, name, ExclusionPolicyConstraintType.F_TARGET_ARCHETYPE_REF, exclusion.getTargetArchetypeRef());
                        }
                    } else if (constraint instanceof HasAssignmentPolicyConstraintType hasAssignment) {
                        // hasAssignment and hasNoAssignment
                        addRef(refs, name, HasAssignmentPolicyConstraintType.F_TARGET_REF,
                                hasAssignment.getTargetRef());
                        if (hasAssignment.getTargetRef() == null) {
                            addRef(refs, name, HasAssignmentPolicyConstraintType.F_TARGET_ARCHETYPE_REF,
                                    hasAssignment.getTargetArchetypeRef());
                        }
                    }

                    return true;
                },
                true,
                false,
                null,
                true
        );
        for (ObjectReferenceType markRef : rule.getMarkRef()) {
            addRef(refs, IndexedRef.of(PolicyRuleIndexPaths.MARK_REF, markRef));
        }

        return new PolicyRuleIndexData(
                Collections.unmodifiableSet(constraints),
                actionNames(rule.getPolicyActions()),
                Collections.unmodifiableSet(refs));
    }

    private static void addRef(
            Set<IndexedRef> refs, QName constraintName, QName refItemName, ObjectReferenceType ref) {
        addRef(refs, IndexedRef.of(PolicyRuleIndexPaths.of(constraintName, refItemName), ref));
    }

    private static void addRef(Set<IndexedRef> refs, IndexedRef ref) {
        if (ref != null) {
            refs.add(ref);
        }
    }

    private static Set<QName> actionNames(PolicyActionsType actions) {
        if (actions == null) {
            return Set.of();
        }
        Set<QName> names = new LinkedHashSet<>();
        PrismContainerValue<?> actionsValue = actions.asPrismContainerValue();
        for (Item<?, ?> item : actionsValue.getItems()) {
            if (item.hasAnyValue()) {
                names.add(item.getElementName());
            }
        }
        return Collections.unmodifiableSet(names);
    }
}
