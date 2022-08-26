/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingValueWrapper;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
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
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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
public abstract class AttributeMappingsTable extends MultivalueContainerListPanel<MappingType> {

    private static final Trace LOGGER = TraceManager.getTrace(AttributeMappingsTable.class);

    private final IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;
    private final ResourceDetailsModel resourceDetailsModel;

    public AttributeMappingsTable(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, MappingType.class);
        resourceDetailsModel = model;
        this.valueModel = valueModel;
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        getTable().setShowAsCard(false);
    }

    @Override
    protected List<Component> createToolbarButtonsList(String idButton) {
        List<Component> buttons = super.createToolbarButtonsList(idButton);
        buttons.forEach(button -> {
            if (button instanceof AjaxIconButton) {
                ((AjaxIconButton) button).showTitleAsLabel(true);
            }
        });
        return buttons;
    }

    @Override
    protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relationSpec) {
        getContainerModel().getObject().getValues().add(createNewMapping(target));
        refreshTable(target);
    }

    protected PrismContainerValueWrapper createNewMapping(AjaxRequestTarget target) {
        try {
            PrismContainerWrapper<ResourceAttributeDefinitionType> mappingAttributeContainer =
                    valueModel.getObject().findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);
            PrismContainerValue<ResourceAttributeDefinitionType> newMapping
                    = mappingAttributeContainer.getItem().createNewValue();

            ResourceAttributeMappingValueWrapper newAttributeMappingWrapper =
                    WebPrismUtil.createNewValueWrapper(mappingAttributeContainer, newMapping, getPageBase(), target);
            newAttributeMappingWrapper.addAttributeMappingType(getMappingType());

            if (newAttributeMappingWrapper instanceof ResourceAttributeMappingValueWrapper) {
                ((ResourceAttributeMappingValueWrapper) newAttributeMappingWrapper)
                        .addAttributeMappingType(getMappingType());
            }

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
        switch (getMappingType()) {
            case INBOUND:
                return ResourceAttributeDefinitionType.F_INBOUND;
            case OUTBOUND:
                return ResourceAttributeDefinitionType.F_OUTBOUND;
            default:
                return null;
        }
    }

    protected abstract WrapperContext.AttributeMappingType getMappingType();

    @Override
    protected boolean isCreateNewObjectVisible() {
        return true;
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {

        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem item = new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_EDIT_MENU_ITEM);
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createEditColumnAction();
            }
        };
        items.add(item);

        item = new ButtonInlineMenuItem(createStringResource("pageAdminFocus.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return createDeleteColumnAction();
            }
        };
        items.add(item);
        return items;
    }

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
                PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> container = valueModel.getObject();
                ItemDefinition<?> def = container.getDefinition().findContainerDefinition(
                        ItemPath.create(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, getPathBaseOnMappingType()));
                try {
                    Task task = getPageBase().createSimpleTask("Create virtual item");
                    OperationResult result = task.getResult();
                    PrismContainerWrapper<MappingType> virtualMappingContainer =
                            getPageBase().createItemWrapper(def.instantiate(), ItemStatus.ADDED, new WrapperContext(task, result));
                    virtualMappingContainer.getValues().clear();

                    PrismContainerWrapper<ResourceAttributeDefinitionType> mappingAttributeContainer =
                            valueModel.getObject().findContainer(ResourceObjectTypeDefinitionType.F_ATTRIBUTE);

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
        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> container = valueModel.getObject();

        PrismPropertyDefinition<Object> propertyDef = container.getDefinition().findPropertyDefinition(
                ItemPath.create(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_REF));

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
    protected List<IColumn<PrismContainerValueWrapper<MappingType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<MappingType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        IModel<PrismContainerDefinition<MappingType>> mappingTypeDef =
                getMappingTypeDefinition();
        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_NAME,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

        columns.addAll(createCustomColumns());

        columns.add(new PrismPropertyWrapperColumn<MappingType, String>(
                mappingTypeDef,
                MappingType.F_ENABLED,
                AbstractItemWrapperColumn.ColumnType.VALUE,
                getPageBase()));

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
    public void refreshTable(AjaxRequestTarget target) {
        getContainerModel().detach();
        clearCache();
        super.refreshTable(target);
    }
}
