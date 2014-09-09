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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectSynchronizationTypeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ResourceSynchronizationDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 * @author shood
 */
public class SynchronizationStep extends WizardStep {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationStep.class);

    private static final String DOT_CLASS = SynchronizationStep.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT_CLASS_LIST = DOT_CLASS + "loadObjectClassList";
    private static final String OPERATION_SAVE_SYNC = DOT_CLASS + "saveResourceSynchronization";

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

    private static final Integer AUTO_COMPLETE_LIST_SIZE = 10;

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
        return new ObjectSynchronizationType();
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
                item.add(delete);

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
                    return model.getObject().getSelected().getName();
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
        editor.add(editorKind);

        TextField editorIntent = new TextField<>(ID_EDITOR_INTENT, new PropertyModel<String>(model,
                ResourceSynchronizationDto.F_SELECTED + ".intent"));
        editor.add(editorIntent);

        //TODO - Insert object class editor

        //TODO - this should be some auto-complete field
        TextField editorFocus = new TextField<>(ID_EDITOR_FOCUS, new PropertyModel<String>(model,
                ResourceSynchronizationDto.F_SELECTED + ".focusType"));
        editor.add(editorFocus);

        CheckBox editorEnabled = new CheckBox(ID_EDITOR_ENABLED, new PropertyModel<Boolean>(model,
                ResourceSynchronizationDto.F_SELECTED + ".enabled"));
        editor.add(editorEnabled);

        //TODO - add some DisabledClassModifier
        AjaxSubmitLink editorCondition = new AjaxSubmitLink(ID_EDITOR_BUTTON_CONDITION){

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                conditionEditPerformed(target);
            }
        };
        editor.add(editorCondition);

        //TODO - add some DisabledClassModifier
        AjaxSubmitLink editorConfirmation = new AjaxSubmitLink(ID_EDITOR_BUTTON_CONFIRMATION){

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                confirmationEditPerformed(target);
            }
        };
        editor.add(editorConfirmation);

        //TODO - add editor for selecting objectTemplate

        CheckBox editorReconcile = new CheckBox(ID_EDITOR_RECONCILE, new PropertyModel<Boolean>(model,
                ResourceSynchronizationDto.F_SELECTED + ".reconcile"));
        editor.add(editorReconcile);

        CheckBox editorOpportunistic = new CheckBox(ID_EDITOR_OPPORTUNISTIC, new PropertyModel<Boolean>(model,
                ResourceSynchronizationDto.F_SELECTED + ".opportunistic"));
        editor.add(editorOpportunistic);

        //TODO - add Correlation and Reaction editors
    }

    private IModel<String> createObjectSyncTypeDisplayModel(final ObjectSynchronizationTypeDto syncObject){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(syncObject != null && syncObject.getSyncType() != null){
                    ObjectSynchronizationType object = syncObject.getSyncType();
                    sb.append(object.getName());

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
        //TODO - implement me
    }

    private void confirmationEditPerformed(AjaxRequestTarget target){
        //TODO - implement me
    }

    @Override
    public void applyState(){
        savePerformed();
    }

    private void savePerformed(){
        //TODO - implement me
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
