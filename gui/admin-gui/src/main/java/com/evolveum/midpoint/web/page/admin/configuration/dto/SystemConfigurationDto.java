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

package com.evolveum.midpoint.web.page.admin.configuration.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SystemConfigurationDto implements Serializable {
    private AEPlevel aepLevel;

    private String auditCleanupValue;
    private String taskCleanupValue;

    private ObjectViewDto<ValuePolicyType> passPolicyDto;
    private ObjectViewDto<ObjectTemplateType> objectTemplateDto;
    private NotificationConfigurationDto notificationConfig;

    public SystemConfigurationDto(){
        this(null, null);
    }

    public SystemConfigurationDto(PrismObject<SystemConfigurationType> config, Protector protector) {
        init(config.asObjectable(), protector);
    }

    private void init(SystemConfigurationType config, Protector protector){
        if(config == null){
            return;
        }

        if(config.getGlobalAccountSynchronizationSettings() != null){
            AssignmentPolicyEnforcementType globalAEP = config.getGlobalAccountSynchronizationSettings().getAssignmentPolicyEnforcement();
            aepLevel = AEPlevel.fromAEPLevelType(globalAEP);
        }

        CleanupPolicyType auditCleanup = config.getCleanupPolicy().getAuditRecords();
        CleanupPolicyType taskCleanup = config.getCleanupPolicy().getClosedTasks();

        auditCleanupValue = auditCleanup.getMaxAge().toString();
        taskCleanupValue = taskCleanup.getMaxAge().toString();

        passPolicyDto = loadPasswordPolicy(config);
        objectTemplateDto = loadObjectTemplate(config);

        if(config.getNotificationConfiguration() != null){
            notificationConfig = new NotificationConfigurationDto(config.getNotificationConfiguration(), protector);
        } else {
            notificationConfig = new NotificationConfigurationDto();
        }
    }

    private ObjectViewDto<ValuePolicyType> loadPasswordPolicy(SystemConfigurationType config){
        ValuePolicyType passPolicy = config.getGlobalPasswordPolicy();

        if(passPolicy != null){
            passPolicyDto = new ObjectViewDto<ValuePolicyType>(passPolicy.getOid(), passPolicy.getName().getOrig());
        }else {
            passPolicyDto = new ObjectViewDto<ValuePolicyType>();
        }

        passPolicyDto.setType(ValuePolicyType.class);
        return passPolicyDto;
    }

    private ObjectViewDto<ObjectTemplateType> loadObjectTemplate(SystemConfigurationType config){
        ObjectTemplateType objectTemplate = config.getDefaultUserTemplate();

        if(objectTemplate != null){
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>(objectTemplate.getOid(), objectTemplate.getName().getOrig());
        }else {
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>();
        }

        objectTemplateDto.setType(ObjectTemplateType.class);
        return objectTemplateDto;
    }

    public String getAuditCleanupValue() {
        return auditCleanupValue;
    }

    public void setAuditCleanupValue(String auditCleanupValue) {
        this.auditCleanupValue = auditCleanupValue;
    }

    public String getTaskCleanupValue() {
        return taskCleanupValue;
    }

    public void setTaskCleanupValue(String taskCleanupValue) {
        this.taskCleanupValue = taskCleanupValue;
    }

    public AEPlevel getAepLevel() {
        return aepLevel;
    }

    public void setAepLevel(AEPlevel aepLevel) {
        this.aepLevel = aepLevel;
    }

    public ObjectViewDto<ValuePolicyType> getPassPolicyDto() {
        return passPolicyDto;
    }

    public void setPassPolicyDto(ObjectViewDto<ValuePolicyType> passPolicyDto) {
        this.passPolicyDto = passPolicyDto;
    }

    public ObjectViewDto<ObjectTemplateType> getObjectTemplateDto() {
        return objectTemplateDto;
    }

    public void setObjectTemplateDto(ObjectViewDto<ObjectTemplateType> objectTemplateDto) {
        this.objectTemplateDto = objectTemplateDto;
    }

    public NotificationConfigurationDto getNotificationConfig() {
        return notificationConfig;
    }

    public void setNotificationConfig(NotificationConfigurationDto notificationConfig) {
        this.notificationConfig = notificationConfig;
    }
}
