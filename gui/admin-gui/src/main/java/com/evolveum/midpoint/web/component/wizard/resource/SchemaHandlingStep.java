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

package com.evolveum.midpoint.web.component.wizard.resource;

import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.WizardUtil;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.*;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ResourceObjectTypeDefinitionTypeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.SchemaHandlingDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.fillDefault;

/**
 *  @author lazyman
 *  @author shood
 */
public class SchemaHandlingStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlingStep.class);

    private static final String DOT_CLASS = SchemaHandlingStep.class.getName() + ".";
    private static final String OPERATION_SAVE_SCHEMA_HANDLING = DOT_CLASS + "saveSchemaHandling";

    private static final String ID_ROWS = "tableRows";
    private static final String ID_ROW_OBJECT_TYPE = "objectTypeRow";
    private static final String ID_LINK_OBJECT_TYPE = "objectTypeLink";
    private static final String ID_NAME_OBJECT_TYPE = "objectTypeName";
    private static final String ID_BUTTON_DELETE_OBJECT_TYPE = "objectTypeDelete";
    private static final String ID_PAGING_OBJECT_TYPE = "objectTypePaging";
    private static final String ID_BUTTON_ADD_OBJECT_TYPE = "objectTypeAddButton";
    private static final String ID_OBJECT_TYPE_EDITOR = "objectTypeConfig";
    private static final String ID_THIRD_ROW_CONTAINER = "thirdRowContainer";
    private static final String ID_EDITOR_NAME = "editorName";
    private static final String ID_EDITOR_KIND = "editorKind";
    private static final String ID_EDITOR_INTENT = "editorIntent";
    private static final String ID_EDITOR_DISPLAY_NAME = "editorDisplayName";
    private static final String ID_EDITOR_DESCRIPTION = "editorDescription";
    private static final String ID_EDITOR_DEFAULT = "editorDefault";
    private static final String ID_EDITOR_BUTTON_DEPENDENCY = "editorDependencyButton";
    private static final String ID_EDITOR_OBJECT_CLASS = "editorObjectClass";
    private static final String ID_EDITOR_ASSIGNMENT_POLICY = "editorAssignmentPolicyRef";
    private static final String ID_EDITOR_BUTTON_ITERATION = "editorIterationButton";
    private static final String ID_EDITOR_BUTTON_PROTECTED = "editorProtectedButton";
    private static final String ID_EDITOR_BUTTON_ACTIVATION = "editorActivationButton";
    private static final String ID_EDITOR_BUTTON_CREDENTIALS = "editorCredentialsButton";
    private static final String ID_EDITOR_ATTRIBUTES = "editorAttributes";
    private static final String ID_EDITOR_ASSOCIATIONS = "editorAssociations";

    private static final String ID_T_KIND = "kindTooltip";
    private static final String ID_T_INTENT = "intentTooltip";
    private static final String ID_T_DEFAULT = "defaultTooltip";
    private static final String ID_T_DEPENDENCY = "dependencyTooltip";
    private static final String ID_T_OBJECT_CLASS = "objectClassTooltip";
    private static final String ID_T_ATTRIBUTES = "attributesTooltip";
    private static final String ID_T_ASSOCIATIONS = "associationsTooltip";
    private static final String ID_T_ASSIGNMENT_POLICY_REF = "assignmentPolicyRefTooltip";
    private static final String ID_T_ITERATION = "iterationTooltip";
    private static final String ID_T_PROTECTED = "protectedTooltip";
    private static final String ID_T_ACTIVATION = "activationTooltip";
    private static final String ID_T_CREDENTIALS = "credentialsTooltip";

    private static final Integer AUTO_COMPLETE_LIST_SIZE = 10;

	@NotNull final private PageResourceWizard parentPage;
    @NotNull final private NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel;
	@NotNull final private NonEmptyLoadableModel<SchemaHandlingDto> schemaHandlingDtoModel;

    public SchemaHandlingStep(@NotNull final NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel, @NotNull PageResourceWizard parentPage) {
        super(parentPage);
		this.parentPage = parentPage;
        this.resourceModel = resourceModel;

        schemaHandlingDtoModel = new NonEmptyLoadableModel<SchemaHandlingDto>(false) {
            @Override
			@NotNull
            protected SchemaHandlingDto load() {
                return loadSchemaHandlingDto();
            }

			@Override
			public void reset() {
				LOGGER.trace("Resetting schemaHandlingDtoModel {}", schemaHandlingDtoModel);
				super.reset();
			}
		};
		parentPage.registerDependentModel(schemaHandlingDtoModel);

        initLayout();
		setOutputMarkupId(true);
    }

    private SchemaHandlingDto loadSchemaHandlingDto() {

        List<ResourceObjectTypeDefinitionTypeDto> objectTypeDefs = new ArrayList<>();
		SchemaHandlingType schemaHandling = getOrCreateSchemaHandling();
		for (ResourceObjectTypeDefinitionType objectType: schemaHandling.getObjectType()) {
			objectTypeDefs.add(new ResourceObjectTypeDefinitionTypeDto(objectType));
		}
		List<QName> objectClasses = loadResourceObjectClassList(resourceModel, LOGGER,
				getString("SchemaHandlingStep.message.errorLoadingObjectTypeList"));
		return new SchemaHandlingDto(objectTypeDefs, objectClasses);
    }

    private boolean isAnySelected() {
		return schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto() != null;
    }

    private void initLayout() {
        final ListDataProvider<ResourceObjectTypeDefinitionTypeDto> objectTypeProvider = new ListDataProvider<>(this,
                new PropertyModel<List<ResourceObjectTypeDefinitionTypeDto>>(schemaHandlingDtoModel, SchemaHandlingDto.F_OBJECT_TYPE_DTO_LIST));

        // first row - object types table
        WebMarkupContainer objectTypesTable = new WebMarkupContainer(ID_ROWS);
        objectTypesTable.setOutputMarkupId(true);
        add(objectTypesTable);

		// second row - object type editor
        WebMarkupContainer objectTypeEditor = new WebMarkupContainer(ID_OBJECT_TYPE_EDITOR);
        objectTypeEditor.setOutputMarkupId(true);
        objectTypeEditor.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible() {
                return isAnySelected();
            }
        });
        add(objectTypeEditor);

        // third row container
        WebMarkupContainer thirdRowContainer = new WebMarkupContainer(ID_THIRD_ROW_CONTAINER);
        thirdRowContainer.setOutputMarkupId(true);
        add(thirdRowContainer);

		// ---------------------- details -----------------------
		// Object type table (list)

        DataView<ResourceObjectTypeDefinitionTypeDto> objectTypeDataView = new DataView<ResourceObjectTypeDefinitionTypeDto>(ID_ROW_OBJECT_TYPE,
                objectTypeProvider, UserProfileStorage.DEFAULT_PAGING_SIZE) {

            @Override
            protected void populateItem(final Item<ResourceObjectTypeDefinitionTypeDto> item) {
                final ResourceObjectTypeDefinitionTypeDto objectType = item.getModelObject();

                AjaxSubmitLink link = new AjaxSubmitLink(ID_LINK_OBJECT_TYPE) {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        editObjectTypePerformed(target, objectType);
                    }

					@Override
					protected void onError(AjaxRequestTarget target, Form<?> form) {
						target.add(parentPage.getFeedbackPanel());
					}
				};
                item.add(link);

                Label label = new Label(ID_NAME_OBJECT_TYPE, createObjectTypeDisplayModel(objectType));
                label.setOutputMarkupId(true);
                link.add(label);

                AjaxLink delete = new AjaxLink(ID_BUTTON_DELETE_OBJECT_TYPE) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteObjectTypePerformed(target, objectType);
                    }
                };
				parentPage.addEditingVisibleBehavior(delete);
                link.add(delete);

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
						return isSelected(item.getModelObject()) ? "success" : null;
					}
				}));
            }
        };
        objectTypesTable.add(objectTypeDataView);

        NavigatorPanel navigator = new NavigatorPanel(ID_PAGING_OBJECT_TYPE, objectTypeDataView, true);
        navigator.setOutputMarkupPlaceholderTag(true);
        navigator.setOutputMarkupId(true);
        add(navigator);

        AjaxSubmitLink add = new AjaxSubmitLink(ID_BUTTON_ADD_OBJECT_TYPE) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                addObjectTypePerformed(target);
            }

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(parentPage.getFeedbackPanel());
			}
        };
		parentPage.addEditingVisibleBehavior(add);
        add(add);

        initObjectTypeEditor(objectTypeEditor);
    }

	@Override
	protected void onConfigure() {
		super.onConfigure();
		if (!isAnySelected()) {
			insertEmptyThirdRow();
		}
	}

	private IModel<String> createObjectTypeDisplayModel(final ResourceObjectTypeDefinitionTypeDto objectType){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if (objectType != null) {
                    ResourceObjectTypeDefinitionType object = objectType.getObjectType();
                    sb.append(object.getDisplayName() != null ? object.getDisplayName() + " " : "");
					addKindAndIntent(sb, object.getKind(), object.getIntent());
					sb.append(" -> ");
					sb.append(object.getObjectClass() != null ? object.getObjectClass().getLocalPart() : "");
				}
                return sb.toString();
            }
        };
    }

	static void addKindAndIntent(StringBuilder sb, ShadowKindType kind, String intent) {
		sb.append("(");
		sb.append(ResourceTypeUtil.fillDefault(kind));
		sb.append(", ");
		sb.append(ResourceTypeUtil.fillDefault(intent));
		sb.append(")");
	}

	private void initObjectTypeEditor(WebMarkupContainer editor){
        Label editorLabel = new Label(ID_EDITOR_NAME, new AbstractReadOnlyModel<Object>() {
            @Override
            public String getObject() {
				ResourceObjectTypeDefinitionTypeDto selected = schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto();
				return selected != null ? selected.getObjectType().getDisplayName() : "";
            }
        });
		editorLabel.setOutputMarkupId(true);
        editor.add(editorLabel);

        DropDownChoice editorKind = new DropDownChoice<>(ID_EDITOR_KIND,
            new PropertyModel<>(schemaHandlingDtoModel, getExpression(ResourceObjectTypeDefinitionType.F_KIND)),
                WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class),
            new EnumChoiceRenderer<>(this));
		editorKind.add(new UpdateNamesBehaviour());
		parentPage.addEditingEnabledBehavior(editorKind);
		editor.add(editorKind);

        TextField editorIntent = new TextField<>(ID_EDITOR_INTENT, new PropertyModel<String>(schemaHandlingDtoModel,
				getExpression(ResourceObjectTypeDefinitionType.F_INTENT)));
		editorIntent.add(new UpdateNamesBehaviour());
		parentPage.addEditingEnabledBehavior(editorIntent);
        editor.add(editorIntent);

        TextField editorDisplayName = new TextField<>(ID_EDITOR_DISPLAY_NAME, new PropertyModel<String>(schemaHandlingDtoModel,
				getExpression(ResourceObjectTypeDefinitionType.F_DISPLAY_NAME)));
		editorDisplayName.add(new UpdateNamesBehaviour());
		parentPage.addEditingEnabledBehavior(editorDisplayName);
        editor.add(editorDisplayName);

        TextArea editorDescription = new TextArea<>(ID_EDITOR_DESCRIPTION, new PropertyModel<String>(schemaHandlingDtoModel,
				getExpression(ResourceObjectTypeDefinitionType.F_DESCRIPTION)));
		parentPage.addEditingEnabledBehavior(editorDescription);
        editor.add(editorDescription);

        final CheckBox editorDefault = new CheckBox(ID_EDITOR_DEFAULT, new PropertyModel<>(schemaHandlingDtoModel,
            getExpression(ResourceObjectTypeDefinitionType.F_DEFAULT)));
        editorDefault.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Boolean newValue = editorDefault.getModelObject();
                if (Boolean.TRUE.equals(newValue)) {
                    SchemaHandlingDto dto = schemaHandlingDtoModel.getObject();
                    ResourceObjectTypeDefinitionTypeDto selected = dto.getSelectedObjectTypeDto();
                    ShadowKindType selectedKind = fillDefault(selected.getObjectType().getKind());
                    for (ResourceObjectTypeDefinitionTypeDto currentObjectTypeDto : dto.getObjectTypeDtoList()) {
                        ShadowKindType currentKind = fillDefault(currentObjectTypeDto.getObjectType().getKind());
                        if (currentObjectTypeDto != selected && currentKind == selectedKind
                                && Boolean.TRUE.equals(currentObjectTypeDto.getObjectType().isDefault())) {
                            currentObjectTypeDto.getObjectType().setDefault(false);
                        }
                    }
                }
				parentPage.refreshIssues(target);
            }
        });
		parentPage.addEditingEnabledBehavior(editorDefault);
        editor.add(editorDefault);

        AjaxSubmitLink editorDependency = new AjaxSubmitLink(ID_EDITOR_BUTTON_DEPENDENCY) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                dependencyEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorDependency);
        editor.add(editorDependency);

        AutoCompleteSettings autoCompleteSettings = new AutoCompleteSettings();
        autoCompleteSettings.setShowListOnEmptyInput(true);
        autoCompleteSettings.setMaxHeightInPx(200);
        AutoCompleteTextField<String> editorObjectClass = new AutoCompleteTextField<String>(ID_EDITOR_OBJECT_CLASS,
            new PropertyModel<>(schemaHandlingDtoModel, SchemaHandlingDto.F_OBJECT_CLASS_NAME), autoCompleteSettings) {
            @Override
            protected Iterator<String> getChoices(String input) {
                return getObjectClassChoices(input);
            }
        };
        editorObjectClass.add(new UpdateNamesBehaviour());
        editorObjectClass.add(createObjectClassValidator(new AbstractReadOnlyModel<List<QName>>() {
            @Override
            public List<QName> getObject() {
                return schemaHandlingDtoModel.getObject().getObjectClassList();
            }
        }));
		parentPage.addEditingEnabledBehavior(editorObjectClass);
		editorObjectClass.setConvertEmptyInputStringToNull(true);
        editor.add(editorObjectClass);

        MultiValueTextEditPanel editorAttributes = new MultiValueTextEditPanel<ResourceAttributeDefinitionType>(ID_EDITOR_ATTRIBUTES,
            new PropertyModel<>(schemaHandlingDtoModel,
                getExpression(ResourceObjectTypeDefinitionType.F_ATTRIBUTE)),
            new PropertyModel<>(schemaHandlingDtoModel, SchemaHandlingDto.F_SELECTED_ATTRIBUTE), false, true,
				parentPage.getReadOnlyModel()) {

            @Override
            protected IModel<String> createTextModel(final IModel<ResourceAttributeDefinitionType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        if (model == null || model.getObject() == null) {
                            return null;
                        }
                        ResourceAttributeDefinitionType attribute = model.getObject();
						return formatItemInfo(attribute, attribute.getRef(), attribute.getDisplayName(), attribute.getInbound(), attribute.getOutbound());
                    }
                };
            }

            @Override
            protected ResourceAttributeDefinitionType createNewEmptyItem(){
                return createEmptyAttributeObject();
            }

			@Override
			protected void performAddValueHook(AjaxRequestTarget target, ResourceAttributeDefinitionType added) {
				parentPage.refreshIssues(target);
			}

			@Override
            protected void editPerformed(AjaxRequestTarget target, ResourceAttributeDefinitionType object){
                editAttributePerformed(target, object);
            }

            @Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }

            @Override
            protected void performRemoveValueHook(AjaxRequestTarget target, ListItem<ResourceAttributeDefinitionType> item) {
				resetThirdRowContainer(target);
				parentPage.refreshIssues(target);
            }
        };
		editorAttributes.setOutputMarkupId(true);
        editor.add(editorAttributes);

        MultiValueTextEditPanel editorAssociations = new MultiValueTextEditPanel<ResourceObjectAssociationType>(ID_EDITOR_ASSOCIATIONS,
            new PropertyModel<>(schemaHandlingDtoModel,
                getExpression(ResourceObjectTypeDefinitionType.F_ASSOCIATION)),
            new PropertyModel<>(schemaHandlingDtoModel, SchemaHandlingDto.F_SELECTED_ASSOCIATION),
				false, true, parentPage.getReadOnlyModel()) {

            @Override
            protected IModel<String> createTextModel(final IModel<ResourceObjectAssociationType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        ResourceObjectAssociationType association = model.getObject();
                        if (association == null) {
                            return null;
                        }
						return formatItemInfo(association, association.getRef(), association.getDisplayName(), association.getInbound(), association.getOutbound());
                    }
                };
            }

            @Override
            protected ResourceObjectAssociationType createNewEmptyItem() {
                return createEmptyAssociationObject();
            }

			@Override
			protected void performAddValueHook(AjaxRequestTarget target, ResourceObjectAssociationType added) {
				parentPage.refreshIssues(target);
			}

			@Override
            protected void editPerformed(AjaxRequestTarget target, ResourceObjectAssociationType object){
                editAssociationPerformed(target, object);
            }

            @Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }

            @Override
            protected void performRemoveValueHook(AjaxRequestTarget target, ListItem<ResourceObjectAssociationType> item) {
				resetThirdRowContainer(target);
				parentPage.refreshIssues(target);
            }
        };
		editorAssociations.setOutputMarkupId(true);
        editor.add(editorAssociations);

//        DropDownChoice editorAssignmentPolicyRef = new DropDownChoice<>(ID_EDITOR_ASSIGNMENT_POLICY,
//                new PropertyModel<AssignmentPolicyEnforcementType>(schemaHandlingDtoModel,
//						getExpression(ResourceObjectTypeDefinitionType.F_ASSIGNMENT_POLICY_ENFORCEMENT)),
//                WebComponentUtil.createReadonlyModelFromEnum(AssignmentPolicyEnforcementType.class),
//                new EnumChoiceRenderer<AssignmentPolicyEnforcementType>(this));
//		parentPage.addEditingEnabledBehavior(editorAssignmentPolicyRef);
//        editor.add(editorAssignmentPolicyRef);

        AjaxSubmitLink editorIteration = new AjaxSubmitLink(ID_EDITOR_BUTTON_ITERATION) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                iterationEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorIteration);
        editor.add(editorIteration);

        AjaxSubmitLink editorProtected = new AjaxSubmitLink(ID_EDITOR_BUTTON_PROTECTED) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                protectedEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorProtected);
        editor.add(editorProtected);

        AjaxSubmitLink editorActivation = new AjaxSubmitLink(ID_EDITOR_BUTTON_ACTIVATION) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                activationEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorActivation);
        editor.add(editorActivation);

        AjaxSubmitLink editorCredentials = new AjaxSubmitLink(ID_EDITOR_BUTTON_CREDENTIALS) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                credentialsEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorCredentials);
        editor.add(editorCredentials);

        Label kindTooltip = new Label(ID_T_KIND);
        kindTooltip.add(new InfoTooltipBehavior());
        editor.add(kindTooltip);

        Label intentTooltip = new Label(ID_T_INTENT);
        intentTooltip.add(new InfoTooltipBehavior());
        editor.add(intentTooltip);

        Label defaultTooltip = new Label(ID_T_DEFAULT);
        defaultTooltip.add(new InfoTooltipBehavior());
        editor.add(defaultTooltip);

        Label dependencyTooltip = new Label(ID_T_DEPENDENCY);
        dependencyTooltip.add(new InfoTooltipBehavior());
        editor.add(dependencyTooltip);

        Label objectClassTooltip = new Label(ID_T_OBJECT_CLASS);
        objectClassTooltip.add(new InfoTooltipBehavior());
        editor.add(objectClassTooltip);

        Label attributesTooltip = new Label(ID_T_ATTRIBUTES);
        attributesTooltip.add(new InfoTooltipBehavior());
        editor.add(attributesTooltip);

        Label associationsTooltip = new Label(ID_T_ASSOCIATIONS);
        associationsTooltip.add(new InfoTooltipBehavior());
        editor.add(associationsTooltip);

        Label assignmentPolicyRefTooltip = new Label(ID_T_ASSIGNMENT_POLICY_REF);
        assignmentPolicyRefTooltip.add(new InfoTooltipBehavior());
        editor.add(assignmentPolicyRefTooltip);

        Label iterationTooltip = new Label(ID_T_ITERATION);
        iterationTooltip.add(new InfoTooltipBehavior());
        editor.add(iterationTooltip);

        Label protectedTooltip = new Label(ID_T_PROTECTED);
        protectedTooltip.add(new InfoTooltipBehavior());
        editor.add(protectedTooltip);

        Label activationTooltip = new Label(ID_T_ACTIVATION);
        activationTooltip.add(new InfoTooltipBehavior());
        editor.add(activationTooltip);

        Label credentialsTooltip = new Label(ID_T_CREDENTIALS);
        credentialsTooltip.add(new InfoTooltipBehavior());
        editor.add(credentialsTooltip);
    }

	private String formatItemInfo(ResourceItemDefinitionType item, ItemPathType ref, String displayName, List<MappingType> inbounds,
			MappingType outbound) {
		StringBuilder sb = new StringBuilder();
		if (ref != null && !ref.getItemPath().isEmpty()) {
			QName name = ItemPathUtil.getOnlySegmentQNameRobust(ref);
			if (name != null) {
				String prefix = SchemaConstants.NS_ICF_SCHEMA.equals(name.getNamespaceURI()) ? "icfs" : "ri";
				sb.append(prefix);
				sb.append(": ");
				sb.append(name.getLocalPart());
			}
		} else {
			sb.append("-");
		}
		String duplicateInfo = getDuplicateInfo(item);
		if (duplicateInfo != null) {
			sb.append(" (").append(duplicateInfo).append(")");
		}
		if (displayName != null) {
			sb.append(" (").append(displayName).append(")");
		}
		if (!inbounds.isEmpty()) {
			sb.append(" | ");
			sb.append(getString("SchemaHandlingStep.in", ""));
			boolean first = true;
			for (MappingType inbound : inbounds) {
				if (inbound != null && inbound.getTarget() != null) {
					if (first) first = false; else sb.append(", ");
					sb.append(formatPath(inbound.getTarget().getPath()));
				}
			}
		}
		if (outbound != null) {
			sb.append(" | ").append(getString("SchemaHandlingStep.out")).append(": ");
			boolean first = true;
			for (VariableBindingDefinitionType source : outbound.getSource()) {
				if (source != null) {
					if (first) first = false; else sb.append(", ");
					sb.append(formatPath(source.getPath()));
				}
			}
		}
		return sb.toString();
	}

	// FIXME brutally hacked for now
	private String formatPath(ItemPathType path) {
		String rv = String.valueOf(path);
		rv = StringUtils.removeStart(rv, "$user/");
		rv = StringUtils.removeStart(rv, "$c:user/");
		rv = StringUtils.removeStart(rv, "$focus/");
		rv = StringUtils.removeStart(rv, "$c:focus/");
		rv = StringUtils.removeStart(rv, "extension/");
		rv = StringUtils.removeStart(rv, "c:extension/");
		return rv;
	}

	private String getDuplicateInfo(ResourceItemDefinitionType item) {
		ResourceObjectTypeDefinitionTypeDto selectedObjectTypeDto = schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto();
		if (selectedObjectTypeDto == null) {
			return null;		// shouldn't occur
		}
		ResourceObjectTypeDefinitionType selectedObjectType = selectedObjectTypeDto.getObjectType();
		List<ItemRefinedDefinitionType> existingItems = new ArrayList<>();
		existingItems.addAll(selectedObjectType.getAttribute());
		existingItems.addAll(selectedObjectType.getAssociation());
		QName name = ItemPathUtil.getOnlySegmentQNameRobust(item.getRef());
		int count = 0, position = 0;
		for (ItemRefinedDefinitionType existingItem : existingItems) {
			QName existingName = ItemPathUtil.getOnlySegmentQNameRobust(existingItem.getRef());
			if (QNameUtil.match(name, existingName)) {
				count++;
			}
			if (item == existingItem) {
				position = count;
			}
		}
		if (count == 1) {
			return null;
		}
		return getString("SchemaHandlingStep.dup", position);
	}

	@NotNull
	private String getExpression(QName property) {
		return SchemaHandlingDto.F_SELECTED_OBJECT_TYPE_DTO + ".objectType." + property.getLocalPart();
	}

	private Iterator<String> getObjectClassChoices(String input) {
        List<QName> resourceObjectClassList = schemaHandlingDtoModel.getObject().getObjectClassList();
        List<String> choices = new ArrayList<>(AUTO_COMPLETE_LIST_SIZE);

        if(Strings.isEmpty(input)){
            for(QName q: resourceObjectClassList){
                choices.add(q.getLocalPart());

                if(choices.size() == AUTO_COMPLETE_LIST_SIZE){
                    break;
                }
            }

            return choices.iterator();
        }

        for(QName q: resourceObjectClassList){
            if(q.getLocalPart().toLowerCase().startsWith(input.toLowerCase())){
                choices.add(q.getLocalPart());

                if(choices.size() == AUTO_COMPLETE_LIST_SIZE){
                    break;
                }
            }
        }

        return choices.iterator();
    }

    private void addDisabledClassModifier(Component component){
        component.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(!isAnySelected()){
                    return " disabled";
                }

                return null;
            }
        }));
    }

    private Component getObjectListTable(){
        return get(ID_ROWS);
    }

    private Component getObjectTypeEditor(){
        return get(ID_OBJECT_TYPE_EDITOR);
    }

	public Component getAttributeList() {
		return get(createComponentPath(ID_OBJECT_TYPE_EDITOR, ID_EDITOR_ATTRIBUTES));
	}

	public Component getAssociationList() {
		return get(createComponentPath(ID_OBJECT_TYPE_EDITOR, ID_EDITOR_ASSOCIATIONS));
	}

	private Component getThirdRowContainer(){
        return get(ID_THIRD_ROW_CONTAINER);
    }

    private Component getNavigator(){
        return get(ID_PAGING_OBJECT_TYPE);
    }

    private void resetSelected(){
        schemaHandlingDtoModel.getObject().setSelectedObjectTypeDto(null);
    }

    private void insertEmptyThirdRow(){
        getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
    }

    private void dependencyEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceDependencyEditor(ID_THIRD_ROW_CONTAINER,
            new PropertyModel<>(schemaHandlingDtoModel,
                getExpression(ResourceObjectTypeDefinitionType.F_DEPENDENCY)), parentPage);
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), parentPage.getFeedbackPanel());
		resetSelections(target);
    }

	private void resetThirdRowContainer(AjaxRequestTarget target) {
		insertEmptyThirdRow();
		target.add(getThirdRowContainer());
		resetSelections(target);
	}

	private void resetSelections(AjaxRequestTarget target) {
		SchemaHandlingDto dto = schemaHandlingDtoModel.getObject();
		if (dto.getSelectedAssociation() != null) {
			dto.setSelectedAssociation(null);
			target.add(getAssociationList());
		}
		if (dto.getSelectedAttribute() != null) {
			dto.setSelectedAttribute(null);
			target.add(getAttributeList());
		}
	}

	private void iterationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceIterationEditor(ID_THIRD_ROW_CONTAINER,
            new PropertyModel<>(schemaHandlingDtoModel,
                getExpression(ResourceObjectTypeDefinitionType.F_ITERATION)), parentPage);
        getThirdRowContainer().replaceWith(newContainer);
		resetSelections(target);
        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), parentPage.getFeedbackPanel());
    }

    private void protectedEditPerformed(AjaxRequestTarget target) {
        WebMarkupContainer newContainer = new ResourceProtectedEditor(ID_THIRD_ROW_CONTAINER,
            new PropertyModel<>(schemaHandlingDtoModel,
                getExpression(ResourceObjectTypeDefinitionType.F_PROTECTED)), parentPage);
        getThirdRowContainer().replaceWith(newContainer);
		resetSelections(target);
        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), parentPage.getFeedbackPanel());
    }

    private void activationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceActivationEditor(ID_THIRD_ROW_CONTAINER,
            new PropertyModel<>(schemaHandlingDtoModel,
                getExpression(ResourceObjectTypeDefinitionType.F_ACTIVATION)), parentPage.getReadOnlyModel());
        getThirdRowContainer().replaceWith(newContainer);
		resetSelections(target);
        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), parentPage.getFeedbackPanel());
    }

    private void credentialsEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceCredentialsEditor(ID_THIRD_ROW_CONTAINER,
            new PropertyModel<>(schemaHandlingDtoModel,
                getExpression(ResourceObjectTypeDefinitionType.F_CREDENTIALS)), parentPage);
        getThirdRowContainer().replaceWith(newContainer);
		resetSelections(target);
        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), parentPage.getFeedbackPanel());
    }

    private void editAttributePerformed(AjaxRequestTarget target, final ResourceAttributeDefinitionType object) {
		resetSelections(target);
        if (schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto() != null && schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto().getObjectType().getObjectClass() != null) {
			schemaHandlingDtoModel.getObject().setSelectedAttribute(object);
            WebMarkupContainer newContainer = new ResourceAttributeEditor(ID_THIRD_ROW_CONTAINER, new Model<>(object),
                    schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto().getObjectType(), resourceModel.getObject(), this, parentPage.getReadOnlyModel());
            getThirdRowContainer().replaceWith(newContainer);

            target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR));
        } else {
            warn(getString("SchemaHandlingStep.message.selectObjectClassAttr"));
            getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
            target.add(parentPage.getFeedbackPanel(), get(ID_OBJECT_TYPE_EDITOR), getThirdRowContainer());
        }
    }

    private void editAssociationPerformed(AjaxRequestTarget target, ResourceObjectAssociationType object) {
		resetSelections(target);
        if (schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto() != null && schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto().getObjectType().getObjectClass() != null) {
			schemaHandlingDtoModel.getObject().setSelectedAssociation(object);
            WebMarkupContainer newContainer = new ResourceAssociationEditor(ID_THIRD_ROW_CONTAINER, new Model<>(object),
                    schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto().getObjectType(), resourceModel.getObject(), this, parentPage.getReadOnlyModel());
            getThirdRowContainer().replaceWith(newContainer);

            target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), parentPage.getFeedbackPanel());
        } else {
            warn(getString("SchemaHandlingStep.message.selectObjectClassAss"));
            getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
            target.add(parentPage.getFeedbackPanel(), get(ID_OBJECT_TYPE_EDITOR), getThirdRowContainer());
        }
    }

    @Override
    public void applyState() {
		parentPage.refreshIssues(null);
		if (parentPage.isReadOnly() || !isComplete()) {
			return;
		}
		savePerformed();
		insertEmptyThirdRow();          // otherwise the original 3rd column would be displayed after returning to the page
										// (but without 2nd column)
    }

    private void savePerformed() {
        PrismObject<ResourceType> oldResource;
        @NotNull PrismObject<ResourceType> newResource = resourceModel.getObject();
        Task task = parentPage.createSimpleTask(OPERATION_SAVE_SCHEMA_HANDLING);
        OperationResult result = task.getResult();
        ModelService modelService = parentPage.getModelService();
        ObjectDelta delta;
		boolean saved = false;

        removeEmptyContainers(newResource);

        try {
            oldResource = WebModelServiceUtils.loadObject(ResourceType.class, newResource.getOid(), parentPage, task, result);
            if (oldResource == null) {
				throw new IllegalStateException("No resource to apply schema handling to");
			}

			delta = parentPage.computeDiff(oldResource, newResource);
			if (!delta.isEmpty()) {
				parentPage.logDelta(delta);
				@SuppressWarnings("unchecked")
				Collection<ObjectDelta<? extends ObjectType>> deltas = WebComponentUtil.createDeltaCollection(delta);
				modelService.executeChanges(deltas, null, parentPage.createSimpleTask(OPERATION_SAVE_SCHEMA_HANDLING), result);
				parentPage.resetModels();
				saved = true;
			}
        } catch (RuntimeException|CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save schema handling", e);
            result.recordFatalError(getString("SchemaHandlingStep.message.saveError", e));
        } finally {
            result.computeStatusIfUnknown();
        }

        setResult(result);
		if (parentPage.showSaveResultInPage(saved, result)) {
            parentPage.showResult(result);
        }
    }

    private void editObjectTypePerformed(AjaxRequestTarget target, ResourceObjectTypeDefinitionTypeDto objectType){
		boolean wasAnySelected = isAnySelected();
        schemaHandlingDtoModel.getObject().setSelectedObjectTypeDto(objectType);
		resetSelections(target);
		insertEmptyThirdRow();
		if (wasAnySelected) {
			target.add(getObjectListTable(), getNavigator(), getObjectTypeEditor(), getThirdRowContainer());
		} else {
			target.add(this);
		}
    }

    private void deleteObjectTypePerformed(AjaxRequestTarget target, ResourceObjectTypeDefinitionTypeDto objectType) {
		ResourceObjectTypeDefinitionType realObjectType = objectType.getObjectType();
		resourceModel.getObject().asObjectable().getSchemaHandling().getObjectType().remove(realObjectType);

        if (isSelected(objectType)) {
			resetSelected();
			resetThirdRowContainer(target);
        }

		ArrayList<ResourceObjectTypeDefinitionTypeDto> list = (ArrayList<ResourceObjectTypeDefinitionTypeDto>) schemaHandlingDtoModel.getObject().getObjectTypeDtoList();
		list.remove(objectType);
		if (list.isEmpty()) {
            resetThirdRowContainer(target);
        }

        target.add(getObjectTypeEditor(), getObjectListTable(), getNavigator());
		parentPage.refreshIssues(target);
    }

	private boolean isSelected(@NotNull ResourceObjectTypeDefinitionTypeDto objectType) {
		return schemaHandlingDtoModel.getObject().getSelectedObjectTypeDto() == objectType;
	}

	private void addObjectTypePerformed(AjaxRequestTarget target) {
        ResourceObjectTypeDefinitionType objectType = new ResourceObjectTypeDefinitionType();
        //objectType.setDisplayName(generateName(getString("SchemaHandlingStep.label.newObjectType")));
        ResourceObjectTypeDefinitionTypeDto dto = new ResourceObjectTypeDefinitionTypeDto(objectType);

        if (schemaHandlingDtoModel.getObject().getObjectTypeDtoList().isEmpty()) {
            objectType.setDefault(true);
        }

        resetSelected();
        schemaHandlingDtoModel.getObject().setSelectedObjectTypeDto(dto);
        schemaHandlingDtoModel.getObject().getObjectTypeDtoList().add(dto);
		getOrCreateSchemaHandling().getObjectType().add(objectType);
        insertEmptyThirdRow();
		resetSelections(target);
		target.add(this);
		parentPage.refreshIssues(target);
    }

	private String generateName(String prefix) {
		List<String> existing = new ArrayList<>();
		for (ResourceObjectTypeDefinitionTypeDto objectTypeDto : schemaHandlingDtoModel.getObject().getObjectTypeDtoList()) {
			CollectionUtils.addIgnoreNull(existing, objectTypeDto.getObjectType().getDisplayName());
		}
		return generateName(existing, prefix);
	}

	static String generateName(List<String> existing, String prefix) {
		for (int i = 1;; i++) {
			String candidate = prefix + (i > 1 ? " "+i : "");
			if (!existing.contains(candidate)) {
				return candidate;
			}
		}
	}

	@NotNull
	private SchemaHandlingType getOrCreateSchemaHandling() {
		PrismObject<ResourceType> resource = resourceModel.getObject();
		try {
			resource.findOrCreateContainer(ResourceType.F_SCHEMA_HANDLING);
		} catch (SchemaException e) {
			throw new IllegalStateException("Couldn't find/create schemaHandling container: " + e.getMessage(), e);
		}
		return resource.asObjectable().getSchemaHandling();
	}

	private void removeEmptyContainers(@NotNull PrismObject<ResourceType> resourcePrism) {

        ResourceType resource = resourcePrism.asObjectable();

        if (resource.getSchemaHandling() != null) {
            SchemaHandlingType schemaHandling = resource.getSchemaHandling();

            for (ResourceObjectTypeDefinitionType objectType: schemaHandling.getObjectType()) {

                //Clear empty/invalid containers from attributes
                List<ResourceAttributeDefinitionType> newAttributeList = new ArrayList<>();
                newAttributeList.addAll(objectType.getAttribute());
				for (ResourceAttributeDefinitionType attribute : objectType.getAttribute()) {
					if (attribute.getRef() == null) {
						newAttributeList.remove(attribute);
					}
				}
				objectType.getAttribute().clear();
                objectType.getAttribute().addAll(newAttributeList);

				for (ResourceAttributeDefinitionType attr : objectType.getAttribute()) {
					List<MappingType> newInbounds = clearEmptyMappings(attr.getInbound());
					attr.getInbound().clear();
					attr.getInbound().addAll(newInbounds);
				}

                //Clear empty/invalid containers from associations
                List<ResourceObjectAssociationType> newAssociationList = new ArrayList<>();
                newAssociationList.addAll(objectType.getAssociation());
				for (ResourceObjectAssociationType association : objectType.getAssociation()) {
					if (association.getKind() == null) {
						newAssociationList.remove(association);
					}
				}
				objectType.getAssociation().clear();
                objectType.getAssociation().addAll(newAssociationList);

                for(ResourceObjectAssociationType association: objectType.getAssociation()){
                    List<MappingType> newInbounds = clearEmptyMappings(association.getInbound());
                    association.getInbound().clear();
                    association.getInbound().addAll(newInbounds);
                }

                prepareActivation(objectType.getActivation());

				// protected accounts
				List<ResourceObjectPatternType> newProtectedList = new ArrayList<>();
				for (ResourceObjectPatternType protectedObject : objectType.getProtected()) {
					if (protectedObject.getFilter() != null && !protectedObject.getFilter().containsFilterClause()) {
						// we know that we lose description for empty filters ... but such filters (description + no clause) cause problems in prisms
						protectedObject.setFilter(null);
					}
					if (protectedObject.getName() != null || protectedObject.getUid() != null || protectedObject.getFilter() != null) {
						newProtectedList.add(protectedObject);
					}
				}
				replace(objectType.getProtected(), newProtectedList);

				// iterator expressions
				if (objectType.getIteration() != null) {
					IterationSpecificationType it = objectType.getIteration();
					if (ExpressionUtil.isEmpty(it.getTokenExpression())) {
						it.setTokenExpression(null);
					}
					if (ExpressionUtil.isEmpty(it.getPreIterationCondition())) {
						it.setPreIterationCondition(null);
					}
					if (ExpressionUtil.isEmpty(it.getPostIterationCondition())) {
						it.setPostIterationCondition(null);
					}
				}
            }
        }
    }

	private <T> void replace(List<T> list, List<T> newList) {
		list.clear();
		list.addAll(newList);
	}

	private List<MappingType> clearEmptyMappings(List<MappingType> list){
        List<MappingType> newList = new ArrayList<>();

        for(MappingType mapping: list){
            if(!WizardUtil.isEmptyMapping(mapping)){
                newList.add(mapping);
            }
        }

        return newList;
    }

    private void prepareActivation(ResourceActivationDefinitionType activation){
		if (activation == null) {
			return;
		}

		if (activation.getAdministrativeStatus() != null) {
			ResourceBidirectionalMappingType administrativeStatus = activation.getAdministrativeStatus();

            List<MappingType> inbounds = administrativeStatus.getInbound();
            List<MappingType> outbounds = administrativeStatus.getOutbound();

            List<MappingType> newInbounds = prepareActivationMappings(inbounds,
                    ResourceActivationEditor.ADM_STATUS_IN_SOURCE_DEFAULT, ResourceActivationEditor.ADM_STATUS_IN_TARGET_DEFAULT);
            administrativeStatus.getInbound().clear();
            administrativeStatus.getInbound().addAll(newInbounds);

            List<MappingType> newOutbounds = prepareActivationMappings(outbounds,
                    ResourceActivationEditor.ADM_STATUS_OUT_SOURCE_DEFAULT, ResourceActivationEditor.ADM_STATUS_OUT_TARGET_DEFAULT);
            administrativeStatus.getOutbound().clear();
            administrativeStatus.getOutbound().addAll(newOutbounds);

            if(isBidirectionalMappingEmpty(administrativeStatus)){
                activation.setAdministrativeStatus(null);
            }
        }

		if (activation.getValidTo() != null) {
			ResourceBidirectionalMappingType validTo = activation.getValidTo();

            List<MappingType> inbounds = validTo.getInbound();
            List<MappingType> outbounds = validTo.getOutbound();

            List<MappingType> newInbounds = prepareActivationMappings(inbounds,
                    ResourceActivationEditor.VALID_TO_IN_SOURCE_DEFAULT, ResourceActivationEditor.VALID_TO_IN_TARGET_DEFAULT);
            validTo.getInbound().clear();
            validTo.getInbound().addAll(newInbounds);

            List<MappingType> newOutbounds = prepareActivationMappings(outbounds,
                    ResourceActivationEditor.VALID_TO_OUT_SOURCE_DEFAULT, ResourceActivationEditor.VALID_TO_OUT_TARGET_DEFAULT);
            validTo.getOutbound().clear();
            validTo.getOutbound().addAll(newOutbounds);

			if (isBidirectionalMappingEmpty(validTo)) {
				activation.setValidTo(null);
			}
		}

		if (activation.getValidFrom() != null) {
			ResourceBidirectionalMappingType validFrom = activation.getValidFrom();

            List<MappingType> inbounds = validFrom.getInbound();
            List<MappingType> outbounds = validFrom.getOutbound();

            List<MappingType> newInbounds = prepareActivationMappings(inbounds,
                    ResourceActivationEditor.VALID_FROM_IN_SOURCE_DEFAULT, ResourceActivationEditor.VALID_FROM_IN_TARGET_DEFAULT);
            validFrom.getInbound().clear();
            validFrom.getInbound().addAll(newInbounds);

            List<MappingType> newOutbounds = prepareActivationMappings(outbounds,
                    ResourceActivationEditor.VALID_FROM_OUT_SOURCE_DEFAULT, ResourceActivationEditor.VALID_FROM_OUT_TARGET_DEFAULT);
            validFrom.getOutbound().clear();
            validFrom.getOutbound().addAll(newOutbounds);

			if (isBidirectionalMappingEmpty(validFrom)) {
				activation.setValidFrom(null);
			}
		}

		if (activation.getExistence() != null) {
			ResourceBidirectionalMappingType existence = activation.getExistence();

            List<MappingType> inbounds = existence.getInbound();
            List<MappingType> newInbounds = new ArrayList<>();

            for(MappingType inbound: inbounds){
                if(WizardUtil.isEmptyMapping(inbound)){
                    continue;
                }

                if(inbound.getSource().size() == 1 && compareItemPath(inbound.getSource().get(0).getPath(), ResourceActivationEditor.EXISTENCE_DEFAULT_SOURCE)){
                    newInbounds.add(new MappingType());
                    continue;
                }

                newInbounds.add(inbound);
            }

            existence.getInbound().clear();
            existence.getInbound().addAll(newInbounds);

            List<MappingType> outbounds = existence.getOutbound();
            List<MappingType> newOutbounds = new ArrayList<>();

            for(MappingType outbound: outbounds){
                if(!WizardUtil.isEmptyMapping(outbound)){
                    newOutbounds.add(outbound);
                }
            }

            existence.getOutbound().clear();
            existence.getOutbound().addAll(newOutbounds);

            if(isBidirectionalMappingEmpty(existence)){
                activation.setExistence(null);
            }
        }
    }

    private boolean isBidirectionalMappingEmpty(ResourceBidirectionalMappingType mapping){
        return mapping.getFetchStrategy() == null && mapping.getInbound().isEmpty() && mapping.getOutbound().isEmpty();

    }

    private List<MappingType> prepareActivationMappings(List<MappingType> list, String defaultSource, String defaultTarget){
        List<MappingType> newMappings = new ArrayList<>();

        for(MappingType mapping: list){
            if(WizardUtil.isEmptyMapping(mapping)){
                continue;
            }

            if(mapping.getTarget() != null){
                if(compareItemPath(mapping.getTarget().getPath(), defaultTarget)){
                    mapping.setTarget(null);
                }
            }

            if(mapping.getSource().size() == 1){
                if(compareItemPath(mapping.getSource().get(0).getPath(), defaultSource)){
                    mapping.getSource().clear();
                }
            }

            newMappings.add(mapping);
        }

        return newMappings;
    }

    private boolean compareItemPath(ItemPathType itemPath, String comparePath){
        if(itemPath != null && itemPath.getItemPath() != null){
            if(comparePath.equals(itemPath.getItemPath().toString())){
                return true;
            }
        }

        return false;
    }

    private ResourceObjectAssociationType createEmptyAssociationObject(){
        ResourceObjectAssociationType association = new ResourceObjectAssociationType();
        association.setTolerant(true);
        return association;
    }

    private ResourceAttributeDefinitionType createEmptyAttributeObject(){
        ResourceAttributeDefinitionType attribute = new ResourceAttributeDefinitionType();
        attribute.setTolerant(true);
        return attribute;
    }

	private class UpdateNamesBehaviour extends EmptyOnChangeAjaxFormUpdatingBehavior {
		@Override
		protected void onUpdate(AjaxRequestTarget target) {
			target.add(getObjectListTable(), getObjectTypeEditor().get(ID_EDITOR_NAME));
			parentPage.refreshIssues(target);
		}
	}
}
