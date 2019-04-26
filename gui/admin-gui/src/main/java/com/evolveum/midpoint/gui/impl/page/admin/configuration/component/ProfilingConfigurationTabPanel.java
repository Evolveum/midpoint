/*
 * Copyright (c) 2018 Evolveum
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

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;

/**
 * @author skublik
 * @deprecated
 * 
 * rework to use special wrapper - smth like profiling wrapper which will prepare all the predefined values, profiling level etc.
 */
@Deprecated
public class ProfilingConfigurationTabPanel extends BasePanel<PrismContainerWrapper<ProfilingConfigurationType>> {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ProfilingConfigurationTabPanel.class);

	private static final String ID_PROFILING_ENABLED_NOTE = "profilingEnabledNote";
	private static final String ID_PROFILING = "profiling";
	private static final String ID_PROFILING_LOGGER_APPENDERS = "profilingLoggerAppenders";
	private static final String ID_PROFILING_LOGGER_LEVEL = "profilingLoggerLevel";

	public static final String LOGGER_PROFILING = "PROFILING";
	private IModel<PrismContainerWrapper<LoggingConfigurationType>> loggingModel;

	public ProfilingConfigurationTabPanel(String id, IModel<PrismContainerWrapper<ProfilingConfigurationType>> profilingModel,
			IModel<PrismContainerWrapper<LoggingConfigurationType>> loggingModel) {
		super(id, profilingModel);
		this.loggingModel = loggingModel;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initLayout();
	}

	private IModel<PrismContainerWrapper<LoggingConfigurationType>> getLoggingModel() {
		return loggingModel;
	}

	private IModel<PrismContainerWrapper<ProfilingConfigurationType>> getProfilingModel() {
		return getModel();
	}

	protected void initLayout() {

		WebMarkupContainer profilingEnabledNote = new WebMarkupContainer(ID_PROFILING_ENABLED_NOTE);
		profilingEnabledNote.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !getPageBase().getMidpointConfiguration().isProfilingEnabled();
			}
		});
		add(profilingEnabledNote);
		
		try {
			Panel panel = getPageBase().initPanel(ID_PROFILING, ProfilingConfigurationType.COMPLEX_TYPE, getProfilingModel(), false);
			add(panel);
		} catch (SchemaException e) {
			LOGGER.error("Cannot create profiling panel. Reason: {}", e.getMessage(), e);
			getSession().error("Cannot create profiling panel.");
		}

		PrismContainerWrapperModel<LoggingConfigurationType, ClassLoggerConfigurationType> loggerModel = PrismContainerWrapperModel.fromContainerWrapper(getLoggingModel(), LoggingConfigurationType.F_CLASS_LOGGER);
		
		PrismContainerValueWrapper<ClassLoggerConfigurationType> profilingLogger = null;

		if (loggerModel != null) {

			for (PrismContainerValueWrapper<ClassLoggerConfigurationType> logger : loggerModel.getObject().getValues()) {
				if (LOGGER_PROFILING.equals(
						new ItemRealValueModel<ClassLoggerConfigurationType>(Model.of(logger)).getObject().getPackage())) {
					profilingLogger = logger;
					continue;
				}
			}
		}

		// TODO init new value
		if (profilingLogger == null) {
			WrapperContext context = new WrapperContext(null, null);
			PrismContainerValue<ClassLoggerConfigurationType> newValue = loggerModel.getObject().createValue();
			try {
				getPageBase().createValueWrapper(loggerModel.getObject(), newValue, ValueStatus.ADDED, context);
			} catch (SchemaException e) {
				LOGGER.error("Cannot create new value for profiling. Reason: {}", e.getMessage(), e);
				getSession().error("Cannot create new value for profiling.");
			}
		}

		PrismPropertyWrapperModel<ClassLoggerConfigurationType, String> appenders = PrismPropertyWrapperModel.fromContainerWrapper(loggerModel,
				ClassLoggerConfigurationType.F_APPENDER);
		// TODO special wrapper with loading predefined values.
//		 appenders.getObject().setPredefinedValues(WebComponentUtil.createAppenderChoices(getPageBase()));
		 
		 try {
			Panel profilingLoggerLevel = getPageBase().initPanel(ID_PROFILING_LOGGER_APPENDERS, DOMUtil.XSD_STRING, appenders, false);
			add(profilingLoggerLevel);
		} catch (SchemaException e) {
			LOGGER.error("Cannot create panel for profiling logger appenders: ", e.getMessage(), e);
			getSession().error("Cannot create panel for profiling logger appenders.");
		}

	}

	private ItemVisibility getAppendersPanelVisibility(ItemPath pathToCheck) {
		return ItemVisibility.VISIBLE;
	}

	private String getInputCssClass() {
		return "col-xs-10";
	}
}
