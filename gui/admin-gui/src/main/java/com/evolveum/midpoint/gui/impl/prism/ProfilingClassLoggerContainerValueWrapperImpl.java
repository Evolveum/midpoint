/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 */
public class ProfilingClassLoggerContainerValueWrapperImpl extends PrismContainerValueWrapperImpl<ClassLoggerConfigurationType> {

    private static final long serialVersionUID = 1L;

    public ProfilingClassLoggerContainerValueWrapperImpl(PrismContainerWrapper<ClassLoggerConfigurationType> parent, PrismContainerValue<ClassLoggerConfigurationType> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }

    @Override
    public String getDisplayName() {
        return ColumnUtils.createStringResource("LoggingConfigPanel.profiling.entryExit").getString();
    }

    @Override
    public PrismContainerValue<ClassLoggerConfigurationType> getValueToAdd() throws SchemaException {
        PrismProperty<Object> level = getNewValue().findProperty(ClassLoggerConfigurationType.F_LEVEL);
        if(level != null && !level.isEmpty() && level.getRealValue() != null) {
            return super.getValueToAdd();
        }
        return null;
    }
}
