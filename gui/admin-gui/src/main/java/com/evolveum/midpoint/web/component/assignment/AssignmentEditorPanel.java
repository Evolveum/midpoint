/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerPanel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.schema.processor.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import com.evolveum.midpoint.gui.api.component.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.objecttypeselect.ObjectTypeSelectPanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.focus.PageFocusDetails;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class AssignmentEditorPanel extends BasePanel<AssignmentEditorDto> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentEditorPanel.class);

    private static final String DOT_CLASS = AssignmentEditorPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_LOAD_ATTRIBUTES = DOT_CLASS + "loadAttributes";
    private static final String OPERATION_LOAD_TARGET_OBJECT = DOT_CLASS + "loadItemSecurityDecisions";
    private static final String OPERATION_LOAD_ASSIGNMENT_TARGET_USER_OBJECT = DOT_CLASS + "loadAssignmentTargetUserObject";
    private static final String OPERATION_LOAD_RELATION_DEFINITIONS = DOT_CLASS + "loadRelationDefinitions";

    private static final String ID_HEADER_ROW = "headerRow";
    private static final String ID_SELECTED = "selected";
    private static final String ID_TYPE_IMAGE = "typeImage";
    private static final String ID_NAME_LABEL = "nameLabel";
    private static final String ID_NAME = "name";
    //    private static final String ID_ACTIVATION = "activation";
    private static final String ID_ACTIVATION_BLOCK = "activationBlock";
    private static final String ID_EXPAND = "expand";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    protected static final String ID_BODY = "body";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_FOCUS_TYPE_CONTAINER = "focusTypeContainer";
    protected static final String ID_RELATION = "relation";
    private static final String ID_RELATION_LABEL = "relationLabel";
    private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
    private static final String ID_VALID_FROM = "validFrom";
    private static final String ID_VALID_TO = "validTo";
    private static final String ID_SHOW_EMPTY = "showEmpty";
    private static final String ID_SHOW_EMPTY_LABEL = "showEmptyLabel";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";
    private static final String ID_TARGET = "target";
    private static final String ID_TARGET_CONTAINER = "targetContainer";
    private static final String ID_CONSTRUCTION_CONTAINER = "constructionContainer";
    private static final String ID_CONTAINER_TENANT_REF = "tenantRefContainer";
    private static final String ID_TENANT_CHOOSER = "tenantRefChooser";
    private static final String ID_CONTAINER_ORG_REF = "orgRefContainer";
    private static final String ID_ORG_CHOOSER = "orgRefChooser";
    //    private static final String ID_BUTTON_SHOW_MORE = "errorLink";
//    private static final String ID_ERROR_ICON = "errorIcon";
    private static final String ID_METADATA_CONTAINER = "metadataContainer";
    private static final String ID_PROPERTY_CONTAINER = "propertyContainer";
    private static final String ID_DESCRIPTION_CONTAINER = "descriptionContainer";
    private static final String ID_ADMIN_STATUS_CONTAINER = "administrativeStatusContainer";
    private static final String ID_VALID_FROM_CONTAINER = "validFromContainer";
    private static final String ID_VALID_TO_CONTAINER = "validToContainer";

    private IModel<List<ACAttributeDto>> attributesModel;
    protected WebMarkupContainer headerRow;
    protected IModel<List<AssignmentInfoDto>> privilegesListModel;
    protected boolean delegatedToMe;
    private LoadableDetachableModel<ItemSecurityConstraints> itemSecurityConstraintsModel;

    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model, boolean delegatedToMe,
            LoadableModel<List<AssignmentInfoDto>> privilegesListModel) {
        super(id, model);
        this.privilegesListModel = privilegesListModel;
        this.delegatedToMe = delegatedToMe;

    }

    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);

        attributesModel = new LoadableModel<>(false) {
            @Override
            protected List<ACAttributeDto> load() {
                return loadAttributes();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initDecisionsModel();
        initLayout();
    }

    protected void initLayout() {
        setOutputMarkupId(true);
        headerRow = new WebMarkupContainer(ID_HEADER_ROW);
        headerRow.add(AttributeModifier.append("class", createHeaderClassModel(getModel())));
        headerRow.setOutputMarkupId(true);
        add(headerRow);

        initHeaderRow();

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.setOutputMarkupId(true);
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
                return !editorDto.isMinimized();
            }
        });
        add(body);

        initBodyLayout(body);
    }

    protected void initHeaderRow() {
        AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
                new PropertyModel<>(getModel(), AssignmentEditorDto.F_SELECTED)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                // do we want to update something?
            }
        };
        selected.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !getModel().getObject().isSimpleView();
            }
        });
        headerRow.add(selected);

        WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        typeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
        headerRow.add(typeImage);

        AjaxLink<Void> name = new AjaxLink<>(ID_NAME) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }
        };
        headerRow.add(name);

        Label nameLabel = new Label(ID_NAME_LABEL, createAssignmentNameLabelModel(false));
        nameLabel.setOutputMarkupId(true);
        name.add(nameLabel);

        ToggleIconButton<Void> expandButton = new ToggleIconButton<>(ID_EXPAND, GuiStyleConstants.CLASS_ICON_EXPAND,
                GuiStyleConstants.CLASS_ICON_COLLAPSE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                nameClickPerformed(target);
            }

            @Override
            public boolean isOn() {
                return !AssignmentEditorPanel.this.getModelObject().isMinimized();
            }
        };
//      expandButton.setOutputMarkupId(true);
        headerRow.add(expandButton);

        AjaxButton removeButton = new AjaxButton(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeButtonClickPerformed(AssignmentEditorPanel.this.getModelObject(), target);
            }
        };
        removeButton.add(AttributeAppender.append("title", getPageBase().createStringResource("AssignmentTablePanel.menu.unassign")));
        removeButton.setOutputMarkupId(true);
        headerRow.add(removeButton);
    }

    protected IModel<String> createAssignmentNameLabelModel(final boolean isManager) {
        return new IModel<>() {

            @Override
            public String getObject() {
                if (getModel() != null && getModel().getObject() != null) {
                    AssignmentEditorDto dto = getModelObject();

                    if (dto.getName() != null) {
                        StringBuilder name = new StringBuilder(dto.getName());
                        if (isManager) {
                            name.append(" - Manager");
                        }
                        return name.toString();
                    }

                    if (dto.getAltName() != null) {
                        return getString("AssignmentEditorPanel.name.focus");
                    }
                }

                return getString("AssignmentEditorPanel.name.noTarget");
            }
        };
    }

    protected IModel<String> createHeaderClassModel(final IModel<AssignmentEditorDto> model) {
        return AssignmentsUtil.createAssignmentStatusClassModel(model.getObject().getStatus());
    }

    protected void initBodyLayout(WebMarkupContainer body) {
        WebMarkupContainer propertyContainer = new WebMarkupContainer(ID_PROPERTY_CONTAINER);
        propertyContainer.setOutputMarkupId(true);
        body.add(propertyContainer);

        WebMarkupContainer descriptionContainer = new WebMarkupContainer(ID_DESCRIPTION_CONTAINER);
        descriptionContainer.setOutputMarkupId(true);
        descriptionContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION));
            }
        });
        body.add(descriptionContainer);

        TextArea<String> description = new TextArea<>(ID_DESCRIPTION,
                new PropertyModel<>(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        description.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getModel().getObject().isEditable();
            }
        });
        descriptionContainer.add(description);

        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        relationContainer.setOutputMarkupPlaceholderTag(true);
        relationContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (!isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF,
                        ObjectReferenceType.F_RELATION))) {
                    return false;
                }
                AssignmentEditorDto dto = getModel().getObject();
                if (dto != null) {
                    if (AssignmentEditorDtoType.ORG_UNIT.equals(dto.getType()) ||
                            AssignmentEditorDtoType.SERVICE.equals(dto.getType()) ||
                            AssignmentEditorDtoType.ROLE.equals(dto.getType())) {
                        return true;
                    }
                }

                return false;
            }
        });
        body.add(relationContainer);
        addRelationDropDown(relationContainer);

        WebMarkupContainer focusTypeContainer = new WebMarkupContainer(ID_FOCUS_TYPE_CONTAINER);
        focusTypeContainer.setOutputMarkupId(true);
        focusTypeContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_FOCUS_TYPE));
            }
        });
        body.add(focusTypeContainer);

        ObjectTypeSelectPanel<FocusType> focusType = new ObjectTypeSelectPanel<>(ID_FOCUS_TYPE,
                new PropertyModel<>(getModel(), AssignmentEditorDto.F_FOCUS_TYPE), FocusType.class);
        focusTypeContainer.add(focusType);

        Label relationLabel = new Label(ID_RELATION_LABEL, new IModel<String>() {

            @Override
            public String getObject() {
                if (getModel() == null || getModel().getObject() == null) {
                    return getString("AssignmentEditorPanel.relation.notSpecified");
                }

                AssignmentEditorDto object = getModel().getObject();
                if (object.getTargetRef() != null) {
                    QName relation = object.getTargetRef() != null ? object.getTargetRef().getRelation() : null;
                    String propertyKey = RelationUtil.getRelationHeaderLabelKey(relation);
                    return createStringResource(propertyKey).getString();
                } else {
                    return "";
                }
            }
        });
        relationLabel.setOutputMarkupId(true);
        relationLabel.setOutputMarkupPlaceholderTag(true);
        relationLabel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !isCreatingNewAssignment();
            }
        });
        relationContainer.add(relationLabel);

        WebMarkupContainer tenantRefContainer = createTenantContainer();
        tenantRefContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF));
            }
        });
        body.add(tenantRefContainer);

        WebMarkupContainer orgRefContainer = createOrgContainer();
        orgRefContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF));
            }
        });
        body.add(orgRefContainer);
        propertyContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION))
                        || isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF,
                        ObjectReferenceType.F_RELATION))
                        || isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_FOCUS_TYPE))
                        || isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF))
                        || isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF));
            }

        });

        WebMarkupContainer activationBlock = new WebMarkupContainer(ID_ACTIVATION_BLOCK);
        body.add(activationBlock);

        WebMarkupContainer adminStatusContainer = new WebMarkupContainer(ID_ADMIN_STATUS_CONTAINER);
        adminStatusContainer.setOutputMarkupId(true);
        adminStatusContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
                        ActivationType.F_ADMINISTRATIVE_STATUS));
            }
        });
        activationBlock.add(adminStatusContainer);

        DropDownChoicePanel administrativeStatus = WebComponentUtil.createEnumPanel(
                ActivationStatusType.class, ID_ADMINISTRATIVE_STATUS,
                new PropertyModel<ActivationStatusType>(getModel(), AssignmentEditorDto.F_ACTIVATION + "."
                        + ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart()),
                this);
        administrativeStatus.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getModel().getObject().isEditable();
            }
        });
        adminStatusContainer.add(administrativeStatus);

        WebMarkupContainer validFromContainer = new WebMarkupContainer(ID_VALID_FROM_CONTAINER);
        validFromContainer.setOutputMarkupId(true);
        validFromContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
                        ActivationType.F_VALID_FROM));
            }
        });
        activationBlock.add(validFromContainer);

        DateTimePickerPanel validFrom = DateTimePickerPanel.createByDateModel(ID_VALID_FROM,
                AssignmentsUtil.createDateModel(new PropertyModel<>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
        validFrom.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getModel().getObject().isEditable();
            }
        });
        validFromContainer.add(validFrom);

        WebMarkupContainer validToContainer = new WebMarkupContainer(ID_VALID_TO_CONTAINER);
        validToContainer.setOutputMarkupId(true);
        validToContainer.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
                        ActivationType.F_VALID_TO));
            }
        });
        activationBlock.add(validToContainer);

        DateTimePickerPanel validTo = DateTimePickerPanel.createByDateModel(ID_VALID_TO,
                AssignmentsUtil.createDateModel(new PropertyModel<>(getModel(),
                        AssignmentEditorDto.F_ACTIVATION + ".validTo")));
        validTo.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getModel().getObject().isEditable();
            }
        });
        validToContainer.add(validTo);

        activationBlock.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                // enabled activation in assignments for now.
                return true;
            }
        });

        WebMarkupContainer targetContainer = new WebMarkupContainer(ID_TARGET_CONTAINER);
        targetContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (!isItemAllowed(ItemPath.create(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF))) {
                    return false;
                }
                AssignmentEditorDto dto = getModel().getObject();
                return !AssignmentEditorDtoType.CONSTRUCTION.equals(dto.getType());
            }
        });
        body.add(targetContainer);

        Label target = new Label(ID_TARGET, createTargetModel());
        targetContainer.add(target);

        WebMarkupContainer constructionContainer = new WebMarkupContainer(ID_CONSTRUCTION_CONTAINER);
        constructionContainer.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                AssignmentEditorDto dto = getModel().getObject();
                return AssignmentEditorDtoType.CONSTRUCTION.equals(dto.getType());
            }
        });
        body.add(constructionContainer);

        AjaxLink<Void> showEmpty = new AjaxLink<>(ID_SHOW_EMPTY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showEmptyPerformed(target);
            }
        };
        constructionContainer.add(showEmpty);

        Label showEmptyLabel = new Label(ID_SHOW_EMPTY_LABEL, createShowEmptyLabel());
        showEmptyLabel.setOutputMarkupId(true);
        showEmpty.add(showEmptyLabel);

        initAttributesLayout(constructionContainer);

        Component metadataPanel;
        if (UserDtoStatus.ADD.equals(getModel().getObject().getStatus()) ||
                getModelObject().getOldValue().asContainerable() == null) {
            metadataPanel = new WebMarkupContainer(ID_METADATA_CONTAINER);
        } else {
            metadataPanel = new MetadataPanel(ID_METADATA_CONTAINER, new IModel<>() {
                @Override
                public MetadataType getObject() {
                    return getModelObject().getOldValue().getValue().getMetadata();
                }
            }, "", "row");
        }
        metadataPanel.setOutputMarkupId(true);
        metadataPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !UserDtoStatus.ADD.equals(getModel().getObject().getStatus());
            }
        });
        body.add(metadataPanel);

        WebComponentUtil.addAjaxOnUpdateBehavior(body);
    }

    private WebMarkupContainer createTenantContainer() {
        WebMarkupContainer tenantRefContainer = new WebMarkupContainer(ID_CONTAINER_TENANT_REF);
        ChooseTypePanel<?> tenantRef = new ChooseTypePanel<>(ID_TENANT_CHOOSER,
                new PropertyModel<>(getModel(), AssignmentEditorDto.F_TENANT_REF)) {

            @Override
            protected ObjectQuery getChooseQuery() {
                return getPageBase().getPrismContext().queryFor(OrgType.class)
                        .item(OrgType.F_TENANT).eq(true)
                        .build();
            }
        };
        tenantRef.setPanelEnabled(getModel().getObject().isEditable());
        tenantRefContainer.add(tenantRef);
        tenantRefContainer.add(new VisibleBehaviour(() -> {
            AssignmentEditorDto dto = getModel().getObject();
            if (dto != null) {
                if (AssignmentEditorDtoType.ROLE.equals(dto.getType())) {
                    return true;
                }
            }

            return false;
        }));
        return tenantRefContainer;
    }

    private WebMarkupContainer createOrgContainer() {
        WebMarkupContainer tenantRefContainer = new WebMarkupContainer(ID_CONTAINER_ORG_REF);
        ChooseTypePanel<?> tenantRef = new ChooseTypePanel<>(ID_ORG_CHOOSER,
                new PropertyModel<>(getModel(), AssignmentEditorDto.F_ORG_REF)) {

            @Override
            protected ObjectQuery getChooseQuery() {
                return getPageBase().getPrismContext().queryFor(OrgType.class)
                        .item(OrgType.F_TENANT).eq(false)
                        .or().item(OrgType.F_TENANT).isNull()
                        .build();
            }
        };
        tenantRefContainer.add(tenantRef);
        tenantRef.setEnabled(getModel().getObject().isEditable());
        tenantRefContainer.add(new VisibleBehaviour(() -> {
            AssignmentEditorDto dto = getModel().getObject();
            if (dto != null) {
                if (AssignmentEditorDtoType.ROLE.equals(dto.getType())) {
                    return true;
                }
            }

            return false;
        }));
        return tenantRefContainer;
    }

    private void initAttributesLayout(WebMarkupContainer constructionContainer) {
        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
        attributes.add(new VisibleBehaviour(() -> {
            AssignmentEditorDto dto = getModel().getObject();
            return AssignmentEditorDtoType.CONSTRUCTION.equals(dto.getType());
        }));
        attributes.setEnabled(getModel().getObject().isEditable());
        constructionContainer.add(attributes);

        ListView<ACAttributeDto> attribute = new ListView<>(ID_ATTRIBUTE, attributesModel) {

            @Override
            protected void populateItem(ListItem<ACAttributeDto> listItem) {
                final IModel<ACAttributeDto> attrModel = listItem.getModel();
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, attrModel, ignoreMandatoryAttributes());
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);
                listItem.setOutputMarkupId(true);

                listItem.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        AssignmentEditorDto editorDto = AssignmentEditorPanel.this.getModel().getObject();
                        if (editorDto.isShowEmpty()) {
                            return true;
                        }

                        ACAttributeDto dto = attrModel.getObject();
                        return !dto.isEmpty();
                    }
                });
            }
        };
        attributes.add(attribute);
        // todo extension
    }

    private IModel<String> createShowEmptyLabel() {
        return new IModel<>() {

            @Override
            public String getObject() {
                AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();

                if (dto.isShowEmpty()) {
                    return getString("AssignmentEditorPanel.hideEmpty");
                } else {
                    return getString("AssignmentEditorPanel.showEmpty");
                }
            }
        };
    }

    private void showEmptyPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = AssignmentEditorPanel.this.getModel().getObject();
        dto.setShowEmpty(!dto.isShowEmpty());

        WebMarkupContainer parent = (WebMarkupContainer) get(
                createComponentPath(ID_BODY, ID_CONSTRUCTION_CONTAINER));

        target.add(parent.get(ID_ATTRIBUTES),
                parent.get(createComponentPath(ID_SHOW_EMPTY, ID_SHOW_EMPTY_LABEL)),
                getPageBase().getFeedbackPanel());
    }

    private List<ACAttributeDto> loadAttributes() {
        AssignmentEditorDto dto = getModel().getObject();

        OperationResult result = new OperationResult(OPERATION_LOAD_ATTRIBUTES);
        List<ACAttributeDto> attributes = new ArrayList<>();
        try {
            ConstructionType construction = WebComponentUtil.getContainerValue(dto.getOldValue(),
                    AssignmentType.F_CONSTRUCTION);

            if (construction == null) {
                return attributes;
            }

            PrismObject<ResourceType> resource = getReference(construction.getResourceRef(), result);

            PrismContext prismContext = getPageBase().getPrismContext();
            ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource, LayerType.PRESENTATION);
            ResourceObjectDefinition objectClassDefinition = refinedSchema.findDefinitionForConstruction(construction);

            if (objectClassDefinition == null) {
                return attributes;
            }

            PrismContainerDefinition definition = objectClassDefinition
                    .toShadowAttributesContainerDefinition();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Refined definition for {}\n{}", construction, definition.debugDump());
            }

            Collection<ItemDefinition<?>> definitions = definition.getDefinitions();
            for (ItemDefinition<?> attrDef : definitions) {
                if (!(attrDef instanceof PrismPropertyDefinition<?> propertyDef)) {
                    // log skipping or something...
                    continue;
                }

                if (propertyDef.isOperational() || propertyDef.isIgnored()) {
                    continue;
                }
                attributes.add(ACAttributeDto.createACAttributeDto(propertyDef,
                        findOrCreateValueConstruction(propertyDef), prismContext));
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Exception occurred during assignment attribute loading", ex);
            result.recordFatalError(createStringResource("AssignmentEditorPanel.message.loadAttributes.fatalError").getString(), ex);
        } finally {
            result.recomputeStatus();
        }

        attributes.sort((a1, a2) ->
                String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName()));

        if (dto.getAttributes() != null && !dto.getAttributes().isEmpty()) {
            for (ACAttributeDto assignmentAttribute : dto.getAttributes()) {
                for (ACAttributeDto attributeDto : attributes) {
                    if (attributeDto.getName().equals(assignmentAttribute.getName())) {
                        attributes.set(attributes.indexOf(attributeDto), assignmentAttribute);
                        continue;
                    }
                }
            }
        }

        dto.setAttributes(attributes);

        getPageBase().showResult(result, false);

        return dto.getAttributes();
    }

    private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
        if (ref.asReferenceValue().getObject() != null) {
            return ref.asReferenceValue().getObject();
        }
        OperationResult subResult = result.createSubresult(OPERATION_LOAD_RESOURCE);
        subResult.addParam("targetRef", ref.getOid());
        PrismObject target = null;
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
            Class type = ObjectType.class;
            if (ref.getType() != null) {
                type = getPageBase().getPrismContext().getSchemaRegistry()
                        .determineCompileTimeClass(ref.getType());
            }
            target = getPageBase().getModelService().getObject(type, ref.getOid(), null, task, subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get account construction resource ref", ex);
            subResult.recordFatalError(createStringResource("AssignmentEditorPanel.message.getReference.fatalError").getString(), ex);
        }

        return target;
    }

    private ResourceAttributeDefinitionType findOrCreateValueConstruction(
            PrismPropertyDefinition<?> attrDef) {
        ResourceAttributeDefinitionType construction = new ResourceAttributeDefinitionType();
        construction.setRef(new ItemPathType(ItemPath.create(attrDef.getItemName())));

        return construction;
    }

    protected IModel<String> createImageTypeModel(final IModel<AssignmentEditorDto> model) {
        return new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                AssignmentEditorDto assignmentEditorDto = model.getObject();

                PrismObject targetObject = null;
                try {
                    targetObject = getTargetObject(assignmentEditorDto);
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
                    // Otherwise ignore, will be pocessed by the fallback code
                    // below
                }

                if (targetObject == null) {
                    AssignmentEditorDtoType type = assignmentEditorDto.getType();
                    return type.getIconCssClass();
                } else {
                    return IconAndStylesUtil.createDefaultIcon(targetObject);
                }
            }
        };
    }

    protected void nameClickPerformed(AjaxRequestTarget target) {
        AssignmentEditorDto dto = getModel().getObject();
        boolean minimized = dto.isMinimized();
        dto.setMinimized(!minimized);

        target.add(this);
    }

    protected IModel<String> createTargetModel() {
        return new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                AssignmentEditorDto dto = getModel().getObject();

                PrismObject targetObject;
                try {
                    targetObject = getTargetObject(dto);
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load object", ex);
                    return getString("AssignmentEditorPanel.loadError");
                }

                if (targetObject == null) {
                    return getString("AssignmentEditorPanel.undefined");
                }

                return WebComponentUtil.getName(targetObject);
            }
        };
    }

    private void addRelationDropDown(WebMarkupContainer relationContainer) {
        QName assignmentRelation = getModelObject().getTargetRef() != null ? getModelObject().getTargetRef().getRelation() : null;

        RelationDropDownChoicePanel relationDropDown = new RelationDropDownChoicePanel(ID_RELATION,
                assignmentRelation != null ? assignmentRelation : RelationUtil.getDefaultRelationOrFail(), getSupportedRelations(), false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onValueChanged(AjaxRequestTarget target) {
                ObjectReferenceType ref = AssignmentEditorPanel.this.getModelObject().getTargetRef();
                if (ref != null) {
                    ref.setRelation(getRelationValue());
                }
            }

            @Override
            protected IModel<String> getRelationLabelModel() {
                return Model.of();
            }

            @Override
            protected boolean isRelationDropDownEnabled() {
                return isRelationEditable();
            }
        };
        relationDropDown.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isCreatingNewAssignment();
            }
        });
        relationContainer.add(relationDropDown);

    }

    protected boolean isRelationEditable() {
        return getModel().getObject().isEditable();
    }

    private List<QName> getSupportedRelations() {
        OperationResult result = new OperationResult("Relations for self service area");
        AssignmentConstraintsType constraints = AssignmentEditorPanel.this.getModelObject().getDefaultAssignmentConstraints();
        if (constraints == null ||
                constraints.isAllowSameTarget() && constraints.isAllowSameRelation()) {
            return RelationUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, getPageBase());
        } else {
            return getModelObject().getNotAssignedRelationsList();
        }
    }

//    protected IModel<RelationTypes> getRelationModel() {
//        return new IModel<RelationTypes>() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public RelationTypes getObject() {
//                if (getModelObject().getTargetRef() == null) {
//                    return RelationTypes.MEMBER;
//                }
//                return RelationTypes.getRelationType(getModelObject().getTargetRef().getRelation());
//            }
//
//            @Override
//            public void setObject(RelationTypes newValue) {
//                ObjectReferenceType ref = getModelObject().getTargetRef();
//                if (ref != null){
//                    ref.setRelation(newValue.getRelation());
//                }
//            }
//
//            @Override
//            public void detach() {
//
//            }
//        };
//    }

    private <O extends ObjectType> PrismObject<O> getTargetObject(AssignmentEditorDto dto)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismContainerValue<AssignmentType> assignment = dto.getOldValue();

        PrismReference targetRef = assignment.findReference(AssignmentType.F_TARGET_REF);
        if (targetRef == null) {
            return null;
        }

        PrismReferenceValue refValue = targetRef.getValue();
        if (refValue != null && refValue.getObject() != null) {
            PrismObject object = refValue.getObject();
            return object;
        }

        String oid = targetRef.getOid();
        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT);

        PageBase page = getPageBase();
        ModelService model = page.getMidpointApplication().getModel();
        Task task = page.createSimpleTask(OPERATION_LOAD_OBJECT);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
                .createCollection(GetOperationOptions.createNoFetch());
        Class<O> type = (Class<O>) ObjectType.class;
        if (refValue.getTargetType() != null) {
            type = getPageBase().getPrismContext().getSchemaRegistry()
                    .determineCompileTimeClass(refValue.getTargetType());
        }
        PrismObject<O> object = model.getObject(type, oid, options, task, result);
        refValue.setObject(object);
        return object;
    }

    private void showErrorPerformed(AjaxRequestTarget target) {
        error(getString("AssignmentEditorPanel.targetError"));
        target.add(getPageBase().getFeedbackPanel());
    }

    /**
     * Override to provide the information if object that contains this
     * assignment is being edited or created.
     */
    protected boolean isCreatingNewAssignment() {
        if (getModelObject() == null) {
            return false;
        }

        return UserDtoStatus.ADD.equals(getModelObject().getStatus());
    }

    protected boolean ignoreMandatoryAttributes() {
        return false;
    }

    private void initDecisionsModel() {
        itemSecurityConstraintsModel = new LoadableDetachableModel<>() {
            @Override
            protected ItemSecurityConstraints load() {
                return loadSecurityConstraints();
            }
        };

    }

    private boolean isItemAllowed(ItemPath itemPath) {
        ItemSecurityConstraints constraints = itemSecurityConstraintsModel.getObject();
        return itemPath == null
                || constraints == null
                || constraints.findItemDecision(itemPath) == AuthorizationDecisionType.ALLOW;
    }

    private ItemSecurityConstraints loadSecurityConstraints() {
        PageBase pageBase = getPageBase();
        if (pageBase == null || getModelObject().getTargetRef() == null) {
            return null;
        }
        PrismObject<? extends FocusType> operationObject = null;
        if (pageBase instanceof PageFocusDetails) {
            operationObject = ((PageFocusDetails) pageBase).getPrismObject();
        }
        if (operationObject == null) {
            return null;
        }
        String targetObjectOid = getModelObject().getTargetRef().getOid();

        Task task = pageBase.createSimpleTask(OPERATION_LOAD_TARGET_OBJECT);
        OperationResult result = new OperationResult(OPERATION_LOAD_TARGET_OBJECT);
        PrismObject<AbstractRoleType> targetRefObject = WebModelServiceUtils.loadObject(AbstractRoleType.class,
                targetObjectOid, pageBase, task, result);
        try {
            return pageBase.getModelInteractionService().getAllowedRequestAssignmentItems(
                    operationObject, targetRefObject, task, result);
        } catch (SchemaException | SecurityViolationException | ObjectNotFoundException | ExpressionEvaluationException |
                CommunicationException | ConfigurationException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load security constraints for assignment items.", ex);
            return null;
        }
    }

    protected void removeButtonClickPerformed(AssignmentEditorDto assignmentDto, AjaxRequestTarget target) {
        //Override if needed
    }

}
