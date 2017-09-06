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
import com.evolveum.midpoint.web.component.form.TextFormGroup;
import org.apache.wicket.markup.html.basic.Label;
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
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * @author lazyman
 */
public class SystemConfigPanel extends BasePanel<SystemConfigurationDto> {

	private static final long serialVersionUID = 1L;
	private static final String ID_GLOBAL_PASSWORD_POLICY_CHOOSER = "passwordPolicyChooser";
    private static final String ID_GLOBAL_SECURITY_POLICY_CHOOSER = "securityPolicyChooser";
    private static final String ID_OBJECT_POLICY_EDITOR = "objectPolicyEditor";
    private static final String ID_GLOBAL_CHOOSEASSIGNEMNTPOLICYENFORCEMENT = "chooseAssignmentPolicyEnforcement";
    private static final String ID_CLEANUP_AUDIT_RECORDS = "auditRecordsCleanup";
    private static final String ID_CLEANUP_CLOSED_TASKS = "closedTasksCleanup";
    private static final String ID_INFORMATION_DEPLOYMENT_NAME = "deploymentInformationName";
    private static final String ID_INFORMATION_DEPLOYMENT_COLOUR = "deploymentInformationColour";
    private static final String ID_CLEANUP_AUDIT_RECORDS_TOOLTIP = "auditRecordsCleanupTooltip";
    private static final String ID_CLEANUP_CLOSED_TASKS_TOOLTIP = "closedTasksCleanupTooltip";

    private static final String ID_EXPERIMENTAL_CODE_CHECKBOX = "experimentalCodeCheckbox";

    private static final String ID_LABEL_SIZE = "col-md-6";
    private static final String ID_INPUT_SIZE = "col-md-6";


    public SystemConfigPanel(String id, IModel<SystemConfigurationDto> model) {
        super(id, model);

        setOutputMarkupId(true);
        initLayout();
    }

    protected void initLayout(){

        ChooseTypePanel<ValuePolicyType> passPolicyChoosePanel = new ChooseTypePanel<ValuePolicyType>(ID_GLOBAL_PASSWORD_POLICY_CHOOSER,
                new PropertyModel<ObjectViewDto<ValuePolicyType>>(getModel(), SystemConfigurationDto.F_PASSWORD_POLICY));

        ChooseTypePanel<SecurityPolicyType> securityPolicyChoosePanel = new ChooseTypePanel<SecurityPolicyType>(ID_GLOBAL_SECURITY_POLICY_CHOOSER,
                new PropertyModel<ObjectViewDto<SecurityPolicyType>>(getModel(), SystemConfigurationDto.F_SECURITY_POLICY));
        add(passPolicyChoosePanel);
        add(securityPolicyChoosePanel);

        ObjectPolicyConfigurationEditor objectPolicyEditor = new ObjectPolicyConfigurationEditor(ID_OBJECT_POLICY_EDITOR,
                new PropertyModel<List<ObjectPolicyConfigurationTypeDto>>(getModel(), SystemConfigurationDto.F_OBJECT_POLICY_LIST));
        add(objectPolicyEditor);

        DropDownFormGroup assignmentPolicyEnforcementLevel = new DropDownFormGroup(ID_GLOBAL_CHOOSEASSIGNEMNTPOLICYENFORCEMENT,
                new PropertyModel<AEPlevel>(getModel(), SystemConfigurationDto.F_ASSIGNMENTPOLICYENFORCEMENT_LEVEL),
                WebComponentUtil.createReadonlyModelFromEnum(AEPlevel.class),new EnumChoiceRenderer<AEPlevel>(SystemConfigPanel.this),
                createStringResource("SystemConfigPanel.assignmentPolicyEnforcement"),ID_LABEL_SIZE,ID_INPUT_SIZE, false);

        assignmentPolicyEnforcementLevel.setOutputMarkupId(true);
        assignmentPolicyEnforcementLevel.getInput().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(assignmentPolicyEnforcementLevel);


       // TextField<String> auditRecordsField = WebComponentUtil.createAjaxTextField(ID_CLEANUP_AUDIT_RECORDS, new PropertyModel<String>(getModel(), SystemConfigurationDto.F_AUDIT_CLEANUP));
        TextFormGroup auditRecordsField = new TextFormGroup(ID_CLEANUP_AUDIT_RECORDS, new PropertyModel<String>(getModel(),SystemConfigurationDto.F_AUDIT_CLEANUP), createStringResource("SystemConfigPanel.cleanupPolicy.auditRecords"),"SystemConfigPanel.tooltip.duration", true,ID_LABEL_SIZE,ID_INPUT_SIZE,false,false);
        TextFormGroup closedTasksField = new TextFormGroup(ID_CLEANUP_CLOSED_TASKS, new PropertyModel<String>(getModel(),SystemConfigurationDto.F_TASK_CLEANUP), createStringResource("SystemConfigPanel.cleanupPolicy.closedTasks"),"SystemConfigPanel.tooltip.duration",true,ID_LABEL_SIZE,ID_INPUT_SIZE, false, false);

        // TextField<String> closedTasksField = WebComponentUtil.createAjaxTextField(ID_CLEANUP_CLOSED_TASKS, new PropertyModel<String>(getModel(), SystemConfigurationDto.F_TASK_CLEANUP));

      //  TextField<String> deploymentNameField = WebComponentUtil.createAjaxTextField(ID_INFORMATION_DEPLOYMENT_NAME, new PropertyModel<String>(getModel(), SystemConfigurationDto.F_DEPLOYMENT_NAME));

        //TextField<String> deploymentColourField = WebComponentUtil.createAjaxTextField(ID_INFORMATION_DEPLOYMENT_COLOUR, new PropertyModel<String>(getModel(), SystemConfigurationDto.F_DEPLOYMENT_COLOUR));

        add(auditRecordsField);
        add(closedTasksField);

       // createTooltip(ID_CLEANUP_AUDIT_RECORDS_TOOLTIP);
        //createTooltip(ID_CLEANUP_CLOSED_TASKS_TOOLTIP);


        //CheckBox experimentalCodeCheck = WebComponentUtil.createAjaxCheckBox(ID_EXPERIMENTAL_CODE_CHECKBOX, new PropertyModel<Boolean>(getModel(), SystemConfigurationDto.F_ENABLE_EXPERIMENTAL_CODE));
        CheckFormGroup experimentalCodeCheck = new CheckFormGroup(ID_EXPERIMENTAL_CODE_CHECKBOX, new PropertyModel<Boolean>(getModel(), SystemConfigurationDto.F_ENABLE_EXPERIMENTAL_CODE), createStringResource("SystemConfigPanel.misc.enableExperimentalCode"),ID_LABEL_SIZE, ID_INPUT_SIZE);
        add(experimentalCodeCheck);


    }



    private void createTooltip(String id) {
        Label tooltip = new Label(id);
        tooltip.add(new InfoTooltipBehavior());
        add(tooltip);
    }
}
