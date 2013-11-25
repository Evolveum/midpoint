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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.apache.wicket.model.IModel;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SystemConfigurationDto implements Serializable {

    private static final String DEFAULT_CHOOSE_VALUE_NAME = "None";
    private static final String DEFAULT_CHOOSE_VALUE_OID = "";

    private PrismObject<SystemConfigurationType> oldConfig;
    private ValuePolicyType globalPasswordPolicy;
    private ObjectTemplateType globalObjectTemplate;
    private AssignmentPolicyEnforcementType globalAEP;
    private CleanupPolicyType auditCleanup;
    private CleanupPolicyType taskCleanup;

    private AEPlevel aepLevel;

    private String auditCleanupValue;
    private String taskCleanupValue;

    private ObjectViewDto<ValuePolicyType> passPolicyDto;
    private ObjectViewDto<ObjectTemplateType> objectTemplateDto;

    public SystemConfigurationDto(){
        this(null);
    }

    public SystemConfigurationDto(PrismObject<SystemConfigurationType> config) {
        this.oldConfig = config;
        init(config.asObjectable());
    }

    private void init(SystemConfigurationType config){
        if(config == null){
            return;
        }
        globalObjectTemplate = config.getDefaultUserTemplate();
        globalPasswordPolicy = config.getGlobalPasswordPolicy();
        globalAEP = config.getGlobalAccountSynchronizationSettings().getAssignmentPolicyEnforcement();
        aepLevel = AEPlevel.fromAEPLevelType(globalAEP);
        auditCleanup = config.getCleanupPolicy().getAuditRecords();
        taskCleanup = config.getCleanupPolicy().getClosedTasks();

        auditCleanupValue = auditCleanup.getMaxAge().toString();
        taskCleanupValue = taskCleanup.getMaxAge().toString();

        passPolicyDto = loadPasswordPolicy(config);
        objectTemplateDto = loadObjectTemplate(config);
    }

    private ObjectViewDto<ValuePolicyType> loadPasswordPolicy(SystemConfigurationType config){
        ValuePolicyType passPolicy = config.getGlobalPasswordPolicy();

        if(passPolicy != null){
            passPolicyDto = new ObjectViewDto<ValuePolicyType>(passPolicy.getOid(), passPolicy.getName().getOrig());
        }else {
            passPolicyDto = new ObjectViewDto<ValuePolicyType>(DEFAULT_CHOOSE_VALUE_OID, DEFAULT_CHOOSE_VALUE_NAME);
        }

        passPolicyDto.setType(ValuePolicyType.class);
        return passPolicyDto;
    }

    private ObjectViewDto<ObjectTemplateType> loadObjectTemplate(SystemConfigurationType config){
        ObjectTemplateType objectTemplate = config.getDefaultUserTemplate();

        if(objectTemplate != null){
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>(objectTemplate.getOid(), objectTemplate.getName().getOrig());
        }else {
            objectTemplateDto = new ObjectViewDto<ObjectTemplateType>(DEFAULT_CHOOSE_VALUE_OID, DEFAULT_CHOOSE_VALUE_NAME);
        }

        objectTemplateDto.setType(ObjectTemplateType.class);
        return objectTemplateDto;
    }

    public PrismObject<SystemConfigurationType> getOldConfig() {
        return oldConfig;
    }

    public void setOldConfig(PrismObject<SystemConfigurationType> oldConfig) {
        this.oldConfig = oldConfig;
    }

    public ValuePolicyType getGlobalPasswordPolicy() {
        return globalPasswordPolicy;
    }

    public void setGlobalPasswordPolicy(ValuePolicyType globalPasswordPolicy) {
        this.globalPasswordPolicy = globalPasswordPolicy;
    }

    public ObjectTemplateType getGlobalObjectTemplate() {
        return globalObjectTemplate;
    }

    public void setGlobalObjectTemplate(ObjectTemplateType globalObjectTemplate) {
        this.globalObjectTemplate = globalObjectTemplate;
    }

    public AssignmentPolicyEnforcementType getGlobalAEP() {
        return globalAEP;
    }

    public void setGlobalAEP(AssignmentPolicyEnforcementType globalAEP) {
        this.globalAEP = globalAEP;
    }

    public CleanupPolicyType getAuditCleanup() {
        return auditCleanup;
    }

    public void setAuditCleanup(CleanupPolicyType auditCleanup) {
        this.auditCleanup = auditCleanup;
    }

    public CleanupPolicyType getTaskCleanup() {
        return taskCleanup;
    }

    public void setTaskCleanup(CleanupPolicyType taskCleanup) {
        this.taskCleanup = taskCleanup;
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
}
