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

import com.evolveum.midpoint.web.component.ObjectPolicyConfigurationEditor;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.*;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.AbstractReadOnlyModel;
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
    private static final String ID_OBJECT_POLICY_DEPRECATED_WARNING = "userTemplateDeprecatedWarning";

    private static final String ID_DEFAULT_FROM = "defaultFrom";
    private static final String ID_DEBUG = "debugCheckbox";
    private static final String ID_EXPERIMENTAL_CODE_CHECKBOX = "experimentalCodeCheckbox";

    private static final String ID_MAIL_SERVER = "mailServer";
    private static final String ID_MAIL_SERVER_CONFIG_CONTAINER = "mailServerConfigContainer";
    private static final String ID_BUTTON_ADD_NEW_MAIL_SERVER_CONFIG = "addNewConfigButton";
    private static final String ID_BUTTON_REMOVE_MAIL_SERVER_CONFIG = "removeConfigButton";
    private static final String ID_MAIL_SERVER_TOOLTIP = "serverConfigTooltip";
    private static final String ID_HOST = "host";
    private static final String ID_PORT = "port";
    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";
    private static final String ID_TRANSPORT_SECURITY = "transportSecurity";
    private static final String ID_REDIRECT_TO_FILE = "redirectToFile";

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_LABEL_SIZE = "col-lg-4";
    private static final String ID_INPUT_SIZE = "col-lg-4";


    public SystemConfigPanel(String id, IModel<SystemConfigurationDto> model) {
        super(id, model);

        setOutputMarkupId(true);
    }

    @Override
    protected void initLayout(){
        Form form = new Form(ID_MAIN_FORM, true);
        form.setOutputMarkupId(true);
        add(form);

        ChooseTypePanel passPolicyChoosePanel = new ChooseTypePanel(ID_GLOBAL_PASSWORD_POLICY_CHOOSER,
                new PropertyModel<ObjectViewDto>(getModel(), SystemConfigurationDto.F_PASSWORD_POLICY));

//        TODO - remove this before 3.2 release, this is kept only for compatibility reasons. ObjectPolicyConfigurationEditor
//        is new incarnation of this deprecated config option.
        ChooseTypePanel userTemplateChoosePanel = new ChooseTypePanel(ID_GLOBAL_USER_TEMPLATE_CHOOSER,
                new PropertyModel<ObjectViewDto>(getModel(), SystemConfigurationDto.F_OBJECT_TEMPLATE));

        form.add(passPolicyChoosePanel);
        form.add(userTemplateChoosePanel);

        Label objectPolicyDeprecationWarningTooltip = new Label(ID_OBJECT_POLICY_DEPRECATED_WARNING);
        objectPolicyDeprecationWarningTooltip.add(new InfoTooltipBehavior(){

            @Override
            public String getCssClass() {
                return "fa fa-fw fa-exclamation-triangle text-danger";
            }
        });
        form.add(objectPolicyDeprecationWarningTooltip);

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

        DropDownChoice mailServerConfigChooser = new DropDownChoice<>(ID_MAIL_SERVER,
                new PropertyModel<MailServerConfigurationTypeDto>(getModel(), "notificationConfig." + NotificationConfigurationDto.F_SELECTED_SERVER),
                new AbstractReadOnlyModel<List<MailServerConfigurationTypeDto>>() {

                    @Override
                    public List<MailServerConfigurationTypeDto> getObject() {
                        return getModel().getObject().getNotificationConfig().getServers();
                    }
                }, new IChoiceRenderer<MailServerConfigurationTypeDto>() {

            @Override
            public String getDisplayValue(MailServerConfigurationTypeDto object) {
                return object.getHost();
            }

            @Override
            public String getIdValue(MailServerConfigurationTypeDto object, int index) {
                return Integer.toString(index);
            }
        });
        mailServerConfigChooser.setNullValid(true);
        mailServerConfigChooser.add(new AjaxFormSubmitBehavior("onclick"){

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                getForm().onFormSubmitted();
            }
        });
        mailServerConfigChooser.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                preparePasswordFieldPlaceholder();
                target.add(SystemConfigPanel.this);
            }
        });
        form.add(mailServerConfigChooser);

        Label serverConfigTooltip = new Label(ID_MAIL_SERVER_TOOLTIP);
        serverConfigTooltip.add(new InfoTooltipBehavior());
        form.add(serverConfigTooltip);

        WebMarkupContainer serverConfigContainer = new WebMarkupContainer(ID_MAIL_SERVER_CONFIG_CONTAINER);
        serverConfigContainer.setOutputMarkupId(true);
        serverConfigContainer.setOutputMarkupPlaceholderTag(true);
        serverConfigContainer.add(new VisibleEnableBehaviour(){

            @Override
            public boolean isVisible() {
                if(getModelObject() != null && getModelObject().getNotificationConfig() != null){
                    return getModelObject().getNotificationConfig().getSelectedServer() != null;
                }

                return false;
            }
        });
        form.add(serverConfigContainer);

        TextField<String> hostField = new TextField<>(ID_HOST, new PropertyModel<String>(getModel(), "notificationConfig.selectedServer.host"));
        TextField<Integer> portField = new TextField<>(ID_PORT, new PropertyModel<Integer>(getModel(), "notificationConfig.selectedServer.port"));
        TextField<String> userNameField = new TextField<>(ID_USERNAME, new PropertyModel<String>(getModel(), "notificationConfig.selectedServer.username"));
        PasswordTextField passwordField = new PasswordTextField(ID_PASSWORD, new PropertyModel<String>(getModel(), "notificationConfig.selectedServer.password"));
        passwordField.setRequired(false);

        TextField<String> redirectToFileField = new TextField<>(ID_REDIRECT_TO_FILE, new PropertyModel<String>(getModel(), "notificationConfig.redirectToFile"));

        DropDownFormGroup transportSecurity = new DropDownFormGroup<>(ID_TRANSPORT_SECURITY, new PropertyModel<MailTransportSecurityType>(getModel(),
                "notificationConfig.selectedServer.mailTransportSecurityType"), WebMiscUtil.createReadonlyModelFromEnum(MailTransportSecurityType.class),
                new EnumChoiceRenderer<MailTransportSecurityType>(this), createStringResource("SystemConfigPanel.mail.transportSecurity"),
                ID_LABEL_SIZE, ID_INPUT_SIZE, false);

        serverConfigContainer.add(hostField);
        serverConfigContainer.add(portField);
        serverConfigContainer.add(userNameField);
        serverConfigContainer.add(passwordField);
        serverConfigContainer.add(transportSecurity);

        form.add(defaultFromField);
        form.add(debugCheck);
        form.add(redirectToFileField);

        AjaxSubmitLink buttonAddNewMailServerConfig = new AjaxSubmitLink(ID_BUTTON_ADD_NEW_MAIL_SERVER_CONFIG) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                MailServerConfigurationTypeDto newConfig = new MailServerConfigurationTypeDto();
                newConfig.setHost(getString("SystemConfigPanel.mail.config.placeholder"));

                if(getModelObject() != null && getModelObject().getNotificationConfig() != null){
                    getModelObject().getNotificationConfig().getServers().add(newConfig);
                    getModelObject().getNotificationConfig().setSelectedServer(newConfig);
                }

                preparePasswordFieldPlaceholder();
                target.add(SystemConfigPanel.this, getPageBase().getFeedbackPanel());
            }

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        form.add(buttonAddNewMailServerConfig);

        AjaxSubmitLink removeMailServerConfig = new AjaxSubmitLink(ID_BUTTON_REMOVE_MAIL_SERVER_CONFIG) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                if(getModelObject() != null && getModelObject().getNotificationConfig() != null){
                    NotificationConfigurationDto notificationConfig = getModelObject().getNotificationConfig();

                    MailServerConfigurationTypeDto selected = notificationConfig.getSelectedServer();

                    if(notificationConfig.getServers().contains(selected)){
                        notificationConfig.getServers().remove(selected);
                        notificationConfig.setSelectedServer(null);
                    } else {
                        warn(getString("SystemConfigPanel.mail.server.remove.warn"));
                    }

                    target.add(SystemConfigPanel.this, getPageBase().getFeedbackPanel());
                }
            }

            @Override
            protected void onError(AjaxRequestTarget target, org.apache.wicket.markup.html.form.Form<?> form) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        removeMailServerConfig.add(new AttributeAppender("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if(getModelObject() != null && getModelObject().getNotificationConfig() != null &&
                        getModelObject().getNotificationConfig().getSelectedServer() != null){
                    return null;
                } else {
                    return " disabled";
                }
            }
        }));
        form.add(removeMailServerConfig);

        CheckBox experimentalCodeCheck = new CheckBox(ID_EXPERIMENTAL_CODE_CHECKBOX, new PropertyModel<Boolean>(getModel(), SystemConfigurationDto.F_ENABLE_EXPERIMENTAL_CODE));
        form.add(experimentalCodeCheck);
    }

    private void preparePasswordFieldPlaceholder(){
        PasswordTextField passwordField = (PasswordTextField)get(ID_MAIN_FORM + ":" + ID_MAIL_SERVER_CONFIG_CONTAINER + ":" + ID_PASSWORD);

        if(getModelObject() != null){
            if(getModelObject().getNotificationConfig().getSelectedServer() != null &&
                    getModelObject().getNotificationConfig().getSelectedServer().getPassword() != null){

                passwordField.add(new AttributeModifier("placeholder", createStringResource("SystemConfigPanel.mail.password.placeholder.set")));
            } else {
                passwordField.add(new AttributeModifier("placeholder", createStringResource("SystemConfigPanel.mail.password.placeholder.empty")));
            }
        }
    }

    private void createTooltip(String id, WebMarkupContainer parent) {
        Label tooltip = new Label(id);
        tooltip.add(new InfoTooltipBehavior());
        parent.add(tooltip);
    }
}
