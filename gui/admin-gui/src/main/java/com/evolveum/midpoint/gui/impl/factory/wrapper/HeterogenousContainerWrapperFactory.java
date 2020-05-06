/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.factory.wrapper.ItemWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionsDefinitionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 */
@Component
public class HeterogenousContainerWrapperFactory<C extends Containerable> implements PrismContainerWrapperFactory<C> {

    private static final Trace LOGGER = TraceManager.getTrace(HeterogenousContainerWrapperFactory.class);

    @Autowired private GuiComponentRegistry registry;

    @Override
    public PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent,
            ItemDefinition<?> def, WrapperContext context) throws SchemaException {
        ItemName name = def.getItemName();

        PrismContainer<C> childItem = parent.getNewValue().findContainer(name);
        ItemStatus status = ItemStatus.NOT_CHANGED;
        if (childItem == null) {
            childItem = parent.getNewValue().findOrCreateContainer(name);
            status = ItemStatus.ADDED;
        }

        PrismContainerWrapper<C> itemWrapper = new PrismContainerWrapperImpl<C>(parent, childItem, status);
        registry.registerWrapperPanel(childItem.getDefinition().getTypeName(), PrismContainerPanel.class);

        List<PrismContainerValueWrapper<C>> valueWrappers  = createValuesWrapper(itemWrapper, childItem, context);
        LOGGER.trace("valueWrappers {}", itemWrapper.getValues());
        itemWrapper.getValues().addAll(valueWrappers);

        return itemWrapper;
    }

    @Override
    public PrismContainerValueWrapper<C> createValueWrapper(PrismContainerWrapper<C> parent,
            PrismContainerValue<C> value, ValueStatus status, WrapperContext context)
            throws SchemaException {
        PrismContainerValueWrapper<C> containerValueWrapper = new PrismContainerValueWrapperImpl<C>(parent, value, status);
        containerValueWrapper.setShowEmpty(context.isShowEmpty());
        containerValueWrapper.setExpanded(shouldBeExpanded(parent, value, context));
        containerValueWrapper.setHeterogenous(true);

        List<ItemWrapper<?,?>> wrappers = new ArrayList<>();

        for (ItemDefinition<?> def : value.getDefinition().getDefinitions()) {

            Item<?,?> childItem = value.findItem(def.getItemName());

            if (childItem == null && def instanceof PrismContainerDefinition) {
                LOGGER.trace("Skipping creating wrapper for {}, only property and reference wrappers are created for heterogeneous containers.", def);
                continue;
            }

            ItemWrapperFactory<?,?,?> factory = registry.findWrapperFactory(def);

            ItemWrapper<?,?> wrapper = factory.createWrapper(containerValueWrapper, def, context);
            if (wrapper != null) {
                wrappers.add(wrapper);
            }
        }

        containerValueWrapper.getItems().addAll((Collection) wrappers);
        return containerValueWrapper;
    }

    protected boolean shouldBeExpanded(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, WrapperContext context) {
        if (value.isEmpty()) {
            return context.isShowEmpty() || containsEmphasizedItems(parent.getDefinitions());
        }

        return true;
    }

    private boolean containsEmphasizedItems(List<? extends ItemDefinition> definitions) {
        for (ItemDefinition def : definitions) {
            if (def.isEmphasized()) {
                return true;
            }
        }

        return false;
    }

    protected List<PrismContainerValueWrapper<C>> createValuesWrapper(PrismContainerWrapper<C> itemWrapper, PrismContainer<C> item, WrapperContext context) throws SchemaException {
        List<PrismContainerValueWrapper<C>> pvWrappers = new ArrayList<>();

        if (item.getValues() == null || item.getValues().isEmpty()) {
            PrismContainerValueWrapper<C> valueWrapper = createValueWrapper(itemWrapper, item.createNewValue(), ValueStatus.ADDED, context);
            pvWrappers.add(valueWrapper);
            return pvWrappers;
        }

        for (PrismContainerValue<C> pcv : item.getValues()) {
            PrismContainerValueWrapper<C> valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
            pvWrappers.add(valueWrapper);
        }

        return pvWrappers;

    }

    /**
     *
     * match single value containers which contains a looot of other conainers, e.g. policy rule, policy action, notification configuration, etc
     */
    @Override
    public boolean match(ItemDefinition<?> def) {
        QName defName = def.getTypeName();

        if (TaskPartitionsDefinitionType.COMPLEX_TYPE.equals(defName)) {
            return true;
        }

        if (!(def instanceof PrismContainerDefinition)) {
            return false;
        }

        PrismContainerDefinition<?> containerDef = (PrismContainerDefinition<?>) def;

        if (containerDef.isMultiValue()) {
            return false;
        }

        List<? extends ItemDefinition> defs = containerDef.getDefinitions();
        int containers = 0;
        for (ItemDefinition<?> itemDef : defs) {
            if (itemDef instanceof PrismContainerDefinition<?> && itemDef.isMultiValue()) {
                containers++;
            }
        }

        if (containers > 2) {
            return true;
        }

        return false;

    }

    @Override
    @PostConstruct
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 110;
    }

    @Override
    public PrismContainerValueWrapper<C> createContainerValueWrapper(PrismContainerWrapper<C> objectWrapper,
            PrismContainerValue<C> objectValue, ValueStatus status, WrapperContext context) {
        return null;
    }

    @Override
    public PrismContainerWrapper<C> createWrapper(Item childContainer, ItemStatus status, WrapperContext context)
            throws SchemaException {
        return null;
    }


}
