/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.PageUser;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.component.DelegationTargetLimitationDialog;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
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
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.addAjaxOnUpdateBehavior;

/**
 * Created by honchar.
 */
public class DelegationEditorPanel extends AssignmentEditorPanel {
    private static final long serialVersionUID = 1L;

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
    private static final String ID_ACTIVATION = "activation";
    private static final String ID_PRIVILEGES_LIST = "privilegesList";
    private static final String ID_PRIVILEGE = "privilege";
    private static final String ID_LIMIT_PRIVILEGES_BUTTON = "limitPrivilegesButton";
    private List<String> privilegesNames = new ArrayList<>();
    private static final String ID_EXPAND = "expand";
    private static final String ID_DELEGATE_APPROVAL_WI = "approvalWorkItems";
    private static final String ID_DELEGATE_CERTIFICATION_WI = "certificationWorkItems";
    private static final String ID_DELEGATE_MANAGEMENT_WI = "managementWorkItems";
    private static final String ID_ALLOW_TRANSITIVE = "allowTransitive";
    private static final String ID_ASSIGNMENT_PRIVILEGES_CHECKBOX = "assignmentPrivilegesCheckbox";
    private static final String ID_ASSIGNMENT_PRIVILEGES_LABEL = "assignmentPrivilegesLabel";
    private static final String ID_ASSIGNMENT_PRIVILEGES_CONTAINER = "assignmentPrivilegesContainer";
    private static final String ID_ASSIGNMENT_PRIVILEGES_LABEL_CONTAINER = "assignmentPrivilegesLabelContainer";

    private static final String DOT_CLASS = DelegationEditorPanel.class.getName() + ".";
    private static final String OPERATION_GET_TARGET_REF_NAME = DOT_CLASS + "getTargetRefName";

    private List<UserType> usersToUpdate;

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> delegationTargetObjectModel, boolean delegatedToMe,
                                 List<AssignmentInfoDto> privilegesList) {
        super(id, delegationTargetObjectModel, delegatedToMe, new LoadableModel<List<AssignmentInfoDto>>(false) {
            @Override
            protected List<AssignmentInfoDto> load() {
                return privilegesList;
            }
        });
    }

    public DelegationEditorPanel(String id, IModel<AssignmentEditorDto> delegationTargetObjectModel, boolean delegatedToMe,
                                 LoadableModel<List<AssignmentInfoDto>> privilegesListModel) {
            super(id, delegationTargetObjectModel, delegatedToMe, privilegesListModel);
        }

    @Override
    protected void initHeaderRow(){
        PageBase pageBase = getPageBase();
        if (delegatedToMe) {
            privilegesListModel = new LoadableModel<List<AssignmentInfoDto>>(false) {
                @Override
                protected List<AssignmentInfoDto> load() {
                    return DelegationEditorPanel.this.getModelObject().getPrivilegeLimitationList();
                }
            };
        }
        AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
                new PropertyModel<>(getModel(), AssignmentEditorDto.F_SELECTED)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(DelegationEditorPanel.this.get("headerRow"));
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

        AjaxLink<Void> name = new AjaxLink<Void>(ID_NAME) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (delegatedToMe){
                    String oid = DelegationEditorPanel.this.getModelObject().getTargetRef().getOid();
                    navigateToDetails(target, oid);
                } else {
                    nameClickPerformed(target);
                }
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
                    IconAndStylesUtil.createDefaultIcon(((PageUser)pageBase).getObjectDetailsModels().getObjectWrapperModel().getObject().getObject())));
        } else {
            if (getModelObject().getDelegationOwner() != null) {
                delegatedToTypeImage.add(AttributeModifier.append("class",
                    IconAndStylesUtil.createDefaultIcon(getModelObject().getDelegationOwner().asPrismObject())));
            }
        }
        headerRow.add(delegatedToTypeImage);

        AjaxLink<Void> delegatedToName = new AjaxLink<Void>(ID_DELEGATED_TO) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (delegatedToMe) {
                    nameClickPerformed(target);
                } else {
                    String oid = DelegationEditorPanel.this.getModelObject().getDelegationOwner().getOid();
                    navigateToDetails(target, oid);
                }
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

        IModel<String> activationModel = () -> {
            AssignmentEditorDto assignmentDto = getModelObject();
            if (assignmentDto == null) {
                return "";
            }

            ActivationType activation = assignmentDto.getActivation();
            if (activation == null) {
                return "";
            }

            if (activation.getAdministrativeStatus() != null) {
                return activation.getAdministrativeStatus().value();
            }

            return AssignmentsUtil.createActivationTitleModel(activation, getPageBase()).getObject();
        };
        Label activation = new Label(ID_ACTIVATION, activationModel);
        headerRow.add(activation);

        ToggleIconButton<Void> expandButton = new ToggleIconButton<Void>(ID_EXPAND, GuiStyleConstants.CLASS_ICON_EXPAND,
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

    private void navigateToDetails(AjaxRequestTarget target, String oid) {
        if (oid == null) {
            nameClickPerformed(target);
            return;
        }

        ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), createStringResource("DelegationEditorPanel.navigate.confirmed", getUserDisplayName())) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                PageParameters params = new PageParameters();
                params.add(OnePageParameterEncoder.PARAMETER, oid);
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil.getObjectDetailsPage(UserType.class);
                getPageBase().navigateToNext(detailsPageClass, params);
            }
        };
        getPageBase().showMainPopup(confirmationPanel, target);
    }

    protected void initBodyLayout(WebMarkupContainer body) {
        DateTimePickerPanel validFrom = DateTimePickerPanel.createByDateModel(ID_DELEGATION_VALID_FROM,
                AssignmentsUtil.createDateModel(new PropertyModel<>(getModel(),
                    AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
        validFrom.setEnabled(getModel().getObject().isEditable());
        body.add(validFrom);

        DateTimePickerPanel validTo = DateTimePickerPanel.createByDateModel(ID_DELEGATION_VALID_TO,
                AssignmentsUtil.createDateModel(new PropertyModel<>(getModel(),
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
                List<AssignmentInfoDto> privilegesList = privilegesListModel.getObject();
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
                                && !getModelObject().getPrivilegeLimitationList().isEmpty();
                    }

                    @Override
                    public void setObject(Boolean value){
                        if (value){
                            getModelObject().setPrivilegeLimitationList(getSelectedPrivilegeList());
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
                return getModel().getObject().isEditable();
            }
        });
        assignmentPrivilegesContainer.add(assignmentPrivilegesCheckbox);

        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_ASSIGNMENT_PRIVILEGES_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);
        assignmentPrivilegesContainer.add(labelContainer);

        Label assignmentPrivilegesLabel = new Label(ID_ASSIGNMENT_PRIVILEGES_LABEL,
                createStringResource("DelegationEditorPanel.allPrivilegesLabel"));
        assignmentPrivilegesLabel.setOutputMarkupId(true);
        labelContainer.add(assignmentPrivilegesLabel);

        addPrivilegesPanel(assignmentPrivilegesContainer);
        AjaxButton limitPrivilegesButton = new AjaxButton(ID_LIMIT_PRIVILEGES_BUTTON,
                createStringResource("DelegationEditorPanel.limitPrivilegesButton")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                DelegationTargetLimitationDialog assignmentsDialog =
                        new DelegationTargetLimitationDialog(getPageBase().getMainPopupBodyId(),
                                selectExistingPrivileges(privilegesListModel.getObject()), getPageBase()) {
                            @Override
                            protected void addButtonClicked(AjaxRequestTarget target, List<AssignmentInfoDto> dtoList){
                                DelegationEditorPanel.this.getModelObject().setPrivilegeLimitationList(dtoList);
                                getPageBase().hideMainPopup(target);
                            }
                        };
                getPageBase().showMainPopup(assignmentsDialog, target);
            }
        };
        limitPrivilegesButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible(){
                return getModel().getObject().isEditable() && assignmentPrivilegesCheckbox.getModelObject();
            }
        });
        labelContainer.add(limitPrivilegesButton);

        AjaxCheckBox approvalRights = new AjaxCheckBox(ID_DELEGATE_APPROVAL_WI,
                new IModel<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject() {
                        AssignmentEditorDto dto = getModelObject();
                        OtherPrivilegesLimitationType limitation = dto.getPrivilegesLimitation();
                        if (limitation == null) {
                            return false;
                        }
                        var newForm = limitation.getCaseManagementWorkItems();
                        if (newForm != null) {
                            return Boolean.TRUE.equals(newForm.isAll());
                        }
                        var legacyForm = limitation.getApprovalWorkItems();
                        if (legacyForm != null) {
                            return Boolean.TRUE.equals(legacyForm.isAll());
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public void setObject(Boolean value) {
                        AssignmentEditorDto dto = getModelObject();
                        OtherPrivilegesLimitationType limitations = dto.getPrivilegesLimitation();
                        if (limitations == null) {
                            limitations = new OtherPrivilegesLimitationType();
                            dto.setPrivilegesLimitation(limitations);
                        }

                        // "approval work items" item is deprecated, "case management" is the replacement
                        limitations.setCaseManagementWorkItems(
                                new WorkItemSelectorType()
                                        .all(value));
                        limitations.setApprovalWorkItems(null); // removing the legacy form
                    }

                    @Override
                    public void detach() {
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        approvalRights.setOutputMarkupId(true);
        approvalRights.add(new AjaxFormComponentUpdatingBehavior("blur") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        approvalRights.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isEnabled(){
                return getModel().getObject().isEditable();
            }
        });
        body.add(approvalRights);

        AjaxCheckBox certificationRights = new AjaxCheckBox(ID_DELEGATE_CERTIFICATION_WI,
                new IModel<>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject() {
                        AssignmentEditorDto dto = getModelObject();
                        if (dto.getPrivilegesLimitation() == null ||
                                dto.getPrivilegesLimitation().getCertificationWorkItems() == null ||
                                dto.getPrivilegesLimitation().getCertificationWorkItems().isAll() == null) {
                            return false;
                        }
                        return dto.getPrivilegesLimitation().getCertificationWorkItems().isAll();
                    }

                    @Override
                    public void setObject(Boolean value) {
                        AssignmentEditorDto dto = getModelObject();
                        OtherPrivilegesLimitationType limitations = dto.getPrivilegesLimitation();
                        if (limitations == null) {
                            limitations = new OtherPrivilegesLimitationType();
                            dto.setPrivilegesLimitation(limitations);
                        }

                        limitations.setCertificationWorkItems(
                                new WorkItemSelectorType()
                                        .all(value));
                    }

                    @Override
                    public void detach() {
                    }
                }) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
            }
        };
        certificationRights.add(new AjaxFormComponentUpdatingBehavior("blur") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        certificationRights.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isEnabled(){
                return getModel().getObject().isEditable();
            }
        });
        certificationRights.setOutputMarkupId(true);
        body.add(certificationRights);

        AjaxCheckBox managementWorkItems = new AjaxCheckBox(ID_DELEGATE_MANAGEMENT_WI,
            new Model<>(false)) {
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

        AjaxCheckBox allowTransitive = new AjaxCheckBox(ID_ALLOW_TRANSITIVE,
                new IModel<Boolean>(){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Boolean getObject() {
                        return getModelObject().isLimitTargetAllowTransitive();
                    }

                    @Override
                    public void setObject(Boolean value){
                        getModelObject().setLimitTargetAllowTransitive(value);
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
        allowTransitive.add(new AjaxFormComponentUpdatingBehavior("blur") {
            private static final long serialVersionUID = 1L;
            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        });
        allowTransitive.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isEnabled(){
                return getModel().getObject().isEditable();
            }
        });
        allowTransitive.setOutputMarkupId(true);
        body.add(allowTransitive);

        addAjaxOnUpdateBehavior(body);
    }

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

    private List<String> getPrivilegesNamesList() {
        List<String> privilegesNamesList = new ArrayList<>();
        List<AssignmentInfoDto> dtos = getModel().getObject().getPrivilegeLimitationList();
        if (dtos != null){
            for (AssignmentInfoDto assignmentInfoDto : dtos) {
                privilegesNamesList.add(assignmentInfoDto.getTargetName());
            }
        }
        return privilegesNamesList;
    }

    private String getUserDisplayName(){
        String displayName = "";
        if (delegatedToMe) {
            OperationResult result = new OperationResult(OPERATION_GET_TARGET_REF_NAME);
            Task task = getPageBase().createSimpleTask(OPERATION_GET_TARGET_REF_NAME);
            displayName = WebModelServiceUtils.resolveReferenceName(getModelObject().getTargetRef(), getPageBase(), task, result);
        } else {
            UserType delegationUser = getModelObject().getDelegationOwner();
            if (getModelObject().getDelegationOwner() != null) {
                if (delegationUser.getFullName() != null && StringUtils.isNotEmpty(delegationUser.getFullName().getOrig())) {
                    displayName = delegationUser.getFullName().getOrig() + " (" + delegationUser.getName().getOrig() + ")";
                } else {
                    displayName = delegationUser.getName() != null ? delegationUser.getName().getOrig() : "";
                }
            }
        }
        return displayName;
    }

    private List<AssignmentInfoDto> selectExistingPrivileges(List<AssignmentInfoDto> list) {
        for (AssignmentInfoDto dto : list) {
            dto.setSelected(false);
        }
        if (getModelObject().getPrivilegeLimitationList() == null) {
            return list;
        }
        for (AssignmentInfoDto dto : list) {
            if (getModelObject().getPrivilegeLimitationList().contains(dto)) {
                dto.setSelected(true);
            }
        }
        return list;
    }

    protected IModel<String> createHeaderClassModel(final IModel<AssignmentEditorDto> model) {
        return () -> {
            if (model != null && model.getObject() != null) {
                if (UserDtoStatus.ADD.equals(model.getObject().getStatus())) {
                    return "table-success";
                }
                if (UserDtoStatus.DELETE.equals(model.getObject().getStatus())) {
                    return "table-danger";
                }
            }
            return "";
        };
    }

    private List<AssignmentInfoDto> getSelectedPrivilegeList() {
        return privilegesListModel.getObject().stream().filter(Selectable::isSelected).toList();
    }
}
