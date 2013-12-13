/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class PageLogging extends PageAdminConfiguration {

	private static final String DOT_CLASS = PageLogging.class.getName() + ".";
	private static final String OPERATION_LOAD_LOGGING_CONFIGURATION = DOT_CLASS + "loadLoggingConfiguration";
	private static final String OPERATION_UPDATE_LOGGING_CONFIGURATION = DOT_CLASS + "updateLoggingConfiguration";

	private static final Trace LOGGER = TraceManager.getTrace(PageLogging.class);

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

		OperationResult result = new OperationResult(OPERATION_LOAD_LOGGING_CONFIGURATION);
		try {
			Task task = createSimpleTask(OPERATION_LOAD_LOGGING_CONFIGURATION);

			PrismObject<SystemConfigurationType> config = getModelService().getObject(
					SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
					task, result);
			SystemConfigurationType systemConfiguration = config.asObjectable();
			LoggingConfigurationType logging = systemConfiguration.getLogging();
			dto = new LoggingDto(config, logging);

			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't load logging configuration.", ex);
		}

		if (!result.isSuccess()) {
			showResult(result);
		}

		if (dto == null) {
			dto = new LoggingDto();
		}

		return dto;
	}

	private void initLayout() {
		Form mainForm = new Form("mainForm");
		add(mainForm);

		Accordion accordion = new Accordion("accordion");
		accordion.setMultipleSelect(true);
		accordion.setOpenedPanel(0);
		mainForm.add(accordion);

		AccordionItem loggers = new AccordionItem("loggers", createStringResource("pageLogging.loggers"));
		accordion.getBodyContainer().add(loggers);
		initLoggers(loggers);
		initFilters(loggers);

		AccordionItem appenders = new AccordionItem("appenders",
				createStringResource("pageLogging.appenders"));
		accordion.getBodyContainer().add(appenders);
		initAppenders(appenders);

		AccordionItem auditing = new AccordionItem("auditing", createStringResource("pageLogging.audit"));
		accordion.getBodyContainer().add(auditing);
		initAudit(auditing);

        AccordionItem profiling = new AccordionItem("profiling", createStringResource("pageLogging.profiling"));
        accordion.getBodyContainer().add(profiling);
        initProfilingSection(profiling);

		initButtons(mainForm);
	}

	private List<IColumn<LoggerConfiguration, String>> initLoggerColumns() {
        List<IColumn<LoggerConfiguration, String>> columns = new ArrayList<IColumn<LoggerConfiguration, String>>();
        IColumn column = new CheckBoxHeaderColumn<LoggerConfiguration>();
        columns.add(column);

        //name editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(
                createStringResource("pageLogging.logger"), "name") {

            @Override
            protected Component createInputPanel(String componentId, final IModel<LoggerConfiguration> model) {
            	if(model.getObject() instanceof ComponentLogger){
            		DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
            				new PropertyModel(model, "component"),
                            WebMiscUtil.createReadonlyModelFromEnum(LoggingComponentType.class),
                            new IChoiceRenderer<LoggingComponentType>() {

    							@Override
    							public Object getDisplayValue(LoggingComponentType item) {
    								return PageLogging.this.getString("pageLogging.logger." + item);
    							}

    							@Override
    							public String getIdValue(LoggingComponentType item, int index) {
    								return Integer.toString(index);
    							}
    						});

            		FormComponent<LoggingComponentType> input = dropDownChoicePanel.getBaseFormComponent();
            		input.add(new LoggerValidator());
                	input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                	return dropDownChoicePanel;
            	} else {
            		TextPanel<String> textPanel = new TextPanel(componentId, new PropertyModel(model, getPropertyExpression()));
            		FormComponent input = textPanel.getBaseFormComponent();
            		input.add(new AttributeAppender("style", "width: 100%"));
            		input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            		input.add(new InputStringValidator());
                	return textPanel;
            	}

            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggerEditPerformed(target, rowModel);
            }
        });

        //level editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(createStringResource("pageLogging.loggersLevel"), "level") {

            @Override
            protected Component createInputPanel(String componentId, IModel<LoggerConfiguration> model) {
                DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                        new PropertyModel(model, getPropertyExpression()),
                        WebMiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));
                FormComponent<LoggingLevelType> input = dropDownChoicePanel.getBaseFormComponent();
                input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                input.add(new LevelValidator());
                return dropDownChoicePanel;
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggerEditPerformed(target, rowModel);
            }

            @Override
            protected IModel<String> createLinkModel(IModel<LoggerConfiguration> rowModel) {
                LoggerConfiguration configuration = rowModel.getObject();
                return WebMiscUtil.createLocalizedModelForEnum(configuration.getLevel(), PageLogging.this);
            }
        });

        //appender editing column
        columns.add(new EditablePropertyColumn<LoggerConfiguration>(createStringResource("pageLogging.loggersAppender"),
                "appenders") {

            @Override
            public IModel<Object> getDataModel(final IModel rowModel) {
                return new LoadableModel<Object>() {

                    @Override
                    protected Object load() {
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

            @Override
            protected InputPanel createInputPanel(String componentId, IModel<LoggerConfiguration> model) {
                ListMultipleChoicePanel panel = new ListMultipleChoicePanel<String>(componentId,
                        new PropertyModel<List<String>>(model, getPropertyExpression()), createAppendersListModel());

                ListMultipleChoice choice = (ListMultipleChoice) panel.getBaseFormComponent();
                choice.setMaxRows(3);

                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

                return panel;
            }
        });

        return columns;
    }

	private List<IColumn<FilterConfiguration, String>> initFilterColumns() {
        List<IColumn<FilterConfiguration, String>> columns = new ArrayList<IColumn<FilterConfiguration, String>>();
        IColumn column = new CheckBoxHeaderColumn<FilterConfiguration>();
        columns.add(column);

        //name editing column
        columns.add(new EditableLinkColumn<FilterConfiguration>(
                createStringResource("pageLogging.filter"), "name") {

            @Override
            protected Component createInputPanel(String componentId, final IModel<FilterConfiguration> model) {
            	DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
            			createFilterModel(model),
                        WebMiscUtil.createReadonlyModelFromEnum(LoggingComponentType.class),
                        new IChoiceRenderer<LoggingComponentType>() {

							@Override
							public Object getDisplayValue(LoggingComponentType item) {
								return PageLogging.this.getString("pageLogging.filter." + item);
							}

							@Override
							public String getIdValue(LoggingComponentType item, int index) {
								return Integer.toString(index);
							}
						});
            	FormComponent<LoggingComponentType> input = dropDownChoicePanel.getBaseFormComponent();
            	input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            	input.add(new FilterValidator());
            	return dropDownChoicePanel;
            }

            @Override
            protected IModel<String> createLinkModel(final IModel<FilterConfiguration> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        FilterConfiguration config = rowModel.getObject();
                        return PageLogging.this.getString("pageLogging.filter." + config.getComponent());
                    }
                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<FilterConfiguration> rowModel) {
            	filterEditPerformed(target, rowModel);
            }
        });


        //level editing column
        columns.add(new EditableLinkColumn<FilterConfiguration>(createStringResource("pageLogging.loggersLevel"),
                "level") {

            @Override
            protected Component createInputPanel(String componentId, IModel<FilterConfiguration> model) {
                DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                        new PropertyModel(model, getPropertyExpression()),
                        WebMiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));

                FormComponent<LoggingLevelType> input = dropDownChoicePanel.getBaseFormComponent();
                input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                input.add(new LevelValidator());
                return dropDownChoicePanel;
            }

            @Override
            protected IModel<String> createLinkModel(IModel<FilterConfiguration> rowModel) {
                FilterConfiguration config = rowModel.getObject();
                return WebMiscUtil.createLocalizedModelForEnum(config.getLevel(), PageLogging.this);
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<FilterConfiguration> rowModel) {
                filterEditPerformed(target, rowModel);
            }
        });

        //appender editing column
        columns.add(new EditablePropertyColumn<FilterConfiguration>(createStringResource("pageLogging.loggersAppender"),
                "appenders") {

            @Override
            public IModel<Object> getDataModel(final IModel rowModel) {
                return new LoadableModel<Object>() {

                    @Override
                    protected Object load() {
                    	FilterConfiguration config = (FilterConfiguration) rowModel.getObject();
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

            @Override
            protected InputPanel createInputPanel(String componentId, IModel<FilterConfiguration> model) {
                ListMultipleChoicePanel panel = new ListMultipleChoicePanel<String>(componentId,
                        new PropertyModel<List<String>>(model, getPropertyExpression()), createAppendersListModel());

                ListMultipleChoice choice = (ListMultipleChoice) panel.getBaseFormComponent();
                choice.setMaxRows(3);

                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

                return panel;
            }
        });

        return columns;
    }

	private IModel<LoggingComponentType> createFilterModel(final IModel<FilterConfiguration> model) {
		return new Model<LoggingComponentType>() {

			@Override
			public LoggingComponentType getObject() {
				String name = model.getObject().getName();
				if (StringUtils.isEmpty(name)) {
					return null;
				}
				return LoggingComponentType.fromValue(name);
			}

			@Override
			public void setObject(LoggingComponentType object) {
				model.getObject().setName(object.name());
			}
		};
	}

	private void initLoggers(AccordionItem loggers) {
		initRoot(loggers);

		ISortableDataProvider<LoggerConfiguration, String> provider = new ListDataProvider<LoggerConfiguration>(this,
				new PropertyModel<List<LoggerConfiguration>>(model, "loggers"));
		TablePanel table = new TablePanel<LoggerConfiguration>("loggersTable", provider, initLoggerColumns());
		table.setStyle("margin-top: 0px;");
		table.setOutputMarkupId(true);
		table.setShowPaging(false);
		table.setTableCssClass("autowidth");
		loggers.getBodyContainer().add(table);

		AjaxLinkButton addComponentLogger = new AjaxLinkButton("addComponentLogger",
				createStringResource("pageLogging.button.addComponentLogger")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				addComponentLoggerPerformed(target);
			}
		};
		loggers.getBodyContainer().add(addComponentLogger);

		AjaxLinkButton addClassLogger = new AjaxLinkButton("addClassLogger",
				createStringResource("pageLogging.button.addClassLogger")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				addClassLoggerPerformed(target);
			}
		};
		loggers.getBodyContainer().add(addClassLogger);

		AjaxLinkButton deleteLogger = new AjaxLinkButton("deleteLogger",
				createStringResource("pageLogging.button.deleteLogger")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteLoggerPerformed(target);
			}
		};
		loggers.getBodyContainer().add(deleteLogger);
		initProfiling(loggers);
	}

	private void initFilters(AccordionItem loggers) {

		ISortableDataProvider<LoggerConfiguration, String> provider = new ListDataProvider<LoggerConfiguration>(this,
				new PropertyModel<List<LoggerConfiguration>>(model, "filters"));
		TablePanel table = new TablePanel<FilterConfiguration>("filtersTable", provider, initFilterColumns());
		table.setStyle("margin-top: 0px;");
		table.setOutputMarkupId(true);
		table.setShowPaging(false);
		table.setTableCssClass("autowidth");
		loggers.getBodyContainer().add(table);

		AjaxLinkButton addComponentLogger = new AjaxLinkButton("addFilter",
				createStringResource("pageLogging.button.addFilter")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				addFilterLoggerPerformed(target);
			}
		};
		loggers.getBodyContainer().add(addComponentLogger);

		AjaxLinkButton deleteLogger = new AjaxLinkButton("deleteFilter",
				createStringResource("pageLogging.button.deleteFilter")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteFilterPerformed(target);
			}
		};
		loggers.getBodyContainer().add(deleteLogger);

	}

	private void initProfiling(AccordionItem loggers) {
		DropDownChoice<ProfilingLevel> profilingLevel = new DropDownChoice<ProfilingLevel>("profilingLevel",
				new PropertyModel<ProfilingLevel>(model, "profilingLevel"),
				WebMiscUtil.createReadonlyModelFromEnum(ProfilingLevel.class),
                new EnumChoiceRenderer(PageLogging.this));
        loggers.getBodyContainer().add(profilingLevel);

		DropDownChoice<String> profilingAppender = new DropDownChoice<String>("profilingAppender",
				new PropertyModel<String>(model, "profilingAppender"), createAppendersListModel());
        profilingAppender.setNullValid(true);
		loggers.getBodyContainer().add(profilingAppender);
	}

	private List<IColumn<AppenderConfiguration, String>> initAppendersColumns() {
		List<IColumn<AppenderConfiguration, String>> columns = new ArrayList<IColumn<AppenderConfiguration, String>>();

		IColumn column = new CheckBoxHeaderColumn<AppenderConfiguration>();
		columns.add(column);

		// name editable column
		column = new EditableLinkColumn<AppenderConfiguration>(
				createStringResource("pageLogging.appenders.name"), "name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<AppenderConfiguration> rowModel) {
				appenderEditPerformed(target, rowModel);
			}

			@Override
			protected Component createInputPanel(String componentId, IModel<AppenderConfiguration> model) {
				TextPanel<String> panel = new TextPanel<String>(componentId, new PropertyModel(model, getPropertyExpression()));
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                panel.add(new InputStringValidator());
                return panel;
			}
		};
		columns.add(column);

		// pattern editable column
		columns.add(new EditablePropertyColumn(createStringResource("pageLogging.appenders.pattern"),
				"pattern") {

            @Override
            protected InputPanel createInputPanel(String componentId, IModel iModel) {
                InputPanel panel = super.createInputPanel(componentId, iModel);
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                panel.add(new InputStringValidator());
                return panel;
            }
        });
		// file path editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.filePath"), "filePath"));
		// file pattern editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.filePattern"),
				"filePattern"));
		// max history editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.maxHistory"),
				"maxHistory") {

			@Override
			protected InputPanel createInputPanel(String componentId, IModel iModel) {
				TextPanel panel = new TextPanel(componentId, new PropertyModel(iModel, getPropertyExpression()));
				FormComponent component = panel.getBaseFormComponent();
                component.add(new AttributeModifier("size", 5));
                component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
				return panel;
			}
		});
		// max file size editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.maxFileSize"),
				"maxFileSize") {

			@Override
			protected InputPanel createInputPanel(String componentId, IModel iModel) {
				TextPanel<String> panel = new TextPanel(componentId, new PropertyModel(iModel,
						getPropertyExpression()));
                FormComponent component = panel.getBaseFormComponent();
                component.add(new AttributeModifier("size", 5));
                component.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                component.add(new InputStringValidator());
				return panel;
			}
		});

		CheckBoxColumn check = new EditableCheckboxColumn(createStringResource("pageLogging.appenders.appending"),
                "appending") {

            @Override
            protected InputPanel createInputPanel(String componentId, IModel iModel) {
                InputPanel panel = super.createInputPanel(componentId, iModel);
                panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                panel.add(new InputStringValidator());
                return panel;
            }
        };
		check.setEnabled(false);
		columns.add(check);

		return columns;
	}

	private void initAppenders(AccordionItem appenders) {
		ISortableDataProvider<AppenderConfiguration, String> provider = new ListDataProvider<AppenderConfiguration>(
				this, new PropertyModel<List<AppenderConfiguration>>(model, "appenders"));
		TablePanel table = new TablePanel<AppenderConfiguration>("appendersTable", provider,
				initAppendersColumns());
		table.setOutputMarkupId(true);
		table.setShowPaging(false);
		appenders.getBodyContainer().add(table);

		AjaxLinkButton addConsoleAppender = new AjaxLinkButton("addConsoleAppender",
				createStringResource("pageLogging.button.addConsoleAppender")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				addConsoleAppenderPerformed(target);
			}
		};
		appenders.getBodyContainer().add(addConsoleAppender);

		AjaxLinkButton addFileAppender = new AjaxLinkButton("addFileAppender",
				createStringResource("pageLogging.button.addFileAppender")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				addFileAppenderPerformed(target);
			}
		};
		appenders.getBodyContainer().add(addFileAppender);

		AjaxLinkButton deleteAppender = new AjaxLinkButton("deleteAppender",
				createStringResource("pageLogging.button.deleteAppender")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				deleteAppenderPerformed(target);
			}
		};
		appenders.getBodyContainer().add(deleteAppender);
	}

	private void initRoot(final AccordionItem loggers) {
		DropDownChoice<LoggingLevelType> rootLevel = new DropDownChoice<LoggingLevelType>("rootLevel",
				new PropertyModel<LoggingLevelType>(model, "rootLevel"),
				WebMiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));

		loggers.getBodyContainer().add(rootLevel);

		DropDownChoice<String> rootAppender = new DropDownChoice<String>("rootAppender",
                new PropertyModel<String>(model,				"rootAppender"), createAppendersListModel());
        rootAppender.setNullValid(true);
		loggers.getBodyContainer().add(rootAppender);
	}

	private void initButtons(final Form mainForm) {
		AjaxSubmitButton saveButton = new AjaxSubmitButton("saveButton",
                createStringResource("pageLogging.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(saveButton);

		AjaxButton resetButton = new AjaxButton("resetButton",
				createStringResource("pageLogging.button.reset")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				resetPerformed(target);
			}
		};
		mainForm.add(resetButton);

        AjaxButton advancedButton = new AjaxButton("advancedButton",
				createStringResource("pageLogging.button.advanced")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				advancedPerformed(target);
			}
		};
		advancedButton.setVisible(false);
		mainForm.add(advancedButton);
	}

	private void initAudit(AccordionItem audit) {
		CheckBox auditLog = new CheckBox("auditLog", new PropertyModel<Boolean>(model, "auditLog"));
		audit.getBodyContainer().add(auditLog);

		CheckBox auditDetails = new CheckBox("auditDetails",
				new PropertyModel<Boolean>(model, "auditDetails"));
		audit.getBodyContainer().add(auditDetails);

		DropDownChoice<String> auditAppender = new DropDownChoice<String>("auditAppender", new PropertyModel<String>(
				model, "auditAppender"), createAppendersListModel());
        auditAppender.setNullValid(true);
		audit.getBodyContainer().add(auditAppender);
	}

    private void initProfilingSection(AccordionItem profiling){
        CheckBox requestFilter = new CheckBox("requestFilter", new PropertyModel<Boolean>(model, "requestFilter"));
        CheckBox performanceStatistics = new CheckBox("performanceStatistics", new PropertyModel<Boolean>(model, "performanceStatistics"));
        CheckBox subsystemModel = new CheckBox("subsystemModel", new PropertyModel<Boolean>(model, "subsystemModel"));
        CheckBox subsystemRepository = new CheckBox("subsystemRepository", new PropertyModel<Boolean>(model, "subsystemRepository"));
        CheckBox subsystemProvisioning = new CheckBox("subsystemProvisioning", new PropertyModel<Boolean>(model, "subsystemProvisioning"));
        CheckBox subsystemUcf = new CheckBox("subsystemUcf", new PropertyModel<Boolean>(model, "subsystemUcf"));
        CheckBox subsystemResourceObjectChangeListener = new CheckBox("subsystemResourceObjectChangeListener", new PropertyModel<Boolean>(model, "subsystemResourceObjectChangeListener"));
        CheckBox subsystemTaskManager = new CheckBox("subsystemTaskManager", new PropertyModel<Boolean>(model, "subsystemTaskManager"));
        CheckBox subsystemWorkflow = new CheckBox("subsystemWorkflow", new PropertyModel<Boolean>(model, "subsystemWorkflow"));
        profiling.getBodyContainer().add(requestFilter);
        profiling.getBodyContainer().add(performanceStatistics);
        profiling.getBodyContainer().add(subsystemModel);
        profiling.getBodyContainer().add(subsystemRepository);
        profiling.getBodyContainer().add(subsystemProvisioning);
        profiling.getBodyContainer().add(subsystemUcf);
        profiling.getBodyContainer().add(subsystemResourceObjectChangeListener);
        profiling.getBodyContainer().add(subsystemTaskManager);
        profiling.getBodyContainer().add(subsystemWorkflow);

        TextField<Integer> dumpInterval = new TextField<Integer>("dumpInterval", new PropertyModel<Integer>(model,
                "dumpInterval"));
        //dumpInterval.setType(Integer.class);

        profiling.getBodyContainer().add(dumpInterval);

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

	private LoggingConfigurationType createConfiguration(LoggingDto dto) {
		LoggingConfigurationType configuration = new LoggingConfigurationType();
		AuditingConfigurationType audit = new AuditingConfigurationType();
		audit.setEnabled(dto.isAuditLog());
		audit.setDetails(dto.isAuditDetails());
        if (StringUtils.isNotEmpty(dto.getAuditAppender())) {
		    audit.getAppender().add(dto.getAuditAppender());
        }
		configuration.setAuditing(audit);
		configuration.setRootLoggerAppender(dto.getRootAppender());
		configuration.setRootLoggerLevel(dto.getRootLevel());

		for (AppenderConfiguration item : dto.getAppenders()) {
			configuration.getAppender().add(item.getConfig());
		}

		for (LoggerConfiguration item : dto.getLoggers()) {
			if (LoggingDto.LOGGER_PROFILING.equals(item.getName())) {
                continue;
			}

			for(ClassLoggerConfigurationType logger : configuration.getClassLogger()){
				if(logger.getPackage().equals(item.getName())){
					error("Logger with name '" + item.getName() + "' is already defined.");
					return null;
				}
			}

			if(item instanceof ComponentLogger){
				configuration.getClassLogger().add(((ComponentLogger) item).toXmlType());
			} else {
				configuration.getClassLogger().add(((ClassLogger) item).toXmlType());
			}

		}

		for (FilterConfiguration item : dto.getFilters()) {
			if (LoggingDto.LOGGER_PROFILING.equals(item.getName())) {
                continue;
            }

			for(SubSystemLoggerConfigurationType  filter : configuration.getSubSystemLogger()){
				if(filter.getComponent().name().equals(item.getName())){
					error("Filter with name '" + item.getName() + "' is already defined.");
					return null;
				}
			}

			configuration.getSubSystemLogger().add(item.toXmlType());
		}

        if (dto.getProfilingLevel() != null) {
            ClassLoggerConfigurationType type = createCustomClassLogger(LoggingDto.LOGGER_PROFILING,
                    ProfilingLevel.toLoggerLevelType(dto.getProfilingLevel()), dto.getProfilingAppender());
            configuration.getClassLogger().add(type);
        }

		return configuration;
	}

    private ProfilingConfigurationType createProfConfig(LoggingDto dto){
        ProfilingConfigurationType config = new ProfilingConfigurationType();

        if(dto.isPerformanceStatistics() || dto.isRequestFilter() || dto.isSubsystemModel() || dto.isSubsystemRepository() || dto.isSubsystemProvisioning()
                || dto.isSubsystemResourceObjectChangeListener() || dto.isSubsystemUcf() || dto.isSubsystemTaskManager() || dto.isSubsystemWorkflow())
            config.setEnabled(true);
        else
            config.setEnabled(false);

        LOGGER.info("Profiling enabled: " + config.isEnabled());

        config.setDumpInterval(dto.getDumpInterval());
        config.setPerformanceStatistics(dto.isPerformanceStatistics());
        config.setRequestFilter(dto.isRequestFilter());
        config.setModel(dto.isSubsystemModel());
        config.setProvisioning(dto.isSubsystemProvisioning());
        config.setRepository(dto.isSubsystemRepository());
        config.setUcf(dto.isSubsystemUcf());
        config.setResourceObjectChangeListener(dto.isSubsystemResourceObjectChangeListener());
        config.setTaskManager(dto.isSubsystemTaskManager());
        config.setWorkflow(dto.isSubsystemWorkflow());

        return config;
    }

    private ClassLoggerConfigurationType createCustomClassLogger(String name, LoggingLevelType level, String appender) {
        ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
        type.setPackage(name);
        type.setLevel(level);
        if (StringUtils.isNotEmpty(appender)) {
            type.getAppender().add(appender);
        }

        return type;
    }

	private TablePanel getLoggersTable() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		AccordionItem item = (AccordionItem) accordion.getBodyContainer().get("loggers");
		return (TablePanel) item.getBodyContainer().get("loggersTable");
	}

	private TablePanel getFiltersTable() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		AccordionItem item = (AccordionItem) accordion.getBodyContainer().get("loggers");
		return (TablePanel) item.getBodyContainer().get("filtersTable");
	}

	private TablePanel getAppendersTable() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		AccordionItem item = (AccordionItem) accordion.getBodyContainer().get("appenders");
		return (TablePanel) item.getBodyContainer().get("appendersTable");
	}

	private void addFilterLoggerPerformed(AjaxRequestTarget target) {
		LoggingDto dto = model.getObject();
		FilterConfiguration logger = new FilterConfiguration(new SubSystemLoggerConfigurationType());
		logger.setEditing(true);
		dto.getFilters().add(logger);
		target.add(getFiltersTable());
	}

	private void addComponentLoggerPerformed(AjaxRequestTarget target) {
		LoggingDto dto = model.getObject();
		ComponentLogger logger = new ComponentLogger(new ClassLoggerConfigurationType());
		logger.setEditing(true);
		dto.getLoggers().add(logger);

		target.add(getLoggersTable());
	}

	private void addClassLoggerPerformed(AjaxRequestTarget target) {
		LoggingDto dto = model.getObject();
		ClassLogger logger = new ClassLogger(new ClassLoggerConfigurationType());
		logger.setEditing(true);
		dto.getLoggers().add(logger);

		target.add(getLoggersTable());
	}

	private void deleteAppenderPerformed(AjaxRequestTarget target) {
		Iterator<AppenderConfiguration> iterator = model.getObject().getAppenders().iterator();
		while (iterator.hasNext()) {
			AppenderConfiguration item = iterator.next();
			if (item.isSelected()) {
				iterator.remove();
			}
		}
		target.add(getAppendersTable());
		target.add(getFiltersTable());
	}

	private void deleteLoggerPerformed(AjaxRequestTarget target) {
		Iterator<LoggerConfiguration> iterator = model.getObject().getLoggers().iterator();
		while (iterator.hasNext()) {
			LoggerConfiguration item = iterator.next();
			if (item.isSelected()) {
				iterator.remove();
			}
		}
		target.add(getLoggersTable());
	}

	private void deleteFilterPerformed(AjaxRequestTarget target) {
		Iterator<FilterConfiguration> iterator = model.getObject().getFilters().iterator();
		while (iterator.hasNext()) {
			FilterConfiguration item = iterator.next();
			if (item.isSelected()) {
				iterator.remove();
			}
		}
		target.add(getFiltersTable());
	}

	private void addConsoleAppenderPerformed(AjaxRequestTarget target) {
		LoggingDto dto = model.getObject();
		AppenderConfiguration appender = new AppenderConfiguration(new AppenderConfigurationType());
		appender.setEditing(true);
		dto.getAppenders().add(appender);

		target.add(getAppendersTable());
	}

	private void addFileAppenderPerformed(AjaxRequestTarget target) {
		LoggingDto dto = model.getObject();
		FileAppenderConfig appender = new FileAppenderConfig(new FileAppenderConfigurationType());
		appender.setEditing(true);
		dto.getAppenders().add(appender);

		target.add(getAppendersTable());
	}

	private void loggerEditPerformed(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
		LoggerConfiguration config = rowModel.getObject();
		config.setEditing(true);
		target.add(getLoggersTable());
	}

	private void filterEditPerformed(AjaxRequestTarget target, IModel<FilterConfiguration> rowModel) {
		FilterConfiguration config = rowModel.getObject();
		config.setEditing(true);
		target.add(getFiltersTable());
	}

	private void appenderEditPerformed(AjaxRequestTarget target, IModel<AppenderConfiguration> model) {
		AppenderConfiguration config = model.getObject();
		config.setEditing(true);
		target.add(getAppendersTable());
	}

	private void savePerformed(AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_UPDATE_LOGGING_CONFIGURATION);
		String oid = SystemObjectsType.SYSTEM_CONFIGURATION.value();
		try {
			LoggingDto dto = model.getObject();

			LoggingConfigurationType config = createConfiguration(dto);
			if(config == null){
				target.add(getFeedbackPanel());
				target.add(get("mainForm"));
				return;
			}

            ProfilingConfigurationType profilingConfiguration = createProfConfig(dto);
            if(profilingConfiguration == null){
                target.add(getFeedbackPanel());
                target.add(get("mainForm"));
                return;
            }

			Task task = createSimpleTask(OPERATION_UPDATE_LOGGING_CONFIGURATION);
			PrismObject<SystemConfigurationType> newObject = dto.getOldConfiguration();

				newObject.asObjectable().setLogging(config);
                newObject.asObjectable().setProfilingConfiguration(profilingConfiguration);

            PrismObject<SystemConfigurationType> oldObject = getModelService().getObject(SystemConfigurationType.class,
                    oid, null, task, result);

			ObjectDelta<SystemConfigurationType> delta = DiffUtil.diff(oldObject, newObject);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Logging configuration delta:\n{}", delta.dump());
			}
			if (delta != null && !delta.isEmpty()){

            getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
			}
			// finish editing for loggers and appenders
			for (LoggerConfiguration logger : dto.getLoggers()) {
				logger.setEditing(false);
			}
			for (FilterConfiguration filter : dto.getFilters()) {
				filter.setEditing(false);
			}
			for (AppenderConfiguration appender : dto.getAppenders()) {
				appender.setEditing(false);
			}
			result.computeStatusIfUnknown();
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't save logging configuration.", ex);
		}

		showResult(result);
		target.add(getFeedbackPanel());
		resetPerformed(target);
	}

	private void resetPerformed(AjaxRequestTarget target) {
		model.reset();
		target.add(get("mainForm"));
		target.appendJavaScript("init();");
	}

	private void advancedPerformed(AjaxRequestTarget target) {
		LoggingDto dto = PageLogging.this.model.getObject();
		dto.setAdvanced(!dto.isAdvanced());

		target.add(get("mainForm"));
	}

    private static class EmptyOnBlurAjaxFormUpdatingBehaviour extends AjaxFormComponentUpdatingBehavior {

        public EmptyOnBlurAjaxFormUpdatingBehaviour() {
            super("onBlur");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
        }
    }

	private static class FileAppenderColumn<T extends Editable> extends EditablePropertyColumn<T> {

		private FileAppenderColumn(IModel<String> displayModel, String propertyExpression) {
			super(displayModel, propertyExpression);
		}

		@Override
		protected boolean isEditing(IModel<T> rowModel) {
			return super.isEditing(rowModel) && (rowModel.getObject() instanceof FileAppenderConfig);
		}

        @Override
        protected InputPanel createInputPanel(String componentId, IModel iModel) {
            InputPanel panel = super.createInputPanel(componentId, iModel);
            panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            panel.add(new InputStringValidator());
            return panel;
        }
	}
}
