/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.ToggleSimulationModeColumn;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardTable;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class AttributeMappingsTable<P extends Containerable> extends AbstractResourceWizardTable<MappingType, P> {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeMappingsTable.class);

    public AttributeMappingsTable(
            String id,
            IModel<PrismContainerValueWrapper<P>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config, MappingType.class);
    }

    protected PrismContainerValueWrapper createNewValue(AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> mappingAttributeContainer =
                    getValueModel().getObject().findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
            PrismContainerValue<ResourceAttributeDefinitionType> newMapping
                    = mappingAttributeContainer.getItem().createNewValue();

            ResourceAttributeMappingValueWrapper newAttributeMappingWrapper =
                    WebPrismUtil.createNewValueWrapper(mappingAttributeContainer, newMapping, getPageBase(), target);
            newAttributeMappingWrapper.addAttributeMappingType(getMappingType());

            PrismContainerWrapper<MappingType> wrapper =
                    newAttributeMappingWrapper.findContainer(getPathBaseOnMappingType());
            PrismContainerValueWrapper newValueWrapper;
            if (wrapper.getValues().isEmpty()) {
                PrismContainerValue<MappingType> newValue = wrapper.getItem().createNewValue();
                newValueWrapper = WebPrismUtil.createNewValueWrapper(wrapper, newValue, getPageBase(), target);
            } else {
                newValueWrapper = wrapper.getValue();
            }

            createVirtualItemInMapping(newValueWrapper);

            return newValueWrapper;

        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new attribute mapping");
        }
        return null;
    }

    private ItemName getPathBaseOnMappingType() {
        if (getMappingType() != null) {
            return getMappingType().getContainerName();
        }
        return null;
    }

    protected abstract MappingDirection getMappingType();

    public void deleteItemPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<MappingType>> toDelete) {
        if (toDelete == null || toDelete.isEmpty()) {
            warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        toDelete.forEach(value -> {
            PrismContainerValueWrapper parentValue = value.getParent().getParent();
            if (parentValue.getStatus() == ValueStatus.ADDED) {
                PrismContainerWrapper wrapper = (PrismContainerWrapper) parentValue.getParent();
                if (wrapper != null) {
                    wrapper.getValues().remove(parentValue);
                }
            } else {
                value.setStatus(ValueStatus.DELETED);
            }
            value.setSelected(false);
        });
        refreshTable(target);
    }

    @Override
    protected IModel<PrismContainerWrapper<MappingType>> getContainerModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismContainerWrapper<MappingType> load() {
                PrismContainerValueWrapper<P> container = getValueModel().getObject();
                ItemDefinition<?> def = container.getDefinition().findContainerDefinition(
                        ItemPath.create(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, getPathBaseOnMappingType()));
                try {
                    Task task = getPageBase().createSimpleTask("Create virtual item");
                    OperationResult result = task.getResult();
                    PrismContainerWrapper<MappingType> virtualMappingContainer =
                            getPageBase().createItemWrapper(def.instantiate(), ItemStatus.ADDED, new WrapperContext(task, result));
                    virtualMappingContainer.getValues().clear();

                    PrismContainerWrapper<ResourceAttributeDefinitionType> mappingAttributeContainer =
                            getValueModel().getObject().findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

                    PrismPropertyDefinition<Object> propertyDef = container.getDefinition().findPropertyDefinition(
                            ItemPath.create(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF));

                    for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> value : mappingAttributeContainer.getValues()) {

                        PrismContainerWrapper<MappingType> mappingContainer = value.findContainer(getPathBaseOnMappingType());

                        for (PrismContainerValueWrapper<MappingType> mapping : mappingContainer.getValues()) {

                            if (mapping.getParent() != null
                                    && mapping.getParent().getParent() != null
                                    && mapping.getParent().getParent() instanceof ResourceAttributeMappingValueWrapper
                                    && ((ResourceAttributeMappingValueWrapper) mapping.getParent().getParent())
                                    .getAttributeMappingTypes().contains(getMappingType())) {

                                createVirtualItemInMapping(mapping, value, propertyDef);

                                virtualMappingContainer.getValues().add(mapping);
                            }
                        }
                    }
                    return virtualMappingContainer;

                } catch (SchemaException e) {
                    LOGGER.error("Couldn't instantiate virtual container for mappings", e);
                }
                return null;
            }
        };
    }

    private void createVirtualItemInMapping(PrismContainerValueWrapper<MappingType> mapping) throws SchemaException {
        PrismContainerValueWrapper<P> container = getValueModel().getObject();

        PrismPropertyDefinition<Object> propertyDef = container.getDefinition().findPropertyDefinition(
                ItemPath.create(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF));

        propertyDef.toMutable().setDisplayOrder(1);

        createVirtualItemInMapping(mapping, null, propertyDef);
    }

    private void createVirtualItemInMapping(
            PrismContainerValueWrapper<MappingType> mapping,
            PrismContainerValueWrapper<ResourceAttributeDefinitionType> value,
            PrismPropertyDefinition<Object> propertyDef)
            throws SchemaException {
        if (mapping.findProperty(ResourceAttributeDefinitionType.F_REF) == null) {

            Task task = getPageBase().createSimpleTask("Create virtual item");
            OperationResult result = task.getResult();

            ItemWrapper refItemWrapper = getPageBase().createItemWrapper(
                    propertyDef.instantiate(),
                    mapping,
                    ItemStatus.ADDED,
                    new WrapperContext(task, result));

            if (value != null && value.getRealValue() != null && value.getRealValue().getRef() != null) {
                refItemWrapper.getValue().setRealValue(value.getRealValue().getRef().clone());
            }

            refItemWrapper.setVisibleOverwrite(UserInterfaceElementVisibilityType.HIDDEN);
            mapping.addItem(refItemWrapper);
            mapping.getNonContainers().clear();
        }
    }

    @Override
    protected String getInlineMenuCssClass() {
        return "";
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<MappingType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();
        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {

            @Override
            public String getCssClass() {
                return "col-xl-2 col-lg-2 col-md-2";
            }
        });

        columns.addAll(createCustomColumns());

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_ENABLED,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<MappingType>> mainModel) {
                Component header = super.createHeader(componentId, mainModel);
                header.add(AttributeAppender.append("class", "d-table-row"));
                return header;
            }
        });

        columns.add(new ToggleSimulationModeColumn<>(getContainerModel(), getPageBase()));

        return columns;
    }

    protected abstract Collection<? extends IColumn<PrismContainerValueWrapper<MappingType>, String>> createCustomColumns();

    protected LoadableModel<PrismContainerDefinition<MappingType>> getMappingTypeDefinition() {
        return new LoadableModel<>() {
            @Override
            protected PrismContainerDefinition<MappingType> load() {
                return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(MappingType.class);
            }
        };
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = new ArrayList<>();
        AjaxIconButton newObjectSimpleButton = new AjaxIconButton(
                idButton,
                new Model<>("fa fa-circle-plus"),
                createStringResource(getKeyOfTitleForNewObjectButton())) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                newItemPerformed(target, null);
            }
        };
        newObjectSimpleButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm ml-3"));
        newObjectSimpleButton.add(new VisibleBehaviour(this::isCreateNewObjectSimpleVisible));
        newObjectSimpleButton.showTitleAsLabel(true);
        buttons.add(newObjectSimpleButton);

        return buttons;
    }
}
