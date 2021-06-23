package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.io.Serializable;
import java.util.List;

public class MultiFunctinalButtonDto implements Serializable {

    public static final String F_MAIN_BUTTON = "mainButton";
    public static final String F_ADDITIONAL_BUTTONS = "additionalButtons";

    private CompositedIconButtonDto mainButton;
    private List<CompositedIconButtonDto> additionalButtons;

    public MultiFunctinalButtonDto() {

    }

    public void setMainButton(CompositedIconButtonDto mainButton) {
        this.mainButton = mainButton;
    }

    public CompositedIconButtonDto getMainButton() {
        return mainButton;
    }

    public void setAdditionalButtons(List<CompositedIconButtonDto> additionalButtons) {
        this.additionalButtons = additionalButtons;
    }

    public List<CompositedIconButtonDto> getAdditionalButtons() {
        return additionalButtons;
    }

    //    private DisplayType additionalButtonDisplayType;
//    private CompositedIcon compositedIcon;
//    private AssignmentObjectRelation assignmentObjectRelation;
//    private CompiledObjectCollectionView collectionView;
//
//    public MultiFunctinalButtonDto() {
//
//    }
//
//    public DisplayType getAdditionalButtonDisplayType() {
//        return additionalButtonDisplayType;
//    }
//
//    public void setAdditionalButtonDisplayType(DisplayType additionalButtonDisplayType) {
//        this.additionalButtonDisplayType = additionalButtonDisplayType;
//    }
//
//    public void setOrCreateDefaultAdditionalButtonDisplayType(DisplayType additionalButtonDisplayType) {
//        if (additionalButtonDisplayType == null) {
//            additionalButtonDisplayType = new DisplayType();
//        }
//        if (additionalButtonDisplayType.getIcon() == null) {
//            additionalButtonDisplayType.setIcon(new IconType());
//        }
//        if (additionalButtonDisplayType.getIcon().getCssClass() == null) {
//            additionalButtonDisplayType.getIcon().setCssClass("");
//        }
//
//        this.additionalButtonDisplayType = additionalButtonDisplayType;
//    }
//
//    public CompositedIcon getCompositedIcon() {
//        return compositedIcon;
//    }
//
//    public void setCompositedIcon(CompositedIcon compositedIcon) {
//        this.compositedIcon = compositedIcon;
//    }
//
//    public AssignmentObjectRelation getAssignmentObjectRelation() {
//        return assignmentObjectRelation;
//    }
//
//    public void setAssignmentObjectRelation(AssignmentObjectRelation assignmentObjectRelation) {
//        this.assignmentObjectRelation = assignmentObjectRelation;
//    }
//
//    public CompiledObjectCollectionView getCollectionView() {
//        return collectionView;
//    }
//
//    public void setCollectionView(CompiledObjectCollectionView collectionView) {
//        this.collectionView = collectionView;
//    }
}
