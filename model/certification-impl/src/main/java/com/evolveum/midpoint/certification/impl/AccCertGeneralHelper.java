/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author mederly
 */
@Component
public class AccCertGeneralHelper {

    @Autowired
    private ModelService modelService;

    public AccessCertificationDefinitionType resolveCertificationDef(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {
        PrismReferenceValue defRefValue = campaign.getDefinitionRef().asReferenceValue();
        if (defRefValue.getObject() != null) {
            return (AccessCertificationDefinitionType) defRefValue.getObject().asObjectable();
        }
        return modelService.getObject(AccessCertificationDefinitionType.class, defRefValue.getOid(), null, task, result).asObjectable();
    }

    AccessCertificationCampaignType getCampaign(String campaignOid, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        return modelService.getObject(AccessCertificationCampaignType.class, campaignOid, options, task, parentResult).asObjectable();
    }

    AccessCertificationCampaignType getCampaignWithDefinition(String campaignOid, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(AccessCertificationCampaignType.F_DEFINITION_REF,
                        GetOperationOptions.createResolve());
        return modelService.getObject(AccessCertificationCampaignType.class, campaignOid, options, task, parentResult).asObjectable();
    }

    // very primitive implementation - if there is any REVOKE, just do it
    public boolean isRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, AccessCertificationDefinitionType definition) {
        for (AccessCertificationDecisionType decision : aCase.getDecision()) {
            if (AccessCertificationResponseType.REVOKE.equals(decision.getResponse())) {
                return true;
            }
        }
        return false;
    }
}
