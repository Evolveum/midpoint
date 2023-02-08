/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.provider;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectDataProvider;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertCampaignListItemDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import org.apache.wicket.Component;
import org.apache.wicket.model.Model;

/**
 * @author Pavol
 */
public class CertCampaignListItemDtoProvider extends ObjectDataProvider<CertCampaignListItemDto, AccessCertificationCampaignType> {

    private ObjectQuery query = null;

    public CertCampaignListItemDtoProvider(Component component) {
        super(component, Model.of());
    }

    @Override
    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    @Override
    public ObjectQuery getQuery() {
        return query;
    }

    @Override
    public Class<AccessCertificationCampaignType> getType() {
        return AccessCertificationCampaignType.class;
    }

    @Override
    public CertCampaignListItemDto createDataObjectWrapper(PrismObject<AccessCertificationCampaignType> obj) {
        return new CertCampaignListItemDto(obj.asObjectable(), getPageBase());
    }
}
