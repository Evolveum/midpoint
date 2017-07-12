/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors;

import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;

/**
 * Helper class used to configure a change processor. (Expects the processor to be a subclass of BaseChangeProcessor;
 * this can be relaxed by moving some methods to ChangeProcessor interface, if needed.)
 *
 * @author mederly
 */

@Component
public class BaseConfigurationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(BaseConfigurationHelper.class);

    private static final String KEY_ENABLED = "enabled";
    private static final List<String> KNOWN_KEYS = Arrays.asList(KEY_ENABLED);

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired(required = true)
	private SystemObjectCache systemObjectCache;

    public void registerProcessor(BaseChangeProcessor changeProcessor) {
        wfConfiguration.registerProcessor(changeProcessor);
    }

    public WfConfigurationType getWorkflowConfiguration(ModelContext<? extends ObjectType> context, OperationResult result) {
        if (context != null && context.getSystemConfiguration() != null) {
            SystemConfigurationType systemConfigurationType = context.getSystemConfiguration().asObjectable();
            return systemConfigurationType.getWorkflowConfiguration();
        }
        PrismObject<SystemConfigurationType> systemConfigurationTypePrismObject = null;
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
