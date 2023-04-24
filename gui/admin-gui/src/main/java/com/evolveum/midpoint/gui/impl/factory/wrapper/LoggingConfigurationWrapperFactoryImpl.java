/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;
import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;

/**
 * @author skublik
 */
@Component
public class LoggingConfigurationWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C>{

    @Autowired
    private ClassLoggerWrapperFactoryImpl classLoggerFactory;
    @Autowired
    private ProfilingClassLoggerWrapperFactoryImpl profilingClassLoggerFactory;

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition
                && QNameUtil.match(def.getTypeName(), LoggingConfigurationType.COMPLEX_TYPE);
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 10;
    }


    @Override
    public void addItemWrapper(ItemDefinition<?> def, PrismContainerValueWrapper<?> containerValueWrapper, WrapperContext context, List<ItemWrapper<?, ?>> wrappers) throws SchemaException {
        if (QNameUtil.match(def.getTypeName(), ClassLoggerConfigurationType.COMPLEX_TYPE)) {
                wrappers.add(createClassLoggingWrapper(def, containerValueWrapper, context));
                wrappers.add(createProfilingWrapper(def, containerValueWrapper, context));
            } else {
                super.addItemWrapper(def, containerValueWrapper, context, wrappers);
            }
    }

    private ItemWrapper<?, ?> createProfilingWrapper(ItemDefinition def, PrismContainerValueWrapper<?> parent, WrapperContext context) throws SchemaException {
        return profilingClassLoggerFactory.createWrapper(parent, def, context);
    }

    private ItemWrapper<?, ?> createClassLoggingWrapper(ItemDefinition def, PrismContainerValueWrapper parent, WrapperContext context) throws SchemaException {
        return classLoggerFactory.createWrapper(parent, def, context);
    }

    @Override
    protected PrismContainerValue<C> createNewValue(PrismContainer<C> item) {
        return item.createNewValue();
    }

    @Override
    protected PrismContainerWrapper<C> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer,
            ItemStatus status, WrapperContext ctx) {
        return new PrismContainerWrapperImpl<>(parent, childContainer, status);
    }

    @Override
    public PrismContainerValueWrapper<C> createContainerValueWrapper(PrismContainerWrapper<C> objectWrapper, PrismContainerValue<C> objectValue, ValueStatus status, WrapperContext context) {
        return new PrismContainerValueWrapperImpl<>(objectWrapper, objectValue, status);
    }


}
