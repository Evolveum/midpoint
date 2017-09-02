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

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentPreviewDialog;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtherPrivilegesLimitationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemSelectorType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.addAjaxOnUpdateBehavior;

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
    private static final String ID_ASSIGNMENT_PRIVILEGES_CHECKBOX = "assignmentPrivilegesCheckbox";
    private static final String ID_ASSIGNMENT_PRIVILEGES_LABEL = "assignmentPrivilegesLabel";
    private static final String ID_ASSIGNMENT_PRIVILEGES_CONTAINER = "assignmentPrivilegesContainer";
    private static final String ID_ASSIGNMENT_PRIVILEGES_LABEL_CONTAINER = "assignmentPrivilegesLabelContainer";

    private static final String DOT_CLASS = DelegationEditorPanel.class.getName() + ".";
    private static final String OPERATION_GET_TARGET_REF_NAME = DOT_CLASS + "getTargetRefName";

    private List<UserType> usersToUpdate;

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> delegationTargetObjectModel, boolean delegatedToMe,
                                 List<AssignmentsPreviewDto> privilegesList, PageBase pageBase) {
        super(id, delegationTargetObjectModel, delegatedToMe, new LoadableModel<List<AssignmentsPreviewDto>>(false) {
            @Override
            protected List<AssignmentsPreviewDto> load() {
                return privilegesList;
            }
        }, pageBase);
    }

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> delegationTargetObjectModel, boolean delegatedToMe,
                                 LoadableModel<List<AssignmentsPreviewDto>> privilegesListModel, PageBase pageBase) {
            super(id, delegationTargetObjectModel, delegatedToMe, privilegesListModel, pageBase);
        }

    @Override
    protected void initHeaderRow(){
        if (delegatedToMe) {
            privilegesListModel = new LoadableModel<List<AssignmentsPreviewDto>>(false) {
                @Override
                protected List<AssignmentsPreviewDto> load() {
                    return DelegationEditorPanel.this.getModelObject().getPrivilegeLimitationList();
                }
            };
        }
        AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
                new PropertyModel<>(getModel(), AssignmentEditorDto.F_SELECTED)) {
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
        typeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
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
            delegatedToTypeImage.add(AttributeModifier.append("class",
                    WebComponentUtil.createDefaultIcon(((PageUser)pageBase).getObjectWrapper().getObject())));
        } else {
            if (getModelObject().getDelegationOwner() != null) {
                delegatedToTypeImage.add(AttributeModifier.append("class",
                    WebComponentUtil.createDefaultIcon(getModelObject().getDelegationOwner().asPrismObject())));
            }
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
                AssignmentsUtil.createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
        validFrom.setEnabled(getModel().getObject().isEditable());
        body.add(validFrom);

        DateInput validTo = new DateInput(ID_DELEGATION_VALID_TO,
                AssignmentsUtil.createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validTo")));
        validTo.setEnabled(getModel().getObject().isEditable());
        body.add(validTo);

        TextArea<String> description = new TextArea<>(ID_DESCRIPTION,
                new PropertyModel<String>(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        description.setEnabled(getModel().getObject().isEditable());
        body.add(description);

        WebMarkupContainer assignmentPrivilegesContainer = new WebMarkupContainer(ID_ASSIGNMENT_PRIVILEGES_CONTAINER);
        assignmentPrivilegesContainer.setOutputMarkupId(true);
        assignmentPrivilegesContainer.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                if (!UserDtoStatus.ADD.equals(getModelObject().getStatus())){
                    return true;
                }
                List<AssignmentsPreviewDto> privilegesList = privilegesListModel.getObject();
                return privilegesList != null && privilegesList.size() > 0;
            }
        });
        body.add(assignmentPrivilegesContainer);

        AjaxCheckBox assignmentPrivilegesCheckbox = new AjaxCheckBox(ID_ASSIGNMENT_PRIVILEGES_CHECKBOX,
                new IModel<Boolean>(){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject(){
                        return getModelObject().getPrivilegeLimitationList() != null
                                && getModelObject().getPrivilegeLimitationList().size() > 0;
                    }

                    @Override
                    public void setObject(Boolean value){
                        if (value){
                            getModelObject().setPrivilegeLimitationList(privilegesListModel.getObject());
                        } else {
                            getModelObject().setPrivilegeLimitationList(new ArrayList<>());
                        }
                    }

                    @Override
                    public void detach(){
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(DelegationEditorPanel.this.get(ID_BODY).get(ID_ASSIGNMENT_PRIVILEGES_CONTAINER));
            }
        };
        assignmentPrivilegesCheckbox.setOutputMarkupId(true);
        assignmentPrivilegesCheckbox.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        assignmentPrivilegesCheckbox.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isEnabled(){
                return UserDtoStatus.ADD.equals(getModelObject().getStatus());
            }
        });
        assignmentPrivilegesContainer.add(assignmentPrivilegesCheckbox);

        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_ASSIGNMENT_PRIVILEGES_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);
        assignmentPrivilegesContainer.add(labelContainer);

        Label assignmentPrivilegesLabel = new Label(ID_ASSIGNMENT_PRIVILEGES_LABEL,
                createStringResource("DelegationEditorPanel.allPrivilegesLabel"));
        assignmentPrivilegesLabel.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
               return true;
            }
        });
        assignmentPrivilegesLabel.setOutputMarkupId(true);
        labelContainer.add(assignmentPrivilegesLabel);

        addPrivilegesPanel(assignmentPrivilegesContainer);
        AjaxButton limitPrivilegesButton = new AjaxButton(ID_LIMIT_PRIVILEGES_BUTTON,
                pageBase.createStringResource("DelegationEditorPanel.limitPrivilegesButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                AssignmentPreviewDialog assignmentPreviewDialog =
                        new AssignmentPreviewDialog(pageBase.getMainPopupBodyId(),
                                selectExistingPrivileges(privilegesListModel.getObject()),
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
                            }
                        };
                pageBase.showMainPopup(assignmentPreviewDialog, target);
            }
        };
        limitPrivilegesButton.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return UserDtoStatus.ADD.equals(getModelObject().getStatus()) &&
                        assignmentPrivilegesCheckbox.getModelObject();
            }
        });
        labelContainer.add(limitPrivilegesButton);

        AjaxCheckBox approvalRights = new AjaxCheckBox(ID_DELEGATE_APPROVAL_WI,
                new IModel<Boolean>(){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject(){
                        AssignmentEditorDto dto = getModelObject();
                        if (dto.getPrivilegesLimitation() == null ||
                                dto.getPrivilegesLimitation().getApprovalWorkItems() == null ||
                                dto.getPrivilegesLimitation().getApprovalWorkItems().isAll() == null){
                            return false;
                        }
                        return dto.getPrivilegesLimitation().getApprovalWorkItems().isAll();
                    }

                    @Override
                    public void setObject(Boolean value){
                        AssignmentEditorDto dto = getModelObject();
                        OtherPrivilegesLimitationType limitations = dto.getPrivilegesLimitation();
                        if (limitations == null ){
                            limitations = new OtherPrivilegesLimitationType();
                            dto.setPrivilegesLimitation(limitations);
                        }

                        WorkItemSelectorType workItemSelector = new WorkItemSelectorType();
                        workItemSelector.all(value);
                        limitations.setApprovalWorkItems(workItemSelector);
                    }

                    @Override
                    public void detach(){
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        approvalRights.setOutputMarkupId(true);
        approvalRights.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        approvalRights.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isEnabled(){
                return getModel().getObject().isEditable();
            }
        });
        body.add(approvalRights);

        AjaxCheckBox certificationRights = new AjaxCheckBox(ID_DELEGATE_CERTIFICATION_WI,
                new IModel<Boolean>(){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject(){
                        AssignmentEditorDto dto = getModelObject();
                        if (dto.getPrivilegesLimitation() == null ||
                                dto.getPrivilegesLimitation().getCertificationWorkItems() == null ||
                                dto.getPrivilegesLimitation().getCertificationWorkItems().isAll() == null){
                            return false;
                        }
                        return dto.getPrivilegesLimitation().getCertificationWorkItems().isAll();
                    }

                    @Override
                    public void setObject(Boolean value){
                        AssignmentEditorDto dto = getModelObject();
                        OtherPrivilegesLimitationType limitations = dto.getPrivilegesLimitation();
                        if (limitations == null ){
                            limitations = new OtherPrivilegesLimitationType();
                            dto.setPrivilegesLimitation(limitations);
                        }

                        WorkItemSelectorType workItemSelector = new WorkItemSelectorType();
                        workItemSelector.all(value);
                        limitations.setCertificationWorkItems(workItemSelector);
                    }

                    @Override
                    public void detach(){
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        certificationRights.add(new AjaxFormComponentUpdatingBehavior("blur") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        certificationRights.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isEnabled(){
                return getModel().getObject().isEditable();
            }
        });
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
        managementWorkItems.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                //TODO not implemented yet
                return false;
            }
        });
        body.add(managementWorkItems);

        addAjaxOnUpdateBehavior(body);
    };

    private void addPrivilegesPanel(WebMarkupContainer body){
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
        privilegesListComponent.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                if (!UserDtoStatus.ADD.equals(getModelObject().getStatus())){
                    return true;
                }
               return false;
            }
        });
        body.addOrReplace(privilegesListComponent);
    }

    private boolean allAssignmentPrivilegesSelected(){
        return getModelObject().getPrivilegeLimitationList() == null ||
                    getModelObject().getPrivilegeLimitationList().size() == 0 ||
                    getModelObject().getPrivilegeLimitationList().size() == privilegesListModel.getObject().size();
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

    private String getUserDisplayName(){
        String displayName = "";
        UserType delegationUser = getModelObject().getDelegationOwner();
        if (getModelObject().getDelegationOwner() != null) {
            if (delegationUser.getFullName() != null && StringUtils.isNotEmpty(delegationUser.getFullName().getOrig())) {
                displayName = delegationUser.getFullName().getOrig() + " (" + delegationUser.getName().getOrig() + ")";
            } else {
                displayName = delegationUser.getName() != null ? delegationUser.getName().getOrig() : "";
            }
        }
        return displayName;
    }

    private List<AssignmentsPreviewDto> selectExistingPrivileges(List<AssignmentsPreviewDto> list){
        for (AssignmentsPreviewDto dto : list){
            dto.setSelected(false);
        }

        if (getModelObject().getPrivilegeLimitationList() == null){
            return list;
        }
        for (AssignmentsPreviewDto dto : list){
            if (getModelObject().getPrivilegeLimitationList().contains(dto)){
                dto.setSelected(true);
            }
        }
        return list;
    }
}
