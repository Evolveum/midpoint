/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;

import org.jetbrains.annotations.NotNull;

/**
 * An event related to access certification.
 */
public interface AccessCertificationEvent extends Event {

    /**
     * Type of the operation: usually
     * - ADD (opening campaign/stage)
     * - DELETE (closing campaign/stage).
     *
     * In special cases there can be MODIFY e.g. meaning "stage deadline approaching".
     */
    EventOperationType getOperationType();

    /**
     * Status of the operation.
     * (Currently always SUCCESS.)
     */
    OperationResultStatus getStatus();

    /**
     * Related certification campaign.
     */
    @NotNull AccessCertificationCampaignType getCampaign();

    /**
     * Name of the related certification campaign.
     */
    default String getCampaignName() {
        return getCampaign().getName().getOrig();
    }

    /**
     * Definition of the current stage.
     */
    AccessCertificationStageDefinitionType getCurrentStageDefinition();
}
