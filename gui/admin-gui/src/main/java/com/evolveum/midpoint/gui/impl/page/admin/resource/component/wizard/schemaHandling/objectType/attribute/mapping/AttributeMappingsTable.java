/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ItemWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AttributeMappingValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author lskublik
 */
public abstract class AttributeMappingsTable<P extends Containerable, AP extends Containerable> extends AbstractMappingsTable<P> {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeMappingsTable.class);

    public AttributeMappingsTable(
            String id,
            IModel<PrismContainerValueWrapper<P>> valueModel, ContainerPanelConfigurationType config) {
        super(id, valueModel, config);
    }

    protected final PrismContainerValueWrapper createNewValue(PrismContainerValue<MappingType> value, AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<AP> mappingAttributeContainer =
                    getValueModel().getObject().findContainer(getItemNameOfContainerWithMappings());
            PrismContainerValue<AP> newMapping
                    = mappingAttributeContainer.getItem().createNewValue();

            AttributeMappingValueWrapper newAttributeMappingWrapper =
                    WebPrismUtil.createNewValueWrapper(mappingAttributeContainer, newMapping, getPageBase(), target);
            newAttributeMappingWrapper.addAttributeMappingType(getMappingType());

            PrismContainerWrapper<MappingType> wrapper =
                    newAttributeMappingWrapper.findContainer(getPathBaseOnMappingType());
            PrismContainerValueWrapper newValueWrapper;
            if (wrapper.getValues().isEmpty()) {
                PrismContainerValue<MappingType> newValue = value;
                if (newValue == null) {
                    newValue = wrapper.getItem().createNewValue();
                }
                newValueWrapper = WebPrismUtil.createNewValueWrapper(wrapper, newValue, getPageBase(), target);
            } else {
                if (value == null) {
                    newValueWrapper = wrapper.getValue();
                } else {
                    wrapper.getValues().clear();
                    newValueWrapper = WebPrismUtil.createNewValueWrapper(wrapper, value, getPageBase(), target);
                }
            }

            newValueWrapper.findProperty(MappingType.F_STRENGTH).getValue().setRealValue(MappingStrengthType.STRONG);

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
                        ItemPath.create(getItemNameOfContainerWithMappings(), getPathBaseOnMappingType()));
                try {
                    Task task = getPageBase().createSimpleTask("Create virtual item");
                    OperationResult result = task.getResult();
                    PrismContainerWrapper<MappingType> virtualMappingContainer =
                            getPageBase().createItemWrapper(def.instantiate(), ItemStatus.ADDED, new WrapperContext(task, result));
                    virtualMappingContainer.getValues().clear();

                    PrismContainerWrapper<AP> mappingAttributeContainer =
                            getValueModel().getObject().findContainer(getItemNameOfContainerWithMappings());

                    PrismPropertyDefinition<Object> propertyDef = container.getDefinition().findPropertyDefinition(
                            ItemPath.create(getItemNameOfContainerWithMappings(), getItemNameOfRefAttribute()));

                    for (PrismContainerValueWrapper<AP> value : mappingAttributeContainer.getValues()) {

                        PrismContainerWrapper<MappingType> mappingContainer = value.findContainer(getPathBaseOnMappingType());

                        for (PrismContainerValueWrapper<MappingType> mapping : mappingContainer.getValues()) {

                            if (mapping.getParent() != null
                                    && mapping.getParent().getParent() != null
                                    && mapping.getParent().getParent() instanceof AttributeMappingValueWrapper
                                    && ((AttributeMappingValueWrapper) mapping.getParent().getParent())
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

    protected abstract ItemName getItemNameOfRefAttribute();

    protected abstract ItemName getItemNameOfContainerWithMappings();

    private void createVirtualItemInMapping(PrismContainerValueWrapper<MappingType> mapping) throws SchemaException {
        PrismContainerValueWrapper<P> container = getValueModel().getObject();

        PrismPropertyDefinition<Object> propertyDef = container.getDefinition().findPropertyDefinition(
                ItemPath.create(getItemNameOfContainerWithMappings(), getItemNameOfRefAttribute()));

        createVirtualItemInMapping(mapping, null, propertyDef);
    }

    private void createVirtualItemInMapping(
            PrismContainerValueWrapper<MappingType> mapping,
            PrismContainerValueWrapper<AP> value,
            PrismPropertyDefinition<Object> propertyDef)
            throws SchemaException {
        if (mapping.findProperty(getItemNameOfRefAttribute()) == null) {

            Task task = getPageBase().createSimpleTask("Create virtual item");
            OperationResult result = task.getResult();

            @NotNull PrismProperty<Object> refvalue = propertyDef.instantiate();
            if (value != null && !ValueStatus.ADDED.equals(value.getStatus())) {
                refvalue.addRealValue(getAttributeRefAttributeValue(value));
            }

            ItemWrapper refItemWrapper = getPageBase().createItemWrapper(
                    refvalue,
                    mapping,
                    ItemStatus.ADDED,
                    new WrapperContext(task, result));

            ((ItemWrapperImpl) refItemWrapper).setDisplayName(
                    getString(getMappingType().name() + "." + getItemNameOfRefAttribute()));
            ((ItemWrapperImpl) refItemWrapper).setDisplayOrder(1);

            if (value != null && value.getRealValue() != null && getAttributeRefAttributeValue(value) != null) {
                refItemWrapper.getValue().setRealValue(getAttributeRefAttributeValue(value).clone());
            }

            refItemWrapper.setVisibleOverwrite(UserInterfaceElementVisibilityType.HIDDEN);
            mapping.addItem(refItemWrapper);
            mapping.getNonContainers().clear();
        }
    }

    protected abstract ItemPathType getAttributeRefAttributeValue(PrismContainerValueWrapper<AP> value);

    protected IColumn<PrismContainerValueWrapper<MappingType>, String> createVirtualRefItemColumn(
            IModel<? extends PrismContainerDefinition> resourceAttributeDef, String cssClasses) {
        return new PrismPropertyWrapperColumn(
                resourceAttributeDef,
                getItemNameOfRefAttribute(),
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()) {
            @Override
            protected Component createHeader(String componentId, IModel mainModel) {
                return new PrismPropertyHeaderPanel<ItemPathType>(
                        componentId,
                        new PrismPropertyWrapperHeaderModel(mainModel, itemName, getPageBase())) {

                    @Override
                    protected boolean isAddButtonVisible() {
                        return false;
                    }

                    @Override
                    protected boolean isButtonEnabled() {
                        return false;
                    }

                    @Override
                    protected Component createTitle(IModel<String> label) {
                        return super.createTitle(getPageBase().createStringResource(
                                getRefColumnPrefix() + getMappingType().name() + "." + getItemNameOfRefAttribute()));
                    }
                };
            }

            @Override
            public String getCssClass() {
                return cssClasses;
            }
        };
    }

    protected String getRefColumnPrefix() {
        return "";
    }
}
