/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.util.List;

import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.form.TextAreaFormGroup;
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.ObjectPolicyConfigurationEditor;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AEPlevel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.ObjectPolicyConfigurationTypeDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

/**
 * @author lazyman
 */
public class SystemConfigPanel extends BasePanel<SystemConfigurationDto> {

    private static final long serialVersionUID = 1L;
    private static final String ID_GLOBAL_SECURITY_POLICY_CHOOSER = "securityPolicyChooser";
    private static final String ID_OBJECT_POLICY_EDITOR = "objectPolicyEditor";
    private static final String ID_GLOBAL_CHOOSEASSIGNEMNTPOLICYENFORCEMENT = "chooseAssignmentPolicyEnforcement";
    private static final String ID_AUDIT_RECORDS_CLEANUP_AGE = "auditRecordsCleanupAge";
    private static final String ID_AUDIT_RECORDS_CLEANUP_RECORDS = "auditRecordsCleanupRecords";
    private static final String ID_CLOSED_TASKS_CLEANUP_AGE = "closedTasksCleanupAge";
    private static final String ID_CERTIFICATION_CAMPAIGNS_CLEANUP_AGE = "certificationCampaignsCleanupAge";
    private static final String ID_CERTIFICATION_CAMPAIGNS_CLEANUP_RECORDS = "certificationCampaignsCleanupRecords";
    private static final String ID_REPORTS_CLEANUP_AGE = "reportsCleanupAge";
    private static final String ID_RESULTS_CLEANUP_AGE = "resultsCleanupAge";
    private static final String ID_RESULTS_CLEANUP_RECORDS = "resultsCleanupRecords";

    private static final String ID_DEPLOYMENT_INFO_NAME = "deploymentInfoName";
    private static final String ID_DEPLOYMENT_INFO_DESCRIPTION = "deploymentInfoDescription";
    private static final String ID_DEPLOYMENT_INFO_HEADER_COLOR = "deploymentInfoHeaderColor";

    private static final String ID_EXPERIMENTAL_CODE_CHECKBOX = "experimentalCodeCheckbox";

    private static final String ID_DEPLOYMENT_INFO_CONTAINER = "deploymentInfoContainer";

    private static final String ID_DEPLOYMENT_INFO_SKIN = "deploymentInfoSkin";
    private static final String ID_DEPLOYMENT_INFO_CUSTOMER_URL = "deploymentInfoCustomerUrl";
    private static final String ID_DEPLOYMENT_INFO_PARTNER_NAME = "deploymentInfoPartnerName";
    private static final String ID_DEPLOYMENT_INFO_SUBSCRIPTION_ID = "deploymentInfoSubscriptionId";
    private static final String ID_DEPLOYMENT_INFO_LOGO_CSS = "deploymentInfoLogoCssClass";
    private static final String ID_DEPLOYMENT_INFO_LOGO_IMAGEURL = "deploymentInfoLogoImageUrl";

    private static final String ID_LABEL_SIZE = "col-md-4";
    private static final String ID_INPUT_SIZE = "col-md-4";

    private static final String CLEANUP_LABEL_SIZE = "col-md-6";
    private static final String CLEANUP_INPUT_SIZE = "col-md-6";


    public SystemConfigPanel(String id, IModel<SystemConfigurationDto> model) {
        super(id, model);

        setOutputMarkupId(true);
        initLayout();
    }

    protected void initLayout() {
        ChooseTypePanel<SecurityPolicyType> securityPolicyChoosePanel = new ChooseTypePanel<SecurityPolicyType>(ID_GLOBAL_SECURITY_POLICY_CHOOSER,
                new PropertyModel<ObjectViewDto<SecurityPolicyType>>(getModel(), SystemConfigurationDto.F_SECURITY_POLICY));

        add(securityPolicyChoosePanel);

        ObjectPolicyConfigurationEditor objectPolicyEditor = new ObjectPolicyConfigurationEditor(ID_OBJECT_POLICY_EDITOR,
                new PropertyModel<List<ObjectPolicyConfigurationTypeDto>>(getModel(), SystemConfigurationDto.F_OBJECT_POLICY_LIST));
        add(objectPolicyEditor);

        DropDownFormGroup assignmentPolicyEnforcementLevel = new DropDownFormGroup(ID_GLOBAL_CHOOSEASSIGNEMNTPOLICYENFORCEMENT,
                new PropertyModel<AEPlevel>(getModel(), SystemConfigurationDto.F_ASSIGNMENTPOLICYENFORCEMENT_LEVEL),
                WebComponentUtil.createReadonlyModelFromEnum(AEPlevel.class), new EnumChoiceRenderer<AEPlevel>(SystemConfigPanel.this),
                createStringResource("SystemConfigPanel.assignmentPolicyEnforcement"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);

        assignmentPolicyEnforcementLevel.setOutputMarkupId(true);
        assignmentPolicyEnforcementLevel.getInput().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(assignmentPolicyEnforcementLevel);

        final String DURATION_TOOLTIP_KEY = "SystemConfigPanel.tooltip.duration";
        TextFormGroup auditRecordsAgeField = new TextFormGroup(ID_AUDIT_RECORDS_CLEANUP_AGE,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_AUDIT_CLEANUP_AGE),
                createStringResource("SystemConfigPanel.cleanupPolicy.auditRecords"),
                DURATION_TOOLTIP_KEY, true, CLEANUP_LABEL_SIZE, CLEANUP_INPUT_SIZE, false, false);
        add(auditRecordsAgeField);
        TextFormGroup auditRecordsRecordsField = new TextFormGroup(ID_AUDIT_RECORDS_CLEANUP_RECORDS,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_AUDIT_CLEANUP_RECORDS),
                createStringResource("SystemConfigPanel.cleanupPolicy.auditRecords.records"),
                null, true, CLEANUP_LABEL_SIZE, CLEANUP_INPUT_SIZE, false, false);
        add(auditRecordsRecordsField);
        TextFormGroup closedTasksAgeField = new TextFormGroup(ID_CLOSED_TASKS_CLEANUP_AGE,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_TASK_CLEANUP_AGE),
                createStringResource("SystemConfigPanel.cleanupPolicy.closedTasks"),
                DURATION_TOOLTIP_KEY, true, ID_LABEL_SIZE, ID_INPUT_SIZE, false, false);
        add(closedTasksAgeField);
        TextFormGroup campaignsAgeField = new TextFormGroup(ID_CERTIFICATION_CAMPAIGNS_CLEANUP_AGE,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_CAMPAIGN_CLEANUP_AGE),
                createStringResource("SystemConfigPanel.cleanupPolicy.certificationCampaigns"),
                DURATION_TOOLTIP_KEY, true, CLEANUP_LABEL_SIZE, CLEANUP_INPUT_SIZE, false, false);
        add(campaignsAgeField);
        TextFormGroup campaignsRecordsField = new TextFormGroup(ID_CERTIFICATION_CAMPAIGNS_CLEANUP_RECORDS,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_CAMPAIGN_CLEANUP_RECORDS),
                createStringResource("SystemConfigPanel.cleanupPolicy.certificationCampaigns.records"),
                null, true, CLEANUP_LABEL_SIZE, CLEANUP_INPUT_SIZE, false, false);
        add(campaignsRecordsField);
        TextFormGroup reportsAgeField = new TextFormGroup(ID_REPORTS_CLEANUP_AGE,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_REPORT_CLEANUP_AGE),
                createStringResource("SystemConfigPanel.cleanupPolicy.reports"),
                DURATION_TOOLTIP_KEY, true, ID_LABEL_SIZE, ID_INPUT_SIZE, false, false);
        add(reportsAgeField);
        TextFormGroup resultsAgeField = new TextFormGroup(ID_RESULTS_CLEANUP_AGE,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_RESULT_CLEANUP_AGE),
                createStringResource("SystemConfigPanel.cleanupPolicy.results"),
                DURATION_TOOLTIP_KEY, true, CLEANUP_LABEL_SIZE, CLEANUP_INPUT_SIZE, false, false);
        add(resultsAgeField);
        TextFormGroup resultsRecordsField = new TextFormGroup(ID_RESULTS_CLEANUP_RECORDS,
                new PropertyModel<>(getModel(), SystemConfigurationDto.F_RESULT_CLEANUP_RECORDS),
                createStringResource("SystemConfigPanel.cleanupPolicy.results.records"),
                null, true, CLEANUP_LABEL_SIZE, CLEANUP_INPUT_SIZE, false, false);
        add(resultsRecordsField);

        TextFormGroup deploymentInfoName = new TextFormGroup(ID_DEPLOYMENT_INFO_NAME, new PropertyModel<String>(getModel(), "deploymentInformation.name"), createStringResource("SystemConfigPanel.deploymentInformation.name"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        TextAreaFormGroup deploymentInfoDescription = new TextAreaFormGroup(ID_DEPLOYMENT_INFO_DESCRIPTION, new PropertyModel<String>(getModel(), "deploymentInformation.description"), createStringResource("SystemConfigPanel.deploymentInformation.description"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        TextFormGroup deploymentInfoHeaderColor = new TextFormGroup(ID_DEPLOYMENT_INFO_HEADER_COLOR, new PropertyModel<String>(getModel(), "deploymentInformation.headerColor"), createStringResource("SystemConfigPanel.deploymentInformation.headerColor"),"SystemConfigPanel.tooltip.color", true, ID_LABEL_SIZE, ID_INPUT_SIZE, false , false); // TODO change to a more user friendly solution

        TextFormGroup deploymentInfoImageUrl = new TextFormGroup(ID_DEPLOYMENT_INFO_LOGO_IMAGEURL, new PropertyModel<String>(getModel(), "deploymentInformation.logo.imageUrl"), createStringResource("SystemConfigPanel.deploymentInformation.logoImageUrl"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        TextFormGroup deploymentInfoCssClass = new TextFormGroup(ID_DEPLOYMENT_INFO_LOGO_CSS, new PropertyModel<String>(getModel(), "deploymentInformation.logo.cssClass"), createStringResource("SystemConfigPanel.deploymentInformation.logoCssClass"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);


        TextFormGroup deploymentInfoSkin = new TextFormGroup(ID_DEPLOYMENT_INFO_SKIN, new PropertyModel<String>(getModel(), "deploymentInformation.skin"), createStringResource("SystemConfigPanel.deploymentInformation.skin"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        TextFormGroup deploymentInfoCustomerUrl = new TextFormGroup(ID_DEPLOYMENT_INFO_CUSTOMER_URL, new PropertyModel<String>(getModel(), "deploymentInformation.customerUrl"), createStringResource("SystemConfigPanel.deploymentInformation.customerUrl"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        TextFormGroup deploymentInfoPartnerName = new TextFormGroup(ID_DEPLOYMENT_INFO_PARTNER_NAME, new PropertyModel<String>(getModel(), "deploymentInformation.partnerName"), createStringResource("SystemConfigPanel.deploymentInformation.partnerName"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);
        TextFormGroup deploymentInfoSubscriptionId = new TextFormGroup(ID_DEPLOYMENT_INFO_SUBSCRIPTION_ID, new PropertyModel<String>(getModel(), "deploymentInformation.subscriptionIdentifier"), createStringResource("SystemConfigPanel.deploymentInformation.subscriptionIdentifier"), ID_LABEL_SIZE, ID_INPUT_SIZE, false);

        WebMarkupContainer deploymentInfoContainer = new WebMarkupContainer(ID_DEPLOYMENT_INFO_CONTAINER);

        deploymentInfoContainer.add(deploymentInfoName);
        deploymentInfoContainer.add(deploymentInfoDescription);
        deploymentInfoContainer.add(deploymentInfoHeaderColor);

        deploymentInfoContainer.add(deploymentInfoImageUrl);
        deploymentInfoContainer.add(deploymentInfoCssClass);

        deploymentInfoContainer.add(deploymentInfoSkin);
        deploymentInfoContainer.add(deploymentInfoCustomerUrl);
        deploymentInfoContainer.add(deploymentInfoPartnerName);
        deploymentInfoContainer.add(deploymentInfoSubscriptionId);

        add(deploymentInfoContainer);


        CheckFormGroup experimentalCodeCheck = new CheckFormGroup(ID_EXPERIMENTAL_CODE_CHECKBOX, new PropertyModel<Boolean>(getModel(), SystemConfigurationDto.F_ENABLE_EXPERIMENTAL_CODE), createStringResource("SystemConfigPanel.misc.enableExperimentalCode"), ID_LABEL_SIZE, ID_INPUT_SIZE);
        add(experimentalCodeCheck);

    }
}
