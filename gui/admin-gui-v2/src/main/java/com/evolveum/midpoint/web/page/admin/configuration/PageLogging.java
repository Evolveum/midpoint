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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.Editable;
import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import com.evolveum.midpoint.web.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
	private static final String OPERATION_LOAD_LOGGING_CONFIGURATION = "loadLoggingConfiguration";
	private static final String OPERATION_UPDATE_LOGGING_CONFIGURATION = DOT_CLASS
			+ "updateLoggingConfiguration";

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

		AccordionItem appenders = new AccordionItem("appenders",
				createStringResource("pageLogging.appenders"));
		accordion.getBodyContainer().add(appenders);
		initAppenders(appenders);

		AccordionItem auditing = new AccordionItem("auditing", createStringResource("pageLogging.audit"));
		accordion.getBodyContainer().add(auditing);
		initAudit(auditing);

		initButtons(mainForm);
	}

	private List<IColumn<LoggerConfiguration>> initLoggerColumns() {
        List<IColumn<LoggerConfiguration>> columns = new ArrayList<IColumn<LoggerConfiguration>>();
        IColumn column = new CheckBoxHeaderColumn<LoggerConfiguration>();
        columns.add(column);

        //name editing column
        columns.add(new EditableLinkColumn<LoggerConfiguration>(
                createStringResource("pageLogging.classPackageSubsystem"), "name") {

            @Override
            protected Component createInputPanel(String componentId, final IModel<LoggerConfiguration> model) {
                if (model.getObject() instanceof ComponentLogger) {
                	DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                            createComponentLoggerModel(model),
                            MiscUtil.createReadonlyModelFromEnum(LoggingComponentType.class));

                	dropDownChoicePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                	return dropDownChoicePanel;
                } else {
                	TextPanel textPanel = new TextPanel(componentId, new PropertyModel(model, getPropertyExpression()));
                	textPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                	return textPanel;
                }
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
                loggedEditPerformed(target, rowModel);
            }
        });


        //level editing column
        columns.add(new EditablePropertyColumn<LoggerConfiguration>(createStringResource("pageLogging.loggersLevel"),
                "level") {

            @Override
            protected Component createInputPanel(String componentId, final IModel<LoggerConfiguration> model) {
                DropDownChoicePanel dropDownChoicePanel = new DropDownChoicePanel(componentId,
                        new PropertyModel(model, getPropertyExpression()),
                        MiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));
            	dropDownChoicePanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            	return dropDownChoicePanel;
            }
        });

        //appender editing column
        columns.add(new EditablePropertyColumn<LoggerConfiguration>(createStringResource("pageLogging.loggersAppender"),
                "appenders") {

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

            @Override
            protected Component createInputPanel(String componentId, IModel<LoggerConfiguration> model) {
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

	private IModel<LoggingComponentType> createComponentLoggerModel(final IModel<LoggerConfiguration> model) {
		return new Model<LoggingComponentType>() {

			@Override
			public LoggingComponentType getObject() {
				String name = model.getObject().getName();
				if (StringUtils.isEmpty(name)) {
					return null;
				}
				return LoggingComponentType.valueOf(name);
			}

			@Override
			public void setObject(LoggingComponentType object) {
				model.getObject().setName(object.name());
			}
		};
	}

	private void initLoggers(AccordionItem loggers) {
		initRoot(loggers);

		ISortableDataProvider<LoggerConfiguration> provider = new ListDataProvider<LoggerConfiguration>(this,
				new PropertyModel<List<LoggerConfiguration>>(model, "loggers"));
		TablePanel table = new TablePanel<LoggerConfiguration>("loggersTable", provider, initLoggerColumns());
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

		initSubsystem(loggers);
	}

	private void initSubsystem(AccordionItem loggers) {
		DropDownChoice<ProfilingLevel> subsystemLevel = createComboBox("profilingLevel",
				new PropertyModel<ProfilingLevel>(model, "profilingLevel"),
				MiscUtil.createReadonlyModelFromEnum(ProfilingLevel.class), new EnumChoiceRenderer(
						PageLogging.this));
		loggers.getBodyContainer().add(subsystemLevel);

		DropDownChoice<String> subsystemAppender = createComboBox("profilingAppender",
				new PropertyModel<String>(model, "profilingAppender"), createAppendersListModel());
		loggers.getBodyContainer().add(subsystemAppender);
	}

	private List<IColumn<AppenderConfiguration>> initAppendersColumns() {
		List<IColumn<AppenderConfiguration>> columns = new ArrayList<IColumn<AppenderConfiguration>>();

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
				return new TextPanel(componentId, new PropertyModel(model, getPropertyExpression()));
			}
		};
		columns.add(column);

		// pattern editable column
		columns.add(new EditablePropertyColumn(createStringResource("pageLogging.appenders.pattern"),
				"pattern"));
		// file path editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.filePath"), "filePath"));
		// file pattern editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.filePattern"),
				"filePattern"));
		// max history editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.maxHistory"),
				"maxHistory") {

			@Override
			protected Component createInputPanel(String componentId, IModel iModel) {
				TextPanel panel = new TextPanel(componentId, new PropertyModel(iModel,
						getPropertyExpression()));
				TextField text = (TextField) panel.getBaseFormComponent();
				text.add(new AttributeModifier("size", 5));
				return panel;
			}
		});
		// max file size editable column
		columns.add(new FileAppenderColumn(createStringResource("pageLogging.appenders.maxFileSize"),
				"maxFileSize") {

			@Override
			protected Component createInputPanel(String componentId, IModel iModel) {
				TextPanel panel = new TextPanel(componentId, new PropertyModel(iModel,
						getPropertyExpression()));
				TextField text = (TextField) panel.getBaseFormComponent();
				text.add(new AttributeModifier("size", 5));
				return panel;
			}
		});

		CheckBoxColumn check = new EditableCheckboxColumn(
				createStringResource("pageLogging.appenders.appending"), "appending");
		check.setEnabled(false);
		columns.add(check);

		return columns;
	}

	private void initAppenders(AccordionItem appenders) {
		ISortableDataProvider<AppenderConfiguration> provider = new ListDataProvider<AppenderConfiguration>(
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
		DropDownChoice<LoggingLevelType> rootLevel = createComboBox("rootLevel",
				new PropertyModel<LoggingLevelType>(model, "rootLevel"),
				MiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));

		loggers.getBodyContainer().add(rootLevel);

		DropDownChoice<String> rootAppender = createComboBox("rootAppender", new PropertyModel<String>(model,
				"rootAppender"), createAppendersListModel());
		loggers.getBodyContainer().add(rootAppender);

		DropDownChoice<LoggingLevelType> midPointLevel = createComboBox("midPointLevel",
				new PropertyModel<LoggingLevelType>(model, "midPointLevel"),
				MiscUtil.createReadonlyModelFromEnum(LoggingLevelType.class));
		loggers.getBodyContainer().add(midPointLevel);

		DropDownChoice<String> midPointAppender = createComboBox("midPointAppender",
				new PropertyModel<String>(model, "midPointAppender"), createAppendersListModel());
		loggers.getBodyContainer().add(midPointAppender);
	}

	private void initButtons(final Form mainForm) {
		AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("saveButton",
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

		AjaxLinkButton resetButton = new AjaxLinkButton("resetButton",
				createStringResource("pageLogging.button.reset")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				resetPerformed(target);
			}
		};
		mainForm.add(resetButton);

		AjaxLinkButton advancedButton = new AjaxLinkButton("advancedButton",
				createStringResource("pageLogging.button.advanced")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				advancedPerformed(target);
			}
		};
		mainForm.add(advancedButton);
	}

	private void initAudit(AccordionItem audit) {
		CheckBox auditLog = new CheckBox("auditLog", new PropertyModel<Boolean>(model, "auditLog"));
		audit.getBodyContainer().add(auditLog);

		CheckBox auditDetails = new CheckBox("auditDetails",
				new PropertyModel<Boolean>(model, "auditDetails"));
		audit.getBodyContainer().add(auditDetails);

		DropDownChoice<String> auditAppender = createComboBox("auditAppender", new PropertyModel<String>(
				model, "auditAppender"), createAppendersListModel());
		audit.getBodyContainer().add(auditAppender);
	}

	private <T> DropDownChoice<T> createComboBox(String id, IModel<T> choice, IModel<List<T>> choices) {
		return new DropDownChoice<T>(id, choice, choices) {

			@Override
			protected CharSequence getDefaultChoice(String selectedValue) {
				return "";
			}
		};
	}

	private <T> DropDownChoice<T> createComboBox(String id, IModel<T> choice, IModel<List<T>> choices,
			IChoiceRenderer renderer) {
		return new DropDownChoice<T>(id, choice, choices, renderer) {

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

	private LoggingConfigurationType createConfiguration() {
		LoggingDto dto = model.getObject();
		LoggingConfigurationType configuration = new LoggingConfigurationType();
		AuditingConfigurationType audit = new AuditingConfigurationType();
		audit.setEnabled(dto.isAuditLog());
		audit.setDetails(dto.isAuditDetails());
		audit.getAppender().add(dto.getAuditAppender());
		configuration.setAuditing(audit);
		configuration.setRootLoggerAppender(dto.getRootAppender());
		configuration.setRootLoggerLevel(dto.getRootLevel());

		for (AppenderConfiguration item : dto.getAppenders()) {
			configuration.getAppender().add(item.getConfig());
		}

		for (LoggerConfiguration item : dto.getLoggers()) {
			if ("PROFILING".equals(item.getName())) {
				continue;
			}

			if (item instanceof ClassLogger) {
				configuration.getClassLogger().add(((ClassLogger) item).toXmlType());
			} else if (item instanceof ComponentLogger) {
				configuration.getSubSystemLogger().add(((ComponentLogger) item).toXmlType());
			}
		}

		if (dto.getProfilingLevel() != null && dto.getProfilingAppender() != null) {
			ClassLoggerConfigurationType type = new ClassLoggerConfigurationType();
			type.setPackage("PROFILING");
			type.setLevel(ProfilingLevel.toLoggerLevelType(dto.getProfilingLevel()));
			type.getAppender().add(dto.getProfilingAppender());
			configuration.getClassLogger().add(type);
		}

		return configuration;
	}

	private TablePanel getLoggersTable() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		AccordionItem item = (AccordionItem) accordion.getBodyContainer().get("loggers");
		return (TablePanel) item.getBodyContainer().get("loggersTable");
	}

	private TablePanel getAppendersTable() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		AccordionItem item = (AccordionItem) accordion.getBodyContainer().get("appenders");
		return (TablePanel) item.getBodyContainer().get("appendersTable");
	}

	private void addComponentLoggerPerformed(AjaxRequestTarget target) {
		LoggingDto dto = model.getObject();
		ComponentLogger logger = new ComponentLogger(new SubSystemLoggerConfigurationType());
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

	private void loggedEditPerformed(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
		LoggerConfiguration config = rowModel.getObject();
		config.setEditing(true);
		target.add(getLoggersTable());
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
			Task task = createSimpleTask(OPERATION_UPDATE_LOGGING_CONFIGURATION);
			LoggingDto dto = model.getObject();

			PrismObject<SystemConfigurationType> newObject = dto.getOldConfiguration().clone();
			LoggingConfigurationType config = createConfiguration();
			newObject.asObjectable().setLogging(config);

			// if (LOGGER.isTraceEnabled()) {
			// LOGGER.trace("Before diff:\nOLD:\n{}\nNEW:\n{}",dto.getOldConfiguration().dump(),
			// newObject.dump());
			// }
			ObjectDelta<SystemConfigurationType> delta = DiffUtil.diff(dto.getOldConfiguration(), newObject);
			// if (LOGGER.isTraceEnabled()) {
			// LOGGER.trace("After diff:\n{}",delta.dump());
			// }
			getModelService().modifyObject(SystemConfigurationType.class, oid, delta.getModifications(),
					task, result);

			// finish editing for loggers and appenders
			for (LoggerConfiguration logger : dto.getLoggers()) {
				logger.setEditing(false);
			}
			for (AppenderConfiguration appender : dto.getAppenders()) {
				appender.setEditing(false);
			}

			result.recordSuccess();
		} catch (Exception ex) {
			result.recomputeStatus();
			result.recordFatalError("Couldn't save logging configuration.", ex);
		}

		showResult(result);
		target.add(getFeedbackPanel());
		target.add(get("mainForm"));
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
	}
}
