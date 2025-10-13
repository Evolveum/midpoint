/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class AccCertGeneralHelper {

    @Autowired private PrismContext prismContext;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private PrismObjectDefinition<AccessCertificationCampaignType> campaignObjectDefinition = null;     // lazily evaluated

    PrismObjectDefinition<AccessCertificationCampaignType> getCampaignObjectDefinition() {
        if (campaignObjectDefinition != null) {
            return campaignObjectDefinition;
        }
        campaignObjectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(AccessCertificationCampaignType.class);
        if (campaignObjectDefinition == null) {
            throw new IllegalStateException("Couldn't find definition for AccessCertificationCampaignType prism object");
        }
        return campaignObjectDefinition;
    }

    @SuppressWarnings("SameParameterValue")
    public AccessCertificationCampaignType getCampaign(String campaignOid, Collection<SelectorOptions<GetOperationOptions>> options,
            @SuppressWarnings("unused") Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        return repositoryService
                .getObject(AccessCertificationCampaignType.class, campaignOid, options, parentResult).asObjectable();
    }

}
