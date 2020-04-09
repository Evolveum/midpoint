/*
\ * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 */
@Component
public class ProfilingClassLoggerWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ClassLoggerConfigurationType> {

    @Autowired private GuiComponentRegistry registry;

    public static final QName PROFILING_LOGGER_PATH = new QName("profilingClassLogger");

    public static final String LOGGER_PROFILING = "PROFILING";

    @Override
    public boolean match(ItemDefinition<?> def) {
        return false;
    }

    @Override
    protected boolean canCreateValueWrapper(PrismContainerValue<ClassLoggerConfigurationType> value) {
        if(value == null || value.getRealValue() == null) {
            return false;
        }
        String loggerPackage = ((ClassLoggerConfigurationType)value.getRealValue()).getPackage();
        if(loggerPackage == null) {
            return false;
        }
        return loggerPackage.equals(LOGGER_PROFILING);
    }

    @Override
    protected PrismContainerWrapper<ClassLoggerConfigurationType> createWrapper(PrismContainerValueWrapper<?> parent,
            PrismContainer<ClassLoggerConfigurationType> childContainer, ItemStatus status, WrapperContext ctx) {
        PrismContainer<ClassLoggerConfigurationType> clone = childContainer.clone();
//        clone.setElementName(PROFILING_LOGGER_PATH);
        registry.registerWrapperPanel(PROFILING_LOGGER_PATH, ProfilingClassLoggerPanel.class);
        return new ProfilingClassLoggerContainerWrapperImpl<>(parent, clone, status);
    }

    @Override
    protected <ID extends ItemDefinition<PrismContainer<ClassLoggerConfigurationType>>> List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> createValuesWrapper(
            PrismContainerWrapper<ClassLoggerConfigurationType> itemWrapper, PrismContainer<ClassLoggerConfigurationType> item, WrapperContext context)
            throws SchemaException {
        List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> pvWrappers = new ArrayList<>();

        for (PrismContainerValue<ClassLoggerConfigurationType> pcv : item.getValues()) {
            if(canCreateValueWrapper(pcv)) {
                PrismContainerValueWrapper<ClassLoggerConfigurationType> valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
                pvWrappers.add(valueWrapper);
            }
        }

        if (pvWrappers.isEmpty()) {
            PrismContainerValue<ClassLoggerConfigurationType> prismValue = createNewValue(item);
            PrismContainerValueWrapper<ClassLoggerConfigurationType> valueWrapper =  createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
            valueWrapper.getRealValue().setPackage(LOGGER_PROFILING);
            pvWrappers.add(valueWrapper);
        }

        return pvWrappers;
    }

    @Override
    public PrismContainerValueWrapper<ClassLoggerConfigurationType> createContainerValueWrapper(PrismContainerWrapper<ClassLoggerConfigurationType> objectWrapper,
            PrismContainerValue<ClassLoggerConfigurationType> objectValue, ValueStatus status, WrapperContext context) {

        ClassLoggerConfigurationType logger = objectValue.getRealValue();
        logger.setPackage(LOGGER_PROFILING);

        return new ProfilingClassLoggerContainerValueWrapperImpl(objectWrapper, objectValue, status);
    }

}
