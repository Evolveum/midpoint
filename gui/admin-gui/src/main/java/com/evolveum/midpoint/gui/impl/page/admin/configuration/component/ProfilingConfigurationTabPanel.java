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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.EditableLinkPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.EditablePropertyWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerHeaderPanel;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingLevel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProfilingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class ProfilingConfigurationTabPanel extends BasePanel<ContainerWrapper<ProfilingConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ProfilingConfigurationTabPanel.class);
	
	private static final String ID_PROFILING = "profiling";
	private static final String ID_PROFILING_LOGGER_APPENDERS = "profilingLoggerAppenders";
	private static final String ID_PROFILING_LOGGER_LEVEL = "profilingLoggerLevel";
//	private static final String ID_APPENDERS_HEADER = "appendersHeader";
//	private static final String ID_APPENDERS = "appenders";
//	private static final String ID_LOGGERS_HEADER = "loggersHeader";
//    private static final String ID_LOGGERS = "loggers";
//    private static final String ID_AUDITING = "audit";

	public static final String LOGGER_PROFILING = "PROFILING";
	
    public ProfilingConfigurationTabPanel(String id, IModel<ContainerWrapper<ProfilingConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {
    	PrismContainerPanel<ProfilingConfigurationType> profilingPanel = new PrismContainerPanel<ProfilingConfigurationType>(ID_PROFILING, getModel(), true, new Form<>("form"), null, getPageBase());
    	add(profilingPanel);
    	
    	ObjectWrapper<SystemConfigurationType> systemConf= WebModelServiceUtils.loadSystemConfigurationAsObjectWrapper(getPageBase());
//    	ContainerWrapperFromObjectWrapperModel<LoggingConfigurationType, SystemConfigurationType> modelLoggingConfig = new ContainerWrapperFromObjectWrapperModel<>(new Model<ObjectWrapper<SystemConfigurationType>>(systemConf), 
//    			new ItemPath(SystemConfigurationType.F_LOGGING));
    	IModel<ContainerWrapper<ClassLoggerConfigurationType>> loggerModel =
    			new ContainerWrapperFromObjectWrapperModel<ClassLoggerConfigurationType, SystemConfigurationType>(Model.of(systemConf), new ItemPath(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER));
    	
    	ContainerValueWrapper<ClassLoggerConfigurationType> profilingLogger = null;
    	
    	for (ContainerValueWrapper<ClassLoggerConfigurationType> logger : loggerModel.getObject().getValues()) {
			if (LOGGER_PROFILING.equals(((ClassLoggerConfigurationType)logger.getContainerValue().getRealValue()).getPackage())) {
				profilingLogger = logger;
				continue;
			}
		}
    	
    	if(profilingLogger == null) {
    		profilingLogger = WebModelServiceUtils.createNewItemContainerValueWrapper(getPageBase(), loggerModel);
    		((ClassLoggerConfigurationType)profilingLogger.getContainerValue().getRealValue()).setPackage(LOGGER_PROFILING);
    	}
    	ClassLoggerConfigurationType realValueLogger = ((ClassLoggerConfigurationType)profilingLogger.getContainerValue().getRealValue());
    	
    	ProfilingLevel profilingLevel = ProfilingLevel.fromLoggerLevelType(realValueLogger.getLevel());
    	
    	DropDownFormGroup<ProfilingLevel> dropDownProfilingLevel = new DropDownFormGroup<>(ID_PROFILING_LOGGER_LEVEL, new Model<ProfilingLevel>(profilingLevel) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void setObject(ProfilingLevel object) {
						super.setObject(object);
						realValueLogger.setLevel(ProfilingLevel.toLoggerLevelType(object));
					}
    		
    			}, WebComponentUtil.createReadonlyModelFromEnum(ProfilingLevel.class), new EnumChoiceRenderer<>(this), createStringResource("LoggingConfigPanel.subsystem.level"), 
    			"", getInputCssClass(), false);
        add(dropDownProfilingLevel);
        
        PropertyWrapper appenders = (PropertyWrapper)profilingLogger.findPropertyWrapper(ClassLoggerConfigurationType.F_APPENDER);
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

