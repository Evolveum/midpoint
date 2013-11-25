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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.AEPlevel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.LoggingDto;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class SystemConfigPanel extends SimplePanel<SystemConfigurationDto> {

    private static final Trace LOGGER = TraceManager.getTrace(SystemConfigPanel.class);

    private static final String DOT_CLASS = LoggingConfigPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadLoggingConfiguration";

    private static final String ID_GLOBAL_PASSWORD_POLICY_CHOOSER = "passwordPolicyChooser";
    private static final String ID_GLOBAL_USER_TEMPLATE_CHOOSER = "userTemplateChooser";
    private static final String ID_GLOBAL_AEP = "aepChooser";
    private static final String ID_CLEANUP_AUDIT_RECORDS = "auditRecordsCleanup";
    private static final String ID_CLEANUP_CLOSED_TASKS = "closedTasksCleanup";


    private static final String DEFAULT_CHOOSE_VALUE_NAME = "None";
    private static final String DEFAULT_CHOOSE_VALUE_OID = "";

    private ObjectViewDto<ValuePolicyType> passPolicyDto;
    private ObjectViewDto<ObjectTemplateType> objectTemplateDto;
    private ChooseTypePanel passPolicyChoosePanel;
    private ChooseTypePanel userTemplateChoosePanel;

    public SystemConfigPanel(String id, IModel<SystemConfigurationDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout(){
        passPolicyDto = loadPasswordPolicy();
        objectTemplateDto = loadObjectTemplate();

        passPolicyChoosePanel = new ChooseTypePanel(ID_GLOBAL_PASSWORD_POLICY_CHOOSER,
                new PropertyModel<ObjectViewDto>(this, "passPolicyDto"));
        userTemplateChoosePanel = new ChooseTypePanel(ID_GLOBAL_USER_TEMPLATE_CHOOSER,
                new PropertyModel<ObjectViewDto>(this, "objectTemplateDto"));

        add(passPolicyChoosePanel);
        add(userTemplateChoosePanel);

        DropDownChoice<AEPlevel> aepLevel = new DropDownChoice<AEPlevel>(ID_GLOBAL_AEP,
                new PropertyModel<AEPlevel>(getModel(), "aepLevel"),
                WebMiscUtil.createReadonlyModelFromEnum(AEPlevel.class),
                new EnumChoiceRenderer<AEPlevel>(SystemConfigPanel.this));
        add(aepLevel);

        TextField<String> auditRecordsField = new TextField<String>(ID_CLEANUP_AUDIT_RECORDS, new PropertyModel<String>(getModel(), "auditCleanupValue"));
        TextField<String> closedTasksField = new TextField<String>(ID_CLEANUP_CLOSED_TASKS, new PropertyModel<String>(getModel(), "taskCleanupValue"));
        add(auditRecordsField);
        add(closedTasksField);
    }

    private ObjectViewDto<ValuePolicyType> loadPasswordPolicy(){
        ValuePolicyType passPolicy = getModel().getObject().getGlobalPasswordPolicy();

        if(passPolicy != null){
            passPolicyDto = new ObjectViewDto<ValuePolicyType>(passPolicy.getOid(), passPolicy.getName().getOrig());
        }else {
            passPolicyDto = new ObjectViewDto<ValuePolicyType>(DEFAULT_CHOOSE_VALUE_OID, DEFAULT_CHOOSE_VALUE_NAME);
        }

        passPolicyDto.setType(ValuePolicyType.class);
        return passPolicyDto;
    }

    private ObjectViewDto<ObjectTemplateType> loadObjectTemplate(){
        ObjectTemplateType objectTemplate = getModel().getObject().getGlobalObjectTemplate();

        if(objectTemplate != null){
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>(objectTemplate.getOid(), objectTemplate.getName().getOrig());
        }else {
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>(DEFAULT_CHOOSE_VALUE_OID, DEFAULT_CHOOSE_VALUE_NAME);
        }

        objectTemplateDto.setType(ObjectTemplateType.class);
        return objectTemplateDto;
    }


}
