/*
 *
 *  Copyright (C) 2010- 2026 Evolveum and contributors
 *
 *   Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import java.util.Collection;
import java.util.UUID;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.schema.policy.IndexedRef;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

public final class PolicyRuleIndexSupport {
    /** Namespace used for unqualified constraint and action names in queries. */
    private static final String DEFAULT_NAMESPACE = PolicyRuleType.COMPLEX_TYPE.getNamespaceURI();

    private static final String TARGET_ARCHETYPE_REF_SUFFIX = "/targetArchetypeRef";

    private PolicyRuleIndexSupport() {
    }

    /**
     * Converts element names (of constraints or actions) to URI IDs for storing, creating the IDs as needed.
     * Never returns null: an empty array means "a rule without any", null in the database means "no rule".
     */
    public static @NotNull Integer[] uriIdsForStore(
            @NotNull SqaleRepoContext repositoryContext, @NotNull Collection<QName> names) {
        return names.stream()
                .map(name -> repositoryContext.processCacheableUri(QNameUtil.qNameToUri(name)))
                .toArray(Integer[]::new);
    }

    /**
     * Fills the target columns of the reference row.
     * Returns false if the reference cannot be stored (its OID is not a UUID); such a reference is not indexed.
     */
    public static boolean fillTarget(
            @NotNull MReference row, @NotNull IndexedRef ref, @NotNull SqaleRepoContext repositoryContext) {
        try {
            row.targetOid = UUID.fromString(ref.targetOid());
        } catch (IllegalArgumentException e) {
            return false;
        }
        row.targetType = targetType(ref, repositoryContext);
        row.relationId = repositoryContext.processCacheableRelation(ref.relation());
        return true;
    }

    /** The target type column is mandatory; if the type is not known, it is derived from the path. */
    private static MObjectType targetType(IndexedRef ref, SqaleRepoContext repositoryContext) {
        if (ref.targetType() != null) {
            return MObjectType.fromSchemaType(repositoryContext.qNameToSchemaClass(ref.targetType()));
        }
        if (QNameUtil.match(ref.refItem(), PolicyRuleType.F_MARK_REF)) {
            return MObjectType.MARK;
        }
        if (QNameUtil.match(ref.refItem(), ExclusionPolicyConstraintType.F_TARGET_ARCHETYPE_REF)) {
            return MObjectType.ARCHETYPE;
        }
        return MObjectType.ABSTRACT_ROLE;
    }
}
