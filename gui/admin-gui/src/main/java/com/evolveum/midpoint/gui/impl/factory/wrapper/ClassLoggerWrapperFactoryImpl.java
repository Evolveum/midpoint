/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
