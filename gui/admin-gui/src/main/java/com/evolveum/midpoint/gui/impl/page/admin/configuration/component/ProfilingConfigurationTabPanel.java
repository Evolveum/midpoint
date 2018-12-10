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
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.gui.impl.model.RealContainerValueFromContainerValueWrapperModel;
import com.evolveum.midpoint.gui.impl.model.ValueWrapperOfSingleValuePropertyFromSingleValueContainerValueWrapperModel;

/**
 * @author skublik
 */

public class ProfilingConfigurationTabPanel extends BasePanel<ContainerWrapper<ProfilingConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ProfilingConfigurationTabPanel.class);
	
	private static final String ID_PROFILING_ENABLED_NOTE = "profilingEnabledNote";
	private static final String ID_PROFILING = "profiling";
	private static final String ID_PROFILING_LOGGER_APPENDERS = "profilingLoggerAppenders";
	private static final String ID_PROFILING_LOGGER_LEVEL = "profilingLoggerLevel";

	public static final String LOGGER_PROFILING = "PROFILING";
	private IModel<ContainerWrapper<LoggingConfigurationType>> loggingModel;
	
	
    public ProfilingConfigurationTabPanel(String id, IModel<ContainerWrapper<ProfilingConfigurationType>> profilingModel, IModel<ContainerWrapper<LoggingConfigurationType>> loggingModel) {
        super(id, profilingModel);
        this.loggingModel = loggingModel;
    }

    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }
    
    private IModel<ContainerWrapper<LoggingConfigurationType>> getLoggingModel() {
    	return loggingModel;
    }
    
    private IModel<ContainerWrapper<ProfilingConfigurationType>> getProfilingModel() {
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
    	
    	PrismContainerPanel<ProfilingConfigurationType> profilingPanel = new PrismContainerPanel<ProfilingConfigurationType>(ID_PROFILING, getProfilingModel(), true, new Form<>("form"), null, getPageBase());
    	add(profilingPanel);
    	
    	IModel<ContainerWrapper<ClassLoggerConfigurationType>> loggerModel = new Model<>(getLoggingModel().getObject()
			    .findContainerWrapper(
					    ItemPath.create(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER)));
    	
    	ContainerValueWrapper<ClassLoggerConfigurationType> profilingLogger = null;
    	
    	for (ContainerValueWrapper<ClassLoggerConfigurationType> logger : loggerModel.getObject().getValues()) {
			if (LOGGER_PROFILING.equals(new RealContainerValueFromContainerValueWrapperModel<>(logger).getObject().getPackage())) {
				profilingLogger = logger;
				continue;
			}
		}
    	
    	if(profilingLogger == null) {
    		profilingLogger = WebModelServiceUtils.createNewItemContainerValueWrapper(getPageBase(), loggerModel);
		    new RealContainerValueFromContainerValueWrapperModel<>(profilingLogger).getObject().setPackage(LOGGER_PROFILING);
    	}
    	
    	ValueWrapperOfSingleValuePropertyFromSingleValueContainerValueWrapperModel<LoggingLevelType, ClassLoggerConfigurationType> level = new ValueWrapperOfSingleValuePropertyFromSingleValueContainerValueWrapperModel<>(profilingLogger, ClassLoggerConfigurationType.F_LEVEL);
    	
    	DropDownFormGroup<ProfilingLevel> dropDownProfilingLevel = new DropDownFormGroup<>(ID_PROFILING_LOGGER_LEVEL, new Model<ProfilingLevel>() {

					private static final long serialVersionUID = 1L;
					
					private PropertyModel<LoggingLevelType> levelModel = new PropertyModel<LoggingLevelType>(level, "value.value");
					
					@Override
					public ProfilingLevel getObject() {
					return ProfilingLevel.fromLoggerLevelType(levelModel.getObject());
					}
					
					@Override
					public void setObject(ProfilingLevel object) {
						super.setObject(object);
						levelModel.setObject(ProfilingLevel.toLoggerLevelType(object));
					}
    		
    			}, WebComponentUtil.createReadonlyModelFromEnum(ProfilingLevel.class), new EnumChoiceRenderer<>(this), createStringResource("LoggingConfigPanel.subsystem.level"), 
    			"", getInputCssClass(), false, true);
        add(dropDownProfilingLevel);
        
        PropertyWrapper appenders = (PropertyWrapper)profilingLogger.findPropertyWrapperByName(ClassLoggerConfigurationType.F_APPENDER);
        appenders.setPredefinedValues(WebComponentUtil.createAppenderChoices(getPageBase()));
        
        PrismPropertyPanel<PropertyWrapper> profilingLoggerLevel = new PrismPropertyPanel<PropertyWrapper>(ID_PROFILING_LOGGER_APPENDERS, new Model(appenders), new Form<>("form"), itemWrapper -> getAppendersPanelVisibility(itemWrapper.getPath()), getPageBase());
        
    	add(profilingLoggerLevel);
    	
    }
    
    private ItemVisibility getAppendersPanelVisibility(ItemPath pathToCheck) {
		return ItemVisibility.VISIBLE;
	}
    
    private String getInputCssClass() {
        return "col-xs-10";
    }
}

