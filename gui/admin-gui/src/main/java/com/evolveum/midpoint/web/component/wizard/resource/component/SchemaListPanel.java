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
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.AttributeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDataProvider;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class SchemaListPanel extends SimplePanel<PrismObject<ResourceType>> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaListPanel.class);

    private static final String ID_TABLE_BODY = "tableBody";
    private static final String ID_OBJECT_CLASS = "objectClass";
    private static final String ID_PAGEABLE = "pageable";
    private static final String ID_CLASS_LINK = "classLink";
    private static final String ID_LABEL = "label";
    private static final String ID_CLEAR_SEARCH = "clearSearch";
    private static final String ID_ATTRIBUTE_TABLE = "attributeTable";

    private IModel<List<ObjectClassDto>> allClasses;
    private LoadableModel<List<AttributeDto>> attributeModel;

    public SchemaListPanel(String id, IModel<PrismObject<ResourceType>> model) {
        super(id, model);
    }

    protected void initModels() {
        allClasses = new LoadableModel<List<ObjectClassDto>>(false) {

            @Override
            protected List<ObjectClassDto> load() {
                return loadAllClasses();
            }
        };

        attributeModel = new LoadableModel<List<AttributeDto>>(false) {

            @Override
            protected List<AttributeDto> load() {
                return loadAttributes();
            }
        };
    }

    @Override
    protected void initLayout() {
        initModels();

        final ObjectClassDataProvider dataProvider = new ObjectClassDataProvider(allClasses);

        TextField objectClass = new TextField(ID_OBJECT_CLASS, new Model<>());
        objectClass.setOutputMarkupId(true);
        objectClass.add(new AjaxFormComponentUpdatingBehavior("keyUp") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateSearchPerformed(target, dataProvider);
            }
        });
        add(objectClass);

        AjaxButton clearSearch = new AjaxButton(ID_CLEAR_SEARCH) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                clearSearchPerformed(target, dataProvider);
            }
        };
        add(clearSearch);

        WebMarkupContainer tableBody = new WebMarkupContainer(ID_TABLE_BODY);
        tableBody.setOutputMarkupId(true);
        add(tableBody);

        DataView<ObjectClassDto> pageable = new DataView<ObjectClassDto>(ID_PAGEABLE, dataProvider) {

            @Override
            protected void populateItem(final Item<ObjectClassDto> item) {
                AjaxLink link = new AjaxLink(ID_CLASS_LINK) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        objectClassClickPerformed(target, item.getModelObject(), allClasses.getObject());
                    }
                };
                item.add(link);

                Label label = new Label(ID_LABEL, new PropertyModel<>(item.getModel(), ObjectClassDto.F_NAME));
                link.add(label);

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<Object>() {
                    @Override
                    public Object getObject() {
                        if (item.getModelObject().isSelected()) {
                            return "success";
                        }

                        return null;
                    }
                }));
            }
        };
        tableBody.add(pageable);

        ISortableDataProvider attributeProvider = new ListDataProvider(this, attributeModel);
        TablePanel attributeTable = new TablePanel(ID_ATTRIBUTE_TABLE, attributeProvider, initColumns());
        attributeTable.setOutputMarkupId(true);
        attributeTable.setItemsPerPage(20);
        add(attributeTable);
    }

    private List<IColumn> initColumns() {
        List<IColumn> columns = new ArrayList<>();

        columns.add(new PropertyColumn(createStringResource("SchemaListPanel.name"), AttributeDto.F_NAME));
        columns.add(new PropertyColumn(createStringResource("SchemaListPanel.displayName"), AttributeDto.F_DISPLAY_NAME));
        columns.add(new PropertyColumn(createStringResource("SchemaListPanel.nativeAttributeName"), AttributeDto.F_NATIVE_ATTRIBUTE_NAME));
        columns.add(new PropertyColumn(createStringResource("SchemaListPanel.minMax"), AttributeDto.F_MIN_MAX_OCCURS));
        columns.add(new PropertyColumn(createStringResource("SchemaListPanel.displayOrder"), AttributeDto.F_DISPLAY_ORDER));

        CheckBoxColumn check = new CheckBoxColumn(createStringResource("SchemaListPanel.returnedByDefault"), AttributeDto.F_RETURNED_BY_DEFAULT);
        check.setEnabled(false);
        columns.add(check);

        return columns;
    }

    private TextField<String> getObjectClassText() {
        return (TextField) get(ID_OBJECT_CLASS);
    }

    private void updateSearchPerformed(AjaxRequestTarget target, ObjectClassDataProvider dataProvider) {
        dataProvider.filterClasses(getObjectClassText().getModelObject());
        target.add(get(ID_TABLE_BODY));
    }

    private void clearSearchPerformed(AjaxRequestTarget target, ObjectClassDataProvider dataProvider) {
        getObjectClassText().setModelObject(null);
        target.add(getObjectClassText());

        updateSearchPerformed(target, dataProvider);
    }

    private void objectClassClickPerformed(AjaxRequestTarget target, ObjectClassDto dto, List<ObjectClassDto> all) {
        for (ObjectClassDto o : all) {
            o.setSelected(false);
        }
        dto.setSelected(true);

        attributeModel.reset();
        target.add(get(ID_TABLE_BODY), get(ID_ATTRIBUTE_TABLE));
    }

    private List<AttributeDto> loadAttributes() {
        List<AttributeDto> list = new ArrayList<>();

        List<ObjectClassDto> all = allClasses.getObject();
        ObjectClassDto selected = null;
        for (ObjectClassDto o : all) {
            if (o.isSelected()) {
                selected = o;
                break;
            }
        }

        if (selected == null) {
            return list;
        }

        for (ResourceAttributeDefinition def : selected.getDefinition().getAttributeDefinitions()) {
            list.add(new AttributeDto(def));
        }

        Collections.sort(list);

        return list;
    }

    private List<ObjectClassDto> loadAllClasses() {
        List<ObjectClassDto> list = new ArrayList<>();

        ResourceSchema schema = loadResourceSchema();
        if (schema == null) {
            return list;
        }

        for (ObjectClassComplexTypeDefinition def : schema.getObjectClassDefinitions()) {
            list.add(new ObjectClassDto(def));
        }

        Collections.sort(list);

        return list;
    }

    private ResourceSchema loadResourceSchema() {
        PrismObject<ResourceType> resource = getModel().getObject();
        Element xsdSchema = ResourceTypeUtil.getResourceXsdSchema(resource);
        if (xsdSchema == null) {
            return null;
        }

        try {
            return ResourceSchema.parse(xsdSchema, resource.toString(), getPageBase().getPrismContext());
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't parse resource schema.", ex);
            getSession().error(getString("SchemaListPanel.message.couldntParseSchema") + " " + ex.getMessage());

            throw new RestartResponseException(PageResources.class);
        }
    }
}
