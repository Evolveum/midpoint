/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.factory.ProfilingClassLoggerWrapperFactoryImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 *
 */
public class ProfilingClassLoggerContainerWrapperImpl<C extends Containerable> extends PrismContainerWrapperImpl<C>{

	private static final long serialVersionUID = 1L;
	
	public ProfilingClassLoggerContainerWrapperImpl(PrismContainerValueWrapper<?> parent, PrismContainer<C> item, ItemStatus status) {
		super(parent, item, status);
	}
	
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
}
