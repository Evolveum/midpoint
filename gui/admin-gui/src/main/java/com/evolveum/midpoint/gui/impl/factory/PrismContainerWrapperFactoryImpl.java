/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
@Component
public class PrismContainerWrapperFactoryImpl<C extends Containerable> extends ItemWrapperFactoryImpl<PrismContainerWrapper<C>, PrismContainerValue<C>, PrismContainer<C>, PrismContainerValueWrapper<C>> implements PrismContainerWrapperFactory<C>{

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperFactoryImpl.class);

    @Autowired private GuiComponentRegistryImpl registry;

    @Override
    public boolean match(ItemDefinition<?> def) {
        return  def instanceof PrismContainerDefinition;
    }

    @PostConstruct
    @Override
    public void register() {
        registry.addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }


    @Override
    public PrismContainerValueWrapper<C> createValueWrapper(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, ValueStatus status, WrapperContext context)
            throws SchemaException {
        PrismContainerValueWrapper<C> containerValueWrapper = createContainerValueWrapper(parent, value, status, context);
        containerValueWrapper.setShowEmpty(context.isShowEmpty());

        List<ItemWrapper<?,?,?,?>> wrappers = new ArrayList<>();
        for (ItemDefinition<?> def : getItemDefinitions(parent, value)) {
            addItemWrapper(def, containerValueWrapper, context, wrappers);
        }

        containerValueWrapper.getItems().addAll((Collection) wrappers);
        containerValueWrapper.setVirtualContainerItems(context.getVirtualItemSpecification());
        parent.setVirtual(context.getVirtualItemSpecification() != null);
        containerValueWrapper.setExpanded(shouldBeExpanded(parent, value, context));

        return containerValueWrapper;
    }

    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<C> parent, PrismContainerValue<C> value) {
        return parent.getDefinitions();
    }

    protected void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper,
            WrapperContext context, List<ItemWrapper<?,?,?,?>> wrappers) throws SchemaException {



        ItemWrapperFactory<?, ?, ?> factory = registry.findWrapperFactory(def);
        if (factory == null) {
            LOGGER.error("Cannot find factory for {}", def);
            throw new SchemaException("Cannot find factory for " + def);
        }

        LOGGER.trace("Found factory {} for {}", factory, def);

        ItemWrapper<?,?,?,?> wrapper = factory.createWrapper(containerValueWrapper, def, context);

        if (wrapper == null) {
            LOGGER.trace("Null wrapper created for {}. Skipping.", def);
            return;
        }

        wrapper.setShowEmpty(context.isShowEmpty(), false);
        wrappers.add(wrapper);
    }

    @Override
    protected PrismContainerValue<C> createNewValue(PrismContainer<C> item) {
        return item.createNewValue();
    }

    @Override
    protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer,
            ItemStatus status, WrapperContext ctx) {
        registry.registerWrapperPanel(childContainer.getDefinition().getTypeName(), PrismContainerPanel.class);
        return new PrismContainerWrapperImpl<C>((PrismContainerValueWrapper<C>) parent, childContainer, status);
    }

    @Override
    public PrismContainerValueWrapper<C> createContainerValueWrapper(PrismContainerWrapper<C> objectWrapper, PrismContainerValue<C> objectValue, ValueStatus status, WrapperContext context) {
        return new PrismContainerValueWrapperImpl<C>(objectWrapper, objectValue, status);
    }

    protected boolean shouldBeExpanded(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, WrapperContext context) {

        if (context.getVirtualItemSpecification() != null) {
            return true;
        }

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

    @Override
    protected void setupWrapper(PrismContainerWrapper<C> wrapper) {
        boolean expanded = false;
        for (PrismContainerValueWrapper<C> valueWrapper : wrapper.getValues()) {
            if (valueWrapper.isExpanded()) {
                expanded = true;
            }
        }

        wrapper.setExpanded(expanded || wrapper.isSingleValue());
    }

}
