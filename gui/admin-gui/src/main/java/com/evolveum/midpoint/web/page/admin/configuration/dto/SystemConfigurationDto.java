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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class SystemConfigurationDto implements Serializable {

    private PrismObject<SystemConfigurationType> oldConfig;
    private ValuePolicyType globalPasswordPolicy;
    private ObjectTemplateType globalObjectTemplate;
    private AssignmentPolicyEnforcementType globalAEP;
    private CleanupPolicyType auditCleanup;
    private CleanupPolicyType taskCleanup;

    private AEPlevel aepLevel;

    private String auditCleanupValue;
    private String taskCleanupValue;

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
        auditCleanup = config.getCleanupPolicy().getAuditRecords();
        taskCleanup = config.getCleanupPolicy().getClosedTasks();

        auditCleanupValue = auditCleanup.getMaxAge().toString();
        taskCleanupValue = taskCleanup.getMaxAge().toString();
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
}
