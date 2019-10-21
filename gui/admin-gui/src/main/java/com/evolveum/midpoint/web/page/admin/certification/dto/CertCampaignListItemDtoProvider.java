/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import org.apache.wicket.Component;

/**
 * @author Pavol
 */
public class CertCampaignListItemDtoProvider extends ObjectDataProvider<CertCampaignListItemDto, AccessCertificationCampaignType> {

    public CertCampaignListItemDtoProvider(Component component) {
        super(component, AccessCertificationCampaignType.class);
    }

    @Override
    public CertCampaignListItemDto createDataObjectWrapper(PrismObject<AccessCertificationCampaignType> obj) {
        return new CertCampaignListItemDto(obj.asObjectable(), getPage());
    }
}
