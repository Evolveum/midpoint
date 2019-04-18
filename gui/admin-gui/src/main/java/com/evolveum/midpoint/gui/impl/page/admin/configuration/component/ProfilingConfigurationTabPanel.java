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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;

/**
 * @author skublik
 */

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

		PrismContainerPanel<ProfilingConfigurationType> profilingPanel = new PrismContainerPanel<>(ID_PROFILING,
				getProfilingModel());
		// PrismContainerPanelOld<ProfilingConfigurationType> profilingPanel =
		// new PrismContainerPanelOld<ProfilingConfigurationType>(ID_PROFILING,
		// getProfilingModel(), new Form<>("form"), null);
		add(profilingPanel);

		IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> loggerModel;
		try {
			loggerModel = new Model<>(
					getLoggingModel().getObject().findContainer(ItemPath.create(LoggingConfigurationType.F_CLASS_LOGGER)));
		} catch (SchemaException e) {
			LOGGER.error("Cannot find class logger container: {}", e.getMessage(), e);
			loggerModel = null;
		}

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
			// ItemWrapperFactory<?, ?> factory =
			// getPageBase().getRegistry().findWrapperFactory(loggerModel.getObject().getDefinition());
			// WrapperContext context = new WrapperContext(null, null);
			//// factory.createValueWrapper(loggerModel, value,
			// ValueStatus.ADDED, context);
			// profilingLogger =
			// WebModelServiceUtils.createNewItemContainerValueWrapper(getPageBase(),
			// loggerModel);
			// profilingLogger.setShowEmpty(true, true);
			// loggerModel.getObject().getValues().add(profilingLogger);
			// new
			// ItemRealValueModel<ClassLoggerConfigurationType>(profilingLogger).getObject().setPackage(LOGGER_PROFILING);
		}

		// TODO: propery/reserence/container models

		// PropertyOrReferenceWrapperFromContainerModel<ClassLoggerConfigurationType>
		// property = new
		// PropertyOrReferenceWrapperFromContainerModel<>(profilingLogger,
		// ClassLoggerConfigurationType.F_LEVEL);
		// DropDownFormGroup<ProfilingLevel> dropDownProfilingLevel = new
		// DropDownFormGroup<>(ID_PROFILING_LOGGER_LEVEL, new
		// Model<ProfilingLevel>() {
		//
		// private static final long serialVersionUID = 1L;
		//
		// private PropertyModel<LoggingLevelType> levelModel = new
		// PropertyModel<LoggingLevelType>(property, "values[0].value.value");
		//
		// @Override
		// public ProfilingLevel getObject() {
		// return ProfilingLevel.fromLoggerLevelType(levelModel.getObject());
		// }
		//
		// @Override
		// public void setObject(ProfilingLevel object) {
		// super.setObject(object);
		// levelModel.setObject(ProfilingLevel.toLoggerLevelType(object));
		// }
		//
		// },
		// WebComponentUtil.createReadonlyModelFromEnum(ProfilingLevel.class),
		// new EnumChoiceRenderer<>(this),
		// createStringResource("LoggingConfigPanel.subsystem.level"),
		// "", getInputCssClass(), false, true);
		// add(dropDownProfilingLevel);

		PrismPropertyWrapperModel<ClassLoggerConfigurationType, String> appenders = PrismPropertyWrapperModel.fromContainerWrapper(loggerModel,
				ClassLoggerConfigurationType.F_APPENDER);
		// TODO special wrapper with loading predefined values.
		// PropertyWrapper appenders =
		// (PropertyWrapper)profilingLogger.findProperty(ClassLoggerConfigurationType.F_APPENDER);
		// appenders.setPredefinedValues(WebComponentUtil.createAppenderChoices(getPageBase()));

		PrismPropertyPanel<String> profilingLoggerLevel = new PrismPropertyPanel<String>(ID_PROFILING_LOGGER_APPENDERS,
				appenders);
		// PrismPropertyPanel<PropertyWrapper> profilingLoggerLevel = new
		// PrismPropertyPanel<PropertyWrapper>(ID_PROFILING_LOGGER_APPENDERS,
		// new Model(appenders), new Form<>("form"), itemWrapper ->
		// getAppendersPanelVisibility(itemWrapper.getPath()));

		add(profilingLoggerLevel);

	}

	private ItemVisibility getAppendersPanelVisibility(ItemPath pathToCheck) {
		return ItemVisibility.VISIBLE;
	}

	private String getInputCssClass() {
		return "col-xs-10";
	}
}
