/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.duplicateresolver;

import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.duplication.ContainerableDuplicateResolver;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.prism.*;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lskublik
 */
public abstract class ContainerDuplicateResolver<C extends Containerable> implements ContainerableDuplicateResolver<C> {

    @Autowired private GuiComponentRegistryImpl registry;

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    /**
     * @return the registry
     */
    protected final GuiComponentRegistry getRegistry() {
        return registry;
    }
}
