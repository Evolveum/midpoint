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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.objecttypeselect.ObjectTypeSelectPanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.ItemSecurityDecisions;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentsPreviewDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.*;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends BasePanel<AssignmentEditorDto> {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentEditorPanel.class);

	private static final String DOT_CLASS = AssignmentEditorPanel.class.getName() + ".";
	private static final String OPERATION_LOAD_OBJECT = DOT_CLASS + "loadObject";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
	private static final String OPERATION_LOAD_ATTRIBUTES = DOT_CLASS + "loadAttributes";
	private static final String OPERATION_LOAD_TARGET_OBJECT = DOT_CLASS + "loadItemSecurityDecisions";

	private static final String ID_HEADER_ROW = "headerRow";
	private static final String ID_SELECTED = "selected";
	private static final String ID_TYPE_IMAGE = "typeImage";
	private static final String ID_NAME_LABEL = "nameLabel";
	private static final String ID_NAME = "name";
	private static final String ID_ACTIVATION = "activation";
	private static final String ID_ACTIVATION_BLOCK = "activationBlock";
	private static final String ID_EXPAND = "expand";
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
	private static final String ID_BUTTON_SHOW_MORE = "errorLink";
	private static final String ID_ERROR_ICON = "errorIcon";
	private static final String ID_METADATA_CONTAINER = "metadataContainer";
	private static final String ID_PROPERTY_CONTAINER = "propertyContainer";
	private static final String ID_DESCRIPTION_CONTAINER = "descriptionContainer";
	private static final String ID_ADMIN_STATUS_CONTAINER = "administrativeStatusContainer";
	private static final String ID_VALID_FROM_CONTAINER = "validFromContainer";
	private static final String ID_VALID_TO_CONTAINER = "validToContainer";

	private IModel<List<ACAttributeDto>> attributesModel;
	protected WebMarkupContainer headerRow;
	protected PageBase pageBase;
	protected List<AssignmentsPreviewDto> privilegesList;
	protected boolean delegatedToMe;
	private LoadableModel<ItemSecurityDecisions> decisionsModel;

	public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model, boolean delegatedToMe,
								 List<AssignmentsPreviewDto> privilegesList,
								 PageBase pageBase) {
		super(id, model);
		this.pageBase = pageBase;
		this.privilegesList = privilegesList;
		this.delegatedToMe = delegatedToMe;

		initDecisionsModel();
		initLayout();
	}

	public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
		this(id, model, null);
	}

	public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model, PageBase pageBase) {
		super(id, model);
		this.pageBase = pageBase;

		attributesModel = new LoadableModel<List<ACAttributeDto>>(false) {
			@Override
			protected List<ACAttributeDto> load() {
				return loadAttributes();
			}
		};
		initDecisionsModel();
		initLayout();
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);

		response.render(CssHeaderItem.forReference(
				new PackageResourceReference(AssignmentEditorPanel.class, "AssignmentEditorPanel.css")));
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

	protected void initHeaderRow(){
		AjaxCheckBox selected = new AjaxCheckBox(ID_SELECTED,
				new PropertyModel<Boolean>(getModel(), AssignmentEditorDto.F_SELECTED)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				// do we want to update something?
			}
		};
		selected.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isVisible(){
				return !getModel().getObject().isSimpleView();
			}
		});
		headerRow.add(selected);

		WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
		typeImage.add(AttributeModifier.append("class", createImageTypeModel(getModel())));
		headerRow.add(typeImage);

		Label errorIcon = new Label(ID_ERROR_ICON);
		errorIcon.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !isTargetValid();
			}
		});
		headerRow.add(errorIcon);

		AjaxLink name = new AjaxLink(ID_NAME) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				nameClickPerformed(target);
			}
		};
		headerRow.add(name);

		AjaxLink errorLink = new AjaxLink(ID_BUTTON_SHOW_MORE) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				showErrorPerformed(target);
			}
		};
		errorLink.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !isTargetValid();
			}
		});
		headerRow.add(errorLink);

		Label nameLabel = new Label(ID_NAME_LABEL, createAssignmentNameLabelModel(false));
		nameLabel.setOutputMarkupId(true);
		name.add(nameLabel);

		Label activation = new Label(ID_ACTIVATION, createActivationModel());
		headerRow.add(activation);

		ToggleIconButton expandButton = new ToggleIconButton(ID_EXPAND, GuiStyleConstants.CLASS_ICON_EXPAND,
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
		expandButton.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isVisible(){
				return !getModel().getObject().isSimpleView();
			}
		});
		headerRow.add(expandButton);
	}

	protected IModel<String> createAssignmentNameLabelModel(final boolean isManager) {
		return new AbstractReadOnlyModel<String>() {

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

	private boolean isTargetValid() {

		if (getModel() != null && getModel().getObject() != null) {
			AssignmentEditorDto dto = getModelObject();

			if (dto.getName() == null && dto.getAltName() == null) {
				return false;
			}
		}

		return true;
	}

	protected IModel<String> createHeaderClassModel(final IModel<AssignmentEditorDto> model) {
		return new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				AssignmentEditorDto dto = model.getObject();
				return dto.getStatus().name().toLowerCase();
			}
		};
	}

	private IModel<String> createActivationModel() {
		return new AbstractReadOnlyModel<String>() {
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				AssignmentEditorDto dto = getModel().getObject();
				ActivationType activation = dto.getActivation();
				if (activation == null) {
					return "-";
				}

				ActivationStatusType status = activation.getAdministrativeStatus();
				String strEnabled = createStringResource(status, "lower", "ActivationStatusType.null")
						.getString();

				if (activation.getValidFrom() != null && activation.getValidTo() != null) {
					return getString("AssignmentEditorPanel.enabledFromTo", strEnabled,
							MiscUtil.asDate(activation.getValidFrom()),
							MiscUtil.asDate(activation.getValidTo()));
				} else if (activation.getValidFrom() != null) {
					return getString("AssignmentEditorPanel.enabledFrom", strEnabled,
							MiscUtil.asDate(activation.getValidFrom()));
				} else if (activation.getValidTo() != null) {
					return getString("AssignmentEditorPanel.enabledTo", strEnabled,
							MiscUtil.asDate(activation.getValidTo()));
				}

				return "-";
			}
		};
	}

	protected IModel<Date> createDateModel(final IModel<XMLGregorianCalendar> model) {
		return new Model<Date>() {

			@Override
			public Date getObject() {
				XMLGregorianCalendar calendar = model.getObject();
				if (calendar == null) {
					return null;
				}
				return MiscUtil.asDate(calendar);
			}

			@Override
			public void setObject(Date object) {
				if (object == null) {
					model.setObject(null);
				} else {
					model.setObject(MiscUtil.asXMLGregorianCalendar(object));
				}
			}
		};
	}

	protected void initBodyLayout(WebMarkupContainer body) {
		WebMarkupContainer propertyContainer = new WebMarkupContainer(ID_PROPERTY_CONTAINER);
		propertyContainer.setOutputMarkupId(true);
		body.add(propertyContainer);

		WebMarkupContainer descriptionContainer = new WebMarkupContainer(ID_DESCRIPTION_CONTAINER);
		descriptionContainer.setOutputMarkupId(true);
		descriptionContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION));
			}
		});
		body.add(descriptionContainer);

		TextArea<String> description = new TextArea<>(ID_DESCRIPTION,
				new PropertyModel<String>(getModel(), AssignmentEditorDto.F_DESCRIPTION));
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
				if (!isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF,
						ObjectReferenceType.F_RELATION))){
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
		focusTypeContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_FOCUS_TYPE));
			}
		});
		body.add(focusTypeContainer);

		ObjectTypeSelectPanel<FocusType> focusType = new ObjectTypeSelectPanel<>(ID_FOCUS_TYPE,
				new PropertyModel<QName>(getModel(), AssignmentEditorDto.F_FOCUS_TYPE), FocusType.class);
		focusTypeContainer.add(focusType);

		Label relationLabel = new Label(ID_RELATION_LABEL, new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				if (getModel() == null || getModel().getObject() == null) {
					return getString("AssignmentEditorPanel.relation.notSpecified");
				}

				AssignmentEditorDto object = getModel().getObject();
                String propertyKey = RelationTypes.class.getSimpleName() + "." +
                        (object.getTargetRef() == null || object.getTargetRef().getRelation() == null ?
                        RelationTypes.MEMBER : RelationTypes.getRelationType(object.getTargetRef().getRelation()));
				return createStringResource(propertyKey).getString();
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
		tenantRefContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF));
			}
		});
		body.add(tenantRefContainer);

		WebMarkupContainer orgRefContainer = createOrgContainer();
		orgRefContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF));
			}
		});
		body.add(orgRefContainer);
		propertyContainer.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION))
						|| isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF,
						ObjectReferenceType.F_RELATION))
						|| isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_FOCUS_TYPE))
						|| isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF))
						|| isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF));
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
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
						ActivationType.F_ADMINISTRATIVE_STATUS));
			}
		});
		activationBlock.add(adminStatusContainer);

		DropDownChoicePanel administrativeStatus = WebComponentUtil.createEnumPanel(
				ActivationStatusType.class, ID_ADMINISTRATIVE_STATUS,
				new PropertyModel<ActivationStatusType>(getModel(), AssignmentEditorDto.F_ACTIVATION + "."
						+ ActivationType.F_ADMINISTRATIVE_STATUS.getLocalPart()),
				this);
		administrativeStatus.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(){
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
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
						ActivationType.F_VALID_FROM));
			}
		});
		activationBlock.add(validFromContainer);

		DateInput validFrom = new DateInput(ID_VALID_FROM,
				createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
						AssignmentEditorDto.F_ACTIVATION + ".validFrom")));
		validFrom.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(){
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
				return isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION,
						ActivationType.F_VALID_TO));
			}
		});
		activationBlock.add(validToContainer);

		DateInput validTo = new DateInput(ID_VALID_TO,
				createDateModel(new PropertyModel<XMLGregorianCalendar>(getModel(),
						AssignmentEditorDto.F_ACTIVATION + ".validTo")));
		validTo.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isEnabled(){
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
				if (!isItemAllowed(new ItemPath(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET))){
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

		AjaxLink showEmpty = new AjaxLink(ID_SHOW_EMPTY) {

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
                getModelObject().getOldValue().asContainerable() == null){
            metadataPanel = new WebMarkupContainer(ID_METADATA_CONTAINER);
        } else {
            metadataPanel = new MetadataPanel(ID_METADATA_CONTAINER, new AbstractReadOnlyModel<MetadataType>() {
                @Override
                public MetadataType getObject() {
                    return getModelObject().getOldValue().getValue().getMetadata();
                }
            }, "row");
        }
        metadataPanel.setOutputMarkupId(true);
		metadataPanel.add(new VisibleEnableBehaviour(){
			@Override
			public boolean isVisible(){
				return !UserDtoStatus.ADD.equals(getModel().getObject().getStatus());
			}
		});
		body.add(metadataPanel);

		addAjaxOnUpdateBehavior(body);
	}
	
	private void updateAssignmentName(AjaxRequestTarget target, Boolean isManager){
		
		Label nameLabel = new Label(ID_NAME_LABEL, createAssignmentNameLabelModel(isManager));
		nameLabel.setOutputMarkupId(true);
		AjaxLink name = (AjaxLink) get(createComponentPath(ID_HEADER_ROW, ID_NAME));
		name.addOrReplace(nameLabel);
		target.add(name);
	}

	private WebMarkupContainer createTenantContainer() {
		WebMarkupContainer tenantRefContainer = new WebMarkupContainer(ID_CONTAINER_TENANT_REF);
		ChooseTypePanel tenantRef = new ChooseTypePanel(ID_TENANT_CHOOSER,
				new PropertyModel<ObjectViewDto>(getModel(), AssignmentEditorDto.F_TENANT_REF)) {

			@Override
			protected ObjectQuery getChooseQuery() {
				return QueryBuilder.queryFor(OrgType.class, getPageBase().getPrismContext())
						.item(OrgType.F_TENANT).eq(true)
						.build();
			}

			@Override
			protected boolean isSearchEnabled() {
				return true;
			}

			@Override
			protected QName getSearchProperty() {
				return OrgType.F_NAME;
			}
		};
		tenantRef.setPanelEnabled(getModel().getObject().isEditable());
		tenantRefContainer.add(tenantRef);
		tenantRefContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				AssignmentEditorDto dto = getModel().getObject();
				if (dto != null) {
					if (AssignmentEditorDtoType.ROLE.equals(dto.getType())) {
						return true;
					}
				}

				return false;
			}
		});
		return tenantRefContainer;
	}

	private WebMarkupContainer createOrgContainer() {
		WebMarkupContainer tenantRefContainer = new WebMarkupContainer(ID_CONTAINER_ORG_REF);
		ChooseTypePanel tenantRef = new ChooseTypePanel(ID_ORG_CHOOSER,
				new PropertyModel<ObjectViewDto>(getModel(), AssignmentEditorDto.F_ORG_REF)) {

			@Override
			protected ObjectQuery getChooseQuery() {
				return QueryBuilder.queryFor(OrgType.class, getPageBase().getPrismContext())
						.item(OrgType.F_TENANT).eq(false)
						.or().item(OrgType.F_TENANT).isNull()
						.build();
			}

			@Override
			protected boolean isSearchEnabled() {
				return true;
			}

			@Override
			protected QName getSearchProperty() {
				return OrgType.F_NAME;
			}
		};
		tenantRefContainer.add(tenantRef);
		tenantRef.setEnabled(getModel().getObject().isEditable());
		tenantRefContainer.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				AssignmentEditorDto dto = getModel().getObject();
				if (dto != null) {
					if (AssignmentEditorDtoType.ROLE.equals(dto.getType())) {
						return true;
					}
				}

				return false;
			}
		});
		return tenantRefContainer;
	}

	private void addAjaxOnBlurUpdateBehaviorToComponent(final Component component) {
		component.setOutputMarkupId(true);
		component.add(new AjaxFormComponentUpdatingBehavior("blur") {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
			}
		});
	}

	protected void addAjaxOnUpdateBehavior(WebMarkupContainer container) {
		container.visitChildren(new IVisitor<Component, Object>() {
			@Override
			public void component(Component component, IVisit<Object> objectIVisit) {
				if (component instanceof InputPanel) {
					addAjaxOnBlurUpdateBehaviorToComponent(((InputPanel) component).getBaseFormComponent());
				} else if (component instanceof FormComponent) {
					addAjaxOnBlurUpdateBehaviorToComponent(component);
				}
			}
		});
	}

	private void initAttributesLayout(WebMarkupContainer constructionContainer) {
		WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
		attributes.setOutputMarkupId(true);
		attributes.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				AssignmentEditorDto dto = getModel().getObject();
				return AssignmentEditorDtoType.CONSTRUCTION.equals(dto.getType());
			}
		});
		attributes.setEnabled(getModel().getObject().isEditable());
		constructionContainer.add(attributes);

		ListView<ACAttributeDto> attribute = new ListView<ACAttributeDto>(ID_ATTRIBUTE, attributesModel) {

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
		return new AbstractReadOnlyModel<String>() {

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
					AssignmentType.F_CONSTRUCTION, ConstructionType.class);

			if (construction == null) {
				return attributes;
			}

			PrismObject<ResourceType> resource = construction.getResource() != null
					? construction.getResource().asPrismObject() : null;
			if (resource == null) {
				resource = getReference(construction.getResourceRef(), result);
			}

			PrismContext prismContext = getPageBase().getPrismContext();
			RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource,
					LayerType.PRESENTATION, prismContext);
			RefinedObjectClassDefinition objectClassDefinition = refinedSchema
					.getRefinedDefinition(ShadowKindType.ACCOUNT, construction.getIntent());

			if (objectClassDefinition == null) {
				return attributes;
			}

			PrismContainerDefinition definition = objectClassDefinition
					.toResourceAttributeContainerDefinition();

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Refined definition for {}\n{}", construction, definition.debugDump());
			}

			List<ResourceAttributeDefinitionType> attrConstructions = construction.getAttribute();

			Collection<ItemDefinition> definitions = definition.getDefinitions();
			for (ItemDefinition attrDef : definitions) {
				if (!(attrDef instanceof PrismPropertyDefinition)) {
					// log skipping or something...
					continue;
				}

				PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) attrDef;
				if (propertyDef.isOperational() || propertyDef.isIgnored()) {
					continue;
				}
				attributes.add(ACAttributeDto.createACAttributeDto(propertyDef,
						findOrCreateValueConstruction(propertyDef, attrConstructions), prismContext));
			}
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Exception occurred during assignment attribute loading", ex);
			result.recordFatalError("Exception occurred during assignment attribute loading.", ex);
		} finally {
			result.recomputeStatus();
		}

		Collections.sort(attributes, new Comparator<ACAttributeDto>() {

			@Override
			public int compare(ACAttributeDto a1, ACAttributeDto a2) {
				return String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName());
			}
		});


		if (dto.getAttributes() != null && !dto.getAttributes().isEmpty()) {
			for (ACAttributeDto assignmentAttribute : dto.getAttributes()) {
				for (ACAttributeDto attributeDto : attributes){
					if (attributeDto.getName().equals(assignmentAttribute.getName())){
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
			subResult.recordFatalError("Couldn't get account construction resource ref.", ex);
		}

		return target;
	}

	private ResourceAttributeDefinitionType findOrCreateValueConstruction(PrismPropertyDefinition attrDef,
			List<ResourceAttributeDefinitionType> attrConstructions) {
		for (ResourceAttributeDefinitionType construction : attrConstructions) {
			if (attrDef.getName().equals(construction.getRef())) {
				return construction;
			}
		}

		ResourceAttributeDefinitionType construction = new ResourceAttributeDefinitionType();
		construction.setRef(new ItemPathType(new ItemPath(attrDef.getName())));

		return construction;
	}

	protected IModel<String> createImageTypeModel(final IModel<AssignmentEditorDto> model) {
		return new AbstractReadOnlyModel<String>() {
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
					return WebComponentUtil.createDefaultIcon(targetObject);
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
		return new LoadableModel<String>(false) {
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

	private void addRelationDropDown(WebMarkupContainer relationContainer){
		List<RelationTypes> availableRelations = getModelObject().getNotAssignedRelationsList();
		DropDownChoicePanel relation = WebComponentUtil.createEnumPanel(RelationTypes.class, ID_RELATION,
				getModelObject().isMultyAssignable() ?
						WebComponentUtil.createReadonlyModelFromEnum(RelationTypes.class) : Model.ofList(availableRelations),
				getRelationModel(availableRelations), this, false);
		relation.setEnabled(getModel().getObject().isEditable());
		relation.setOutputMarkupId(true);
		relation.setOutputMarkupPlaceholderTag(true);
		relation.add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return isCreatingNewAssignment();
			}
		});
		relationContainer.add(relation);

	}

	private IModel<RelationTypes> getRelationModel(List<RelationTypes> availableRelations){
		return new IModel<RelationTypes>() {
			@Override
			public RelationTypes getObject() {
				RelationTypes defaultRelation = RelationTypes.MEMBER;
				if (!getModelObject().isMultyAssignable() &&
						getModelObject().getAssignedRelationsList().contains(defaultRelation)){
					defaultRelation = availableRelations.get(0);
				}
				if (getModelObject().getTargetRef() == null){
					return defaultRelation;
				}
				return RelationTypes.getRelationType(getModelObject().getTargetRef().getRelation());
			}

			@Override
			public void setObject(RelationTypes relationTypes) {
				if (getModelObject().getTargetRef() != null){
					getModelObject().getTargetRef().setRelation(relationTypes.getRelation());
				}
			}

			@Override
			public void detach() {

			}
		};
	}


	protected IModel<RelationTypes> getRelationModel(){
		return new IModel<RelationTypes>() {
			private static final long serialVersionUID = 1L;

			@Override
			public RelationTypes getObject() {
				if (getModelObject().getTargetRef() == null) {
					return RelationTypes.MEMBER;
				}
				return RelationTypes.getRelationType(getModelObject().getTargetRef().getRelation());
			}

			@Override
			public void setObject(RelationTypes newValue) {
				ObjectReferenceType ref = getModelObject().getTargetRef();
				if (ref != null){
					ref.setRelation(newValue.getRelation());
				}
			}

			@Override
			public void detach() {

			}
		};
	}

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

	protected boolean ignoreMandatoryAttributes(){
		return false;
	}

	private void initDecisionsModel(){
		decisionsModel = new LoadableModel<ItemSecurityDecisions>(false) {
			@Override
			protected ItemSecurityDecisions load() {
				return loadSecurityDecisions();
			}
		};

	}

	private boolean isItemAllowed(ItemPath itemPath){
		ItemSecurityDecisions decisions = decisionsModel.getObject();
		if (itemPath == null || decisions == null || decisions.getItemDecisionMap() == null
				|| decisions.getItemDecisionMap().size() == 0){
			return true;
		}
		Map<ItemPath, AuthorizationDecisionType> decisionsMap = decisions.getItemDecisionMap();
		boolean isAllowed = false;
		for (ItemPath path : decisionsMap.keySet()) {
			if (path.equivalent(itemPath)
					&& AuthorizationDecisionType.ALLOW.value().equals(decisionsMap.get(path).value())) {
				return true;
			}
		}
		return isAllowed;
	}

	private ItemSecurityDecisions loadSecurityDecisions(){
		if (pageBase == null || getModelObject().getTargetRef() == null){
			return null;
		}
		PrismObject<UserType> user = null;
		List<PrismObject<UserType>> targetUserList = pageBase.getSessionStorage().getRoleCatalog().getTargetUserList();
		if (targetUserList == null || targetUserList.size() == 0){
			user = pageBase.loadUserSelf(pageBase);
		} else {
			user = targetUserList.get(0);
		}
		String targetObjectOid = getModelObject().getTargetRef().getOid();

		Task task = pageBase.createSimpleTask(OPERATION_LOAD_TARGET_OBJECT);
		OperationResult result = new OperationResult(OPERATION_LOAD_TARGET_OBJECT);
		PrismObject<AbstractRoleType> targetRefObject = WebModelServiceUtils.loadObject(AbstractRoleType.class,
				targetObjectOid, pageBase, task, result);
		ItemSecurityDecisions decisions = null;
		try{
			decisions =
					pageBase.getModelInteractionService().getAllowedRequestAssignmentItems(user, targetRefObject);

		} catch (SchemaException|SecurityViolationException ex){
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load security decisions for assignment items.", ex);
		}
		return decisions;
	}
}
