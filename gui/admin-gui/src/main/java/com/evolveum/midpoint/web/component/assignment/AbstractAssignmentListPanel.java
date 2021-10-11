/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Created by honchar.
 */
public abstract class AbstractAssignmentListPanel extends BasePanel<List<AssignmentEditorDto>>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractAssignmentListPanel.class);

    public AbstractAssignmentListPanel(String id, IModel<List<AssignmentEditorDto>> assignmentsModel){
        super(id, assignmentsModel);
    }

    protected void deleteAssignmentPerformed(AjaxRequestTarget target, AssignmentEditorDto dto) {
        List<AssignmentEditorDto> selected = getSelectedAssignments();

        if (dto == null && selected.isEmpty()) {
            warn(getNoAssignmentsSelectedMessage());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }


        getPageBase().showMainPopup(getDeleteAssignmentPopupContent(dto), target);
    }

    public Popupable getDeleteAssignmentPopupContent(AssignmentEditorDto dto) {
        return new ConfirmationPanel(getPageBase().getMainPopupBodyId(), new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getAssignmentsDeleteMessage(dto, getSelectedAssignments().size());
            }
        }) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                List<AssignmentEditorDto> assignmentsListToDelete;
                if (dto != null){
                    assignmentsListToDelete = new ArrayList<>();
                    assignmentsListToDelete.add(dto);
                } else {
                    assignmentsListToDelete = getSelectedAssignments();
                }
                deleteAssignmentConfirmedPerformed(target, assignmentsListToDelete);
                reloadMainFormButtons(target);
            }
        };
    }

    protected String getAssignmentsDeleteMessage(AssignmentEditorDto dto, int size){
        if (dto != null){
            return createStringResource("pageUser.message.deleteAssignmentRowConfirm",
                    dto.getName()).getString();
        } else {
            return createStringResource("AssignmentTablePanel.modal.message.delete",
                    size).getString();
        }
    }

    protected void reloadMainFormButtons(AjaxRequestTarget target){
        AbstractObjectMainPanel panel = AbstractAssignmentListPanel.this.findParent(AbstractObjectMainPanel.class);
        if (panel != null){
            panel.reloadSavePreviewButtons(target);
        }
    }

    protected void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target,
            List<AssignmentEditorDto> toDelete) {
        List<AssignmentEditorDto> assignments = getAssignmentModel().getObject();

        for (AssignmentEditorDto assignment : toDelete) {
            if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
                assignments.remove(assignment);
            } else {
                assignment.setStatus(UserDtoStatus.DELETE);
                assignment.setSelected(false);
            }
        }
        target.add(getPageBase().getFeedbackPanel());
        reloadMainAssignmentsComponent(target);
    }

    protected abstract void reloadMainAssignmentsComponent(AjaxRequestTarget target);

    protected IModel<List<AssignmentEditorDto>> getAssignmentModel() {
        return getModel();
    }

    protected AssignmentEditorDto createAssignmentFromSelectedObjects(ObjectType object, QName relation){
        try {

            if (object instanceof ResourceType) {
                AssignmentEditorDto dto = addSelectedResourceAssignPerformed((ResourceType) object);
                return dto;
            }
            if (object instanceof UserType) {
                AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object,
                        WebComponentUtil.getDefaultRelationOrFail(RelationKindType.DELEGATION), getPageBase());
                dto.getTargetRef().setRelation(relation);
                return dto;
            } else {
                AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object, getPageBase());
                dto.getTargetRef().setRelation(relation);
                return dto;
            }
        } catch (Exception e) {
            error(getString("AssignmentTablePanel.message.couldntAssignObject", object.getName(),
                    e.getMessage()));
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assign object", e);
        }
        return null;

    }

    protected AssignmentEditorDto addSelectedResourceAssignPerformed(ResourceType resource) {
        AssignmentType assignment = new AssignmentType();
        ConstructionType construction = new ConstructionType();
        assignment.setConstruction(construction);

        try {
            getPageBase().getPrismContext().adopt(assignment, UserType.class, UserType.F_ASSIGNMENT);
        } catch (SchemaException e) {
            error(getString("Could not create assignment", resource.getName(), e.getMessage()));
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create assignment", e);
            return null;
        }

        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.asReferenceValue().setObject(resource.asPrismObject());
        construction.setResourceRef(resourceRef);

        AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, getPageBase());

        dto.setMinimized(true);
        dto.setShowEmpty(true);
        return dto;
    }

    protected List<AssignmentEditorDto> getSelectedAssignments() {
        List<AssignmentEditorDto> selected = new ArrayList<>();

        List<AssignmentEditorDto> all = getAssignmentModel().getObject();

        for (AssignmentEditorDto dto : all) {
            if (dto.isSelected()) {
                selected.add(dto);
            }
        }

        return selected;
    }

    protected String getNoAssignmentsSelectedMessage(){
        return getString("AssignmentTablePanel.message.noAssignmentSelected");
    }

}
