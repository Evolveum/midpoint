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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.wizard.resource.dto.AttributeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDataProvider;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDetailsDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

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
import org.apache.wicket.markup.html.form.CheckBox;
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
    private static final String ID_OBJECT_CLASS_LIST = "objectClassList";
    private static final String ID_CLASS_LINK = "classLink";
    private static final String ID_LABEL = "label";
    private static final String ID_CLEAR_SEARCH = "clearSearch";
    private static final String ID_ATTRIBUTE_TABLE = "attributeTable";
    private static final String ID_NAVIGATOR = "objectClassNavigator";
    private static final String ID_DETAILS_PANEL = "detailsPanel";
    private static final String ID_DETAILS_DISPLAY_NAME = "displayName";
    private static final String ID_DETAILS_DESCRIPTION = "description";
    private static final String ID_DETAILS_KIND = "kind";
    private static final String ID_DETAILS_INTENT = "intent";
    private static final String ID_DETAILS_NATIVE_OBJECT_CLASS = "nativeObjectClass";
    private static final String ID_DETAILS_DEFAULT = "isDefault";
    private static final String ID_DETAILS_KIND_DEFAULT = "isKindDefault";
    private static final String ID_T_KIND = "kindTooltip";
    private static final String ID_T_INTENT = "intentTooltip";
    private static final String ID_T_NATIVE_OBJECT_CLASS = "nativeObjectClassTooltip";
    private static final String ID_T_DEFAULT = "isDefaultTooltip";
    private static final String ID_T_KIND_DEFAULT = "isKindDefaultTooltip";

    private IModel<List<ObjectClassDto>> allClasses;
    private LoadableModel<ObjectClassDetailsDto> detailsModel;
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

        detailsModel = new LoadableModel<ObjectClassDetailsDto>() {

            @Override
            protected ObjectClassDetailsDto load() {
                return loadDetails();
            }
        };
    }

    @Override
    protected void initLayout() {
        initModels();

        final ObjectClassDataProvider dataProvider = new ObjectClassDataProvider(allClasses);

        TextField objectClass = new TextField<>(ID_OBJECT_CLASS, new Model<>());
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

        DataView<ObjectClassDto> objectClassDataView = new DataView<ObjectClassDto>(ID_OBJECT_CLASS_LIST, dataProvider,
                UserProfileStorage.DEFAULT_PAGING_SIZE) {

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
        tableBody.add(objectClassDataView);

        initDetailsPanel();

        ISortableDataProvider attributeProvider = new ListDataProvider<>(this, attributeModel);
        TablePanel attributeTable = new TablePanel(ID_ATTRIBUTE_TABLE, attributeProvider, initColumns());
        attributeTable.setOutputMarkupId(true);
        attributeTable.setItemsPerPage(UserProfileStorage.DEFAULT_PAGING_SIZE);
        attributeTable.setShowPaging(true);
        add(attributeTable);

        NavigatorPanel objectClassNavigator = new NavigatorPanel(ID_NAVIGATOR, objectClassDataView, true);
        objectClassNavigator.setOutputMarkupId(true);
        objectClassNavigator.setOutputMarkupPlaceholderTag(true);
        add(objectClassNavigator);
    }

    private void initDetailsPanel(){
        WebMarkupContainer detailsContainer = new WebMarkupContainer(ID_DETAILS_PANEL);
        detailsContainer.setOutputMarkupId(true);
        detailsContainer.setOutputMarkupPlaceholderTag(true);
        add(detailsContainer);

        Label displayName = new Label(ID_DETAILS_DISPLAY_NAME, new PropertyModel<String>(detailsModel, ObjectClassDetailsDto.F_DISPLAY_NAME));
        detailsContainer.add(displayName);

        Label description = new Label(ID_DETAILS_DESCRIPTION, new PropertyModel<String>(detailsModel, ObjectClassDetailsDto.F_DESCRIPTION));
        detailsContainer.add(description);

        Label kind = new Label(ID_DETAILS_KIND, new PropertyModel<String>(detailsModel, ObjectClassDetailsDto.F_KIND));
        detailsContainer.add(kind);

        Label intent = new Label(ID_DETAILS_INTENT, new PropertyModel<String>(detailsModel, ObjectClassDetailsDto.F_INTENT));
        detailsContainer.add(intent);

        Label nativeObjectClass = new Label(ID_DETAILS_NATIVE_OBJECT_CLASS, new PropertyModel<String>(detailsModel, ObjectClassDetailsDto.F_NATIVE_OBJECT_CLASS));
        detailsContainer.add(nativeObjectClass);

        CheckBox isDefault = new CheckBox(ID_DETAILS_DEFAULT, new PropertyModel<Boolean>(detailsModel, ObjectClassDetailsDto.F_IS_DEFAULT));
        isDefault.setEnabled(false);
        detailsContainer.add(isDefault);

        CheckBox idKindDefault = new CheckBox(ID_DETAILS_KIND_DEFAULT, new PropertyModel<Boolean>(detailsModel, ObjectClassDetailsDto.F_IS_KIND_DEFAULT));
        idKindDefault.setEnabled(false);
        detailsContainer.add(idKindDefault);

        Label kindTooltip = new Label(ID_T_KIND);
        kindTooltip.add(new InfoTooltipBehavior());
        detailsContainer.add(kindTooltip);

        Label intentTooltip = new Label(ID_T_INTENT);
        intentTooltip.add(new InfoTooltipBehavior());
        detailsContainer.add(intentTooltip);

        Label nativeObjClassTooltip = new Label(ID_T_NATIVE_OBJECT_CLASS);
        nativeObjClassTooltip.add(new InfoTooltipBehavior());
        detailsContainer.add(nativeObjClassTooltip);

        Label defaultTooltip = new Label(ID_T_DEFAULT);
        defaultTooltip.add(new InfoTooltipBehavior());
        detailsContainer.add(defaultTooltip);

        Label kindDefaultTooltip = new Label(ID_T_KIND_DEFAULT);
        kindDefaultTooltip.add(new InfoTooltipBehavior());
        detailsContainer.add(kindDefaultTooltip);
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
        detailsModel.reset();
        target.add(get(ID_TABLE_BODY), get(ID_ATTRIBUTE_TABLE), get(ID_DETAILS_PANEL));
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

    private ObjectClassDetailsDto loadDetails(){
        List<ObjectClassDto> all = allClasses.getObject();
        ObjectClassDto selected = null;
        for(ObjectClassDto o: all){
            if(o.isSelected()){
                selected = o;
                break;
            }
        }

        if(selected == null){
            return new ObjectClassDetailsDto(null);
        }

        return new ObjectClassDetailsDto(selected.getDefinition());
    }

    private List<ObjectClassDto> loadAllClasses() {
        List<ObjectClassDto> list = new ArrayList<>();

        RefinedResourceSchema schema = loadResourceSchema();
        if (schema == null) {
            return list;
        }

        for(RefinedObjectClassDefinition definition: schema.getRefinedDefinitions()){
            list.add(new ObjectClassDto(definition));
        }

        Collections.sort(list);

        return list;
    }

    private RefinedResourceSchema loadResourceSchema() {
        PrismObject<ResourceType> resource = getModel().getObject();
        Element xsdSchema = ResourceTypeUtil.getResourceXsdSchema(resource);
        if (xsdSchema == null) {
            return null;
        }

        try {
            return RefinedResourceSchema.getRefinedSchema(getModel().getObject(), getPageBase().getPrismContext());
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't parse resource schema.", ex);
            getSession().error(getString("SchemaListPanel.message.couldntParseSchema") + " " + ex.getMessage());

            throw new RestartResponseException(PageResources.class);
        }
    }
}
