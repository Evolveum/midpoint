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
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.dto.AttributeDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDataProvider;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDetailsDto;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class SchemaListPanel extends BasePanel<PrismObject<ResourceType>> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaListPanel.class);

    private static final String ID_TABLE_BODY = "tableBody";
    private static final String ID_OBJECT_CLASS = "objectClass";
    private static final String ID_OBJECT_CLASS_LIST = "objectClassList";
    private static final String ID_CLASS_LINK = "classLink";
    private static final String ID_LABEL = "label";
    private static final String ID_CLEAR_SEARCH = "clearSearch";
    private static final String ID_ATTRIBUTE_TABLE = "attributeTable";
    private static final String ID_NAVIGATOR = "objectClassNavigator";
	private static final String ID_OBJECT_CLASS_INFO_CONTAINER = "objectClassInfoContainer";
	private static final String ID_OBJECT_CLASS_INFO_COLUMN = "objectClassInfoColumn";
    private static final String ID_DETAILS_PANEL = "detailsPanel";
    private static final String ID_DETAILS_DISPLAY_NAME = "displayName";
    private static final String ID_DETAILS_DESCRIPTION = "description";
    private static final String ID_DETAILS_KIND = "kind";
    private static final String ID_DETAILS_INTENT = "intent";
    private static final String ID_DETAILS_NATIVE_OBJECT_CLASS = "nativeObjectClass";
    private static final String ID_DETAILS_DEFAULT = "isDefault";
    private static final String ID_T_KIND = "kindTooltip";
    private static final String ID_T_INTENT = "intentTooltip";
    private static final String ID_T_NATIVE_OBJECT_CLASS = "nativeObjectClassTooltip";
    private static final String ID_T_DEFAULT = "isDefaultTooltip";

    @NotNull private final NonEmptyLoadableModel<List<ObjectClassDto>> allClasses;
	@NotNull private final NonEmptyLoadableModel<ObjectClassDetailsDto> detailsModel;
	@NotNull private final NonEmptyLoadableModel<List<AttributeDto>> attributeModel;

    public SchemaListPanel(String id, IModel<PrismObject<ResourceType>> model, PageResourceWizard parentPage) {
        super(id, model);

		allClasses = new NonEmptyLoadableModel<List<ObjectClassDto>>(false) {
			@Override @NotNull
			protected List<ObjectClassDto> load() {
				return loadAllClasses();
			}
		};
		parentPage.registerDependentModel(allClasses);

		attributeModel = new NonEmptyLoadableModel<List<AttributeDto>>(false) {
			@Override @NotNull
			protected List<AttributeDto> load() {
				return loadAttributes();
			}
		};
		parentPage.registerDependentModel(attributeModel);

		detailsModel = new NonEmptyLoadableModel<ObjectClassDetailsDto>(true) {
			@Override @NotNull
			protected ObjectClassDetailsDto load() {
				return loadDetails();
			}
		};
		parentPage.registerDependentModel(detailsModel);

		initLayout();
    }

    protected void initLayout() {

        final ObjectClassDataProvider dataProvider = new ObjectClassDataProvider(allClasses);

        TextField objectClass = new TextField<>(ID_OBJECT_CLASS, new Model<>());
        objectClass.setOutputMarkupId(true);
        objectClass.add(new AjaxFormComponentUpdatingBehavior("keyup") {

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
                        objectClassClickPerformed(target, item.getModelObject());
                    }
                };
                item.add(link);

                Label label = new Label(ID_LABEL, new PropertyModel<>(item.getModel(), ObjectClassDto.F_DISPLAY_NAME));
                link.add(label);

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<Object>() {
                    @Override
                    public Object getObject() {
                        return item.getModelObject().isSelected() ? "success" : null;
                    }
                }));
            }
        };
        tableBody.add(objectClassDataView);

		NavigatorPanel objectClassNavigator = new NavigatorPanel(ID_NAVIGATOR, objectClassDataView, true);
		objectClassNavigator.setOutputMarkupId(true);
		objectClassNavigator.setOutputMarkupPlaceholderTag(true);
		add(objectClassNavigator);

		WebMarkupContainer objectClassInfoContainer = new WebMarkupContainer(ID_OBJECT_CLASS_INFO_CONTAINER);
		objectClassInfoContainer.setOutputMarkupId(true);
		add(objectClassInfoContainer);

		WebMarkupContainer objectClassInfoColumn = new WebMarkupContainer(ID_OBJECT_CLASS_INFO_COLUMN);
		objectClassInfoColumn.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getSelectedObjectClass() != null;
			}
		});
		objectClassInfoContainer.add(objectClassInfoColumn);

		initDetailsPanel(objectClassInfoColumn);

		ListDataProvider<AttributeDto> attributeProvider = new ListDataProvider<>(this, attributeModel, true);
		attributeProvider.setSort(AttributeDto.F_DISPLAY_ORDER, SortOrder.ASCENDING);
        BoxedTablePanel<AttributeDto> attributeTable = new BoxedTablePanel<>(ID_ATTRIBUTE_TABLE, attributeProvider, initColumns());
        attributeTable.setOutputMarkupId(true);
        attributeTable.setItemsPerPage(UserProfileStorage.DEFAULT_PAGING_SIZE);
        attributeTable.setShowPaging(true);
        objectClassInfoColumn.add(attributeTable);
    }

    private void initDetailsPanel(WebMarkupContainer parent) {
        WebMarkupContainer detailsContainer = new WebMarkupContainer(ID_DETAILS_PANEL);
        detailsContainer.setOutputMarkupId(true);
        detailsContainer.setOutputMarkupPlaceholderTag(true);
        parent.add(detailsContainer);

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

        CheckBox isDefault = new CheckBox(ID_DETAILS_DEFAULT, new PropertyModel<>(detailsModel, ObjectClassDetailsDto.F_IS_DEFAULT));
        isDefault.setEnabled(false);
        detailsContainer.add(isDefault);

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
    }

    private List<IColumn<AttributeDto, String>> initColumns() {
        List<IColumn<AttributeDto, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.name"), AttributeDto.F_NAME, AttributeDto.F_NAME));
        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.displayName"), AttributeDto.F_DISPLAY_NAME));
        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.nativeAttributeName"), AttributeDto.F_NATIVE_ATTRIBUTE_NAME, AttributeDto.F_NATIVE_ATTRIBUTE_NAME));
        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.minMax"), AttributeDto.F_MIN_MAX_OCCURS));
        columns.add(new PropertyColumn<>(createStringResource("SchemaListPanel.displayOrder"), AttributeDto.F_DISPLAY_ORDER, AttributeDto.F_DISPLAY_ORDER));

        CheckBoxColumn<AttributeDto> check = new CheckBoxColumn<>(createStringResource("SchemaListPanel.returnedByDefault"), AttributeDto.F_RETURNED_BY_DEFAULT);
        check.setEnabled(false);
        columns.add(check);

        return columns;
    }

	@SuppressWarnings("unchecked")
    private TextField<String> getObjectClassText() {
        return (TextField<String>) get(ID_OBJECT_CLASS);
    }

    private void updateSearchPerformed(AjaxRequestTarget target, ObjectClassDataProvider dataProvider) {
        dataProvider.setFilter(getObjectClassText().getModelObject());
        target.add(get(ID_TABLE_BODY), get(ID_NAVIGATOR), get(ID_OBJECT_CLASS_INFO_CONTAINER));
    }

    private void clearSearchPerformed(AjaxRequestTarget target, ObjectClassDataProvider dataProvider) {
        getObjectClassText().setModelObject(null);
        target.add(getObjectClassText());

        updateSearchPerformed(target, dataProvider);
    }

    private void objectClassClickPerformed(AjaxRequestTarget target, ObjectClassDto dto) {
        for (ObjectClassDto o : allClasses.getObject()) {
			o.setSelected(false);
        }
		dto.setSelected(true);
        attributeModel.reset();
        detailsModel.reset();
        target.add(get(ID_TABLE_BODY), get(ID_OBJECT_CLASS_INFO_CONTAINER));
    }

    private List<AttributeDto> loadAttributes() {
        List<AttributeDto> list = new ArrayList<>();

		ObjectClassDto selected = getSelectedObjectClass();
        if (selected == null) {
            return list;
        }

        for (ResourceAttributeDefinition def : selected.getDefinition().getAttributeDefinitions()) {
            list.add(new AttributeDto(def));
        }
        return list;
    }

	@Nullable
	private ObjectClassDto getSelectedObjectClass() {
		for (ObjectClassDto o : allClasses.getObject()) {
			if (o.isSelected()) {
				return o;
			}
		}
		return null;
	}

	private ObjectClassDetailsDto loadDetails() {
        ObjectClassDto selected = getSelectedObjectClass();
        if (selected == null){
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

        try {
            Element xsdSchema = ResourceTypeUtil.getResourceXsdSchema(resource);
            if (xsdSchema == null) {
                return null;
            }

            return RefinedResourceSchemaImpl.getRefinedSchema(resource, getPageBase().getPrismContext());
        } catch (SchemaException|RuntimeException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse resource schema.", ex);
            getSession().error(getString("SchemaListPanel.message.couldntParseSchema") + " " + ex.getMessage());

            throw new RestartResponseException(PageResources.class);
        }
    }
}
