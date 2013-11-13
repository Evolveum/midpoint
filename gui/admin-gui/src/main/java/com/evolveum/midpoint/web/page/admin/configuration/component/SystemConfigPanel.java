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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ValuePolicyType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class SystemConfigPanel extends SimplePanel<SystemConfigurationDto> {

    private static final Trace LOGGER = TraceManager.getTrace(SystemConfigPanel.class);

    private static final String ID_GLOBAL_PASSWORD_POLICY_CHOOSER = "passwordPolicyChooser";
    private static final String ID_GLOBAL_USER_TEMPLATE_CHOOSER = "userTemplateChooser";
    private static final String DEFAULT_CHOOSE_VALUE_NAME = "None";
    private static final String DEFAULT_CHOOSE_VALUE_OID = "";

    private ObjectViewDto<ValuePolicyType> passPolicyDto;
    private ObjectViewDto<ObjectTemplateType> objectTemplateDto;

    public SystemConfigPanel(String id, IModel<SystemConfigurationDto> model) {
        super(id, model);
    }

    private ObjectViewDto<ValuePolicyType> loadPasswordPolicy(){
        ValuePolicyType passPolicy = getModel().getObject().getConfig().asObjectable().getGlobalPasswordPolicy();

        if(passPolicy != null){
            passPolicyDto = new ObjectViewDto<ValuePolicyType>(passPolicy.getOid(), passPolicy.getName().getOrig());
        }else {
            passPolicyDto = new ObjectViewDto<ValuePolicyType>(DEFAULT_CHOOSE_VALUE_OID, DEFAULT_CHOOSE_VALUE_NAME);
        }

        passPolicyDto.setType(ValuePolicyType.class);
        return passPolicyDto;
    }

    private ObjectViewDto<ObjectTemplateType> loadObjectTemplate(){
        ObjectTemplateType objectTemplate = getModel().getObject().getConfig().asObjectable().getDefaultUserTemplate();

        if(objectTemplate != null){
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>(objectTemplate.getOid(), objectTemplate.getName().getOrig());
        }else {
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>(DEFAULT_CHOOSE_VALUE_OID, DEFAULT_CHOOSE_VALUE_NAME);
        }

        objectTemplateDto.setType(ObjectTemplateType.class);
        return objectTemplateDto;
    }

    @Override
    protected void initLayout(){
        passPolicyDto = loadPasswordPolicy();
        objectTemplateDto = loadObjectTemplate();

        ChooseTypePanel passPolicyChoosePanel = new ChooseTypePanel(ID_GLOBAL_PASSWORD_POLICY_CHOOSER,
                new PropertyModel<ObjectViewDto>(this, "passPolicyDto"));
        ChooseTypePanel userTemplateChoosePanel = new ChooseTypePanel(ID_GLOBAL_USER_TEMPLATE_CHOOSER,
                new PropertyModel<ObjectViewDto>(this, "objectTemplateDto"));
        add(passPolicyChoosePanel);
        add(userTemplateChoosePanel);
    }
}
