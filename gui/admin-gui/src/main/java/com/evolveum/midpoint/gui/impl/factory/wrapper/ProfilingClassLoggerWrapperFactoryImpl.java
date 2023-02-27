/*
\ * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ProfilingClassLoggerPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ProfilingClassLoggerContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ProfilingClassLoggerContainerWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author skublik
 */
@Component
public class ProfilingClassLoggerWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ClassLoggerConfigurationType> {

    public static final QName PROFILING_LOGGER_PATH = new QName("profilingClassLogger");

    public static final String LOGGER_PROFILING = "PROFILING";

    @Override
    public boolean match(ItemDefinition<?> def) {
        return false;
    }

    @Override
    protected PrismContainerWrapper<ClassLoggerConfigurationType> createWrapperInternal(PrismContainerValueWrapper<?> parent,
            PrismContainer<ClassLoggerConfigurationType> childContainer, ItemStatus status, WrapperContext ctx) {

        PrismContainer<ClassLoggerConfigurationType> clone = childContainer.clone();

        return new ProfilingClassLoggerContainerWrapperImpl<>(parent, clone, status);
    }

    @Override
    public void registerWrapperPanel(PrismContainerWrapper<ClassLoggerConfigurationType> wrapper) {
        getRegistry().registerWrapperPanel(PROFILING_LOGGER_PATH, ProfilingClassLoggerPanel.class);
    }

    @Override
    protected List<PrismContainerValue<ClassLoggerConfigurationType>> getValues(PrismContainer<ClassLoggerConfigurationType> item) {
        return item.getValues().stream().filter(value -> {
            if (value == null || value.getRealValue() == null) {
                return false;
            }
            String loggerPackage = ((ClassLoggerConfigurationType) value.getRealValue()).getPackage();
            if (loggerPackage == null) {
                return false;
            }
            return loggerPackage.equals(LOGGER_PROFILING);
        }).collect(Collectors.toList());
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<ClassLoggerConfigurationType> item, WrapperContext context) {
        return true;
    }

    @Override
    protected PrismContainerValue<ClassLoggerConfigurationType> createNewValue(PrismContainer<ClassLoggerConfigurationType> item) {
        PrismContainerValue<ClassLoggerConfigurationType> profilingLogger = super.createNewValue(item);
        profilingLogger.asContainerable().setPackage(LOGGER_PROFILING);
        return profilingLogger;
    }

    @Override
    public PrismContainerValueWrapper<ClassLoggerConfigurationType> createContainerValueWrapper(PrismContainerWrapper<ClassLoggerConfigurationType> objectWrapper,
            PrismContainerValue<ClassLoggerConfigurationType> objectValue, ValueStatus status, WrapperContext context) {

        ClassLoggerConfigurationType logger = objectValue.getRealValue();
        if (logger != null) {
            logger.setPackage(LOGGER_PROFILING);
        }

        return new ProfilingClassLoggerContainerValueWrapperImpl(objectWrapper, objectValue, status);
    }
}
