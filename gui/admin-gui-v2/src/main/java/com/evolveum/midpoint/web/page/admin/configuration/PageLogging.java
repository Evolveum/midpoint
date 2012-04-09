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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggerConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggerProvider;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SubsystemLevel;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        List<IColumn<TaskType>> columns = new ArrayList<IColumn<TaskType>>();

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
                        LoggerConfiguration config  = (LoggerConfiguration) rowModel.getObject();

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

        TablePanel table = new TablePanel<TaskType>("loggersTable", new LoggerProvider(model), columns);
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
        IChoiceRenderer<SubsystemLevel> renderer = new IChoiceRenderer<SubsystemLevel>() {

            @Override
            public Object getDisplayValue(SubsystemLevel object) {
                return new StringResourceModel(object.getLocalizationKey(),
                        PageLogging.this, null).getString();
            }

            @Override
            public String getIdValue(SubsystemLevel object, int index) {
                return object.name();
            }
        };

        DropDownChoice<SubsystemLevel> subsystemLevel = createComboBox("subsystemLevel",
                new PropertyModel<SubsystemLevel>(model, "subsystemLevel"), createSubsystemLevelModel(), renderer);
        loggers.getBodyContainer().add(subsystemLevel);

        DropDownChoice<String> subsystemAppender = createComboBox("subsystemAppender",
                new PropertyModel<String>(model, "subsystemAppender"), createAppendersListModel());
        loggers.getBodyContainer().add(subsystemAppender);
    }

    private IModel<List<SubsystemLevel>> createSubsystemLevelModel() {
        return new AbstractReadOnlyModel<List<SubsystemLevel>>() {

            @Override
            public List<SubsystemLevel> getObject() {
                List<SubsystemLevel> levels = new ArrayList<SubsystemLevel>();
                Collections.addAll(levels, SubsystemLevel.values());

                return levels;
            }
        };
    }

    private void initAppenders(AccordionItem appenders) {
        //todo implement

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
                System.out.println(model.getObject().getRootLevel());
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

                //todo implement
                list.add("implement");

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
