/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import java.io.Serial;

public class MyActiveCampaignsPanel extends ActiveCampaignsPanel {

    @Serial private static final long serialVersionUID = 1L;

    public MyActiveCampaignsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    protected ObjectQuery getCustomCampaignsQuery() {
        FocusType principal = getPageBase().getPrincipalFocus();

        return getPrismContext().queryFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                .ref(principal.getOid())
                .and()
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull()
                .build();
    }

    protected MidPointPrincipal getPrincipal() {
        return getPageBase().getPrincipal();
    }
}
