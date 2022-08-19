/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardStepPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class SelectObjectClassesStepPanel extends AbstractResourceWizardStepPanel {

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_FIELD = "searchFiled";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_SELECTED_ITEMS_CONTAINER = "selectedItemsContainer";
    private static final String ID_SELECTED_ITEM_CONTAINER = "selectedItemContainer";
    private static final String ID_SELECTED_ITEM = "selectedItem";
    private static final String ID_DESELECT_BUTTON = "deselectButton";
    private static final String ID_TABLE = "table";

    private final LoadableModel<List<QName>> selectedItems;

    public SelectObjectClassesStepPanel(ResourceDetailsModel model) {
        super(model);
//        selectedItems = new ItemRealValueModel<>()

        selectedItems = new LoadableModel<>() {
            @Override
            protected List<QName> load() {
                @NotNull ResourceType resource = getResourceModel().getObjectType();
                if (resource.getSchema() == null) {
                    resource.beginSchema();
                }
                if (resource.getSchema().getGenerationConstraints() == null) {
                    resource.getSchema().beginGenerationConstraints();
                }

                List<QName> list = resource.getSchema().getGenerationConstraints().getGenerateObjectClass();
                if (list.size() == 1 && list.get(0) == null) {
                    list.clear();
                }
                return list;
            }
        };
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.selectObjectClasses");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.selectObjectClasses.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.selectObjectClasses.subText");
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ObjectClassDataProvider provider = createProvider();

        MidpointForm searchForm = new MidpointForm(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);
        add(searchForm);

        TextField<String> objectClass =
                new TextField<>(ID_SEARCH_FIELD, new PropertyModel<>(provider, ObjectClassDataProvider.F_FILTER));
        objectClass.setOutputMarkupId(true);
        objectClass.add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
        searchForm.add(objectClass);

        AjaxSubmitButton searchButton = new AjaxSubmitButton(ID_SEARCH_BUTTON) {
            @Override
            public void onSubmit(AjaxRequestTarget target) {
                updateSearchPerformed(target);
            }
        };
        searchButton.setOutputMarkupId(true);
        searchButton.setOutputMarkupPlaceholderTag(true);
        searchForm.add(searchButton);

        WebMarkupContainer selectedItemsContainer = new WebMarkupContainer(ID_SELECTED_ITEMS_CONTAINER);
        selectedItemsContainer.setOutputMarkupId(true);
        add(selectedItemsContainer);
        ListView<QName> selectedContainer = new ListView<>(ID_SELECTED_ITEM_CONTAINER, selectedItems) {

            @Override
            protected void populateItem(ListItem<QName> item) {
                QName objectClass = item.getModelObject();

                item.add(new Label(ID_SELECTED_ITEM, () -> objectClass.getLocalPart()));
                item.add(new AjaxButton(ID_DESELECT_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deselectItem(objectClass, target);
                    }
                });
            }
        };
        selectedContainer.setOutputMarkupId(true);
        selectedItemsContainer.add(selectedContainer);

        BoxedTablePanel<SelectableBean<ObjectClassDto>> table = new BoxedTablePanel(ID_TABLE, provider, createColumns());
        add(table);
    }

    private void deselectItem(QName objectClass, AjaxRequestTarget target) {
        selectedItems.getObject().remove(objectClass);

        BoxedTablePanel table = getTable();
        ((ObjectClassDataProvider) table.getDataTable().getDataProvider()).clearCache();

        target.add(table);
        target.add(getSelectedItemsContainer());
    }

    private void updateSearchPerformed(AjaxRequestTarget target) {
        BoxedTablePanel table = getTable();
        ((ObjectClassDataProvider) table.getDataTable().getDataProvider()).setFilter(getObjectClassText().getModelObject());
        ((ObjectClassDataProvider) table.getDataTable().getDataProvider()).clearCache();
        target.add(table);
    }

    private TextField<String> getObjectClassText() {
        return (TextField) get(getPageBase().createComponentPath(ID_SEARCH_FORM, ID_SEARCH_FIELD));
    }

    private BoxedTablePanel getTable() {
        return (BoxedTablePanel) get(ID_TABLE);
    }

    private List<IColumn<SelectableBean<ObjectClassWrapper>, String>> createColumns() {
        List<IColumn<SelectableBean<ObjectClassWrapper>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>() {
            @Override
            protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<SelectableBean<ObjectClassWrapper>> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(target, table, rowModel, selected);
                if (Boolean.TRUE.equals(selected.getObject())) {
                    selectedItems.getObject().add(rowModel.getObject().getValue().getObjectClassName());
                } else {
                    selectedItems.getObject().remove(rowModel.getObject().getValue().getObjectClassName());
                }
                target.add(getSelectedItemsContainer());
            }

            @Override
            protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                super.onUpdateHeader(target, selected, table);
                ObjectClassDataProvider provider = (ObjectClassDataProvider) table.getDataProvider();
                if (selected) {
                    provider.getListFromModel().forEach(objectClass -> selectedItems.getObject().add(objectClass.getObjectClassName()));
                } else {
                    provider.getListFromModel().forEach(objectClass -> selectedItems.getObject().remove(objectClass.getObjectClassName()));
                }
                target.add(getSelectedItemsContainer());

            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("SelectObjectClassesStepPanel.table.column.name")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ObjectClassWrapper>>> item,
                    String id,
                    IModel<SelectableBean<ObjectClassWrapper>> iModel) {
                item.add(new Label(id, new PropertyModel<>(iModel, "value." + ObjectClassWrapper.F_OBJECT_CLASS_NAME)));
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("SelectObjectClassesStepPanel.table.column.nativeName")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ObjectClassWrapper>>> item,
                    String id,
                    IModel<SelectableBean<ObjectClassWrapper>> iModel) {
                item.add(new Label(id, new PropertyModel<>(iModel, "value." + ObjectClassWrapper.F_NATIVE_OBJECT_CLASS)));
            }
        });

        columns.add(new AbstractColumn<>(getPageBase().createStringResource("SelectObjectClassesStepPanel.table.column.type")) {
            @Override
            public void populateItem(
                    Item<ICellPopulator<SelectableBean<ObjectClassWrapper>>> item,
                    String id,
                    IModel<SelectableBean<ObjectClassWrapper>> iModel) {
                item.add(new Label(id, getPageBase().createStringResource(
                        "SelectObjectClassesStepPanel.table.column.type.${value.auxiliary}",
                        iModel)));
            }
        });
        return columns;
    }

    private Component getSelectedItemsContainer() {
        return get(ID_SELECTED_ITEMS_CONTAINER);
    }

    private ObjectClassDataProvider createProvider() {
        return new ObjectClassDataProvider(SelectObjectClassesStepPanel.this, getResourceModel().getObjectClassesModel()) {
            @Override
            protected SelectableBean<ObjectClassWrapper> createObjectWrapper(ObjectClassWrapper object) {
                SelectableBean<ObjectClassWrapper> wrapper = super.createObjectWrapper(object);
                wrapper.setSelected(selectedItems.getObject().contains(object.getObjectClassName()));
                return wrapper;
            }
        };
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("SelectObjectClassesStepPanel.submitLabel");
    }
}
