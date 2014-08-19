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
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.CountToolbar;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.wizard.WizardStep;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ResourceObjectTypeDefinitionTypeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.SchemaHandlingDto;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author lazyman
 *  @author shood
 */
public class SchemaHandlingStep extends WizardStep {

    private static final String ID_ROWS = "tableRows";
    private static final String ID_ROW_OBJECT_TYPE = "objectTypeRow";
    private static final String ID_LINK_OBJECT_TYPE = "objectTypeLink";
    private static final String ID_NAME_OBJECT_TYPE = "objectTypeName";
    private static final String ID_BUTTON_DELETE_OBJECT_TYPE = "objectTypeDelete";
    private static final String ID_PAGING_OBJECT_TYPE = "objectTypePaging";
    private static final String ID_BUTTON_ADD_OBJECT_TYPE = "objectTypeAddButton";
    private static final String ID_OBJECT_TYPE_EDITOR = "objectTypeConfig";
    private static final String ID_THIRD_ROW_CONTAINER = "thirdRowContainer";

    private IModel<PrismObject<ResourceType>> resourceModel;
    private IModel<SchemaHandlingDto> model;

    public SchemaHandlingStep(final IModel<PrismObject<ResourceType>> resourceModel) {
        this.resourceModel = resourceModel;

        model = new LoadableModel<SchemaHandlingDto>() {

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
                    obj = new ResourceObjectTypeDefinitionTypeDto(objectType);
                    list.add(obj);
                }
            }
        }

        dto.setObjectTypeList(list);
        return dto;
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
        //TODO - implement
    }

    private void initModals(){
        //TODO - init all modal windows here
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

    @Override
    public void applyState() {
        // TODO - implement
    }

    private void editObjectTypePerformed(AjaxRequestTarget target, ResourceObjectTypeDefinitionTypeDto objectType){
        //TODO - implement
    }

    private void deleteObjectTypePerformed(AjaxRequestTarget target, ResourceObjectTypeDefinitionTypeDto objectType){
        resetSelected();
        ArrayList<ResourceObjectTypeDefinitionTypeDto> list = (ArrayList<ResourceObjectTypeDefinitionTypeDto>) model.getObject().getObjectTypeList();

        list.remove(objectType);

        if(list.isEmpty()){
            ResourceObjectTypeDefinitionType newObj = new ResourceObjectTypeDefinitionType();
            newObj.setDisplayName(getString("SchemaHandlingStep.label.newObjectType"));
            ResourceObjectTypeDefinitionTypeDto dto = new ResourceObjectTypeDefinitionTypeDto(newObj);
            dto.setSelected(true);
            list.add(dto);
        }

        target.add(getObjectTypeEditor().replaceWith(new WebMarkupContainer(ID_OBJECT_TYPE_EDITOR)),
                getObjectListTable(), getNavigator());

    }

    private void addObjectTypePerformed(AjaxRequestTarget target){
        ResourceObjectTypeDefinitionType objectType = new ResourceObjectTypeDefinitionType();
        objectType.setDisplayName(getString("SchemaHandlingStep.label.newObjectType"));
        ResourceObjectTypeDefinitionTypeDto dto = new ResourceObjectTypeDefinitionTypeDto(objectType);

        resetSelected();
        dto.setSelected(true);
        model.getObject().getObjectTypeList().add(dto);
        target.add(getObjectListTable(), getNavigator());
    }
}
