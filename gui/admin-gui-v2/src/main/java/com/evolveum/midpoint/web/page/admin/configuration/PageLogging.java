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
import java.util.Iterator;
import java.util.List;

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
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
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
import com.evolveum.midpoint.web.page.admin.configuration.dto.AppenderConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ClassLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ComponentLogger;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggerConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ProfilingLevel;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AuditingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;

/**
 * @author lazyman
 */
public class PageLogging extends PageAdminConfiguration {
	private String CLASS_NAME = PageLogging.class.getName() + ".";
	private String UPDATE_LOGGING_CONFIGURATION = CLASS_NAME + "updateLoggingConfiguration";

	private LoadableModel<LoggingDto> model;
	private PrismObject<SystemConfigurationType> oldObject;

	@SpringBean(name = "taskManager")
	TaskManager taskManager;

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
			PrismObject<SystemConfigurationType> config = getModelService().getObject(
					SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null,
					result);
			oldObject = config;
			SystemConfigurationType systemConfiguration = config.asObjectable();
			LoggingConfigurationType logging = systemConfiguration.getLogging();
			dto = new LoggingDto(logging);
		} catch (Exception ex) {
			ex.printStackTrace();
			// todo implement
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

	private void initLoggers(AccordionItem loggers) {
		List<IColumn<LoggerConfiguration>> columns = new ArrayList<IColumn<LoggerConfiguration>>();

		initRoot(loggers);

		IColumn column = new CheckBoxHeaderColumn<LoggerConfiguration>();
		columns.add(column);

		column = new LinkColumn<LoggerConfiguration>(
				createStringResource("pageLogging.classPackageSubsystem"), "name") {

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
		table.setOutputMarkupId(true);
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
				return new StringResourceModel(object.getLocalizationKey(), PageLogging.this, null)
						.getString();
			}

			@Override
			public String getIdValue(ProfilingLevel object, int index) {
				return object.name();
			}
		};

		DropDownChoice<ProfilingLevel> subsystemLevel = createComboBox("profilingLevel",
				new PropertyModel<ProfilingLevel>(model, "profilingLevel"), createSubsystemLevelModel(),
				renderer);
		loggers.getBodyContainer().add(subsystemLevel);

		DropDownChoice<String> subsystemAppender = createComboBox("profilingAppender",
				new PropertyModel<String>(model, "profilingAppender"), createAppendersListModel());
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

		column = new LinkColumn<LoggerConfiguration>(createStringResource("pageLogging.appenders.name"),
				"name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
				onLoggerClick(target, rowModel);
			}
		};
		columns.add(column);

		columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.pattern"), "pattern"));
		columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.filePath"), "filePath"));
		columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.filePattern"),
				"filePattern"));
		columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.maxHistory"), "maxHistory"));
		columns.add(new PropertyColumn(createStringResource("pageLogging.appenders.maxFileSize"),
				"maxFileSize"));
		columns.add(new CheckBoxColumn(createStringResource("pageLogging.appenders.appending"), "appending"));

		ISortableDataProvider<AppenderConfiguration> provider = new ListDataProvider<AppenderConfiguration>(
				new PropertyModel<List<AppenderConfiguration>>(model, "appenders"));
		TablePanel table = new TablePanel<AppenderConfiguration>("appendersTable", provider, columns);
		table.setOutputMarkupId(true);
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

	private void initRoot(final AccordionItem loggers) {
		DropDownChoice<LoggingLevelType> rootLevel = createComboBox("rootLevel",
				new PropertyModel<LoggingLevelType>(model, "rootLevel"), createLoggingLevelModel());

		loggers.getBodyContainer().add(rootLevel);

		DropDownChoice<String> rootAppender = createComboBox("rootAppender", new PropertyModel<String>(model,
				"rootAppender"), createAppendersListModel());
		loggers.getBodyContainer().add(rootAppender);

		DropDownChoice<LoggingLevelType> midPointLevel = createComboBox("midPointLevel",
				new PropertyModel<LoggingLevelType>(model, "midPointLevel"), createLoggingLevelModel());
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
				OperationResult result = new OperationResult("Save logging configuration");
				String oid = SystemObjectsType.SYSTEM_CONFIGURATION.value();

				try {
					Task task = taskManager.createTaskInstance(UPDATE_LOGGING_CONFIGURATION);
					PrismObject<SystemConfigurationType> newObject = oldObject.clone();
					newObject.asObjectable().setLogging(createConfiguration());

					ObjectDelta<SystemConfigurationType> delta = DiffUtil.diff(oldObject, newObject);
					getModelService().modifyObject(SystemConfigurationType.class, oid,
							delta.getModifications(), task, result);
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				// todo implement
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
		AccordionItem item = (AccordionItem)accordion.getBodyContainer().get("loggers");
		return (TablePanel) item.getBodyContainer().get("loggersTable");
	}
	
	private TablePanel getAppendersTable() {
		Accordion accordion = (Accordion) get("mainForm:accordion");
		AccordionItem item = (AccordionItem)accordion.getBodyContainer().get("appenders");
		return (TablePanel) item.getBodyContainer().get("appendersTable");
	}

	private void addLoggerPerformed(AjaxRequestTarget target) {
		Iterator<LoggerConfiguration> iterator = model.getObject().getLoggers().iterator();
	}

	private void addAppenderPerformed(AjaxRequestTarget target) {
		// todo implement
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

	private void onLoggerClick(AjaxRequestTarget target, IModel<LoggerConfiguration> rowModel) {
		// todo implement
	}
}
