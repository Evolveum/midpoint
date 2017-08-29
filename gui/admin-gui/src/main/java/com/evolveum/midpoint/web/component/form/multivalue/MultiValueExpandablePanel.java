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
package com.evolveum.midpoint.web.component.form.multivalue;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by honchar.
 */
public class MultiValueExpandablePanel <T extends Containerable> extends BasePanel<List<T>> {
    private static final String ID_REPEATER = "repeater";
    private static final String ID_BOX_TITLE = "boxTitle";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_BOX_BODY = "boxBody";
    private static final String ID_ATTRIBUTE_REPEATER = "attributeRepeater";
    private static final String ID_ATTRIBUTE_LABEL = "attributeLabel";
    private static final String ID_ATTRIBUTE_VALUE = "attributeValue";

    public MultiValueExpandablePanel(String id, IModel<List<T>> model){
        super(id, model);
        initLayout();
    }

    private void initLayout(){
        ListView repeater = new ListView<T>(ID_REPEATER, getModel()) {

            @Override
            protected void populateItem(final ListItem<T> listItem) {
                Label boxTitle = new Label(ID_BOX_TITLE, listItem.getModel().getObject().asPrismContainerValue().getPath().last());
                listItem.add(boxTitle);

                AjaxButton removeRowButton = new AjaxButton(ID_REMOVE_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                    }
                };
                listItem.add(removeRowButton);

                T itemModel = listItem.getModelObject();
                PrismContainerValue containerValue = itemModel.asPrismContainerValue();

                List<Item> items = containerValue.getItems();
                Map<ItemDefinition, Item> attributesDefinitionValueMap = new HashMap<>();
                for (ItemDefinition itemDefinition : containerValue.getComplexTypeDefinition().getDefinitions()){
                    attributesDefinitionValueMap.put(itemDefinition, containerValue.findItem(itemDefinition.getName()));
                }

                RepeatingView attributesPanel = new RepeatingView(ID_ATTRIBUTE_REPEATER);
                for (ItemDefinition itemDefinition : attributesDefinitionValueMap.keySet()){
                    WebMarkupContainer attributePanelContainer = new WebMarkupContainer(attributesPanel.newChildId());
                    attributesPanel.add(attributePanelContainer);

                    attributePanelContainer.add(new Label(ID_ATTRIBUTE_LABEL, Model.of(itemDefinition.getName().getLocalPart())));

                    Item item = attributesDefinitionValueMap.get(itemDefinition);
                    IModel itemValueModel = item != null ? Model.of(item.getValue(0)) : null;
                    attributePanelContainer.add(createTypedAttributePanel(ID_ATTRIBUTE_VALUE,
                            getItemType(itemDefinition), itemValueModel, "value"));
                    WebComponentUtil.addAjaxOnUpdateBehavior(attributePanelContainer);
                }
                listItem.add(attributesPanel);
            }
        };

        add(repeater);

    }

    private Class getItemType(ItemDefinition itemDefinition){
        Class type = WebComponentUtil.qnameToClass(itemDefinition.getPrismContext(), itemDefinition.getTypeName());
        if (type == null){
            type = itemDefinition.getPrismContext().getSchemaRegistry().determineClassForType(itemDefinition.getTypeName());
        }
        if (type == null){
            type = itemDefinition.getTypeClass();
        }
        return type;
    }

    private Component createTypedAttributePanel(String componentId, Class type, IModel model, String expression) {
        Panel panel;
        if (type == null){
            return new WebMarkupContainer(componentId);
        }
        if (type.isEnum()) {
            panel = WebComponentUtil.createEnumPanel(type, componentId, model != null ? new PropertyModel<>(model, expression) : new Model(), this);
        } else if (ObjectReferenceType.class.isAssignableFrom(type)) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setupReferenceValue((PrismReferenceValue) model.getObject());
            panel = new ChooseTypePanel<AbstractRoleType>(componentId, ort) {

                private static final long serialVersionUID = 1L;


                @Override
                protected void executeCustomRemoveAction(AjaxRequestTarget target) {

                }

                @Override
                protected ObjectQuery getChooseQuery() {
                    return new ObjectQuery();
                }


                @Override
                public Class<AbstractRoleType> getObjectTypeClass() {
                    return AbstractRoleType.class;
                }

            };
        } else {
            panel = new TextPanel<>(componentId, Model.of(""), type);
        }

        panel.setOutputMarkupId(true);
        return panel;
    }

}
