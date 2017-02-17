package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.self.dto.AssignmentConflictDto;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

/**
 * Created by honchar.
 */
public class AssignmentConflictPanel extends BasePanel<AssignmentConflictDto> {
    private static final String ID_STATUS_ICON = "statusIcon";
    private static final String ID_EXISTING_ASSIGNMENT = "existingAssignment";
    private static final String ID_ADDED_ASSIGNMENT = "addedAssignment";
    private static final String ID_UNSELECT_BUTTON = "unselectButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";

    private static final String STATUS_FIXED = GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED + " fa-lg";
    private static final String STATUS_ERROR = GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_FATAL_ERROR_COLORED + " fa-lg";

    public AssignmentConflictPanel(String id, IModel<AssignmentConflictDto> model){
        super(id, model);
        initLayout();
    }

    private void initLayout(){
        setOutputMarkupId(true);

        Label statusIconLabel = new Label(ID_STATUS_ICON);
        statusIconLabel.add(AttributeModifier.replace("class", getStatusIconClass()));
        add(statusIconLabel);

        Label existingAssignment = new Label(ID_EXISTING_ASSIGNMENT,
                getExistingAssignmentLabelModel());
        add(existingAssignment);

        Label addedAssignment = new Label(ID_ADDED_ASSIGNMENT,
                getAddedAssignmentLabelModel());
        add(addedAssignment);

        AjaxButton removeButton = new AjaxButton(ID_REMOVE_BUTTON,
                createStringResource("AssignmentConflictPanel.removeButton")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(removeButton);

        AjaxButton unselectButton = new AjaxButton(ID_UNSELECT_BUTTON,
                createStringResource("AssignmentConflictPanel.unselectButton")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(unselectButton);
    }

    private Model<String> getExistingAssignmentLabelModel(){
        if (getModelObject() != null){
            String name = getModelObject().getExistingAssignmentTargetObj().asObjectable().getName() != null ?
                    getModelObject().getExistingAssignmentTargetObj().asObjectable().getName().getOrig() :
                    getModelObject().getExistingAssignmentTargetObj().getOid();
            return Model.of(name + " " + createStringResource("AssignmentConflictPanel.existingAssignmentLabelMessage").getString());
        }
        return Model.of("");
    }

    private Model<String> getAddedAssignmentLabelModel(){
        if (getModelObject() != null){
            String name = getModelObject().getAddedAssignmentTargetObj().asObjectable().getName() != null ?
                    getModelObject().getAddedAssignmentTargetObj().asObjectable().getName().getOrig() :
                    getModelObject().getAddedAssignmentTargetObj().getOid();
            return Model.of(name + " " + createStringResource("AssignmentConflictPanel.addedAssignmentLabelMessage").getString());
        }
        return Model.of("");
    }

    private String getStatusIconClass(){
        return getModelObject() != null ? (getModelObject().isSolved() ? STATUS_FIXED : STATUS_ERROR) : STATUS_ERROR;
    }



}
