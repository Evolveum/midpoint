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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueAutoCompleteTextPanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueTextEditPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.component.synchronization.ConditionalSearchFilterEditor;
import com.evolveum.midpoint.web.component.wizard.resource.component.synchronization.SynchronizationExpressionEditor;
import com.evolveum.midpoint.web.component.wizard.resource.component.synchronization.SynchronizationReactionEditor;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectSynchronizationTypeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ResourceSynchronizationDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidator;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 * @author shood
 */
public class SynchronizationStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationStep.class);

    private static final String DOT_CLASS = SynchronizationStep.class.getName() + ".";
    private static final String OPERATION_SAVE_SYNC = DOT_CLASS + "saveResourceSynchronization";
    private static final String OPERATION_LOAD_OBJECT_TEMPLATE_LIST = "loadObjectTemplateList";

    private static final String ID_TABLE_ROWS = "tableRows";
    private static final String ID_OBJECT_SYNC_ROW = "objectSyncRow";
    private static final String ID_OBJECT_SYNC_LINK = "objectSyncLink";
    private static final String ID_OBJECT_SYNC_LABEL = "objectSyncName";
    private static final String ID_OBJECT_SYNC_DELETE = "objectSyncDelete";
    private static final String ID_PAGING = "objectSyncPaging";
    private static final String ID_OBJECT_SYNC_ADD = "objectSyncAddButton";
    private static final String ID_OBJECT_SYNC_EDITOR = "objectSyncConfig";
    private static final String ID_THIRD_ROW_CONTAINER = "thirdRowContainer";

    private static final String ID_EDITOR_LABEL = "editorLabel";
    private static final String ID_EDITOR_NAME = "editorName";
    private static final String ID_EDITOR_DESCRIPTION = "editorDescription";
    private static final String ID_EDITOR_KIND = "editorKind";
    private static final String ID_EDITOR_INTENT = "editorIntent";
    private static final String ID_EDITOR_FOCUS = "editorFocus";
    private static final String ID_EDITOR_ENABLED = "editorEnabled";
    private static final String ID_EDITOR_BUTTON_CONDITION = "editorConditionButton";
    private static final String ID_EDITOR_BUTTON_CONFIRMATION = "editorConfirmationButton";
    private static final String ID_EDITOR_OBJECT_TEMPLATE = "editorObjectTemplate";
    private static final String ID_EDITOR_RECONCILE = "editorReconcile";
    private static final String ID_EDITOR_OPPORTUNISTIC = "editorOpportunistic";
    private static final String ID_EDITOR_OBJECT_CLASS = "editorObjectClass";
    private static final String ID_EDITOR_EDITOR_CORRELATION = "editorCorrelation";
    private static final String ID_EDITOR_REACTION = "editorReaction";
    private static final String ID_T_KIND = "kindTooltip";
    private static final String ID_T_INTENT = "intentTooltip";
    private static final String ID_T_OBJ_CLASS = "objectClassTooltip";
    private static final String ID_T_FOCUS = "focusTooltip";
    private static final String ID_T_ENABLED = "enabledTooltip";
    private static final String ID_T_CONDITION = "conditionTooltip";
    private static final String ID_T_CONFIRMATION = "confirmationTooltip";
    private static final String ID_T_OBJ_TEMPLATE = "objectTemplateTooltip";
    private static final String ID_T_RECONCILE = "reconcileTooltip";
    private static final String ID_T_OPPORTUNISTIC = "opportunisticTooltip";
    private static final String ID_T_CORRELATION = "correlationTooltip";
    private static final String ID_T_REACTION = "reactionTooltip";

    private IModel<PrismObject<ResourceType>> resourceModel;
    private IModel<ResourceSynchronizationDto> model;

    public SynchronizationStep(IModel<PrismObject<ResourceType>> resourceModel) {
        this.resourceModel = resourceModel;

        model = new LoadableModel<ResourceSynchronizationDto>(false) {
            @Override
            protected ResourceSynchronizationDto load() {
                return loadResourceSynchronization();
            }
        };

        initLayout();
    }

    private ResourceSynchronizationDto loadResourceSynchronization(){
        ResourceSynchronizationDto dto = new ResourceSynchronizationDto();
        List<ObjectSynchronizationTypeDto> list = new ArrayList<>();

        if(resourceModel != null && resourceModel.getObject() != null &&
                resourceModel.getObject().asObjectable() != null){

            SynchronizationType sync = resourceModel.getObject().asObjectable().getSynchronization();

            ObjectSynchronizationTypeDto obj;
            if(sync != null && sync.getObjectSynchronization() != null){
                for(ObjectSynchronizationType syncObject: sync.getObjectSynchronization()){

                    if(syncObject.getObjectClass().isEmpty()){
                        syncObject.getObjectClass().add(new QName(""));
                    }

                    if(syncObject.getCorrelation().isEmpty()){
                        syncObject.getCorrelation().add(new ConditionalSearchFilterType());
                    }

                    if(syncObject.getReaction().isEmpty()){
                        syncObject.getReaction().add(new SynchronizationReactionType());
                    }

                    obj = new ObjectSynchronizationTypeDto(syncObject);
                    list.add(obj);
                }
            }
        }

        dto.setSelected(createPlaceholderObjectType());
        dto.setObjectSyncList(list);
        dto.setObjectClassList(loadResourceObjectClassList(resourceModel, LOGGER, getString("SynchronizationStep.message.errorLoadingObjectSyncList")));

        return dto;
    }

    private ObjectSynchronizationType createPlaceholderObjectType(){
        ObjectSynchronizationType syncObject = new ObjectSynchronizationType();
        syncObject.getCorrelation().add(new ConditionalSearchFilterType());
        syncObject.getReaction().add(new SynchronizationReactionType());
        return syncObject;
    }

    private boolean isAnySelected(){
        for(ObjectSynchronizationTypeDto dto: model.getObject().getObjectSyncList()){
            if(dto.isSelected()){
                return true;
            }
        }

        return false;
    }

    private void initLayout(){
        final ListDataProvider<ObjectSynchronizationTypeDto> syncProvider = new ListDataProvider<>(this,
                new PropertyModel<List<ObjectSynchronizationTypeDto>>(model, ResourceSynchronizationDto.F_OBJECT_SYNC_LIST));

        //first row - object sync list
        WebMarkupContainer tableBody = new WebMarkupContainer(ID_TABLE_ROWS);
        tableBody.setOutputMarkupId(true);
        add(tableBody);

        //second row - ObjectSynchronizationType editor
        WebMarkupContainer objectSyncEditor = new WebMarkupContainer(ID_OBJECT_SYNC_EDITOR);
        objectSyncEditor.setOutputMarkupId(true);
        objectSyncEditor.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isEnabled(){
                return isAnySelected();
            }

        });
        add(objectSyncEditor);

        //third row - container for more specific editors
        WebMarkupContainer thirdRowContainer = new WebMarkupContainer(ID_THIRD_ROW_CONTAINER);
        thirdRowContainer.setOutputMarkupId(true);
        add(thirdRowContainer);

        DataView<ObjectSynchronizationTypeDto> syncDataView = new DataView<ObjectSynchronizationTypeDto>(ID_OBJECT_SYNC_ROW,
                syncProvider, UserProfileStorage.DEFAULT_PAGING_SIZE) {

            @Override
            protected void populateItem(Item<ObjectSynchronizationTypeDto> item) {
                final ObjectSynchronizationTypeDto syncObject = item.getModelObject();

                AjaxSubmitLink link = new AjaxSubmitLink(ID_OBJECT_SYNC_LINK) {

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        editSyncObjectPerformed(target, syncObject);
                    }
                };
                item.add(link);

                Label label = new Label(ID_OBJECT_SYNC_LABEL, createObjectSyncTypeDisplayModel(syncObject));
                label.setOutputMarkupId(true);
                link.add(label);

                AjaxLink delete = new AjaxLink(ID_OBJECT_SYNC_DELETE){

                    @Override
                    public void onClick(AjaxRequestTarget target){
                        deleteSyncObjectPerformed(target, syncObject);
                    }
                };
                link.add(delete);

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if(syncObject.isSelected()){
                            return "success";
                        }

                        return null;
                    }
                }));

            }
        };
        tableBody.add(syncDataView);

        NavigatorPanel navigator = new NavigatorPanel(ID_PAGING, syncDataView, true);
        navigator.setOutputMarkupId(true);
        navigator.setOutputMarkupPlaceholderTag(true);
        add(navigator);

        AjaxLink add = new AjaxLink(ID_OBJECT_SYNC_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addSyncObjectPerformed(target);
            }
        };
        add(add);

        initObjectSyncEditor(objectSyncEditor);
    }

    private void initObjectSyncEditor(WebMarkupContainer editor){
        Label editorLabel = new Label(ID_EDITOR_LABEL, new LoadableModel<String>() {

            @Override
            protected String load() {
                if(isAnySelected()){
                    String name = model.getObject().getSelected().getName() != null ? model.getObject().getSelected().getName() : "-";
                    return getString("SynchronizationStep.label.editSyncObject", name);
                } else {
                    return getString("SynchronizationStep.label.emptyDisplayName");
                }
            }
        });
        editor.add(editorLabel);

        TextField editorName = new TextField<>(ID_EDITOR_NAME, new PropertyModel<String>(model,
                ResourceSynchronizationDto.F_SELECTED + ".name"));
        editor.add(editorName);

        TextArea editorDescription = new TextArea<>(ID_EDITOR_DESCRIPTION, new PropertyModel<String>(model,
                ResourceSynchronizationDto.F_SELECTED + ".description"));
        editor.add(editorDescription);

        DropDownChoice editorKind = new DropDownChoice<>(ID_EDITOR_KIND,
                new PropertyModel<ShadowKindType>(model, ResourceSynchronizationDto.F_SELECTED + ".kind"),
                WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                new EnumChoiceRenderer<ShadowKindType>(this));
        editorKind.setNullValid(true);
        editor.add(editorKind);

        TextField editorIntent = new TextField<>(ID_EDITOR_INTENT, new PropertyModel<String>(model,
                ResourceSynchronizationDto.F_SELECTED + ".intent"));
        editor.add(editorIntent);

        MultiValueAutoCompleteTextPanel<QName> editorObjectClass = new MultiValueAutoCompleteTextPanel<QName>(ID_EDITOR_OBJECT_CLASS,
                new PropertyModel<List<QName>>(model, ResourceSynchronizationDto.F_SELECTED + ".objectClass"), true, false){

            @Override
            protected IModel<String> createTextModel(final IModel<QName> model) {
                return new Model<String>(){

                    @Override
                    public String getObject(){
                        if(model.getObject() != null){
                            return model.getObject().getLocalPart();
                        }

                        return " - ";
                    }
                };
            }

            @Override
            protected QName createNewEmptyItem() {
                return new QName("");
            }

            @Override
            protected boolean buttonsDisabled() {
                return !isAnySelected();
            }

            @Override
            protected List<QName> createObjectList() {
                return model.getObject().getObjectClassList();
            }

            @Override
            protected String createAutoCompleteObjectLabel(QName object) {
                return object.getLocalPart();
            }

            @Override
            protected IValidator<String> createAutoCompleteValidator(){
                return createObjectClassValidator(new LoadableModel<List<QName>>(false) {

                    @Override
                    protected List<QName> load() {
                        return model.getObject().getObjectClassList();
                    }
                });
            }
        };
        editor.add(editorObjectClass);

        DropDownChoice editorFocus = new DropDownChoice<>(ID_EDITOR_FOCUS, new PropertyModel<QName>(model,
                ResourceSynchronizationDto.F_SELECTED + ".focusType"),
                new AbstractReadOnlyModel<List<QName>>() {

                    @Override
                    public List<QName> getObject() {
                        return createFocusTypeList();
                    }
                }, new IChoiceRenderer<QName>() {

            @Override
            public Object getDisplayValue(QName object) {
                return object.getLocalPart();
            }

            @Override
            public String getIdValue(QName object, int index) {
                return Integer.toString(index);
            }
        });
        editorFocus.setNullValid(true);
        editor.add(editorFocus);

        CheckBox editorEnabled = new CheckBox(ID_EDITOR_ENABLED, new PropertyModel<Boolean>(model,
                ResourceSynchronizationDto.F_SELECTED + ".enabled"));
        editor.add(editorEnabled);

        AjaxSubmitLink editorCondition = new AjaxSubmitLink(ID_EDITOR_BUTTON_CONDITION){

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                conditionEditPerformed(target);
            }
        };
        addDisableClassModifier(editorCondition);
        editor.add(editorCondition);

        AjaxSubmitLink editorConfirmation = new AjaxSubmitLink(ID_EDITOR_BUTTON_CONFIRMATION){

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                confirmationEditPerformed(target);
            }
        };
        addDisableClassModifier(editorConfirmation);
        editor.add(editorConfirmation);

        DropDownChoice editorObjectTemplate = new DropDownChoice<>(ID_EDITOR_OBJECT_TEMPLATE,
                new PropertyModel<ObjectReferenceType>(model, ResourceSynchronizationDto.F_SELECTED + ".objectTemplateRef"),
                new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                    @Override
                    public List<ObjectReferenceType> getObject() {
                        return createObjectTemplateList();
                    }
                }, new IChoiceRenderer<ObjectReferenceType>() {

            @Override
            public Object getDisplayValue(ObjectReferenceType object) {
                return model.getObject().getObjectTemplateMap().get(object.getOid());
            }

            @Override
            public String getIdValue(ObjectReferenceType object, int index) {
                return Integer.toString(index);
            }
        });
        editorObjectTemplate.setNullValid(true);
        editor.add(editorObjectTemplate);

        CheckBox editorReconcile = new CheckBox(ID_EDITOR_RECONCILE, new PropertyModel<Boolean>(model,
                ResourceSynchronizationDto.F_SELECTED + ".reconcile"));
        editor.add(editorReconcile);

        CheckBox editorOpportunistic = new CheckBox(ID_EDITOR_OPPORTUNISTIC, new PropertyModel<Boolean>(model,
                ResourceSynchronizationDto.F_SELECTED + ".opportunistic"));
        editor.add(editorOpportunistic);

        MultiValueTextEditPanel editorCorrelation = new MultiValueTextEditPanel<ConditionalSearchFilterType>(ID_EDITOR_EDITOR_CORRELATION,
                new PropertyModel<List<ConditionalSearchFilterType>>(model, ObjectSynchronizationTypeDto.F_SELECTED + ".correlation"), false, false){

            @Override
            protected IModel<String> createTextModel(final IModel<ConditionalSearchFilterType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();

                        ConditionalSearchFilterType searchFilter = model.getObject();
                        if(searchFilter != null && searchFilter.getDescription() != null){
                            sb.append(searchFilter.getDescription());
                        }

                        if(sb.toString().isEmpty()){
                            sb.append(getString("SynchronizationStep.label.notSpecified"));
                        }

                        return sb.toString();
                    }
                };
            }

            @Override
            protected ConditionalSearchFilterType createNewEmptyItem(){
                return new ConditionalSearchFilterType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, ConditionalSearchFilterType object){
                correlationEditPerformed(target, object);
            }

            @Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }
        };
        editor.add(editorCorrelation);

        MultiValueTextEditPanel editorReaction = new MultiValueTextEditPanel<SynchronizationReactionType>(ID_EDITOR_REACTION,
                new PropertyModel<List<SynchronizationReactionType>>(model, ObjectSynchronizationTypeDto.F_SELECTED + ".reaction"), false, false){

            @Override
            protected IModel<String> createTextModel(final IModel<SynchronizationReactionType> model) {
                return new Model<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();

                        if(model.getObject() != null){
                            SynchronizationReactionType reaction = model.getObject();

                            sb.append(reaction.getName() != null ? reaction.getName() : "- ");

                            if(reaction.getSituation() != null){
                                sb.append(" (");
                                sb.append(reaction.getSituation());
                                sb.append(")");
                            }
                        }

                        return sb.toString();
                    }
                };
            }

            @Override
            protected SynchronizationReactionType createNewEmptyItem(){
                return new SynchronizationReactionType();
            }

            @Override
            protected void editPerformed(AjaxRequestTarget target, SynchronizationReactionType object){
                reactionEditPerformed(target, object);
            }

            @Override
            protected boolean buttonsDisabled(){
                return !isAnySelected();
            }
        };
        editor.add(editorReaction);

        Label kindTooltip = new Label(ID_T_KIND);
        kindTooltip.add(new InfoTooltipBehavior());
        editor.add(kindTooltip);

        Label intentTooltip = new Label(ID_T_INTENT);
        intentTooltip.add(new InfoTooltipBehavior());
        editor.add(intentTooltip);

        Label objClassTooltip = new Label(ID_T_OBJ_CLASS);
        objClassTooltip.add(new InfoTooltipBehavior());
        editor.add(objClassTooltip);

        Label focusTooltip = new Label(ID_T_FOCUS);
        focusTooltip.add(new InfoTooltipBehavior());
        editor.add(focusTooltip);

        Label enabledTooltip = new Label(ID_T_ENABLED);
        enabledTooltip.add(new InfoTooltipBehavior());
        editor.add(enabledTooltip);

        Label conditionTooltip = new Label(ID_T_CONDITION);
        conditionTooltip.add(new InfoTooltipBehavior());
        editor.add(conditionTooltip);

        Label confirmationTooltip = new Label(ID_T_CONFIRMATION);
        confirmationTooltip.add(new InfoTooltipBehavior());
        editor.add(confirmationTooltip);

        Label objTemplateTooltip = new Label(ID_T_OBJ_TEMPLATE);
        objTemplateTooltip.add(new InfoTooltipBehavior());
        editor.add(objTemplateTooltip);

        Label reconcileTooltip = new Label(ID_T_RECONCILE);
        reconcileTooltip.add(new InfoTooltipBehavior());
        editor.add(reconcileTooltip);

        Label opportunisticTooltip = new Label(ID_T_OPPORTUNISTIC);
        opportunisticTooltip.add(new InfoTooltipBehavior());
        editor.add(opportunisticTooltip);

        Label correlationTooltip = new Label(ID_T_CORRELATION);
        correlationTooltip.add(new InfoTooltipBehavior());
        editor.add(correlationTooltip);

        Label reactionTooltip = new Label(ID_T_REACTION);
        reactionTooltip.add(new InfoTooltipBehavior());
        editor.add(reactionTooltip);
    }

    private IModel<String> createObjectSyncTypeDisplayModel(final ObjectSynchronizationTypeDto syncObject){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(syncObject != null && syncObject.getSyncType() != null){
                    ObjectSynchronizationType object = syncObject.getSyncType();
                    sb.append(object.getName() != null ? object.getName() : "(name not specified) ");

                    if(object.getKind() != null || object.getIntent() != null){
                        sb.append("(");
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

    private List<ObjectReferenceType> createObjectTemplateList(){
        model.getObject().getObjectTemplateMap().clear();
        OperationResult result = new OperationResult(OPERATION_LOAD_OBJECT_TEMPLATE_LIST);
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_OBJECT_TEMPLATE_LIST);
        List<PrismObject<ObjectTemplateType>> templates = null;
        List<ObjectReferenceType> references = new ArrayList<>();

        try {
            templates = getPageBase().getModelService().searchObjects(ObjectTemplateType.class, new ObjectQuery(), null, task, result);
            result.recomputeStatus();

        } catch (Exception e){
            result.recordFatalError(getString("SynchronizationStep.message.errorLoadingObjectTemplates"), e);
            LoggingUtils.logException(LOGGER, "Couldn't load object templates from repository.", e);
        }

        // TODO - show error somehow
        // if(!result.isSuccess()){
        //    getPageBase().showResult(result);
        // }

        if(templates != null){
            ObjectReferenceType ref;

            for(PrismObject<ObjectTemplateType> template: templates){
                if(model.getObject() != null){
                    model.getObject().getObjectTemplateMap().put(template.getOid(), WebMiscUtil.getName(template));
                }

                ref = new ObjectReferenceType();
                ref.setType(ObjectTemplateType.COMPLEX_TYPE);
                ref.setOid(template.getOid());
                references.add(ref);
            }
        }

        return references;
    }

    //TODO - add more FocusType items if needed
    private List<QName> createFocusTypeList(){
        List<QName> focusTypeList = new ArrayList<>();

        focusTypeList.add(UserType.COMPLEX_TYPE);
        focusTypeList.add(OrgType.COMPLEX_TYPE);
        focusTypeList.add(RoleType.COMPLEX_TYPE);

        return focusTypeList;
    }

    private void addDisableClassModifier(Component component){
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

    private void resetSelected(){
        for(ObjectSynchronizationTypeDto dto: model.getObject().getObjectSyncList()){
            dto.setSelected(false);
        }
    }

    private Component getSyncObjectTable(){
        return get(ID_TABLE_ROWS);
    }

    private Component getNavigator(){
        return get(ID_PAGING);
    }

    private Component getSyncObjectEditor(){
        return get(ID_OBJECT_SYNC_EDITOR);
    }

    private Component getThirdRowContainer(){
        return get(ID_THIRD_ROW_CONTAINER);
    }

    private void insertEmptyThirdRow(){
        getThirdRowContainer().replaceWith(new WebMarkupContainer(ID_THIRD_ROW_CONTAINER));
    }

    private void conditionEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new SynchronizationExpressionEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ExpressionType>(model, ResourceSynchronizationDto.F_SELECTED + ".condition")){

            @Override
            public String getLabel(){
                return "SynchronizationExpressionEditor.label.condition";
            }
        };
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void confirmationEditPerformed(AjaxRequestTarget target){
        WebMarkupContainer newContainer = new SynchronizationExpressionEditor(ID_THIRD_ROW_CONTAINER,
                new PropertyModel<ExpressionType>(model, ResourceSynchronizationDto.F_SELECTED + ".confirmation")){

            @Override
            public String getLabel(){
                return "SynchronizationExpressionEditor.label.confirmation";
            }
        };
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void correlationEditPerformed(AjaxRequestTarget target, ConditionalSearchFilterType condition){
        WebMarkupContainer newContainer = new ConditionalSearchFilterEditor(ID_THIRD_ROW_CONTAINER,
                new Model<>(condition));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    private void reactionEditPerformed(AjaxRequestTarget target, SynchronizationReactionType reaction){
        WebMarkupContainer newContainer = new SynchronizationReactionEditor(ID_THIRD_ROW_CONTAINER,
                new Model<>(reaction));
        getThirdRowContainer().replaceWith(newContainer);

        target.add(getThirdRowContainer(), get(ID_OBJECT_SYNC_EDITOR), getPageBase().getFeedbackPanel());
    }

    @Override
    public void applyState(){
        savePerformed();
    }

    private void savePerformed(){
        PrismObject<ResourceType> oldResource;
        PrismObject<ResourceType> newResource = resourceModel.getObject();
        OperationResult result = new OperationResult(OPERATION_SAVE_SYNC);
        ModelService modelService = getPageBase().getModelService();
        ObjectDelta delta;

        try{
            oldResource = WebModelUtils.loadObject(ResourceType.class, newResource.getOid(), result, getPageBase());
            if(oldResource != null){
                delta = oldResource.diff(newResource);

                if(LOGGER.isTraceEnabled()){
                    LOGGER.trace(delta.debugDump());
                }

                Collection<ObjectDelta<? extends ObjectType>> deltas = WebMiscUtil.createDeltaCollection(delta);
                modelService.executeChanges(deltas, null, getPageBase().createSimpleTask(OPERATION_SAVE_SYNC), result);
            }

        } catch (Exception e){
            LoggingUtils.logException(LOGGER, "Couldn't save resource synchronization.", e);
            result.recordFatalError(getString("SynchronizationStep.message.cantSave", e));
        } finally {
            result.computeStatusIfUnknown();
            setResult(result);
        }

        if(WebMiscUtil.showResultInPage(result)){
            getPageBase().showResult(result);
        }
    }

    private void editSyncObjectPerformed(AjaxRequestTarget target, ObjectSynchronizationTypeDto syncObject){
        resetSelected();
        syncObject.setSelected(true);
        model.getObject().setSelected(syncObject.getSyncType());

        insertEmptyThirdRow();
        target.add(getSyncObjectTable(), getNavigator(), getSyncObjectEditor(), getThirdRowContainer());
    }

    private void deleteSyncObjectPerformed(AjaxRequestTarget target, ObjectSynchronizationTypeDto syncObject){
        ArrayList<ObjectSynchronizationTypeDto> list = (ArrayList<ObjectSynchronizationTypeDto>)model.getObject().getObjectSyncList();

        list.remove(syncObject);

        if(syncObject.isSelected()){
            model.getObject().setSelected(createPlaceholderObjectType());
            insertEmptyThirdRow();
            target.add(getThirdRowContainer());
        }

        if(list.isEmpty()){
            ObjectSynchronizationType newObj = new ObjectSynchronizationType();
            newObj.setName(getString("SynchronizationStep.label.newObjectType"));
            ObjectSynchronizationTypeDto dto = new ObjectSynchronizationTypeDto(newObj);
            dto.setSelected(true);
            list.add(dto);
        }

        target.add(getSyncObjectEditor(), getSyncObjectTable(), getNavigator());
    }

    private void addSyncObjectPerformed(AjaxRequestTarget target){
        ObjectSynchronizationType syncObject = new ObjectSynchronizationType();
        syncObject.getCorrelation().add(new ConditionalSearchFilterType());
        syncObject.getReaction().add(new SynchronizationReactionType());
        syncObject.getObjectClass().add(new QName(""));
        syncObject.setName(getString("SynchronizationStep.label.newObjectType"));

        ObjectSynchronizationTypeDto dto = new ObjectSynchronizationTypeDto(syncObject);

        resetSelected();
        dto.setSelected(true);
        model.getObject().setSelected(dto.getSyncType());
        model.getObject().getObjectSyncList().add(dto);
        insertEmptyThirdRow();
        target.add(getSyncObjectTable(), getNavigator(), getSyncObjectEditor(), getThirdRowContainer());
    }
}
