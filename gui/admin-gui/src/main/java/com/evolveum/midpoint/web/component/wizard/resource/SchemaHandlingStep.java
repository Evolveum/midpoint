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


import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
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
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;

import javax.xml.namespace.QName;
import java.util.*;

/**
 *  @author lazyman
 *  @author shood
 */
public class SchemaHandlingStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlingStep.class);

    private static final String DOT_CLASS = SchemaHandlingStep.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT_CLASS_LIST = DOT_CLASS + "loadObjectClassList";

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

        if(resourceModel.getObject() != null && resourceModel.getObject() != null
                && resourceModel.getObject().asObjectable() != null){
            SchemaHandlingType schemaHandling = resourceModel.getObject().asObjectable().getSchemaHandling();

            ResourceObjectTypeDefinitionTypeDto obj;
            if(schemaHandling.getObjectType() != null){
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

        //TODO - delete this when this step is finished and tested
        ResourceObjectTypeDefinitionType sample = new ResourceObjectTypeDefinitionType();
        sample.setDisplayName("Test Object Class");
        sample.getAttribute().add(new ResourceAttributeDefinitionType());
        sample.getAssociation().add(new ResourceObjectAssociationType());
        ResourceObjectTypeDefinitionTypeDto sampleDto = new ResourceObjectTypeDefinitionTypeDto(sample);
        list.add(sampleDto);
        //TODO - delete

        dto.setSelected(createPlaceholderObjectType());
        dto.setObjectClassList(loadResourceObjectClassList());
        dto.setObjectTypeList(list);
        return dto;
    }

    private List<QName> loadResourceObjectClassList(){
        List<QName> list = new ArrayList<>();

        try {
            ResourceSchema schema = RefinedResourceSchema.getResourceSchema(resourceModel.getObject(), getPageBase().getPrismContext());
            schema.getObjectClassDefinitions();

            for(Definition def: schema.getDefinitions()){
                list.add(def.getTypeName());
            }

        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Couldn't load object class list from resource.", e);
            error(getString("SchemaHandlingStep.message.errorLoadingObjectTypeList") + " " + e.getMessage());
        }

        return list;
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

                AjaxLink link = new AjaxLink(ID_LINK_OBJECT_TYPE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editObjectTypePerformed(target, objectType);
                    }
                };
                item.add(link);

                Label label = new Label(ID_NAME_OBJECT_TYPE, new PropertyModel<>(objectType, "objectType.displayName"));
                label.setOutputMarkupId(true);
                link.add(label);

                AjaxLink delete = new AjaxLink(ID_BUTTON_DELETE_OBJECT_TYPE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteObjectTypePerformed(target, objectType);
                    }
                };
                item.add(delete);

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<Object>() {

                    @Override
                    public Object getObject() {
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
        initModals();
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

        AjaxLink editorDependency = new AjaxLink(ID_EDITOR_BUTTON_DEPENDENCY) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                dependencyEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorDependency);
        editor.add(editorDependency);

        AutoCompleteTextField<String> editorObjectClass = new AutoCompleteTextField<String>(ID_EDITOR_OBJECT_CLASS,
                new PropertyModel<String>(model, SchemaHandlingDto.F_SELECTED + ".objectClass.localPart")) {

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
        editorObjectClass.add(createObjectClassValidator());
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

        AjaxLink editorIteration = new AjaxLink(ID_EDITOR_BUTTON_ITERATION) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                iterationEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorIteration);
        editor.add(editorIteration);

        AjaxLink editorProtected = new AjaxLink(ID_EDITOR_BUTTON_PROTECTED) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                protectedEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorProtected);
        editor.add(editorProtected);

        AjaxLink editorActivation = new AjaxLink(ID_EDITOR_BUTTON_ACTIVATION) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                activationEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorActivation);
        editor.add(editorActivation);

        AjaxLink editorCredentials = new AjaxLink(ID_EDITOR_BUTTON_CREDENTIALS) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                credentialsEditPerformed(target);
            }
        };
        addDisabledClassModifier(editorCredentials);
        editor.add(editorCredentials);
    }

    private void initModals(){
        //TODO - init all modal windows here
    }

    private IValidator<String> createObjectClassValidator(){
        return new IValidator<String>() {

            @Override
            public void validate(IValidatable<String> validatable) {
                String value = validatable.getValue();
                List<QName> list = model.getObject().getObjectClassList();
                List<String> stringList = new ArrayList<>();

                for(QName q: list){
                    stringList.add(q.getLocalPart());
                }

                if(!stringList.contains(value)){
                    error(createStringResource("SchemaHandlingStep.message.validationError", value).getString());
                }
            }
        };
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

        target.add(getThirdRowContainer());
    }

    private void iterationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceIterationEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<IterationSpecificationType>(model, SchemaHandlingDto.F_SELECTED + ".iteration"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer());
    }

    private void protectedEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceProtectedEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<List<ResourceObjectPatternType>>(model, SchemaHandlingDto.F_SELECTED + "._protected"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer());
    }

    private void activationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceActivationEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ResourceActivationDefinitionType>(model, SchemaHandlingDto.F_SELECTED + ".activation"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer());
    }

    private void credentialsEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new ResourceCredentialsEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ResourceCredentialsDefinitionType>(model, SchemaHandlingDto.F_SELECTED + ".credentials"));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer());
    }

    private void editAttributePerformed(AjaxRequestTarget target, ResourceAttributeDefinitionType object){
        WebMarkupContainer newContainer = new ResourceAttributeEditor(ID_THIRD_ROW_CONTAINER, new Model<>(object));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer());
    }

    private void editAssociationPerformed(AjaxRequestTarget target, ResourceObjectAssociationType object){
        WebMarkupContainer newContainer = new ResourceAssociationEditor(ID_THIRD_ROW_CONTAINER, new Model<>(object));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer());
    }

    @Override
    public void applyState() {
        // TODO - implement
    }

    private void editObjectTypePerformed(AjaxRequestTarget target, ResourceObjectTypeDefinitionTypeDto objectType){
        resetSelected();
        objectType.setSelected(true);
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
}
