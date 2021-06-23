package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;

import org.apache.commons.collections4.CollectionUtils;

import java.io.Serializable;
import java.util.List;

public class AssignmentPopupDto implements Serializable {

    private final List<AssignmentObjectRelation> assignmentObjectRelation;
    private boolean selectionVisible;

    public AssignmentPopupDto(List<AssignmentObjectRelation> assignmentObjectRelation) {
        this.assignmentObjectRelation = assignmentObjectRelation;
        this.selectionVisible = CollectionUtils.isNotEmpty(assignmentObjectRelation);
    }

    public boolean isSelectionVisible() {
        return selectionVisible;
    }

    public void setSelectionVisible(boolean selectionVisible) {
        this.selectionVisible = selectionVisible;
    }

    public List<AssignmentObjectRelation> getAssignmentObjectRelation() {
        return assignmentObjectRelation;
    }

    public boolean hasSelectionEnabled() {
        return CollectionUtils.isNotEmpty(assignmentObjectRelation);
    }
}
