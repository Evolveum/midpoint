/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardStepPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;

import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectClassDataProvider;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

/**
 * On this panel the user selects what object classes should the resource schema contain, i.e., what to put into
 * the `generationConstraints` property of the resource schema.
 *
 * @author lskublik
 */
public class SelectObjectClassesStepPanel extends AbstractWizardStepPanel<ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectObjectClassesStepPanel.class);

    private static final String PANEL_TYPE = "rw-select-object-classes";

    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_SEARCH_FIELD = "searchFiled";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_SELECTED_ITEMS_CONTAINER = "selectedItemsContainer";
    private static final String ID_SELECTED_ITEM_CONTAINER = "selectedItemContainer";
    private static final String ID_SELECTED_ITEM = "selectedItem";
    private static final String ID_DESELECT_BUTTON = "deselectButton";
    private static final String ID_TABLE = "table";

    /**
     * We suggest some classes only if there are too many of them (e.g. for LDAP or AD).
     *
     * If there are less classes, it is not so important, because if user selects nothing,
     * all classes will be available in the resulting schema, which is absolutely OK if they are not many.
     */
    private static final int SUGGESTION_THRESHOLD = 20;

    /**
     * Names that will be initially suggested. TODO consider making it specific for particular connectors (e.g., AD vs LDAP).
     *
     * TODO Do we want to suggest these classes also when revisiting the panel?
     */
    private static final Collection<String> INITIALLY_SUGGESTED_NAMES = List.of(
            "inetOrgPerson", // LDAP
            "groupOfNames", // LDAP
            "groupOfUniqueNames", // LDAP
            "organizationalUnit", // LDAP + AD
            "user", // AD
            "group" // AD
    );

    /**
     * Model for currently selected object classes.
     *
     * - The key is QName of the object class.
     * - The value contains related restrictions: whether it can be added/removed, see {@link SelectedClassWrapper}.
     */
    private final LoadableModel<Map<QName, SelectedClassWrapper>> selectedItems;

    public SelectObjectClassesStepPanel(ResourceDetailsModel model) {
        super(model);
        selectedItems = new LoadableModel<>() {
            @Override
            protected Map<QName, SelectedClassWrapper> load() {
                List<ObjectClassWrapper> allAvailableClasses = getDetailsModel().getObjectClassesModel().getObject();
                Map<QName, SelectedClassWrapper> map = new HashMap<>(); // resulting initial map of selected object classes
                try {
                    CompleteResourceSchema schema = getDetailsModel().getRefinedSchema();
                    List<ResourceObjectTypeDefinition> objectTypes =
                            new ArrayList<>(schema != null ? schema.getObjectTypeDefinitions() : List.of());
                    PrismPropertyWrapper<QName> generationConstraints = getDetailsModel().getObjectWrapper().findProperty(
                            ItemPath.create(
                                    ResourceType.F_SCHEMA,
                                    XmlSchemaType.F_GENERATION_CONSTRAINTS,
                                    SchemaGenerationConstraintsType.F_GENERATE_OBJECT_CLASS));
                    if (generationConstraints != null && !generationConstraints.isEmpty()) {
                        generationConstraints.getValues().forEach(value -> {
                            QName objectClassName = value.getRealValue();
                            if (objectClassName != null) {

                                boolean classUsedInSchemaHandling = false;

                                Iterator<ResourceObjectTypeDefinition> iterator = objectTypes.iterator();
                                while (iterator.hasNext()) {
                                    ResourceObjectTypeDefinition objectType = iterator.next();
                                    if (QNameUtil.match(objectClassName, objectType.getObjectClassName())) {
                                        classUsedInSchemaHandling = true;
                                        iterator.remove();
                                        break;
                                    }
                                }

                                PrismObject<ResourceType> resource = getDetailsModel().getObjectType().asPrismObject();
                                // If the "generation constraint" related to this class comes from the template,
                                // we are not free to remove it. We cannot remove it also if it is used in schema handling.
                                boolean classComingFromTemplate =
                                        WebPrismUtil.isValueFromResourceTemplate(value.getNewValue(), resource);

                                boolean enabled = !classComingFromTemplate && !classUsedInSchemaHandling;
                                boolean canSave = !classComingFromTemplate;

                                map.put(
                                        objectClassName,
                                        new SelectedClassWrapper(enabled, canSave)
                                );

                            }
                        });
                    } else {
                        // No generation constraints -> we will suggest at least something (but only for large schemas)
                        if (allAvailableClasses.size() >= SUGGESTION_THRESHOLD) {
                            allAvailableClasses.stream()
                                    .filter(classWrapper ->
                                            INITIALLY_SUGGESTED_NAMES.stream().anyMatch(
                                                    classWrapper.getObjectClassNameAsString()::equalsIgnoreCase))
                                    .forEach(classWrapper -> map.put(
                                            classWrapper.getObjectClassName(),
                                            new SelectedClassWrapper()));
                        }
                    }

                    // These object types have classes that were not mentioned in generation constraints, if present.
                    // They will be eventually saved into generation constraints (at exit).
                    //
                    // TODO decide if we want to suggest anything if there are some object types
                    objectTypes.forEach(objectType -> map.put(
                            objectType.getObjectClassName(),
                            new SelectedClassWrapper(false, true)));

                } catch (SchemaException | ConfigurationException e) {
                    LOGGER.error("Couldn't find property for schema generation constraints", e);
                }

                // TODO is this necessary? Single class does not need to be selected
                if (map.isEmpty() && allAvailableClasses.size() == 1) {
                    ObjectClassWrapper singleClass = allAvailableClasses.iterator().next();
                    map.put(singleClass.getObjectClassName(), new SelectedClassWrapper());
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

        // Contains the names of classes that were selected, each with "deselect" button (hidden if the class cannot be removed).
        ListView<QName> selectedContainer = new ListView<>(
                ID_SELECTED_ITEM_CONTAINER,
                () -> new ArrayList<>(selectedItems.getObject().keySet())) {

            @Override
            protected void populateItem(ListItem<QName> item) {
                QName objectClass = item.getModelObject();

                item.add(new Label(ID_SELECTED_ITEM, objectClass::getLocalPart));
                AjaxButton deselectButton = new AjaxButton(ID_DESELECT_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deselectItem(objectClass, target);
                    }
                };
                item.add(deselectButton);
                deselectButton.add(new VisibleBehaviour(() -> selectedItems.getObject().get(objectClass).enabled));
            }
        };
        selectedContainer.setOutputMarkupId(true);
        selectedItemsContainer.add(selectedContainer);

        BoxedTablePanel<SelectableBean<ObjectClassWrapper>> table = new BoxedTablePanel<>(ID_TABLE, provider, createColumns());
        table.setShowAsCard(false);
        add(table);
    }

    private void deselectItem(QName objectClass, AjaxRequestTarget target) {
        selectedItems.getObject().remove(objectClass);

        BoxedTablePanel<SelectableBean<ObjectClassWrapper>> table = getTable();
        ((ObjectClassDataProvider) table.getDataTable().getDataProvider()).clearCache();

        target.add(table);
        target.add(getSelectedItemsContainer());
    }

    private void updateSearchPerformed(AjaxRequestTarget target) {
        BoxedTablePanel<SelectableBean<ObjectClassWrapper>> table = getTable();
        ((ObjectClassDataProvider) table.getDataTable().getDataProvider()).setFilter(getObjectClassText().getModelObject());
        ((ObjectClassDataProvider) table.getDataTable().getDataProvider()).clearCache();
        target.add(table);
    }

    private TextField<String> getObjectClassText() {
        return (TextField<String>) get(getPageBase().createComponentPath(ID_SEARCH_FORM, ID_SEARCH_FIELD));
    }

    private BoxedTablePanel<SelectableBean<ObjectClassWrapper>> getTable() {
        return (BoxedTablePanel<SelectableBean<ObjectClassWrapper>>) get(ID_TABLE);
    }

    private List<IColumn<SelectableBean<ObjectClassWrapper>, String>> createColumns() {
        List<IColumn<SelectableBean<ObjectClassWrapper>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>() {

            @Override
            public Component getHeader(String componentId) {
                IsolatedCheckBoxPanel header = (IsolatedCheckBoxPanel) super.getHeader(componentId);
                if (selectedItems.getObject().size() == getDetailsModel().getObjectClassesModel().getObject().size()) {
                    header.getPanelComponent().setModelObject(true);
                }
                return header;
            }

            @Override
            protected void onUpdateRow(Item<ICellPopulator<SelectableBean<ObjectClassWrapper>>> cellItem, AjaxRequestTarget target, DataTable table, IModel<SelectableBean<ObjectClassWrapper>> rowModel, IModel<Boolean> selected) {
                super.onUpdateRow(cellItem, target, table, rowModel, selected);
                if (Boolean.TRUE.equals(selected.getObject())) {
                    if (!selectedItems.getObject().containsKey(rowModel.getObject().getValue().getObjectClassName())) {
                        selectedItems.getObject().put(rowModel.getObject().getValue().getObjectClassName(), new SelectedClassWrapper());
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
                            selectedItems.getObject().put(objectClass.getObjectClassName(), new SelectedClassWrapper());
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
                    if (selectedItems.getObject().size() == getDetailsModel().getObjectClassesModel().getObject().size()) {
                        return Model.of(selectedItems.getObject().values().stream().anyMatch(wrapper -> wrapper.enabled));
                    }
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
        return new ObjectClassDataProvider(SelectObjectClassesStepPanel.this, getDetailsModel().getObjectClassesModel()) {
            @Override
            protected SelectableBean<ObjectClassWrapper> createObjectWrapper(ObjectClassWrapper object) {
                SelectableBean<ObjectClassWrapper> wrapper = super.createObjectWrapper(object);
                if (selectedItems.getObject().containsKey(object.getObjectClassName())) {
                    wrapper.setSelected(true);
                    object.setEnabled(selectedItems.getObject().get(object.getObjectClassName()).enabled);
                }
                return wrapper;
            }
        };
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        getPageBase().closeRightSidebar(target);

        List<QName> classesForSave = new ArrayList<>();

        selectedItems.getObject().forEach((key, wrapper) -> {
            if (wrapper.canBeSaved) {
                classesForSave.add(key);
            }
        });

        PrismObjectWrapper<ResourceType> resource = getDetailsModel().getObjectWrapper();
        PrismPropertyWrapper<QName> generateObjectClassProperty;
        try {
            generateObjectClassProperty = resource.findProperty(ItemPath.create(
                    ResourceType.F_SCHEMA,
                    XmlSchemaType.F_GENERATION_CONSTRAINTS,
                    SchemaGenerationConstraintsType.F_GENERATE_OBJECT_CLASS));
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find property for generate object class.", e);
            return;
        }

        generateObjectClassProperty.getValues().forEach(value -> {
            boolean match = classesForSave.stream().anyMatch(clazz -> QNameUtil.match(clazz, value.getRealValue()));
            if (!match) {
                value.setStatus(ValueStatus.DELETED);
            } else {
                classesForSave.removeIf(clazz -> QNameUtil.match(clazz, value.getRealValue()));
            }

        });
        classesForSave.forEach(clazz -> {
            PrismPropertyValue<QName> newValue = getPrismContext().itemFactory().createPropertyValue();
            newValue.setValue(clazz);
            try {
                generateObjectClassProperty.add(newValue, getPageBase());
            } catch (SchemaException e) {
                LOGGER.error("Couldn't create new value for generate object class.", e);
            }
        });
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("SelectObjectClassesStepPanel.submitLabel");
    }

    @Override
    public String getStepId() {
        return PANEL_TYPE;
    }

    private static class SelectedClassWrapper implements Serializable {

        /** Can the object class be removed from the generation constraints? TODO consider renaming */
        private final boolean enabled;

        /** Can the object class be added to or removed from the generation constraints? TODO consider renaming */
        private final boolean canBeSaved;

        private SelectedClassWrapper() {
            this(true, true);
        }

        private SelectedClassWrapper(boolean enabled, boolean canBeSaved) {
            this.enabled = enabled;
            this.canBeSaved = canBeSaved;
        }
    }
}
