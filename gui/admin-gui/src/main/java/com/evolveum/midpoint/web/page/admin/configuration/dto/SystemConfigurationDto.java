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
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SystemConfigurationDto implements Serializable {

    public static final String F_AEP_LEVEL = "aepLevel";
    public static final String F_AUDIT_CLEANUP = "auditCleanupValue";
    public static final String F_TASK_CLEANUP = "taskCleanupValue";
    public static final String F_PASSWORD_POLICY = "passPolicyDto";
    public static final String F_OBJECT_TEMPLATE = "objectTemplateDto";
    public static final String F_OBJECT_POLICY_LIST = "objectPolicyList";
    public static final String F_NOTIFICATION_CONFIGURATION = "notificationConfig";

    private AEPlevel aepLevel;

    private String auditCleanupValue;
    private String taskCleanupValue;

    private ObjectViewDto<ValuePolicyType> passPolicyDto;
    private ObjectViewDto<ObjectTemplateType> objectTemplateDto;
    private List<ObjectPolicyConfigurationTypeDto> objectPolicyList;
    private NotificationConfigurationDto notificationConfig;

    public SystemConfigurationDto(){
        this(null);
    }

    public SystemConfigurationDto(PrismObject<SystemConfigurationType> config) {
        init(config.asObjectable());
    }

    private void init(SystemConfigurationType config){
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

        objectPolicyList = new ArrayList<>();
        List<ObjectPolicyConfigurationType> objectPolicies = config.getDefaultObjectPolicyConfiguration();
        if(objectPolicies != null && !objectPolicies.isEmpty()){
            for(ObjectPolicyConfigurationType policy: objectPolicies){
                objectPolicyList.add(new ObjectPolicyConfigurationTypeDto(policy));
            }
        } else {
            objectPolicyList.add(new ObjectPolicyConfigurationTypeDto());
        }

        if(config.getNotificationConfiguration() != null){
            notificationConfig = new NotificationConfigurationDto(config.getNotificationConfiguration());
        } else {
            notificationConfig = new NotificationConfigurationDto();
        }
    }

    private ObjectViewDto<ValuePolicyType> loadPasswordPolicy(SystemConfigurationType config){
        ValuePolicyType passPolicy = config.getGlobalPasswordPolicy();

        if(passPolicy != null){
            passPolicyDto = new ObjectViewDto<>(passPolicy.getOid(), passPolicy.getName().getOrig());
        }else {
            passPolicyDto = new ObjectViewDto<>();
        }

        passPolicyDto.setType(ValuePolicyType.class);
        return passPolicyDto;
    }

    private ObjectViewDto<ObjectTemplateType> loadObjectTemplate(SystemConfigurationType config){
        ObjectTemplateType objectTemplate = config.getDefaultUserTemplate();

        if(objectTemplate != null){
            objectTemplateDto = new ObjectViewDto<>(objectTemplate.getOid(), objectTemplate.getName().getOrig());
        }else {
            objectTemplateDto = new ObjectViewDto<>();
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

    public List<ObjectPolicyConfigurationTypeDto> getObjectPolicyList() {
        return objectPolicyList;
    }

    public void setObjectPolicyList(List<ObjectPolicyConfigurationTypeDto> objectPolicyList) {
        this.objectPolicyList = objectPolicyList;
    }
}
