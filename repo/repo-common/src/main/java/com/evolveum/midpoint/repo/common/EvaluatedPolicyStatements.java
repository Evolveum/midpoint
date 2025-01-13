/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private final Collection<ObjectReferenceType> refsToAdd = new ArrayList<>();
    private final Collection<ObjectReferenceType> refsToDelete = new ArrayList<>();
    /**
     * force delete, because policyStatement is exclude. we need to differentiate
     * between deleted marks and excluded marks, e.g.:
     * if the effectiveMarkRef was computed from policy rule and also added manually (with type = apply)
     * and manual mark is removed, we still want to keep effective mark
     * but
     * if the effectiveMarkRef was computed from policy rule and also added manually (with type = exclude)
     * we don't want effectiveMarkRef to be present
     */
    private final Collection<ObjectReferenceType> refsToExclude = new ArrayList<>();

    public void addMarkRefToAdd(ObjectReferenceType ref) {
        refsToAdd.add(ref);
        if (refsToDelete.stream().anyMatch(r -> r.getOid().equals(ref.getOid()))) {
            refsToDelete.remove(ref);
        }
    }

    public void addMarkRefToDelete(ObjectReferenceType ref) {
        refsToDelete.add(ref);
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
     * Returns values to add. "Existing values" parameter is used to filter out values already present in the object,
     * to avoid phantom adds, like in MID-10121. We assume no metadata, relations or similar exotic features here.
     * If present, more sophisticated comparison would be needed.
     */
    public Collection<ObjectReferenceType> collectMarksToAdd(@NotNull Collection<ObjectReferenceType> existingValues) {
        Collection<ObjectReferenceType> finalCollection = new ArrayList<>(refsToAdd);
        finalCollection.removeIf(refsToExclude::contains);
        finalCollection.removeIf(existingValues::contains);
        return finalCollection;
    }

    /**
     * Returns values to delete. "Existing values" parameter is used to filter out values not present in the object,
     * to avoid phantom deletes. We assume no metadata, relations or similar exotic features here. If present, more sophisticated
     * comparison would be needed.
     */
    public Collection<ObjectReferenceType> collectMarksToDelete(@NotNull Collection<ObjectReferenceType> existingValues) {
        List<ObjectReferenceType> allItemsToDelete = new ArrayList<>();
        allItemsToDelete.addAll(refsToDelete);
        allItemsToDelete.addAll(refsToExclude);
        allItemsToDelete.removeIf(ref -> !existingValues.contains(ref));
        return allItemsToDelete;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return refsToAdd.isEmpty() && refsToDelete.isEmpty() && refsToExclude.isEmpty();
    }
}
