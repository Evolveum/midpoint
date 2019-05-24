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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.EditableLinkPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.EditablePrismPropertyColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.LinkPrismPropertyColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.model.PropertyOrReferenceWrapperFromContainerModel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanelOld;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SyslogAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class LoggingConfigurationTabPanel<S extends Serializable> extends BasePanel<PrismContainerWrapper<LoggingConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(LoggingConfigurationTabPanel.class);
	
	private static final String ID_LOGGING = "logging";
	private static final String ID_APPENDERS = "appenders";
    private static final String ID_LOGGERS = "loggers";
    private static final String ID_AUDITING = "audit";
	private static final String ID_NEW_ITEM_BUTTON = "newItemButton";
    private static final String ID_BUTTON_TOOLBAR_FRAGMENT = "buttonToolbarFragment";
	private static final String ID_APPENDERS_CHOICE = "appendersChoice";
	private static final String ID_CHOICE_APPENDER_TYPE_FORM = "choiceAppenderTypeForm";
	

    public LoggingConfigurationTabPanel(String id, IModel<PrismContainerWrapper<LoggingConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {
    	try {
    		getModelObject().setShowOnTopLevel(true);
    		Panel loggingPanel = getPageBase().initItemPanel(ID_LOGGING, LoggingConfigurationType.COMPLEX_TYPE, getModel(), itemWrapper -> getLoggingVisibility(itemWrapper.getPath()));
			add(loggingPanel);
		} catch (SchemaException e) {
			LOGGER.error("Cannot create panel for logging: {}", e.getMessage(), e);
			getSession().error("Cannot create panle for logging");
		}
    	
    	TableId tableIdLoggers = UserProfileStorage.TableId.LOGGING_TAB_LOGGER_TABLE;
    	PageStorage pageStorageLoggers = getPageBase().getSessionStorage().getLoggingConfigurationTabLoggerTableStorage();
    	

    	PrismContainerWrapperModel<LoggingConfigurationType, ClassLoggerConfigurationType> loggerModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), LoggingConfigurationType.F_CLASS_LOGGER);
    	
    	MultivalueContainerListPanel<ClassLoggerConfigurationType, S> loggersMultivalueContainerListPanel =
				new MultivalueContainerListPanel<ClassLoggerConfigurationType, S>(ID_LOGGERS, loggerModel,
    			tableIdLoggers, pageStorageLoggers) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> postSearch(
					List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> items) {
//				for (int i = 0; i < items.size(); i++) {
//					PrismContainerValueWrapper<ClassLoggerConfigurationType> logger = items.get(i);
//					if (ProfilingConfigurationTabPanel.LOGGER_PROFILING.equals(((ClassLoggerConfigurationType)logger.getRealValue()).getPackage())) {
//						items.remove(logger);
//						continue;
//					}
//				}
				return items;
			}
			
			@Override
			protected void newItemPerformed(AjaxRequestTarget target) {
				PrismContainerValue<ClassLoggerConfigurationType> newLogger = loggerModel.getObject().getItem().createNewValue();
		        PrismContainerValueWrapper<ClassLoggerConfigurationType> newLoggerWrapper = getLoggersMultivalueContainerListPanel().createNewItemContainerValueWrapper(newLogger, loggerModel.getObject(), target);
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
			protected List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> createColumns() {
				return initLoggersBasicColumns(loggerModel);
			}

			@Override
			protected void itemPerformedForDefaultAction(AjaxRequestTarget target,
					IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
					List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
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
    	PageStorage pageStorageAppenders = getPageBase().getSessionStorage().getLoggingConfigurationTabAppenderTableStorage();


		PrismContainerWrapperModel<LoggingConfigurationType, AppenderConfigurationType> appenderModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), LoggingConfigurationType.F_APPENDER);

    	MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType, S> appendersMultivalueContainerListPanel =
				new MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType, S>(ID_APPENDERS, appenderModel,
    			tableIdAppenders, pageStorageAppenders) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected List<PrismContainerValueWrapper<AppenderConfigurationType>> postSearch(
					List<PrismContainerValueWrapper<AppenderConfigurationType>> items) {
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
			protected List<IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>> createColumns() {
				return initAppendersBasicColumns(appenderModel);
			}

			@Override
			protected MultivalueContainerDetailsPanel<AppenderConfigurationType> getMultivalueContainerDetailsPanel(
					ListItem<PrismContainerValueWrapper<AppenderConfigurationType>> item) {
				return LoggingConfigurationTabPanel.this.getAppendersMultivalueContainerDetailsPanel(item);
			}

			@Override
			protected List<SearchItemDefinition> initSearchableItems(
					PrismContainerDefinition<AppenderConfigurationType> containerDef) {
				List<SearchItemDefinition> defs = new ArrayList<>();
				
				return defs;
			}
			
			@Override
			protected WebMarkupContainer initButtonToolbar(String contentAreaId) {
		    	Fragment searchContainer = new Fragment(contentAreaId, ID_BUTTON_TOOLBAR_FRAGMENT, LoggingConfigurationTabPanel.this);
		    	
		    	Form appenderTypeForm = new Form(ID_CHOICE_APPENDER_TYPE_FORM);
		    	searchContainer.add(appenderTypeForm);
		    	
		    	AjaxSubmitButton newObjectIcon = new AjaxSubmitButton(ID_NEW_ITEM_BUTTON, new Model<>("<i class=\"fa fa-plus\"></i>")) {

					private static final long serialVersionUID = 1L;
					
					@Override
					protected void onSubmit(AjaxRequestTarget target) {
						newItemPerformed(target);
					}
				};

				newObjectIcon.add(new VisibleEnableBehaviour() {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						return enableActionNewObject();
					}

					@Override
					public boolean isEnabled() {
						return isNewObjectButtonEnabled();
					}
				});
				newObjectIcon.add(AttributeModifier.append("class", createStyleClassModelForNewObjectIcon()));
				appenderTypeForm.add(newObjectIcon);
		    	List<QName> appendersChoicesList = new ArrayList<QName>();
		    	appendersChoicesList.add(FileAppenderConfigurationType.COMPLEX_TYPE);
		    	appendersChoicesList.add(SyslogAppenderConfigurationType.COMPLEX_TYPE);
		    	DropDownChoicePanel<QName> appenderChoise =  new DropDownChoicePanel(ID_APPENDERS_CHOICE, new Model(FileAppenderConfigurationType.COMPLEX_TYPE), Model.of(appendersChoicesList),
		    			new QNameIChoiceRenderer("LoggingConfigurationTabPanel." + ID_APPENDERS_CHOICE));
		    	appenderChoise.setOutputMarkupId(true);
		    	appenderTypeForm.addOrReplace(appenderChoise);
		        return searchContainer;
			}
			
		};
		add(appendersMultivalueContainerListPanel);
		
		IModel<PrismContainerWrapper<AuditingConfigurationType>> auditModel = PrismContainerWrapperModel.fromContainerWrapper(getModel(), LoggingConfigurationType.F_AUDITING);
		try {
			Panel auditPanel = getPageBase().initItemPanel(ID_AUDITING, AuditingConfigurationType.COMPLEX_TYPE, auditModel, null);
			add(auditPanel);
		} catch (SchemaException e) {
			LOGGER.error("Cannot create panel for auditing: {}", e.getMessage(), e);
			getSession().error("Cannot create panel for auditing.");
		}
		setOutputMarkupId(true);
	}
    
    private ItemVisibility getLoggingVisibility(ItemPath pathToCheck) {
    	if(pathToCheck.isSubPathOrEquivalent(ItemPath.create(getModelObject().getPath(), LoggingConfigurationType.F_ROOT_LOGGER_APPENDER)) ||
    			pathToCheck.isSubPathOrEquivalent(ItemPath.create(getModelObject().getPath(), LoggingConfigurationType.F_ROOT_LOGGER_LEVEL))){
    		return ItemVisibility.AUTO;
		}
		return ItemVisibility.HIDDEN;
	}

    
    private List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> initLoggersBasicColumns(IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> loggersModel) {
    	List<IColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>, String>> columns = new ArrayList<>();
    	
    	columns.add(new CheckBoxHeaderColumn<>());
		columns.add(new IconColumn<PrismContainerValueWrapper<ClassLoggerConfigurationType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel) {
				return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));

			}

		});
		
		columns.add(new PrismPropertyWrapperColumn(loggersModel, ClassLoggerConfigurationType.F_PACKAGE, ColumnType.VALUE, getPageBase()) {
			
			@Override
			public String getCssClass() {
				return " col-md-5 ";
			}
			
		});
		columns.add(new PrismPropertyWrapperColumn<>(loggersModel, ClassLoggerConfigurationType.F_LEVEL, ColumnType.VALUE, getPageBase()));
		columns.add(new PrismPropertyWrapperColumn<>(loggersModel, ClassLoggerConfigurationType.F_APPENDER, ColumnType.VALUE, getPageBase()));
		
		List<InlineMenuItem> menuActionsList = getLoggersMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {
			
			@Override
			public String getCssClass() {
				return " col-md-1 ";
			}
		});
		
        return columns;
	}
    
    private void loggerEditPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ClassLoggerConfigurationType>> rowModel,
    		List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> listItems) {
    	if(rowModel != null) {
    		PrismContainerValueWrapper<ClassLoggerConfigurationType> logger = rowModel.getObject();
        	logger.setSelected(true);
    	} else {
    		for(PrismContainerValueWrapper<ClassLoggerConfigurationType> logger : listItems) {
    			logger.setSelected(true);
    		}
    	}
        target.add(getLoggersMultivalueContainerListPanel());
    }
    
    
    protected void newAppendersClickPerformed(AjaxRequestTarget target) {
    	MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType, S> appenders
				= (MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType, S>) get(ID_APPENDERS);
    	DropDownChoicePanel<QName> appendersChoice = (DropDownChoicePanel<QName>) getAppendersMultivalueContainerListPanel().getItemTable().getFooterButtonToolbar().get(createComponentPath(ID_CHOICE_APPENDER_TYPE_FORM ,ID_APPENDERS_CHOICE));
    	PrismContainerValue<AppenderConfigurationType> newObjectPolicy = null;
    	if(QNameUtil.match(appendersChoice.getModel().getObject(), FileAppenderConfigurationType.COMPLEX_TYPE)){
    		newObjectPolicy = new FileAppenderConfigurationType().asPrismContainerValue();
    	} else {
    		newObjectPolicy = new SyslogAppenderConfigurationType().asPrismContainerValue();
    	}
    	newObjectPolicy.setParent(appenders.getModelObject().getItem());
    	newObjectPolicy.setPrismContext(getPageBase().getPrismContext());
    	
    	PrismContainerValueWrapper<AppenderConfigurationType> newAppenderContainerWrapper = getAppendersMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, appenders.getModelObject(), target);
        getAppendersMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newAppenderContainerWrapper));
	}
    
    private MultivalueContainerDetailsPanel<AppenderConfigurationType> getAppendersMultivalueContainerDetailsPanel(
			ListItem<PrismContainerValueWrapper<AppenderConfigurationType>> item) {
    	MultivalueContainerDetailsPanel<AppenderConfigurationType> detailsPanel =
				new  MultivalueContainerDetailsPanel<AppenderConfigurationType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayNamePanel<AppenderConfigurationType> createDisplayNamePanel(String displayNamePanelId) {
				IModel<AppenderConfigurationType> displayNameModel = new IModel<AppenderConfigurationType>() {

		    		private static final long serialVersionUID = 1L;

					@Override
		    		public AppenderConfigurationType getObject() {
		    			return item.getModelObject().getRealValue();
		    		}
		    	};
				return new DisplayNamePanel<AppenderConfigurationType>(displayNamePanelId, displayNameModel);
			}
		};
		return detailsPanel;
	}
    
    private boolean isFileAppender(IModel<AppenderConfigurationType> appender) {
    	if(appender == null || appender.getObject() == null) {
    		return false;
    	}
    	return (appender.getObject() instanceof FileAppenderConfigurationType);
    }
    
    private boolean isSyslogAppender(IModel<AppenderConfigurationType> appender) {
    	if(appender == null || appender.getObject() == null) {
    		return false;
    	}
    	return (appender.getObject() instanceof SyslogAppenderConfigurationType);
    }
    
    private String getInputCssClass() {
        return "col-xs-10";
    }
    
    private Label createHeader(String id, String displayName) {
	    if (StringUtils.isEmpty(displayName)) {
	    	displayName = "displayName.not.set";
	    }
	    StringResourceModel headerLabelModel = createStringResource(displayName);
	    Label header = new Label(id, headerLabelModel);
	    header.add(AttributeAppender.prepend("class", "prism-title pull-left"));
	    return header;
	}
    
	private MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType, S> getAppendersMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanelWithDetailsPanel<AppenderConfigurationType, S>)get(ID_APPENDERS));
	}
	
	private MultivalueContainerListPanel<ClassLoggerConfigurationType, S> getLoggersMultivalueContainerListPanel(){
		return ((MultivalueContainerListPanel<ClassLoggerConfigurationType, S>)get(ID_LOGGERS));
	}

    private void initAppenderPaging() {
    	getPageBase().getSessionStorage().getLoggingConfigurationTabAppenderTableStorage().setPaging(
    			getPrismContext().queryFactory().createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE)));
    }
    
    private void initLoggerPaging() {
    	getPageBase().getSessionStorage().getLoggingConfigurationTabLoggerTableStorage().setPaging(
    			getPrismContext().queryFactory().createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.LOGGING_TAB_APPENDER_TABLE)));
    }
    
    private List<IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>> initAppendersBasicColumns(IModel<PrismContainerWrapper<AppenderConfigurationType>> appenderModel) {
		List<IColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>> columns = new ArrayList<>();

		columns.add(new CheckBoxHeaderColumn<>());
		columns.add(new IconColumn<PrismContainerValueWrapper<AppenderConfigurationType>>(Model.of("")) {

			private static final long serialVersionUID = 1L;

			@Override
			protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AppenderConfigurationType>> rowModel) {
				return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));
			}
		});
		
		columns.add(new PrismPropertyWrapperColumn<AppenderConfigurationType, String>(appenderModel, AppenderConfigurationType.F_NAME, ColumnType.LINK, getPageBase()) {
		
			@Override
			protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AppenderConfigurationType>> rowModel) {
				getAppendersMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
			}
			
		});
		
		columns.add(new PrismPropertyWrapperColumn<AppenderConfigurationType, String>(appenderModel, AppenderConfigurationType.F_PATTERN, ColumnType.VALUE, getPageBase()) {
			@Override
			public String getCssClass() {
				return " col-md-5 ";
			}
		});
		
		columns.add(new AbstractColumn<PrismContainerValueWrapper<AppenderConfigurationType>, String>(createStringResource("LoggingConfigurationTabPanel.appender.typeColumn")){
            private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AppenderConfigurationType>>> item, String componentId,
									 final IModel<PrismContainerValueWrapper<AppenderConfigurationType>> rowModel) {
				ItemRealValueModel<AppenderConfigurationType> appender = 
            			new ItemRealValueModel<>(rowModel);
            	String type = "";
            	if(appender != null && appender.getObject() instanceof FileAppenderConfigurationType) {
            		type = "File appender";
            	} else if(appender != null && appender.getObject() instanceof SyslogAppenderConfigurationType) {
            		type = "Syslog appender";
            	}
				item.add(new Label(componentId, Model.of(type)));
			}
        });
		
		List<InlineMenuItem> menuActionsList = getAppendersMultivalueContainerListPanel().getDefaultMenuActions();
		columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {
			@Override
			public String getCssClass() {
				return " col-md-1 ";
			}
		});
		
        return columns;
	}
}

