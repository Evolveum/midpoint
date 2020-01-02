package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.io.Serializable;

public class MultiFunctinalButtonDto implements Serializable {

    private DisplayType additionalButtonDisplayType;
    private CompositedIcon compositedIcon;
    private AssignmentObjectRelation assignmentObjectRelation;
    private CompiledObjectCollectionView collectionView;

    public MultiFunctinalButtonDto() {

    }

    public DisplayType getAdditionalButtonDisplayType() {
        return additionalButtonDisplayType;
    }

    public void setAdditionalButtonDisplayType(DisplayType additionalButtonDisplayType) {
        this.additionalButtonDisplayType = additionalButtonDisplayType;
    }

    public CompositedIcon getCompositedIcon() {
        return compositedIcon;
    }

    public void setCompositedIcon(CompositedIcon compositedIcon) {
        this.compositedIcon = compositedIcon;
    }

    public AssignmentObjectRelation getAssignmentObjectRelation() {
        return assignmentObjectRelation;
    }

    public void setAssignmentObjectRelation(AssignmentObjectRelation assignmentObjectRelation) {
        this.assignmentObjectRelation = assignmentObjectRelation;
    }

    public CompiledObjectCollectionView getCollectionView() {
        return collectionView;
    }

    public void setCollectionView(CompiledObjectCollectionView collectionView) {
        this.collectionView = collectionView;
    }
}
