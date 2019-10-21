/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.enums;

import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;

/**
 * @author mederly
 */
@JaxbType(type = AccessCertificationCampaignStateType.class)
public enum RAccessCertificationCampaignState implements SchemaEnum<AccessCertificationCampaignStateType> {

    CREATED(AccessCertificationCampaignStateType.CREATED),
    IN_REVIEW_STAGE(AccessCertificationCampaignStateType.IN_REVIEW_STAGE),
    REVIEW_STAGE_DONE(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE),
    IN_REMEDIATION(AccessCertificationCampaignStateType.IN_REMEDIATION),
    CLOSED(AccessCertificationCampaignStateType.CLOSED);

    private AccessCertificationCampaignStateType status;

    private RAccessCertificationCampaignState(AccessCertificationCampaignStateType status) {
        this.status = status;
    }

    @Override
    public AccessCertificationCampaignStateType getSchemaValue() {
        return status;
    }
}
