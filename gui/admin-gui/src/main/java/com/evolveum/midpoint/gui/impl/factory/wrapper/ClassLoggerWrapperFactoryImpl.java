/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 */
@Component
public class ClassLoggerWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C>{

    @Override
    public boolean match(ItemDefinition<?> def) {
        return false;
    }

    @Override
    protected boolean canCreateValueWrapper(PrismContainerValue<C> value) {
        if(value == null || value.getRealValue() == null) {
            return true;
        }
        String loggerPackage = ((ClassLoggerConfigurationType)value.getRealValue()).getPackage();
        if(loggerPackage == null) {
            return true;
        }
        return !loggerPackage.equals(ProfilingClassLoggerWrapperFactoryImpl.LOGGER_PROFILING);
    }
}
