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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CREATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 * @author mederly
 */
@Component
public class AccCertGeneralHelper {

    @Autowired
    private ModelService modelService;

    @Autowired
    private SecurityEnforcer securityEnforcer;

    @Autowired
    private PrismContext prismContext;

    // TODO temporary hack because of some problems in model service...
    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    @Autowired
    private CertificationManagerImpl certificationManager;

    private PrismObjectDefinition<AccessCertificationCampaignType> campaignObjectDefinition = null;     // lazily evaluated

    public PrismObjectDefinition<?> getCampaignObjectDefinition() {
        if (campaignObjectDefinition != null) {
            return campaignObjectDefinition;
        }
        campaignObjectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccessCertificationCampaignType.class);
        if (campaignObjectDefinition == null) {
            throw new IllegalStateException("Couldn't find definition for AccessCertificationCampaignType prism object");
        }
        return campaignObjectDefinition;
    }

    AccessCertificationCampaignType getCampaign(String campaignOid, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        return modelService.getObject(AccessCertificationCampaignType.class, campaignOid, options, task, parentResult).asObjectable();
    }

    public boolean isRevoke(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign) {
        return aCase.getCurrentResponse() == REVOKE;
    }

    AccessCertificationCampaignType createCampaignObject(AccessCertificationDefinitionType definition, AccessCertificationCampaignType campaign,
                                                                 Task task, OperationResult result) throws SecurityViolationException, SchemaException {
        AccessCertificationCampaignType newCampaign = new AccessCertificationCampaignType(prismContext);
        Date now = new Date();

        if (campaign != null && campaign.getName() != null) {
            campaign.setName(campaign.getName());
        } else if (definition != null && definition.getName() != null) {
            newCampaign.setName(generateCampaignName(definition.getName().getOrig(), task, result));
        } else {
            throw new SchemaException("Couldn't create a campaign without name");
        }

        if (campaign != null && campaign.getDescription() != null) {
            newCampaign.setDescription(newCampaign.getDescription());
        } else if (definition != null) {
            newCampaign.setDescription(definition.getDescription());
        }

        if (campaign != null && campaign.getOwnerRef() != null) {
            newCampaign.setOwnerRef(campaign.getOwnerRef());
        } else if (definition.getOwnerRef() != null) {
            newCampaign.setOwnerRef(definition.getOwnerRef());
        } else {
            newCampaign.setOwnerRef(securityEnforcer.getPrincipal().toObjectReference());
        }

        if (campaign != null && campaign.getTenantRef() != null) {
            newCampaign.setTenantRef(campaign.getTenantRef());
        } else {
            newCampaign.setTenantRef(definition.getTenantRef());
        }

        if (definition != null && definition.getOid() != null) {
            newCampaign.setDefinitionRef(ObjectTypeUtil.createObjectRef(definition));
        }

        if (campaign != null && campaign.getHandlerUri() != null) {
            newCampaign.setHandlerUri(campaign.getHandlerUri());
        } else if (definition != null && definition.getHandlerUri() != null) {
            newCampaign.setHandlerUri(definition.getHandlerUri());
        } else {
            throw new SchemaException("Couldn't create a campaign without handlerUri");
        }

        if (campaign != null && campaign.getScopeDefinition() != null) {
            newCampaign.setScopeDefinition(campaign.getScopeDefinition());
        } else if (definition != null && definition.getScopeDefinition() != null) {
            newCampaign.setScopeDefinition(definition.getScopeDefinition());
        }

        if (campaign != null && campaign.getRemediationDefinition() != null) {
            newCampaign.setRemediationDefinition(campaign.getRemediationDefinition());
        } else if (definition != null && definition.getRemediationDefinition() != null) {
            newCampaign.setRemediationDefinition(definition.getRemediationDefinition());
        }

        if (campaign != null && CollectionUtils.isNotEmpty(campaign.getStageDefinition())) {
            newCampaign.getStageDefinition().addAll(CloneUtil.cloneCollectionMembers(campaign.getStageDefinition()));
        } else if (definition != null && CollectionUtils.isNotEmpty(definition.getStageDefinition())) {
            newCampaign.getStageDefinition().addAll(CloneUtil.cloneCollectionMembers(definition.getStageDefinition()));
        }
        CertCampaignTypeUtil.checkStageDefinitionConsistency(newCampaign.getStageDefinition());

        newCampaign.setStart(null);
        newCampaign.setEnd(null);
        newCampaign.setState(CREATED);
        newCampaign.setStageNumber(0);

        return newCampaign;
    }

    private PolyStringType generateCampaignName(String prefix, Task task, OperationResult result) throws SchemaException {
        for (int i = 1;; i++) {
            String name = generateName(prefix, i);
            if (!campaignExists(name, task, result)) {
                return new PolyStringType(name);
            }
        }
    }

    private boolean campaignExists(String name, Task task, OperationResult result) throws SchemaException {
        ObjectQuery query = ObjectQueryUtil.createNameQuery(AccessCertificationCampaignType.class, prismContext, name);
        SearchResultList<PrismObject<AccessCertificationCampaignType>> existingCampaigns =
                repositoryService.searchObjects(AccessCertificationCampaignType.class, query, null, result);
        return !existingCampaigns.isEmpty();
    }

    private String generateName(String prefix, int i) {
        return prefix + " " + i;
    }

}
