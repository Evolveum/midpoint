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
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.EditableLinkPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.EditablePropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.model.PropertyWrapperFromContainerValueWrapperModel;
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
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerHeaderPanel;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class LoggingConfigurationTabPanel extends BasePanel<ContainerWrapper<LoggingConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(LoggingConfigurationTabPanel.class);
	
	private static final String ID_LOGGING = "logging";
	private static final String ID_APPENDERS = "appenders";
    private static final String ID_LOGGERS = "loggers";
    private static final String ID_AUDITING = "audit";

    public LoggingConfigurationTabPanel(String id, IModel<ContainerWrapper<LoggingConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {
    	PrismContainerPanel<LoggingConfigurationType> loggingPanel = new PrismContainerPanel<LoggingConfigurationType>(ID_LOGGING, getModel(), true, new Form<>("form"), itemWrapper -> getLoggingVisibility(itemWrapper.getPath()), getPageBase());
    	add(loggingPanel);
    	

    	TableId tableIdLoggers = UserProfileStorage.TableId.LOGGING_TAB_LOGGER_TABLE;
    	int itemPerPageLoggers = (int) getPageBase().getItemsPerPage(tableIdLoggers);
    	PageStorage pageStorageLoggers = getPageBase().getSessionStorage().getLoggingConfigurationTabLoggerTableStorage();
    	

    	IModel<ContainerWrapper<ClassLoggerConfigurationType>> loggerModel =
    			new ContainerWrapperFromObjectWrapperModel<ClassLoggerConfigurationType, SystemConfigurationType>(Model.of(getModelObject().getObjectWrapper()), new ItemPath(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_CLASS_LOGGER));

    	
    	MultivalueContainerListPanel<ClassLoggerConfigurationType> loggersMultivalueContainerListPanel = new MultivalueContainerListPanel<ClassLoggerConfigurationType>(ID_LOGGERS, loggerModel,
    			tableIdLoggers, itemPerPageLoggers, pageStorageLoggers) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<ContainerValueWrapper<ClassLoggerConfigurationType>> postSearch(
					List<ContainerValueWrapper<ClassLoggerConfigurationType>> items) {
				for (int i = 0; i < items.size(); i++) {
					ContainerValueWrapper<ClassLoggerConfigurationType> logger = items.get(i);
					if (ProfilingConfigurationTabPanel.LOGGER_PROFILING.equals(((ClassLoggerConfigurationType)logger.getContainerValue().getRealValue()).getPackage())) {
						items.remove(logger);
						continue;
					}
				}
				return items;
			}
			
			@Override
			protected void newItemPerformed(AjaxRequestTarget target) {
				PrismContainerValue<ClassLoggerConfigurationType> newLogger = loggerModel.getObject().getItem().createNewValue();
		        ContainerValueWrapper<ClassLoggerConfigurationType> newLoggerWrapper = getLoggersMultivalueContainerListPanel().createNewItemContainerValueWrapper(newLogger, loggerModel);
		        newLoggerWrapper.setShowEmpty(true, false);
		        newLoggerWrapper.computeStripes();
		        loggerEditPerformed(target, Model.of(newLoggerWrapper), null);
			}
			
			@Override
			protected void initPaging() {
				initLoggerPaging();
			}
			
			@Override
			protected boolean enableActionNewObject() {
				return true;
			}
			
			@Override
			protected ObjectQuery createQuery() {
			   return null;
			}
			
			@Override
			protected List<IColumn<ContainerValueWrapper<ClassLoggerConfigurationType>, String>> createColumns() {
				return initLoggersBasicColumns();
			}

			@Override
			protected void initCustomLayout() {
				
			}

			@Override
			protected void itemPerformedForDefaultAction(AjaxRequestTarget target,
					IModel<ContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
					List<ContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
				loggerEditPerformed(target, rowModel, listItems);
			}

			@Override
			protected List<SearchItemDefinition> initSearchableItems(
					PrismContainerDefinition<ClassLoggerConfigurationType> containerDef) {
				List<SearchItemDefinition> defs = new ArrayList<>();
				
				return defs;
			}
		};
		add(loggersMultivalueContainerListPanel);

		TableId tableIdAppenders = UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE;
    	int itemPerPageAppenders = (int) ((PageBase)LoggingConfigurationTabPanel.this.getPage()).getItemsPerPage(UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE);
    	PageStorage pageStorageAppenders = ((PageBase)LoggingConfigurationTabPanel.this.getPage()).getSessionStorage().getLoggingConfigurationTabAppenderTableStorage();


		IModel<ContainerWrapper<AppenderConfigurationType>> appenderModel =
    			new ContainerWrapperFromObjectWrapperModel<AppenderConfigurationType, SystemConfigurationType>(Model.of(getModelObject().getObjectWrapper()), new ItemPath(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_APPENDER));

    	MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType> appendersMultivalueContainerListPanel = new MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType>(ID_APPENDERS, appenderModel,
    			tableIdAppenders, itemPerPageAppenders, pageStorageAppenders) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<ContainerValueWrapper<AppenderConfigurationType>> postSearch(
					List<ContainerValueWrapper<AppenderConfigurationType>> items) {
				return items;
			}
			
			@Override
			protected void newItemPerformed(AjaxRequestTarget target) {
				newAppendersClickPerformed(target);
			}
			
			@Override
			protected void initPaging() {
				initAppenderPaging(); 
			}
			
			@Override
			protected boolean enableActionNewObject() {
				return true;
			}
			
			@Override
			protected ObjectQuery createQuery() {
			    return null;
			}
			
			@Override
			protected List<IColumn<ContainerValueWrapper<AppenderConfigurationType>, String>> createColumns() {
				return initAppendersBasicColumns();
			}

			@Override
			protected MultivalueContainerDetailsPanel<AppenderConfigurationType> getMultivalueContainerDetailsPanel(
					ListItem<ContainerValueWrapper<AppenderConfigurationType>> item) {
				return LoggingConfigurationTabPanel.this.getAppendersMultivalueContainerDetailsPanel(item);
			}

			@Override
			protected List<SearchItemDefinition> initSearchableItems(
					PrismContainerDefinition<AppenderConfigurationType> containerDef) {
				List<SearchItemDefinition> defs = new ArrayList<>();
				
				return defs;
			}
		};
		add(appendersMultivalueContainerListPanel);
		
		IModel<ContainerWrapper<AuditingConfigurationType>> auditModel =
    			new ContainerWrapperFromObjectWrapperModel<AuditingConfigurationType, SystemConfigurationType>(Model.of(getModelObject().getObjectWrapper()), new ItemPath(SystemConfigurationType.F_LOGGING, LoggingConfigurationType.F_AUDITING));
		PrismContainerPanel<AuditingConfigurationType> auditPanel = new PrismContainerPanel<AuditingConfigurationType>(ID_AUDITING, auditModel, true, new Form<>("form"), null, getPageBase());
    	add(auditPanel);
		setOutputMarkupId(true);
	}
    
    private ItemVisibility getLoggingVisibility(ItemPath pathToCheck) {
    	if(pathToCheck.isSubPathOrEquivalent(new ItemPath(getModelObject().getPath(), LoggingConfigurationType.F_ROOT_LOGGER_APPENDER)) ||
    			pathToCheck.isSubPathOrEquivalent(new ItemPath(getModelObject().getPath(), LoggingConfigurationType.F_ROOT_LOGGER_LEVEL))){
			return ItemVisibility.AUTO;
		}
		return ItemVisibility.HIDDEN;
	}

    
    private List<IColumn<ContainerValueWrapper<ClassLoggerConfigurationType>, String>> initLoggersBasicColumns() {
    	List<IColumn<ContainerValueWrapper<ClassLoggerConfigurationType>, String>> columns = new ArrayList<>();
    	
    	columns.add(new CheckBoxHeaderColumn<>());
		columns.add(new IconColumn<ContainerValueWrapper<ClassLoggerConfigurationType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<ContainerValueWrapper<ClassLoggerConfigurationType>> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE);
					}
				};
			}
		});
		
		columns.add(new EditableLinkPropertyWrapperColumn<ClassLoggerConfigurationType>(createStringResource("LoggingConfigurationTabPanel.loggers.package"), ClassLoggerConfigurationType.F_PACKAGE, getPageBase()) {
			@Override
			public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<ClassLoggerConfigurationType>> rowModel) {
				loggerEditPerformed(target, rowModel, null);
			}
			
			@Override
			public String getCssClass() {
				return " col-md-5 ";
			}
			
		});
		
		columns.add(new EditablePropertyWrapperColumn<ClassLoggerConfigurationType, String>(createStringResource("LoggingConfigurationTabPanel.loggers.level"), ClassLoggerConfigurationType.F_LEVEL, getPageBase()));
		
		columns.add(new EditablePropertyWrapperColumn<ClassLoggerConfigurationType, String>(createStringResource("LoggingConfigurationTabPanel.loggers.appender"), ClassLoggerConfigurationType.F_APPENDER, getPageBase()));
		
		List<InlineMenuItem> menuActionsList = getLoggersMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {
			
			@Override
			public String getCssClass() {
				return " col-md-1 ";
			}
		});
		
        return columns;
	}
    
    private void loggerEditPerformed(AjaxRequestTarget target, IModel<ContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
    		List<ContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
    	if(rowModel != null) {
    		ContainerValueWrapper<ClassLoggerConfigurationType> logger = rowModel.getObject();
        	logger.setSelected(true);
    	} else {
    		for(ContainerValueWrapper<ClassLoggerConfigurationType> logger : listItems) {
    			logger.setSelected(true);
    		}
    	}
        target.add(getLoggersMultivalueContainerListPanel());
    }
    
    
    protected void newAppendersClickPerformed(AjaxRequestTarget target) {
    	MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType> appenders = (MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType>) get(ID_APPENDERS);
    	PrismContainerValue<AppenderConfigurationType> newObjectPolicy = appenders.getModelObject().getItem().createNewValue();
        ContainerValueWrapper<AppenderConfigurationType> newAppenderContainerWrapper = getAppendersMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, appenders.getModel());
        newAppenderContainerWrapper.setShowEmpty(true, false);
        newAppenderContainerWrapper.computeStripes();
        getAppendersMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newAppenderContainerWrapper));
	}
    
    private MultivalueContainerDetailsPanel<AppenderConfigurationType> getAppendersMultivalueContainerDetailsPanel(
			ListItem<ContainerValueWrapper<AppenderConfigurationType>> item) {
    	MultivalueContainerDetailsPanel<AppenderConfigurationType> detailsPanel = new  MultivalueContainerDetailsPanel<AppenderConfigurationType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayNamePanel<AppenderConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
				IModel<AppenderConfigurationType> displayNameModel = new AbstractReadOnlyModel<AppenderConfigurationType>() {

		    		private static final long serialVersionUID = 1L;

					@Override
		    		public AppenderConfigurationType getObject() {
		    			return item.getModelObject().getContainerValue().getValue();
		    		}
		    	};
				return new DisplayNamePanel<AppenderConfigurationType>(displayNamePanelId, displayNameModel);
			}
			
			@Override
			protected void addBasicContainerValuePanel(String idPanel) {
				Form form = new Form<>("form");
		    	ItemPath itemPath = getModelObject().getPath();
		    	IModel<ContainerValueWrapper<AppenderConfigurationType>> model = getModel();
		    	model.getObject().getContainer().setShowOnTopLevel(true);
		    	ContainerValuePanel panel;
		    	if(item.getModelObject().getContainerValue().getValue() instanceof FileAppenderConfigurationType) {

		    		FileAppenderConfigurationType appender = (FileAppenderConfigurationType) item.getModelObject().getContainerValue().getValue();
		    		ContainerWrapperFactory cwf = new ContainerWrapperFactory(getPageBase());
		    		Task task = LoggingConfigurationTabPanel.this.getPageBase().createSimpleTask("create appender");
		    		ContainerWrapper<FileAppenderConfigurationType> wrapper = cwf.createContainerWrapper(item.getModelObject().getContainer().getObjectWrapper(), (PrismContainer<FileAppenderConfigurationType>)appender.asPrismContainerValue().getContainer(), item.getModelObject().getObjectStatus(), 
		    				new ItemPath(FileAppenderConfigurationType.COMPLEX_TYPE), task);
		    		wrapper.setShowOnTopLevel(true);

		    		ContainerValueWrapper<FileAppenderConfigurationType> value = cwf.createContainerValueWrapper(wrapper, (PrismContainerValue<FileAppenderConfigurationType>)appender.asPrismContainerValue(), item.getModelObject().getObjectStatus(), item.getModelObject().getStatus(), new ItemPath(FileAppenderConfigurationType.COMPLEX_TYPE), task);
		    		IModel<ContainerValueWrapper<FileAppenderConfigurationType>> valueModel = new LoadableModel<ContainerValueWrapper<FileAppenderConfigurationType>>(false) {

		    			private static final long serialVersionUID = 1L;

		    			@Override
		    			protected ContainerValueWrapper<FileAppenderConfigurationType> load() {
		    				return value;
		    			}
		    		};
		    		
		    		panel = new ContainerValuePanel<FileAppenderConfigurationType>(idPanel, valueModel, true, form,
		    				itemWrapper -> getBasicTabVisibity(itemWrapper, itemPath), getPageBase());
		    	} else {
		    		panel = new ContainerValuePanel<AppenderConfigurationType>(idPanel, getModel(), true, form,
		    				itemWrapper -> getBasicTabVisibity(itemWrapper, itemPath), getPageBase());
		    	}
		    	add(panel);
			}
			
			@Override
			protected ItemVisibility getBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentPath) {
				return ItemVisibility.VISIBLE;
			}
		};
		return detailsPanel;
	}
    
	private MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType> getAppendersMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType>)get(ID_APPENDERS));
	}
	
	private MultivalueContainerListPanel<ClassLoggerConfigurationType> getLoggersMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanel<ClassLoggerConfigurationType>)get(ID_LOGGERS));
	}

    private void initAppenderPaging() {
    	getPageBase().getSessionStorage().getLoggingConfigurationTabAppenderTableStorage().setPaging(ObjectPaging.createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE)));
    }
    
    private void initLoggerPaging() {
    	getPageBase().getSessionStorage().getLoggingConfigurationTabLoggerTableStorage().setPaging(ObjectPaging.createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE)));
    }
    
    private List<IColumn<ContainerValueWrapper<AppenderConfigurationType>, String>> initAppendersBasicColumns() {
		List<IColumn<ContainerValueWrapper<AppenderConfigurationType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<>());
		columns.add(new IconColumn<ContainerValueWrapper<AppenderConfigurationType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected IModel<String> createIconModel(IModel<ContainerValueWrapper<AppenderConfigurationType>> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					private static final long serialVersionUID = 1L;

					@Override
					public String getObject() {
						return WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE);
					}
				};
			}
		});
		
		columns.add(new LinkColumn<ContainerValueWrapper<AppenderConfigurationType>>(createStringResource("PolicyRulesPanel.nameColumn")){
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<ContainerValueWrapper<AppenderConfigurationType>> rowModel) {
            	AppenderConfigurationType appender = rowModel.getObject().getContainerValue().getValue();
            	String name = appender.getName();
           		if (StringUtils.isBlank(name)) {
            		return createStringResource("AssignmentPanel.noName");
            	}
            	return Model.of(name);
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ContainerValueWrapper<AppenderConfigurationType>> rowModel) {
            	getAppendersMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }
        });
		
		List<InlineMenuItem> menuActionsList = getAppendersMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn<>(menuActionsList, getPageBase()));
		
        return columns;
	}
}

