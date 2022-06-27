/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

public enum CertCampaignStateFilter {
    ALL,
    NOT_CLOSED,
    CREATED,
    IN_REVIEW_STAGE,
    REVIEW_STAGE_DONE,
    IN_REMEDIATION,
    CLOSED;

    public S_FilterEntry appendFilter(S_FilterEntry q) {
        switch (this) {
            case ALL:
                return q;
            case NOT_CLOSED:
                return q.block().not().item(AccessCertificationCampaignType.F_STATE)
                        .eq(AccessCertificationCampaignStateType.CLOSED).endBlock().and();
            case CREATED:
                return q.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CREATED).and();
            case IN_REVIEW_STAGE:
                return q.item(AccessCertificationCampaignType.F_STATE)
                        .eq(AccessCertificationCampaignStateType.IN_REVIEW_STAGE).and();
            case REVIEW_STAGE_DONE:
                return q.item(AccessCertificationCampaignType.F_STATE)
                        .eq(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE).and();
            case IN_REMEDIATION:
                return q.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.IN_REMEDIATION)
                        .and();
            case CLOSED:
                return q.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED).and();
            default:
                throw new SystemException("Unknown value for StatusFilter: " + this);
        }
    }
}
