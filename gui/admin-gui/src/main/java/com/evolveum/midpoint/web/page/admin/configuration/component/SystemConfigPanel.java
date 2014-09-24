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

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectPolicyConfigurationEditor;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AEPlevel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectPolicyConfigurationTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import com.evolveum.midpoint.web.component.form.Form;

import java.util.List;

/**
 * @author lazyman
 */
public class SystemConfigPanel extends SimplePanel<SystemConfigurationDto> {

    private static final String ID_GLOBAL_PASSWORD_POLICY_CHOOSER = "passwordPolicyChooser";
    private static final String ID_GLOBAL_USER_TEMPLATE_CHOOSER = "userTemplateChooser";
    private static final String ID_OBJECT_POLICY_EDITOR = "objectPolicyEditor";
    private static final String ID_GLOBAL_AEP = "aepChooser";
    private static final String ID_CLEANUP_AUDIT_RECORDS = "auditRecordsCleanup";
    private static final String ID_CLEANUP_CLOSED_TASKS = "closedTasksCleanup";
    private static final String ID_CLEANUP_AUDIT_RECORDS_TOOLTIP = "auditRecordsCleanupTooltip";
    private static final String ID_CLEANUP_CLOSED_TASKS_TOOLTIP = "closedTasksCleanupTooltip";

    private static final String ID_DEFAULT_FROM = "defaultFrom";
    private static final String ID_DEBUG = "debugCheckbox";
    private static final String ID_HOST = "host";
    private static final String ID_PORT = "port";
    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";
    private static final String ID_TRANSPORT_SECURITY = "transportSecurity";
    private static final String ID_REDIRECT_TO_FILE = "redirectToFile";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_LABEL_SIZE = "col-lg-4";
    private static final String ID_INPUT_SIZE = "col-lg-4";

//    private ChooseTypePanel passPolicyChoosePanel;
//    private ChooseTypePanel userTemplateChoosePanel;

    public SystemConfigPanel(String id, IModel<SystemConfigurationDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout(){
        Form form = new Form(ID_MAIN_FORM, true);
        form.setOutputMarkupId(true);
        add(form);

        ChooseTypePanel passPolicyChoosePanel = new ChooseTypePanel(ID_GLOBAL_PASSWORD_POLICY_CHOOSER,
                new PropertyModel<ObjectViewDto>(getModel(), SystemConfigurationDto.F_PASSWORD_POLICY));
        ChooseTypePanel userTemplateChoosePanel = new ChooseTypePanel(ID_GLOBAL_USER_TEMPLATE_CHOOSER,
                new PropertyModel<ObjectViewDto>(getModel(), SystemConfigurationDto.F_OBJECT_TEMPLATE));

        form.add(passPolicyChoosePanel);
        form.add(userTemplateChoosePanel);

        ObjectPolicyConfigurationEditor objectPolicyEditor = new ObjectPolicyConfigurationEditor(ID_OBJECT_POLICY_EDITOR,
                new PropertyModel<List<ObjectPolicyConfigurationTypeDto>>(getModel(), SystemConfigurationDto.F_OBJECT_POLICY_LIST));
        form.add(objectPolicyEditor);

        DropDownChoice<AEPlevel> aepLevel = new DropDownChoice<>(ID_GLOBAL_AEP,
                new PropertyModel<AEPlevel>(getModel(), SystemConfigurationDto.F_AEP_LEVEL),
                WebMiscUtil.createReadonlyModelFromEnum(AEPlevel.class),
                new EnumChoiceRenderer<AEPlevel>(SystemConfigPanel.this));
        aepLevel.setOutputMarkupId(true);
        if(aepLevel.getModel().getObject() == null){
            aepLevel.getModel().setObject(null);
        }
        form.add(aepLevel);

        TextField<String> auditRecordsField = new TextField<>(ID_CLEANUP_AUDIT_RECORDS, new PropertyModel<String>(getModel(), SystemConfigurationDto.F_AUDIT_CLEANUP));
        TextField<String> closedTasksField = new TextField<>(ID_CLEANUP_CLOSED_TASKS, new PropertyModel<String>(getModel(), SystemConfigurationDto.F_TASK_CLEANUP));
        form.add(auditRecordsField);
        form.add(closedTasksField);

        createTooltip(ID_CLEANUP_AUDIT_RECORDS_TOOLTIP, form);
        createTooltip(ID_CLEANUP_CLOSED_TASKS_TOOLTIP, form);

        TextField<String> defaultFromField = new TextField<>(ID_DEFAULT_FROM, new PropertyModel<String>(getModel(), "notificationConfig.defaultFrom"));
        CheckBox debugCheck = new CheckBox(ID_DEBUG, new PropertyModel<Boolean>(getModel(), "notificationConfig.debug"));
        TextField<String> hostField = new TextField<>(ID_HOST, new PropertyModel<String>(getModel(), "notificationConfig.host"));
        TextField<Integer> portField = new TextField<>(ID_PORT, new PropertyModel<Integer>(getModel(), "notificationConfig.port"));
        TextField<String> userNameField = new TextField<>(ID_USERNAME, new PropertyModel<String>(getModel(), "notificationConfig.username"));
        PasswordTextField passwordField = new PasswordTextField(ID_PASSWORD, new PropertyModel<String>(getModel(), "notificationConfig.password"));
        passwordField.setRequired(false);

        if(getModel().getObject() != null){
            if(getModel().getObject().getNotificationConfig().getPassword() != null){
                passwordField.add(new AttributeAppender("placeholder", createStringResource("SystemConfigPanel.mail.password.placeholder.set")));
            } else {
                passwordField.add(new AttributeAppender("placeholder", createStringResource("SystemConfigPanel.mail.password.placeholder.empty")));
            }
        }

        TextField<String> redirectToFileField = new TextField<>(ID_REDIRECT_TO_FILE, new PropertyModel<String>(getModel(), "notificationConfig.redirectToFile"));

        DropDownFormGroup transportSecurity = new DropDownFormGroup<>(ID_TRANSPORT_SECURITY, new PropertyModel<MailTransportSecurityType>(getModel(),
                "notificationConfig.mailTransportSecurityType"), WebMiscUtil.createReadonlyModelFromEnum(MailTransportSecurityType.class),
                new EnumChoiceRenderer<MailTransportSecurityType>(this), createStringResource("SystemConfigPanel.mail.transportSecurity"),
                ID_LABEL_SIZE, ID_INPUT_SIZE, false);

        form.add(defaultFromField);
        form.add(debugCheck);
        form.add(hostField);
        form.add(portField);
        form.add(userNameField);
        form.add(passwordField);
        form.add(redirectToFileField);
        form.add(transportSecurity);
    }

    private void createTooltip(String id, WebMarkupContainer parent) {
        Label tooltip = new Label(id);
        tooltip.add(new InfoTooltipBehavior());
        parent.add(tooltip);
    }
}
