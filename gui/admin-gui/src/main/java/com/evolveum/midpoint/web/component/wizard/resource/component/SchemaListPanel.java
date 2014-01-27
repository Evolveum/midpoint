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

package com.evolveum.midpoint.web.component.wizard.resource.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SchemaListPanel extends SimplePanel<PrismObject<ResourceType>> {

    private static final String ID_OBJECT_CLASS = "objectClass";
    private static final String ID_PANEL = "panel";

    private LoadableModel<List<ObjectClassDto>> objectsModel;

    public SchemaListPanel(String id, IModel<PrismObject<ResourceType>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        objectsModel = new LoadableModel<List<ObjectClassDto>>(false) {

            @Override
            protected List<ObjectClassDto> load() {
                return createObjectClassList();
            }
        };

        ListView objectClass = new ListView<ObjectClassDto>(ID_OBJECT_CLASS, objectsModel) {

            @Override
            protected void populateItem(ListItem<ObjectClassDto> item) {
                ObjectClassPanel panel = new ObjectClassPanel(ID_PANEL, item.getModel());
                item.setRenderBodyOnly(true);

                item.add(panel);
            }
        };
        add(objectClass);
    }

    private List<ObjectClassDto> createObjectClassList() {
        List<ObjectClassDto> classes = new ArrayList<>();

        PrismObject<ResourceType> resource = getModel().getObject();
        Element xsdSchema = ResourceTypeUtil.getResourceXsdSchema(resource);
        if (xsdSchema == null) {
            return classes;
        }

        try {
            ResourceSchema schema = ResourceSchema.parse(xsdSchema, resource.toString(), getPageBase().getPrismContext());
            for (ObjectClassComplexTypeDefinition def : schema.getObjectClassDefinitions()) {
                classes.add(new ObjectClassDto(def));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //todo error handling
        }

        return classes;
    }
}
