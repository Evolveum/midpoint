/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.factory.wrapper.ProfilingClassLoggerWrapperFactoryImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 *
 */
public class ProfilingClassLoggerContainerWrapperImpl<C extends Containerable> extends PrismContainerWrapperImpl<C> {

    private static final long serialVersionUID = 1L;

    public ProfilingClassLoggerContainerWrapperImpl(PrismContainerValueWrapper<?> parent, PrismContainer<C> item, ItemStatus status) {
        super(parent, item, status);
    }

    @NotNull
    @Override
    public ItemName getItemName() {
        return ItemName.fromQName(ProfilingClassLoggerWrapperFactoryImpl.PROFILING_LOGGER_PATH);
    }

    @Override
    public String getDisplayName() {
        return ColumnUtils.createStringResource("LoggingConfigPanel.profiling.entryExit").getString();
    }

    @Override
    public boolean isMultiValue() {
        return false;
    }

    @Override
    protected ItemPath getDeltaPathForStatus(ItemStatus status) {
        return ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER);
    }

    @Override
    public <D extends ItemDelta<? extends PrismValue,? extends ItemDefinition>> Collection<D> getDelta()
            throws SchemaException {
        Collection<D> deltas = super.getDelta();
        if (!isChanged()) {
            deltas = new ArrayList<>();
        }
        return deltas;
    }

    private boolean isChanged() {
        try {
            PrismContainerValueWrapper<C> value = getValue();

            if (value != null) {
                PrismPropertyWrapper<LoggingLevelType> level = value.findProperty(ClassLoggerConfigurationType.F_LEVEL);
                Collection deltas = level.getDelta();
                if (deltas != null && !deltas.isEmpty()) {
                    return true;
                }

                PrismPropertyWrapper<LoggingLevelType> appender = value.findProperty(ClassLoggerConfigurationType.F_APPENDER);
                if (!appender.getValues().isEmpty()) {
                    if (appender.getValues().get(0).getRealValue() != null) {
                        return true;
                    }
                }
            }
        } catch (SchemaException e) {
            return false;
        }

        return false;
    }

    @Override
    public @NotNull ContainerDelta<C> createEmptyDelta(ItemPath path) {
        path = ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER);
        return getItemDefinition().createEmptyDelta(path);
    }
}
