package com.evolveum.midpoint.common.mining.objects.statistic;

import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

public class UserAccessDistribution implements Serializable {

    List<ObjectReferenceType> directAssignments;
    List<ObjectReferenceType> indirectAssignments;
    List<ObjectReferenceType> duplicates;

    int directAssignmentsCount = 0;
    int indirectAssignmentsCount = 0;
    int duplicatesCount = 0;
    int allAccessCount = 0;

    public UserAccessDistribution() {
    }

    public UserAccessDistribution(
            List<ObjectReferenceType> directAssignments,
            List<ObjectReferenceType> indirectAssignments,
            List<ObjectReferenceType> duplicates) {
        this.directAssignments = directAssignments;
        this.indirectAssignments = indirectAssignments;
        this.duplicates = duplicates;

        if (directAssignments != null) {
            this.directAssignmentsCount = directAssignments.size();
        }
        if (indirectAssignments != null) {
            this.indirectAssignmentsCount = indirectAssignments.size();
        }
        if (duplicates != null) {
            this.duplicatesCount = duplicates.size();
        }
    }

    public @Nullable List<ObjectReferenceType> getDirectAssignments() {
        return directAssignments;
    }

    public void setDirectAssignments(List<ObjectReferenceType> directAssignments) {
        this.directAssignments = directAssignments;
    }

    public @Nullable List<ObjectReferenceType> getIndirectAssignments() {
        return indirectAssignments;
    }

    public void setIndirectAssignments(List<ObjectReferenceType> indirectAssignments) {
        this.indirectAssignments = indirectAssignments;
    }

    public @Nullable List<ObjectReferenceType> getDuplicates() {
        return duplicates;
    }

    public int getDirectAssignmentsCount() {
        return directAssignmentsCount;
    }

    public int getIndirectAssignmentsCount() {
        return indirectAssignmentsCount;
    }

    public int getDuplicatesCount() {
        return duplicatesCount;
    }

    public int getAllAccessCount() {
        return allAccessCount;
    }

    public void setAllAccessCount(int allAccessCount) {
        this.allAccessCount = allAccessCount;
    }

}
