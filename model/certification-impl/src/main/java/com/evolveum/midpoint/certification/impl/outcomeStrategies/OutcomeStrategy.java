/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.certification.impl.outcomeStrategies;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

import java.util.List;

public interface OutcomeStrategy {

    /**
     * Computes stage outcome for a case, or final outcome for a series of stages.
     *
     * @param responsesSummary Summarized responses for the given case. Null or missing responses are treated as NO_RESPONSE.
     *                         Summary is never empty!
     * @return The computed response.
     */
    AccessCertificationResponseType computeOutcome(ResponsesSummary responsesSummary);

    List<AccessCertificationResponseType> getOutcomesToStopOn();

}
