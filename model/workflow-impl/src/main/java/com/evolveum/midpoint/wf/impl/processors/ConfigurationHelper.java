/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.processors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Helper class used to configure a change processor.
 */
@Component
public class ConfigurationHelper {

    @Autowired private WfConfiguration wfConfiguration;
    @Autowired private SystemObjectCache systemObjectCache;

    public void registerProcessor(ChangeProcessor changeProcessor) {
        wfConfiguration.registerProcessor(changeProcessor);
    }

    public WfConfigurationType getWorkflowConfiguration(ModelContext<? extends ObjectType> context, OperationResult result) {
        if (context != null && context.getSystemConfiguration() != null) {
            SystemConfigurationType systemConfigurationType = context.getSystemConfiguration().asObjectable();
            return systemConfigurationType.getWorkflowConfiguration();
        }
        PrismObject<SystemConfigurationType> systemConfigurationTypePrismObject;
        try {
            systemConfigurationTypePrismObject = systemObjectCache.getSystemConfiguration(result);
        } catch (SchemaException e) {
            throw new SystemException("Couldn't get system configuration because of schema exception - cannot continue", e);
        }
        if (systemConfigurationTypePrismObject == null) {
            // this is possible e.g. when importing initial objects; warning is already issued by Utils.getSystemConfiguration method
            return null;
        }
        return systemConfigurationTypePrismObject.asObjectable().getWorkflowConfiguration();
    }

    public DefaultApprovalPolicyRulesUsageType getUseDefaultApprovalPolicyRules(WfConfigurationType wfConfiguration) {
        if (wfConfiguration == null || wfConfiguration.getUseDefaultApprovalPolicyRules() == null) {
            return DefaultApprovalPolicyRulesUsageType.IF_NO_APPROVAL_POLICY_ACTION;
        } else {
            return wfConfiguration.getUseDefaultApprovalPolicyRules();
        }
    }

    public PrimaryChangeProcessorConfigurationType getPcpConfiguration(WfConfigurationType wfConfigurationType) {
        return wfConfigurationType != null ? wfConfigurationType.getPrimaryChangeProcessor() : null;
    }
}
