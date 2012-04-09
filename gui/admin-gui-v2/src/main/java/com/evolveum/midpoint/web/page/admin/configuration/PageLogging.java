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
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

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

        IColumn column = new CheckBoxColumn<TaskType>();
        columns.add(column);

        column = new LinkColumn<Selectable<TaskType>>(createStringResource("pageLogging.classPackageSubsystem"), "value.name") {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<Selectable<TaskType>> rowModel) {
                //todo implement
            }
        };
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("pageLogging.loggersLevel"), "value.level"));
        columns.add(new PropertyColumn(createStringResource("pageLogging.loggersAppender"), "value.appender"));

        TablePanel table = new TablePanel<TaskType>("loggersTable", TaskType.class, columns);
        table.setShowPaging(false);
        table.setTableCssClass("autowidth");
        loggers.getBodyContainer().add(table);
        //todo implement

        AjaxLinkButton addLogger = new AjaxLinkButton("addLogger",
                createStringResource("pageLogging.button.addLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //todo implement
            }
        };
        loggers.getBodyContainer().add(addLogger);

        AjaxLinkButton deleteLogger = new AjaxLinkButton("deleteLogger",
                createStringResource("pageLogging.button.deleteLogger")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //todo implement
            }
        };
        loggers.getBodyContainer().add(deleteLogger);

        initSubsystem(loggers);
    }

    private void initSubsystem(AccordionItem loggers) {
        DropDownChoice<SubsystemLevel> subsystemLevel = createComboBox("subsystemLevel",
                new PropertyModel<SubsystemLevel>(model, "subsystemLevel"), createSubsystemLevelModel());
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
}
