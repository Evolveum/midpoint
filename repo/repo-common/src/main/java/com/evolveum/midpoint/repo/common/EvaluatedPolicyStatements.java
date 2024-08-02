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

    public Collection<ObjectReferenceType> collectMarksToAdd() {
        Collection<ObjectReferenceType> finalCollection = new ArrayList<>(refsToAdd);
        finalCollection.removeIf(refsToExclude::contains);
        return finalCollection;
    }

    public Collection<ObjectReferenceType> collectMarksToDelete() {
        List<ObjectReferenceType> allItemsToDelete = new ArrayList<>();
        allItemsToDelete.addAll(refsToDelete);
        allItemsToDelete.addAll(refsToExclude);
        return allItemsToDelete;
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public boolean isEmpty() {
        return refsToAdd.isEmpty() && refsToDelete.isEmpty() && refsToExclude.isEmpty();
    }
}
