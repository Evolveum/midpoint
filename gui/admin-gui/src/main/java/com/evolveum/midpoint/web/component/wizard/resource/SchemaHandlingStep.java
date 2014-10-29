/*
 * Copyright (c) 2010-2013 Evolveum
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


import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling.*;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ResourceObjectTypeDefinitionTypeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.SchemaHandlingDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.string.Strings;

import javax.xml.namespace.QName;
import java.util.*;

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

    private IModel<PrismObject<ResourceType>> resourceModel;
    private IModel<SchemaHandlingDto> model;

    public SchemaHandlingStep(final IModel<PrismObject<ResourceType>> resourceModel) {
        this.resourceModel = resourceModel;

        model = new LoadableModel<SchemaHandlingDto>(false) {

            @Override
            protected SchemaHandlingDto load() {
                return lodObjectTypes();
            }
        };

        initLayout();
    }

    private SchemaHandlingDto lodObjectTypes(){
        SchemaHandlingDto dto = new SchemaHandlingDto();
        List<ResourceObjectTypeDefinitionTypeDto> list = new ArrayList<>();

        if(resourceModel != null && resourceModel.getObject() != null
                && resourceModel.getObject().asObjectable() != null){
            SchemaHandlingType schemaHandling = resourceModel.getObject().asObjectable().getSchemaHandling();

            ResourceObjectTypeDefinitionTypeDto obj;
            if(schemaHandling != null && schemaHandling.getObjectType() != null){
                for(ResourceObjectTypeDefinitionType objectType: schemaHandling.getObjectType()){

                    // temporary fix - think about better solution
                    if(objectType.getAttribute().isEmpty()){
                        objectType.getAttribute().add(new ResourceAttributeDefinitionType());
                    }

                    if(objectType.getAssociation().isEmpty()){
                        objectType.getAssociation().add(new ResourceObjectAssociationType());
                    }

                    obj = new ResourceObjectTypeDefinitionTypeDto(objectType);
                    list.add(obj);
                }
            }
        }

        dto.setSelected(createPlaceholderObjectType());
        dto.setObjectClassList(loadResourceObjectClassList(resourceModel, LOGGER, getString("SchemaHandlingStep.message.errorLoadingObjectTypeList")));
        dto.setObjectTypeList(list);
        return dto;
    }

    private ResourceObjectTypeDefinitionType createPlaceholderObjectType(){
        // temporary fix - think about better solution
        ResourceObjectTypeDefinitionType placeholder = new ResourceObjectTypeDefinitionType();
        placeholder.getAttribute().add(new ResourceAttributeDefinitionType());
        placeholder.getAssociation().add(new ResourceObjectAssociationType());

        return placeholder;
    }

    private boolean isAnySelected(){
        for(ResourceObjectTypeDefinitionTypeDto dto: model.getObject().getObjectTypeList()){
            if(dto.isSelected()){
                return true;
            }
        }

        return false;
    }

    private void initLayout(){
        final ListDataProvider<ResourceObjectTypeDefinitionTypeDto> objectTypeProvider = new ListDataProvider<>(this,
                new PropertyModel<List<ResourceObjectTypeDefinitionTypeDto>>(model, SchemaHandlingDto.F_OBJECT_TYPES));

        // first row - object types list
        WebMarkupContainer tableBody = new WebMarkupContainer(ID_ROWS);
        tableBody.setOutputMarkupId(true);
        add(tableBody);

        // second row - object type editor
        WebMarkupContainer objectTypeEditor = new WebMarkupContainer(ID_OBJECT_TYPE_EDITOR);
        objectTypeEditor.setOutputMarkupId(true);
        objectTypeEditor.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isEnabled() {
                return isAnySelected();
            }
        });
        add(objectTypeEditor);

        // third row container
        WebMarkupContainer thirdRowContainer = new WebMarkupContainer(ID_THIRD_ROW_CONTAINER);
        thirdRowContainer.setOutputMarkupId(true);
        add(thirdRowContainer);

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
                link.add(delete);

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if(item.getModelObject().isSelected()){
                            return "success";
                        }

                        return null;
                    }
                }));
            }
        };
        tableBody.add(objectTypeDataView);

        NavigatorPanel navigator = new NavigatorPanel(ID_PAGING_OBJECT_TYPE, objectTypeDataView, true);
        navigator.setOutputMarkupPlaceholderTag(true);
        navigator.setOutputMarkupId(true);
        add(navigator);

        AjaxLink add = new AjaxLink(ID_BUTTON_ADD_OBJECT_TYPE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addObjectTypePerformed(target);
            }
        };
        add(add);

        initObjectTypeEditor(objectTypeEditor);
    }

    private IModel<String> createObjectTypeDisplayModel(final ResourceObjectTypeDefinitionTypeDto objectType){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(objectType != null && objectType.getObjectType() != null){
                    ResourceObjectTypeDefinitionType object = objectType.getObjectType();
                    sb.append(object.getDisplayName() != null ? object.getDisplayName() : "- ");

                    if(object.getKind() != null || object.getIntent() != null){
                        sb.append(" (");
                        sb.append(object.getKind() != null ? object.getKind() : " - ");
                        sb.append(", ");
                        sb.append(object.getIntent() != null ? object.getIntent() : "- ");
                        sb.append(")");
                    }
                }

                return sb.toString();
            }
        };
    }

    private void initObjectTypeEditor(WebMarkupContainer editor){
        Label editorLabel = new Label(ID_EDITOR_NAME, new LoadableModel<String>() {

            @Override
            protected String load() {
                if(!isAnySelected()){
                    return getString("SchemaHandlingStep.label.emptyDisplayName");
                } else {
                    return model.getObject().getSelected().getDisplayName();
                }
            }
        });
        editor.add(editorLabel);

        DropDownChoice editorKind = new DropDownChoice<>(ID_EDITOR_KIND,
                new PropertyModel<ShadowKindType>(model, SchemaHandlingDto.F_SELECTED + ".kind"),
                WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                new EnumChoiceRenderer<ShadowKindType>(this));
        editor.add(editorKind);

        TextField editorIntent = new TextField<>(ID_EDITOR_INTENT, new PropertyModel<String>(model,
                SchemaHandlingDto.F_SELECTED + ".intent"));
        editor.add(editorIntent);

        TextField editorDisplayName = new TextField<>(ID_EDITOR_DISPLAY_NAME, new PropertyModel<String>(model,
                SchemaHandlingDto.F_SELECTED + ".displayName"));
        editor.add(editorDisplayName);

        TextArea editorDescription = new TextArea<>(ID_EDITOR_DESCRIPTION, new PropertyModel<String>(model,
                SchemaHandlingDto.F_SELECTED + ".description"));
        editor.add(editorDescription);

        CheckBox editorDefault = new CheckBox(ID_EDITOR_DEFAULT, new PropertyModel<Boolean>(model,
                SchemaHandlingDto.F_SELECTED + "._default"));
        editor.add(editorDefault);

        AjaxSubmitLink editorDependency = new AjaxSubmitLink(ID_EDITOR_BUTTON_DEPENDENCY) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                dependencyEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorDependency);
        editor.add(editorDependency);

        AutoCompleteTextField<String> editorObjectClass = new AutoCompleteTextField<String>(ID_EDITOR_OBJECT_CLASS,
                new PropertyModel<String>(model, SchemaHandlingDto.F_SELECTED_OBJECT_CLASS)) {

            @Override
            protected Iterator<String> getChoices(String input) {
                if(Strings.isEmpty(input)){
                    List<String> emptyList = Collections.emptyList();
                    return emptyList.iterator();
                }

                List<QName> resourceObjectClassList = model.getObject().getObjectClassList();
                List<String> choices = new ArrayList<>(AUTO_COMPLETE_LIST_SIZE);

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
        };
        editorObjectClass.add(createObjectClassValidator(new LoadableModel<List<QName>>(false) {

            @Override
            protected List<QName> load() {
                return model.getObject().getObjectClassList();
            }
        }));
        editor.add(editorObjectClass);

        MultiValueTextEditPanel editorAttributes = new MultiValueTextEditPanel<ResourceAttributeDefinitionType>(ID_EDITOR_ATTRIBUTES,
                new PropertyModel<List<ResourceAttributeDefinitionType>>(model, SchemaHandlingDto.F_SELECTED + ".attribute"), false, false){

            @Override
            protected IModel<String> createTextModel(final IModel<ResourceAttributeDefinitionType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();

                        if(model.getObject().getRef() != null){
                            sb.append(model.getObject().getRef().getLocalPart());
                        } else {
                            return null;
                        }

                        if(model.getObject().getDisplayName() != null){
                            sb.append(" (").append(model.getObject().getDisplayName()).append(")");
                        }

                        return sb.toString();
                    }
                };
            }

            @Override
            protected ResourceAttributeDefinitionType createNewEmptyItem(){
                return new ResourceAttributeDefinitionType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, ResourceAttributeDefinitionType object){
                editAttributePerformed(target, object);
            }

            @Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }
        };
        editor.add(editorAttributes);

        MultiValueTextEditPanel editorAssociations = new MultiValueTextEditPanel<ResourceObjectAssociationType>(ID_EDITOR_ASSOCIATIONS,
                new PropertyModel<List<ResourceObjectAssociationType>>(model, SchemaHandlingDto.F_SELECTED + ".association"), false, false){

            @Override
            protected IModel<String> createTextModel(final IModel<ResourceObjectAssociationType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();

                        if(model.getObject().getRef() != null){
                            sb.append(model.getObject().getRef().getLocalPart());
                        } else {
                            return null;
                        }

                        if(model.getObject().getDisplayName() != null){
                            sb.append(" (").append(model.getObject().getDisplayName()).append(")");
                        }

                        return sb.toString();
                    }
                };
            }

            @Override
            protected ResourceObjectAssociationType createNewEmptyItem(){
                return new ResourceObjectAssociationType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, ResourceObjectAssociationType object){
                editAssociationPerformed(target, object);
            }

            @Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }
        };
        editor.add(editorAssociations);

        DropDownChoice editorAssignmentPolicyRef = new DropDownChoice<>(ID_EDITOR_ASSIGNMENT_POLICY,
                new PropertyModel<AssignmentPolicyEnforcementType>(model, SchemaHandlingDto.F_SELECTED + ".assignmentPolicyEnforcement"),
                WebMiscUtil.createReadonlyModelFromEnum(AssignmentPolicyEnforcementType.class),
                new EnumChoiceRenderer<AssignmentPolicyEnforcementType>(this));
        editor.add(editorAssignmentPolicyRef);

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

    private Component getThirdRowContainer(){
        return get(ID_THIRD_ROW_CONTAINER);
    }

    private Component getNavigator(){
        return get(ID_PAGING_OBJECT_TYPE);
    }

    private void resetSelected(){
        for(ResourceObjectTypeDefinitionTypeDto dto: model.getObject().getObjectTypeList()){
            dto.setSelected(false);
        }
    }

    private void insertEmptyThirdRow(){
        getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
    }

    private void dependencyEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceDependencyEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<List<ResourceObjectTypeDependencyType>>(model, SchemaHandlingDto.F_SELECTED + ".dependency"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void iterationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceIterationEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<IterationSpecificationType>(model, SchemaHandlingDto.F_SELECTED + ".iteration"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void protectedEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceProtectedEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<List<ResourceObjectPatternType>>(model, SchemaHandlingDto.F_SELECTED + "._protected"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void activationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceActivationEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ResourceActivationDefinitionType>(model, SchemaHandlingDto.F_SELECTED + ".activation"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void credentialsEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceCredentialsEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ResourceCredentialsDefinitionType>(model, SchemaHandlingDto.F_SELECTED + ".credentials"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void editAttributePerformed(AjaxRequestTarget target, final ResourceAttributeDefinitionType object){
        if(model.getObject().getSelected() != null && model.getObject().getSelected().getObjectClass() != null){
            WebMarkupContainer newContainer = new ResourceAttributeEditor(ID_THIRD_ROW_CONTAINER, new Model<>(object),
                    model.getObject().getSelected(), resourceModel.getObject());
            getThirdRowContainer().replaceWith(newContainer);

            target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR));
        } else {
            warn(getString("SchemaHandlingStep.message.selectObjectClassAttr"));
            getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
            target.add(getPageBase().getFeedbackPanel(), get(ID_OBJECT_TYPE_EDITOR), getThirdRowContainer());
        }
    }

    private void editAssociationPerformed(AjaxRequestTarget target, ResourceObjectAssociationType object){
        if(model.getObject().getSelected() != null && model.getObject().getSelected().getObjectClass() != null){
            WebMarkupContainer newContainer = new ResourceAssociationEditor(ID_THIRD_ROW_CONTAINER, new Model<>(object),
                    model.getObject().getSelected(), resourceModel.getObject());
            getThirdRowContainer().replaceWith(newContainer);

            target.add(getThirdRowContainer(), get(ID_OBJECT_TYPE_EDITOR), getPageBase().getFeedbackPanel());
        } else {
            warn(getString("SchemaHandlingStep.message.selectObjectClassAss"));
            getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
            target.add(getPageBase().getFeedbackPanel(), get(ID_OBJECT_TYPE_EDITOR), getThirdRowContainer());
        }
    }

    @Override
    public void applyState() {
        savePerformed();
    }

    private void savePerformed(){
        PrismObject<ResourceType> oldResource;
        PrismObject<ResourceType> newResource = resourceModel.getObject();
        OperationResult result = new OperationResult(OPERATION_SAVE_SCHEMA_HANDLING);
        ModelService modelService = getPageBase().getModelService();
        ObjectDelta delta;

        removeEmptyContainers(newResource);

        try{
            oldResource = WebModelUtils.loadObject(ResourceType.class, newResource.getOid(), result, getPageBase());
            if(oldResource != null){
                delta = oldResource.diff(newResource);

                if(LOGGER.isTraceEnabled()){
                    LOGGER.trace(delta.debugDump());
                }

                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                modelService.executeChanges(deltas, null, getPageBase().createSimpleTask(OPERATION_SAVE_SCHEMA_HANDLING), result);
            }

        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Couldn't save schema handling", e);
            result.recordFatalError(getString("SchemaHandlingStep.message.saveError", e));
        } finally {
            result.computeStatusIfUnknown();
        }

        if(WebMiscUtil.showResultInPage(result)){
            getPageBase().showResult(result);
        }
    }

    private void editObjectTypePerformed(AjaxRequestTarget target, ResourceObjectTypeDefinitionTypeDto objectType){
        resetSelected();
        objectType.setSelected(true);

        if(objectType.getObjectType().getAssociation().isEmpty()){
            objectType.getObjectType().getAssociation().add(new ResourceObjectAssociationType());
        }

        if(objectType.getObjectType().getAttribute().isEmpty()){
            objectType.getObjectType().getAttribute().add(new ResourceAttributeDefinitionType());
        }

        model.getObject().setSelected(objectType.getObjectType());

        insertEmptyThirdRow();
        target.add(getObjectListTable(), getNavigator(), getObjectTypeEditor(), getThirdRowContainer());
    }

    private void deleteObjectTypePerformed(AjaxRequestTarget target, ResourceObjectTypeDefinitionTypeDto objectType){
        ArrayList<ResourceObjectTypeDefinitionTypeDto> list = (ArrayList<ResourceObjectTypeDefinitionTypeDto>) model.getObject().getObjectTypeList();

        list.remove(objectType);

        if(objectType.isSelected()){
            model.getObject().setSelected(createPlaceholderObjectType());
            insertEmptyThirdRow();
            target.add(getThirdRowContainer());
        }

        if(list.isEmpty()){
            ResourceObjectTypeDefinitionType newObj = new ResourceObjectTypeDefinitionType();
            newObj.setDisplayName(getString("SchemaHandlingStep.label.newObjectType"));
            ResourceObjectTypeDefinitionTypeDto dto = new ResourceObjectTypeDefinitionTypeDto(newObj);
            dto.setSelected(true);
            list.add(dto);
        }

        target.add(getObjectTypeEditor(), getObjectListTable(), getNavigator());
    }

    private void addObjectTypePerformed(AjaxRequestTarget target){
        ResourceObjectTypeDefinitionType objectType = new ResourceObjectTypeDefinitionType();
        objectType.getAttribute().add(new ResourceAttributeDefinitionType());
        objectType.getAssociation().add(new ResourceObjectAssociationType());
        objectType.setDisplayName(getString("SchemaHandlingStep.label.newObjectType"));
        ResourceObjectTypeDefinitionTypeDto dto = new ResourceObjectTypeDefinitionTypeDto(objectType);

        resetSelected();
        dto.setSelected(true);
        model.getObject().setSelected(dto.getObjectType());
        model.getObject().getObjectTypeList().add(dto);
        insertEmptyThirdRow();
        target.add(getObjectListTable(), getNavigator(), getObjectTypeEditor(), getThirdRowContainer());
    }

    private void removeEmptyContainers(PrismObject<ResourceType> resourcePrism){
        if(resourcePrism == null){
            return;
        }

        ResourceType resource = resourcePrism.asObjectable();

        if(resource != null && resource.getSchemaHandling() != null){
            SchemaHandlingType schemaHandling = resource.getSchemaHandling();

            for(ResourceObjectTypeDefinitionType objectType: schemaHandling.getObjectType()){

                //Clear obsolete containers from attributes
                List<ResourceAttributeDefinitionType> newAttributeList = new ArrayList<>();
                newAttributeList.addAll(objectType.getAttribute());
                for(ResourceAttributeDefinitionType attribute: objectType.getAttribute()){
                    if(attribute.getRef() == null){
                        newAttributeList.remove(attribute);
                    }
                }
                objectType.getAttribute().clear();
                objectType.getAttribute().addAll(newAttributeList);

                //Clear obsolete containers from associations
                List<ResourceObjectAssociationType> newAssociationList = new ArrayList<>();
                newAssociationList.addAll(objectType.getAssociation());
                for(ResourceObjectAssociationType association: objectType.getAssociation()){
                    if(association.getKind() == null){
                        newAssociationList.remove(association);
                    }
                }
                objectType.getAssociation().clear();
                objectType.getAssociation().addAll(newAssociationList);
            }
        }
    }
}
