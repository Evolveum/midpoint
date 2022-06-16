/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RequestAccess implements Serializable {

    private Map<ObjectReferenceType, List<AssignmentType>> requestItems = new HashMap<>();

    private Set<AssignmentType> assignments = new HashSet<>();

    private QName relation;

    private String comment;

    private int warningCount;

    private int errorCount;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ObjectReferenceType> getPersonOfInterest() {
        return List.copyOf(new ArrayList<>(requestItems.keySet()));
    }

    public void addPersonOfInterest(ObjectReferenceType ref) {
        addPersonOfInterest(List.of(ref));
    }

    public void addPersonOfInterest(List<ObjectReferenceType> refs) {
        if (refs == null) {
            return;
        }

        Set<String> newOids = refs.stream().map(o -> o.getOid()).collect(Collectors.toSet());

        Set<ObjectReferenceType> existing = requestItems.keySet();
        Set<String> existingOids = existing.stream().map(o -> o.getOid()).collect(Collectors.toSet());

        // add new persons if they're not in set yet
        for (ObjectReferenceType ref : refs) {
            if (existingOids.contains(ref.getOid())) {
                continue;
            }

            List<AssignmentType> assignments = this.assignments.stream().map(a -> a.clone()).collect(Collectors.toList());
            requestItems.put(ref, assignments);
        }

        // remove persons that were not selected (or were unselected)
        for (ObjectReferenceType ref : existing) {
            if (newOids.contains(ref.getOid())) {
                continue;
            }

            requestItems.remove(ref);
        }
    }

    public void addAssignments(List<AssignmentType> assignments) {
        //todo remove naive implementation
        assignments.forEach(a -> this.assignments.add(a.clone()));

        for (List<AssignmentType> list : requestItems.values()) {
            assignments.forEach(a -> list.add(a.clone()));
        }
    }

    public List<AssignmentType> getShoppingCartAssignments() {
        Set<AssignmentType> assignments = new HashSet<>();

        requestItems.values().stream().forEach(list -> assignments.addAll(list));

        return List.copyOf(assignments);
    }

    public List<RequestAccessItem> getRequestAccessItems() {
        return requestItems.entrySet().stream()
                .map(e -> new RequestAccessItem(e.getKey(), e.getValue()))
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    public List<ShoppingCartItem> getShoppingCartItems() {
        return getShoppingCartAssignmentCounts().entrySet().stream()
                .map(e -> new ShoppingCartItem(e.getKey(), e.getValue()))
                .sorted()
                .collect(Collectors.toUnmodifiableList());
    }

    private Map<AssignmentType, Integer> getShoppingCartAssignmentCounts() {
        Map<AssignmentType, Integer> counts = new HashMap<>();

        for (ObjectReferenceType ref : requestItems.keySet()) {
            List<AssignmentType> assignments = requestItems.get(ref);
            for (AssignmentType a : assignments) {
                Integer count = counts.get(a);
                if (count == null) {
                    count = 0;
                    counts.put(a, count);
                }

                counts.replace(a, count + 1);
            }
        }

        return counts;
    }

    public QName getRelation() {
        return relation;
    }

    public void setRelation(QName relation) {
        this.relation = relation;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public void clearCart() {
        requestItems.clear();
        assignments.clear();
        relation = null;

        comment = null;

        warningCount = 0;
        errorCount = 0;
    }

    public boolean canSubmit() {
        return errorCount == 0 && !requestItems.isEmpty();
    }
}
