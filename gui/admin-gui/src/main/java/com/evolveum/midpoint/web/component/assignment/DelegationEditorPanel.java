/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar.
 */
public class DelegationEditorPanel extends AssignmentEditorPanel {
    private static final String ID_DELEGATION_VALID_FROM = "delegationValidFrom";
    private static final String ID_DELEGATION_VALID_TO = "delegationValidTo";
    private static final String ID_DESCRIPTION = "delegationDescription";
    private static final String ID_ARROW_ICON = "arrowIcon";
    private static final String ID_DELEGATED_TO_IMAGE = "delegatedToImage";
    private static final String ID_DELEGATED_TO = "delegatedTo";
    private static final String ID_DELEGATED_TO_LABEL = "delegatedToLabel";
    private static final String ID_SELECTED = "selected";
    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";
    private static final String ID_HEADER_ROW = "headerRow";
    private static final String ID_PRIVILEGES_LIST = "privilegesList";
    private static final String ID_PRIVILEGE = "privilege";
    private static final String ID_LIMIT_PRIVILEGES_BUTTON = "limitPrivilegesButton";
    private List<String> privilegesNames = new ArrayList<>();
    private static final String ID_EXPAND = "expand";
    private static final String ID_DELEGATE_APPROVAL_WI = "approvalWorkItems";
    private static final String ID_DELEGATE_CERTIFICATION_WI = "certificationWorkItems";
    private static final String ID_DELEGATE_MANAGEMENT_WI = "managementWorkItems";

    private static final String DOT_CLASS = DelegationEditorPanel.class.getName() + ".";
    private static final String OPERATION_GET_TARGET_REF_NAME = DOT_CLASS + "getTargetRefName";

    private List<UserType> usersToUpdate;

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> delegationTargetObjectModel, boolean delegatedToMe,
                                 List<AssignmentsPreviewDto> privilegesList, PageBase pageBase) {
            super(id, delegationTargetObjectModel, delegatedToMe, privilegesList, pageBase);
        }

    @Override
    protected void initHeaderRow(){
        if (delegatedToMe){
            privilegesList = getModelObject().getPrivilegeLimitationList();
        }
        AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
                new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_SELECTED)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        selected.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return !getModel().getObject().isSimpleView();
            }
        });
        headerRow.add(selected);
        Label arrowIcon = new Label(ID_ARROW_ICON);
        headerRow.add(arrowIcon);

        WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        if (delegatedToMe){
            typeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
        } else {
            typeImage.add(AttributeModifier.append("class", AssignmentEditorDtoType.USER.getIconCssClass()));
        }
        headerRow.add(typeImage);

        AjaxLink name = new AjaxLink(ID_NAME) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        headerRow.add(name);

        Label nameLabel;
        if (delegatedToMe) {
            OperationResult result = new OperationResult(OPERATION_GET_TARGET_REF_NAME);
            Task task = pageBase.createSimpleTask(OPERATION_GET_TARGET_REF_NAME);
            nameLabel = new Label(ID_NAME_LABEL,
                    WebModelServiceUtils.resolveReferenceName(getModelObject().getTargetRef(), pageBase, task, result));
        } else {
            nameLabel = new Label(ID_NAME_LABEL, pageBase.createStringResource("DelegationEditorPanel.meLabel"));
        }
        nameLabel.setOutputMarkupId(true);
        name.add(nameLabel);

        AssignmentEditorDto dto = getModelObject();
        dto.getTargetRef();

        WebMarkupContainer delegatedToTypeImage = new WebMarkupContainer(ID_DELEGATED_TO_IMAGE);
        if (delegatedToMe){
            delegatedToTypeImage.add(AttributeModifier.append("class", AssignmentEditorDtoType.USER.getIconCssClass()));
        } else {
            delegatedToTypeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
        }
        headerRow.add(delegatedToTypeImage);

        AjaxLink delegatedToName = new AjaxLink(ID_DELEGATED_TO) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        headerRow.add(delegatedToName);

        Label delegatedToNameLabel;
        if (delegatedToMe) {
            delegatedToNameLabel = new Label(ID_DELEGATED_TO_LABEL, pageBase.createStringResource("DelegationEditorPanel.meLabel"));
        } else {
            delegatedToNameLabel = new Label(ID_DELEGATED_TO_LABEL, getUserDisplayName());
        }
        delegatedToNameLabel.setOutputMarkupId(true);
        delegatedToName.add(delegatedToNameLabel);

        ToggleIconButton expandButton = new ToggleIconButton(ID_EXPAND, GuiStyleConstants.CLASS_ICON_EXPAND,
                GuiStyleConstants.CLASS_ICON_COLLAPSE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }

            @Override
            public boolean isOn() {
                return !DelegationEditorPanel.this.getModelObject().isMinimized();
            }
        };
        headerRow.add(expandButton);
    }

    protected void initBodyLayout(WebMarkupContainer body) {
        DateInput validFrom = new DateInput(ID_DELEGATION_VALID_FROM,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
        validFrom.setEnabled(getModel().getObject().isEditable());
        body.add(validFrom);

        DateInput validTo = new DateInput(ID_DELEGATION_VALID_TO,
                createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validTo")));
        validTo.setEnabled(getModel().getObject().isEditable());
        body.add(validTo);

        TextArea<String> description = new TextArea<>(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        description.setEnabled(getModel().getObject().isEditable());
        body.add(description);

        addOrReplacePrivilegesPanel(body);
        AjaxButton limitPrivilegesButton = new AjaxButton(ID_LIMIT_PRIVILEGES_BUTTON,
                pageBase.createStringResource("DelegationEditorPanel.limitPrivilegesButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPreviewDialog assignmentPreviewDialog =
                        new AssignmentPreviewDialog(pageBase.getMainPopupBodyId(),
                                privilegesList,
                                null, pageBase, true){
                            @Override
                            protected boolean isDelegationPreview(){
                                return true;
                            }

                            @Override
                            public StringResourceModel getTitle() {
                                return new StringResourceModel("AssignmentPreviewDialog.delegationPreviewLabel");
                            }

                            @Override
                            protected void addButtonClicked(AjaxRequestTarget target, List<AssignmentsPreviewDto> dtoList){
                                DelegationEditorPanel.this.getModelObject().setPrivilegeLimitationList(dtoList);
                                pageBase.hideMainPopup(target);
                                reloadBodyComponent(target);
                            }
                        };
                pageBase.showMainPopup(assignmentPreviewDialog, target);
            }
        };
        limitPrivilegesButton.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return UserDtoStatus.ADD.equals(getModelObject().getStatus());
            }
        });
        body.add(limitPrivilegesButton);

        AjaxCheckBox approvalRights = new AjaxCheckBox(ID_DELEGATE_APPROVAL_WI,
                new Model<Boolean>(false)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        approvalRights.setOutputMarkupId(true);
        body.add(approvalRights);

        AjaxCheckBox certificationRights = new AjaxCheckBox(ID_DELEGATE_CERTIFICATION_WI,
                new Model<Boolean>(false)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        certificationRights.setOutputMarkupId(true);
        body.add(certificationRights);

        AjaxCheckBox managementWorkItems = new AjaxCheckBox(ID_DELEGATE_MANAGEMENT_WI,
                new Model<Boolean>(false)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        managementWorkItems.setOutputMarkupId(true);
        body.add(managementWorkItems);
    };

    private void addOrReplacePrivilegesPanel(WebMarkupContainer body){
        privilegesNames = getPrivilegesNamesList();
        ListView<String> privilegesListComponent = new ListView<String>(ID_PRIVILEGES_LIST, privilegesNames){
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                Label privilageNameLabel = new Label(ID_PRIVILEGE, item.getModel());
                item.add(privilageNameLabel);
            }
        };
        privilegesListComponent.setOutputMarkupId(true);
        body.addOrReplace(privilegesListComponent);
    }

    private List<String> getPrivilegesNamesList(){
        List<String> privilegesNamesList = new ArrayList<>();
        List<AssignmentsPreviewDto> dtos = getModel().getObject().getPrivilegeLimitationList();
        if (dtos != null){
            for (AssignmentsPreviewDto assignmentsPreviewDto : dtos){
                    privilegesNamesList.add(assignmentsPreviewDto.getTargetName());
            }
        }
        return privilegesNamesList;
    }

    private void reloadBodyComponent(AjaxRequestTarget target){
        addOrReplacePrivilegesPanel((WebMarkupContainer) DelegationEditorPanel.this.get(ID_BODY));
        target.add(get(ID_BODY));
    }

    private String getUserDisplayName(){
        String displayName = "";
        UserType delegationUser = getModelObject().getDelegationOwner();
        if (getModelObject().getDelegationOwner() != null) {
            if (delegationUser.getFullName() != null && StringUtils.isNotEmpty(delegationUser.getFullName().getOrig())) {
                displayName = delegationUser.getFullName().getOrig() + " (" + delegationUser.getName().getOrig() + ")";
            } else {
                displayName = delegationUser.getName().getOrig();
            }
        }
        return displayName;
    }
}
