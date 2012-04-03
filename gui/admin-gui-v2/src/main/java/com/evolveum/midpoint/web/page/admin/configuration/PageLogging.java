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
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
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
        //todo implement
    }

    private void initAppenders(AccordionItem appenders) {
        //todo implement
    }

    private void initRoot(final Form mainForm) {
        DropDownChoice<LoggingLevelType> rootLevel = new DropDownChoice<LoggingLevelType>("rootLevel",
                new PropertyModel<LoggingLevelType>(model, "rootLevel"), createLoggingLevelModel());
        mainForm.add(rootLevel);

        DropDownChoice<String> rootAppender = new DropDownChoice<String>("rootAppender",
                new PropertyModel<String>(model, "rootAppender"), createAppendersListModel());
        mainForm.add(rootAppender);

        DropDownChoice<LoggingLevelType> midPointLevel = new DropDownChoice<LoggingLevelType>("midPointLevel",
                new PropertyModel<LoggingLevelType>(model, "midPointLevel"), createLoggingLevelModel());
        mainForm.add(midPointLevel);

        DropDownChoice<String> midPointAppender = new DropDownChoice<String>("midPointAppender",
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
                target.appendJavaScript("init();");
                target.add(mainForm);
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

        DropDownChoice<String> auditAppender = new DropDownChoice<String>("auditAppender",
                new PropertyModel<String>(model, "auditAppender"), createAppendersListModel());
        mainForm.add(auditAppender);
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

    private StringResourceModel createStringResource(String resourceKey) {
        return new StringResourceModel(resourceKey, this, null, null, null);
    }

}
