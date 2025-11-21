/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serializable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

/**
 * Class that holds summary information about certification campaign.
 *
 * Used primaryly in dashboard panels.
 */
public record CampaignSummary(
        AccessCertificationCampaignType campaign,
        long decidedCount,
        long openNotDecidedCount
) implements Serializable {
}
