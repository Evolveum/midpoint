/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Helper class used to configure a change processor. (Expects the processor to be a subclass of BaseChangeProcessor;
 * this can be relaxed by moving some methods to ChangeProcessor interface, if needed.)
 *
 * @author mederly
 */

@Component
public class ConfigurationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ConfigurationHelper.class);

    @Autowired private WfConfiguration wfConfiguration;
    @Autowired private SystemObjectCache systemObjectCache;

    public void registerProcessor(BaseChangeProcessor changeProcessor) {
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

    public LegacyApproversSpecificationUsageType getUseLegacyApproversSpecification(WfConfigurationType wfConfiguration) {
        if (wfConfiguration == null || wfConfiguration.getUseLegacyApproversSpecification() == null) {
            return LegacyApproversSpecificationUsageType.IF_NO_EXPLICIT_APPROVAL_POLICY_ACTION;
        } else {
            return wfConfiguration.getUseLegacyApproversSpecification();
        }
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
