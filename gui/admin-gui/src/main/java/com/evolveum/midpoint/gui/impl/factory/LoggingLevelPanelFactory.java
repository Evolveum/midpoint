/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.gui.impl.factory;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ProfilingLevel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 *
 */
@Component
public class LoggingLevelPanelFactory implements GuiComponentFactory<PrismPropertyPanelContext<LoggingLevelType>>, Serializable {

	private static final long serialVersionUID = 1L;

	@Autowired private transient GuiComponentRegistry registry;

	@Override
	public Integer getOrder() {
		return Integer.MAX_VALUE-1;
	}
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}
	
	@Override
	public <IW extends ItemWrapper> boolean match(IW wrapper) {
		if(!wrapper.getPath().removeIds().equivalent(
				ItemPath.create(
						SystemConfigurationType.F_LOGGING,
						LoggingConfigurationType.F_CLASS_LOGGER,
						ClassLoggerConfigurationType.F_LEVEL))) {
			return false;
		}
		ClassLoggerConfigurationType logger = (ClassLoggerConfigurationType)wrapper.getParent().getRealValue();
		return logger != null && logger.getPackage() != null
				&& logger.getPackage().equals(ProfilingClassLoggerWrapperFactoryImpl.LOGGER_PROFILING);
	}

	@Override
	public Panel createPanel(PrismPropertyPanelContext<LoggingLevelType> panelCtx) {
		DropDownChoicePanel<ProfilingLevel> dropDownProfilingLevel = new DropDownChoicePanel<>(panelCtx.getComponentId(), new Model<ProfilingLevel>() {

			private static final long serialVersionUID = 1L;
			
			
			@Override
			public ProfilingLevel getObject() {
			return ProfilingLevel.fromLoggerLevelType(panelCtx.getRealValueModel().getObject());
			}
			
			@Override
			public void setObject(ProfilingLevel object) {
				super.setObject(object);
				panelCtx.getRealValueModel().setObject(ProfilingLevel.toLoggerLevelType(object));
			}
	
		}, WebComponentUtil.createReadonlyModelFromEnum(ProfilingLevel.class), new EnumChoiceRenderer<>(), true);

		
		return dropDownProfilingLevel;
	}
}
