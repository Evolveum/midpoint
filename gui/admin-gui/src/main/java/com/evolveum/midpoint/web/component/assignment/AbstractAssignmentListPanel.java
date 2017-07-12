/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public abstract class AbstractAssignmentListPanel extends BasePanel<List<AssignmentEditorDto>>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractAssignmentListPanel.class);

    protected PageBase pageBase;

    public AbstractAssignmentListPanel(String id, IModel<List<AssignmentEditorDto>> assignmentsModel, PageBase pageBase){
        super(id, assignmentsModel);
        this.pageBase = pageBase != null ? pageBase : getPageBase();
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
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(), new AbstractReadOnlyModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return getAssignmentsDeleteMessage(getSelectedAssignments().size());
            }
        }) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                ModalWindow modalWindow = findParent(ModalWindow.class);
                if (modalWindow != null) {
                    modalWindow.close(target);
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
            }
        };
    }

    protected String getAssignmentsDeleteMessage(int size){
        return createStringResource("AssignmentTablePanel.modal.message.delete",
                size).getString();
    }

    protected void reloadMainFormButtons(AjaxRequestTarget target){}

    private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target,
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

    protected AssignmentEditorDto createAssignmentFromSelectedObjects(ObjectType object, RelationTypes relation){
        try {

            if (object instanceof ResourceType) {
                AssignmentEditorDto dto = addSelectedResourceAssignPerformed((ResourceType) object, pageBase);
                return dto;
            }
            if (object instanceof UserType) {
                AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object,
                        SchemaConstants.ORG_DEPUTY, pageBase);
                dto.getTargetRef().setRelation(relation.getRelation());
                return dto;
            } else {
                AssignmentEditorDto dto = AssignmentEditorDto.createDtoAddFromSelectedObject(object, pageBase);
                dto.getTargetRef().setRelation(relation.getRelation());
                return dto;
            }
        } catch (Exception e) {
            error(getString("AssignmentTablePanel.message.couldntAssignObject", object.getName(),
                    e.getMessage()));
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't assign object", e);
        }
        return null;

    }

    protected AssignmentEditorDto addSelectedResourceAssignPerformed(ResourceType resource, PageBase pageBase) {
        AssignmentType assignment = new AssignmentType();
        ConstructionType construction = new ConstructionType();
        assignment.setConstruction(construction);

        try {
            pageBase.getPrismContext().adopt(assignment, UserType.class,
                    new ItemPath(UserType.F_ASSIGNMENT));
        } catch (SchemaException e) {
            error(getString("Could not create assignment", resource.getName(), e.getMessage()));
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create assignment", e);
            return null;
        }

        construction.setResource(resource);

        AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, pageBase);

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
