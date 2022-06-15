/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RequestAccess implements Serializable {

    private List<ObjectReferenceType> personOfInterest;

    private QName relation;

    private List<AssignmentType> shoppingCartAssignments;

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
        if (personOfInterest == null) {
            personOfInterest = new ArrayList<>();
        }
        return personOfInterest;
    }

    public void setPersonOfInterest(List<ObjectReferenceType> personOfInterest) {
        this.personOfInterest = personOfInterest;
    }

    public List<AssignmentType> getShoppingCartAssignments() {
        if (shoppingCartAssignments == null) {
            shoppingCartAssignments = new ArrayList<>();
        }
        return shoppingCartAssignments;
    }

    public void setShoppingCartAssignments(List<AssignmentType> shoppingCartAssignments) {
        this.shoppingCartAssignments = shoppingCartAssignments;
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
}
