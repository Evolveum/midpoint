/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardStepPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaGenerationConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

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
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lskublik
 */
public class SelectObjectClassesStepPanel extends AbstractResourceWizardStepPanel {

    private static final Trace LOGGER = TraceManager.getTrace(SelectObjectClassesStepPanel.class);

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_FIELD = "searchFiled";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_SELECTED_ITEMS_CONTAINER = "selectedItemsContainer";
    private static final String ID_SELECTED_ITEM_CONTAINER = "selectedItemContainer";
    private static final String ID_SELECTED_ITEM = "selectedItem";
    private static final String ID_DESELECT_BUTTON = "deselectButton";
    private static final String ID_TABLE = "table";

    private final LoadableModel<Map<QName, Boolean>> selectedItems;

    public SelectObjectClassesStepPanel(ResourceDetailsModel model) {
        super(model);
        selectedItems = new LoadableModel<>() {
            @Override
            protected Map<QName, Boolean> load() {
                Map<QName, Boolean> map = new HashMap();
                try {
                    PrismPropertyWrapper<QName> generationConstraints = getResourceModel().getObjectWrapper().findProperty(
                            ItemPath.create(
                                    ResourceType.F_SCHEMA,
                                    XmlSchemaType.F_GENERATION_CONSTRAINTS,
                                    SchemaGenerationConstraintsType.F_GENERATE_OBJECT_CLASS));
                    if (generationConstraints != null) {
                        generationConstraints.getValues().forEach(v -> {
                                if (v.getRealValue() != null) {
                                    map.put(
                                            v.getRealValue(),
                                            !WebPrismUtil.isValueFromResourceTemplate(
                                                    v.getNewValue(),
                                                    getResourceModel().getObjectType().asPrismObject())
                                    );
                                }
                        });

                    }
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't find property for schema generation constraints", e);
                }
                if (map.isEmpty() && getResourceModel().getObjectClassesModel().getObject().size() == 1) {
                    ObjectClassWrapper value = getResourceModel().getObjectClassesModel().getObject().iterator().next();
                    map.put(value.getObjectClassName(), true);
                }
                return map;
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

        ListView<QName> selectedContainer = new ListView<>(
                ID_SELECTED_ITEM_CONTAINER,
                () -> new ArrayList<>(selectedItems.getObject().keySet())) {

            @Override
            protected void populateItem(ListItem<QName> item) {
                QName objectClass = item.getModelObject();

                item.add(new Label(ID_SELECTED_ITEM, () -> objectClass.getLocalPart()));
                AjaxButton deselectButton = new AjaxButton(ID_DESELECT_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deselectItem(objectClass, target);
                    }
                };
                item.add(deselectButton);
                deselectButton.add(new VisibleBehaviour(() -> selectedItems.getObject().get(objectClass)));
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
                    if (!selectedItems.getObject().containsKey(rowModel.getObject().getValue().getObjectClassName())) {
                        selectedItems.getObject().put(rowModel.getObject().getValue().getObjectClassName(), true);
                    }
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
                    provider.getListFromModel().forEach(objectClass -> {
                        if (!selectedItems.getObject().containsKey(objectClass.getObjectClassName())) {
                            selectedItems.getObject().put(objectClass.getObjectClassName(), true);
                        }
                    });
                } else {
                    provider.getListFromModel().forEach(objectClass -> selectedItems.getObject().remove(objectClass.getObjectClassName()));
                }
                target.add(getSelectedItemsContainer());

            }

            @Override
            protected IModel<Boolean> getEnabled(IModel<SelectableBean<ObjectClassWrapper>> rowModel) {
                if (rowModel == null) {
                    return super.getEnabled(rowModel);
                }
                return new PropertyModel<>(rowModel, "value.enabled");
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
                if (selectedItems.getObject().containsKey(object.getObjectClassName())) {
                    wrapper.setSelected(true);
                    object.setEnabled(selectedItems.getObject().get(object.getObjectClassName()));
                }
                return wrapper;
            }
        };
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        List<QName> classForSave = new ArrayList<>();

        selectedItems.getObject().forEach((key, enabled) -> {
            if (enabled) {
                classForSave.add(key);
            }
        });

        if (classForSave.isEmpty()) {
            return;
        }

        @NotNull ResourceType resource = getResourceModel().getObjectType();
        if (resource.getSchema() == null) {
            resource.beginSchema();
        }
        if (resource.getSchema().getGenerationConstraints() == null) {
            resource.getSchema().beginGenerationConstraints();
        }

        resource.getSchema().getGenerationConstraints().getGenerateObjectClass().addAll(classForSave);
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("SelectObjectClassesStepPanel.submitLabel");
    }
}
