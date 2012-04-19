/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingLevel;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FileAppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SubSystemLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;

/**
 * @author lazyman
 */
public class PageLogging extends PageAdminConfiguration {

    private LoadableModel<LoggingDto> model;

    public PageLogging() {
        model = new LoadableModel<LoggingDto>(false) {

            @Override
            protected LoggingDto load() {
                return initLoggingModel();
            }
        };
        initLayout();
    }

    private LoggingDto initLoggingModel() {
        LoggingDto dto = null;

        OperationResult result = new OperationResult("Get model");
        try {
            PrismObject<SystemConfigurationType> config = getModelService().getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);
            SystemConfigurationType systemConfiguration = config.asObjectable();
            LoggingConfigurationType logging = systemConfiguration.getLogging();
            dto = new LoggingDto(logging);
        } catch (Exception ex) {
            ex.printStackTrace();
            //todo implement
        }

        if (dto == null) {
            dto = new LoggingDto();
        }

        return dto;
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        initRoot(mainForm);

        Accordion accordion = new Accordion("accordion");
        accordion.setMultipleSelect(true);
        accordion.setOpenedPanel(0);
        mainForm.add(accordion);

        AccordionItem loggers = new AccordionItem("loggers", createStringResource("pageLogging.loggers"));
        accordion.getBodyContainer().add(loggers);
        initLoggers(loggers);

        AccordionItem appenders = new AccordionItem("appenders", createStringResource("pageLogging.appenders"));
        accordion.getBodyContainer().add(appenders);
        initAppenders(appenders);

        initAudit(mainForm);

        initButtons(mainForm);
    }

    private void initLoggers(AccordionItem loggers) {
        List<IColumn<LoggerConfiguration>> columns = new ArrayList<IColumn<LoggerConfiguration>>();

        IColumn column = new CheckBoxHeaderColumn<LoggerConfiguration>();
        columns.add(column);

        column = new LinkColumn<LoggerConfiguration>(createStringResource("pageLogging.classPackageSubsystem"), "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                onLoggerClick(target, rowModel);
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageLogging.loggersLevel"), "level"));
        columns.add(new PropertyColumn(createStringResource("pageLogging.loggersAppender"), "appenders") {

            @Override
            protected IModel<String> createLabelModel(final IModel rowModel) {
                return new LoadableModel<String>() {

                    @Override
                    protected String load() {
                        LoggerConfiguration config = (LoggerConfiguration) rowModel.getObject();

                        StringBuilder builder = new StringBuilder();
                        for (String appender : config.getAppenders()) {
                            if (config.getAppenders().indexOf(appender) != 0) {
                                builder.append(", ");
                            }
                            builder.append(appender);
                        }

                        return builder.toString();
                    }
                };
            }
        });

        ISortableDataProvider<LoggerConfiguration> provider = new ListDataProvider<LoggerConfiguration>(
                new PropertyModel<List<LoggerConfiguration>>(model, "loggers"));
        TablePanel table = new TablePanel<LoggerConfiguration>("loggersTable", provider, columns);
        table.setShowPaging(false);
        table.setTableCssClass("autowidth");
        loggers.getBodyContainer().add(table);

        AjaxLinkButton addLogger = new AjaxLinkButton("addLogger",
                createStringResource("pageLogging.button.addLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addLoggerPerformed(target);
            }
        };
        loggers.getBodyContainer().add(addLogger);

        AjaxLinkButton deleteLogger = new AjaxLinkButton("deleteLogger",
                createStringResource("pageLogging.button.deleteLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteLoggerPerformed(target);
            }
        };
        loggers.getBodyContainer().add(deleteLogger);

        initSubsystem(loggers);
    }

    private void initSubsystem(AccordionItem loggers) {
        IChoiceRenderer<ProfilingLevel> renderer = new IChoiceRenderer<ProfilingLevel>() {

            @Override
            public Object getDisplayValue(ProfilingLevel object) {
                return new StringResourceModel(object.getLocalizationKey(),
                        PageLogging.this, null).getString();
            }

            @Override
            public String getIdValue(ProfilingLevel object, int index) {
                return object.name();
            }
        };

        DropDownChoice<ProfilingLevel> subsystemLevel = createComboBox("subsystemLevel",
                new PropertyModel<ProfilingLevel>(model, "subsystemLevel"), createSubsystemLevelModel(), renderer);
        loggers.getBodyContainer().add(subsystemLevel);

        DropDownChoice<String> subsystemAppender = createComboBox("subsystemAppender",
                new PropertyModel<String>(model, "subsystemAppender"), createAppendersListModel());
        loggers.getBodyContainer().add(subsystemAppender);
    }

    private IModel<List<ProfilingLevel>> createSubsystemLevelModel() {
        return new AbstractReadOnlyModel<List<ProfilingLevel>>() {

            @Override
            public List<ProfilingLevel> getObject() {
                List<ProfilingLevel> levels = new ArrayList<ProfilingLevel>();
                Collections.addAll(levels, ProfilingLevel.values());

                return levels;
            }
        };
    }

    private void initAppenders(AccordionItem appenders) {
        List<IColumn<AppenderConfiguration>> columns = new ArrayList<IColumn<AppenderConfiguration>>();

        IColumn column = new CheckBoxHeaderColumn<LoggerConfiguration>();
        columns.add(column);

        column = new LinkColumn<LoggerConfiguration>(createStringResource("pageLogging.appenders.name"), "name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                onLoggerClick(target, rowModel);
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.pattern"), "pattern"));
        columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.filePath"), "filePath"));
        columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.filePattern"), "filePattern"));
        columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.maxHistory"), "maxHistory"));
        columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.maxFileSize"), "maxFileSize"));
        columns.add(new CheckBoxColumn(createStringResource("pageLogging.appenders.appending"), "appending"));

        ISortableDataProvider<AppenderConfiguration> provider = new ListDataProvider<AppenderConfiguration>(
                new PropertyModel<List<AppenderConfiguration>>(model, "appenders"));
        TablePanel table = new TablePanel<AppenderConfiguration>("appendersTable", provider, columns);
        table.setShowPaging(false);
        appenders.getBodyContainer().add(table);

        AjaxLinkButton addAppender = new AjaxLinkButton("addAppender",
                createStringResource("pageLogging.button.addAppender")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addAppenderPerformed(target);
            }
        };
        appenders.getBodyContainer().add(addAppender);

        AjaxLinkButton deleteAppender = new AjaxLinkButton("deleteAppender",
                createStringResource("pageLogging.button.deleteAppender")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAppenderPerformed(target);
            }
        };
        appenders.getBodyContainer().add(deleteAppender);
    }

    private void initRoot(final Form mainForm) {
        DropDownChoice<LoggingLevelType> rootLevel = createComboBox("rootLevel",
                new PropertyModel<LoggingLevelType>(model, "rootLevel"), createLoggingLevelModel());
        mainForm.add(rootLevel);

        DropDownChoice<String> rootAppender = createComboBox("rootAppender",
                new PropertyModel<String>(model, "rootAppender"), createAppendersListModel());
        mainForm.add(rootAppender);

        DropDownChoice<LoggingLevelType> midPointLevel = createComboBox("midPointLevel",
                new PropertyModel<LoggingLevelType>(model, "midPointLevel"), createLoggingLevelModel());
        mainForm.add(midPointLevel);

        DropDownChoice<String> midPointAppender = createComboBox("midPointAppender",
                new PropertyModel<String>(model, "midPointAppender"), createAppendersListModel());
        mainForm.add(midPointAppender);
    }

    private void initButtons(final Form mainForm) {
        AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
                createStringResource("pageLogging.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
            	LoggingConfigurationType configuration = new LoggingConfigurationType();
            	
            	
            	//System.out.println(model.getObject().getRootLevel());
                //todo implement
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                //todo implement
            }
        };
        mainForm.add(saveButton);

        AjaxLinkButton resetButton = new AjaxLinkButton("resetButton",
                createStringResource("pageLogging.button.reset")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                model.reset();
                target.add(mainForm);
                target.appendJavaScript("init();");
            }
        };
        mainForm.add(resetButton);

        AjaxLinkButton advancedButton = new AjaxLinkButton("advancedButton",
                createStringResource("pageLogging.button.advanced")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                LoggingDto dto = PageLogging.this.model.getObject();
                dto.setAdvanced(!dto.isAdvanced());

                target.add(mainForm);
            }
        };
        mainForm.add(advancedButton);
    }

    private void initAudit(Form mainForm) {
        CheckBox auditLog = new CheckBox("auditLog", new PropertyModel<Boolean>(model, "auditLog"));
        mainForm.add(auditLog);

        CheckBox auditDetails = new CheckBox("auditDetails", new PropertyModel<Boolean>(model, "auditDetails"));
        mainForm.add(auditDetails);

        DropDownChoice<String> auditAppender = createComboBox("auditAppender",
                new PropertyModel<String>(model, "auditAppender"), createAppendersListModel());
        mainForm.add(auditAppender);
    }

    private <T> DropDownChoice<T> createComboBox(String id, IModel<T> choice, IModel<List<T>> choices) {
        return new DropDownChoice<T>(id,
                choice, choices) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
    }

    private <T> DropDownChoice<T> createComboBox(String id, IModel<T> choice, IModel<List<T>> choices,
            IChoiceRenderer renderer) {
        return new DropDownChoice<T>(id,
                choice, choices, renderer) {

            @Override
            protected CharSequence getDefaultChoice(String selectedValue) {
                return "";
            }
        };
    }

    private IModel<List<String>> createAppendersListModel() {
        return new AbstractReadOnlyModel<List<String>>() {
            @Override
            public List<String> getObject() {
                List<String> list = new ArrayList<String>();

                LoggingDto dto = model.getObject();
                for (AppenderConfiguration appender : dto.getAppenders()) {
                    list.add(appender.getName());
                }

                return list;
            }
        };
    }

    private IModel<List<LoggingLevelType>> createLoggingLevelModel() {
        return new AbstractReadOnlyModel<List<LoggingLevelType>>() {

            @Override
            public List<LoggingLevelType> getObject() {
                List<LoggingLevelType> list = new ArrayList<LoggingLevelType>();
                for (LoggingLevelType type : LoggingLevelType.values()) {
                    list.add(type);
                }

                return list;
            }
        };
    }
    
    private LoggingConfigurationType createConfiguration() {
		LoggingConfigurationType configuration = new LoggingConfigurationType();
		AuditingConfigurationType audit = new AuditingConfigurationType();
		audit.setDetails(model.getObject().isAuditDetails());
		audit.setEnabled(model.getObject().isAuditLog());
		configuration.setAuditing(audit);
		configuration.setRootLoggerAppender(model.getObject().getRootAppender());
		configuration.setRootLoggerLevel(model.getObject().getRootLevel());
		List<LoggerConfiguration> loggersList = new ArrayList<LoggerConfiguration>();

		for (AppenderConfiguration item : model.getObject().getAppenders()) {
			AppenderConfigurationType appender = createAppenderType(item);
			configuration.getAppender().add(appender);
		}

		for (LoggerConfiguration item : model.getObject().getLoggers()) {
			if (item instanceof ClassLogger) {
				loggersList.add(item);
				continue;
			}
			else if (item instanceof ComponentLogger) {
				SubSystemLoggerConfigurationType logger = createSubsystemLogger((ComponentLogger)item, model.getObject().getAppenders());
				configuration.getSubSystemLogger().add(logger);
			}
		}
		
		for (LoggerConfiguration item : loggersList) {
			if (((ClassLogger) item).getName().equals("PROFILING")) {
				continue;
			}
			ClassLoggerConfigurationType logger = createClassLogger((ClassLogger) item, model.getObject().getAppenders());
			configuration.getClassLogger().add(logger);
		}
		
//
		updateProfilingLogger(model.getObject().getProfilingLogger());
		//ClassLoggerConfigurationType logger = createClassLogger((ClassLogger) profilingLogger, appenders);
//		configuration.getClassLogger().add(logger);

		return configuration;
	}
    
    private FileAppenderConfigurationType createAppenderType(AppenderConfiguration item) {
		FileAppenderConfigurationType appender = new FileAppenderConfigurationType();
		appender.setAppend(item.appending());
		appender.setFileName(item.getFilePath());
		appender.setFilePattern(item.getFilePattern());
		appender.setMaxFileSize(item.getMaxFileSize());
		appender.setMaxHistory(item.getMaxHistory());
		appender.setName(item.getName());
		appender.setPattern(item.getPattern());

		return appender;
	}
    
    private SubSystemLoggerConfigurationType createSubsystemLogger(ComponentLogger item,
			List<AppenderConfiguration> appenders) {
		SubSystemLoggerConfigurationType logger = new SubSystemLoggerConfigurationType();
		logger.setLevel(item.getLevel());
		logger.setComponent(item.getComponent());
		item.getAppenders().addAll(createAppendersForLogger(item, appenders));
		return logger;
	}
    
    private List<String> createAppendersForLogger(LoggerConfiguration item, List<AppenderConfiguration> appenders) {
		Set<String> existingAppenders = new HashSet<String>();
		for (AppenderConfiguration appender : appenders) {
			existingAppenders.add(appender.getName());
		}

		List<String> appenderNames = new ArrayList<String>();
		for (String appender : item.getAppenders()) {
			if (!existingAppenders.contains(appender)) {
				String name = "unknown";
				if (item instanceof ClassLogger) {
					name = ((ClassLogger) item).getName();
				} else if (item instanceof ComponentLogger) {
					name = ((ComponentLogger) item).getName();
				}
	
				//TODO:	FacesUtils.addWarnMessage("Ignoring unknown appender '" + appender + "' for logger '" + name + "'.");
				continue;
			}
			appenderNames.add(appender);
		}

		return appenderNames;
	}
    
    private ClassLoggerConfigurationType createClassLogger(ClassLogger item,
			List<AppenderConfiguration> appenders) {
		ClassLoggerConfigurationType logger = new ClassLoggerConfigurationType();
		logger.setLevel(item.getLevel());
		logger.setPackage(item.getName());
		logger.getAppender().addAll(createAppendersForLogger(item, appenders));

		return logger;
	}
    
    private void updateProfilingLogger(LoggerConfiguration profilingLogger) {
		//LoggingLevelType level = model.getObject().getSubsystemLevel().;
		//profilingLogger.setLevel(level);
	}

    private void addLoggerPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void addAppenderPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void deleteAppenderPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void deleteLoggerPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void onLoggerClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
        //todo implement
    }
}
