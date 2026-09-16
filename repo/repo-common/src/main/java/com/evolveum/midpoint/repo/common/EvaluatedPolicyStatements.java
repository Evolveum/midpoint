/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Processed form of {@link ObjectType#F_POLICY_STATEMENT} values plus mark references derived from the policy rules.
 *
 * Somewhat similar to {@link ObjectMarkHelper.SmartMarkRefCollection} but does not support `org:related` relation
 * nor the value metadata (yet).
 */
public class EvaluatedPolicyStatements implements Serializable {

    /** Marks that should be present: those from policy rules and from `apply` statements. */
    private final Collection<ObjectReferenceType> refsToAdd = new ArrayList<>();

    /**
     * Marks that must not be present, because an `exclude` statement says so. Exclusion always wins over
     * policy rules and `apply` statements. A mark that is neither here nor in {@link #refsToAdd} is removed
     * from the object; this covers e.g. a removed `apply` statement or a policy rule that no longer applies.
     */
    private final Collection<ObjectReferenceType> refsToExclude = new ArrayList<>();

    public void addMarkRefToAdd(ObjectReferenceType ref) {
        refsToAdd.add(ref);
    }

    public void addMarkRefToExclude(ObjectReferenceType ref) {
        refsToExclude.add(ref);
    }

    public boolean isExclude(ObjectReferenceType markRef) {
        List<String> refsToExcludeOids = refsToExclude.stream()
                .map(ObjectReferenceType::getOid)
                .toList();
        return refsToExcludeOids.contains(markRef.getOid());
    }

    /**
     * Returns the desired marks (from policy rules and `apply` statements, minus the excluded ones) that are not yet
     * present in the object. Values are compared by OID, to avoid phantom adds, like in MID-10121. We assume no metadata,
     * relations or similar exotic features here. If present, more sophisticated comparison would be needed.
     */
    public Collection<ObjectReferenceType> collectMarksToAdd(@NotNull Collection<ObjectReferenceType> existingValues) {
        Set<String> existingOids = collectOids(existingValues);
        Set<String> alreadyAdded = new HashSet<>();
        List<ObjectReferenceType> marksToAdd = new ArrayList<>();
        for (ObjectReferenceType ref : refsToAdd) {
            String oid = ref.getOid();
            if (oid != null && !isExclude(ref) && !existingOids.contains(oid) && alreadyAdded.add(oid)) {
                marksToAdd.add(ref);
            }
        }
        return marksToAdd;
    }

    /**
     * Returns existing values that should be deleted, i.e. those not among the desired marks. This covers marks excluded
     * by statements, marks whose `apply` statement was removed, and marks computed earlier by policy rules that no longer
     * apply (issue 12154). Values are compared by OID; see {@link #collectMarksToAdd(Collection)}.
     */
    public Collection<ObjectReferenceType> collectMarksToDelete(@NotNull Collection<ObjectReferenceType> existingValues) {
        Set<String> desiredOids = collectOids(refsToAdd);
        desiredOids.removeAll(collectOids(refsToExclude));
        return existingValues.stream()
                .filter(ref -> !desiredOids.contains(ref.getOid()))
                .toList();
    }

    private static Set<String> collectOids(Collection<ObjectReferenceType> refs) {
        return refs.stream()
                .map(ObjectReferenceType::getOid)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
    }
}
