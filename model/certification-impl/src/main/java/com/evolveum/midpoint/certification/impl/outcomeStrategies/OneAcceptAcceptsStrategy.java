/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.outcomeStrategies;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ONE_ACCEPT_ACCEPTS;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

@Component
public class OneAcceptAcceptsStrategy extends BaseOutcomeStrategy {

    @PostConstruct
    public void init() {
        register(ONE_ACCEPT_ACCEPTS);
    }

    @Override
    public AccessCertificationResponseType computeOutcome(ResponsesSummary sum) {
        if (sum.has(ACCEPT)) {
            return ACCEPT;
        } else if (sum.has(REVOKE)) {
            return REVOKE;
        } else if (sum.has(REDUCE)) {
            return REDUCE;
        } else if (sum.has(NOT_DECIDED)) {
            return NOT_DECIDED;
        } else if (sum.has(NO_RESPONSE)) {
            return NO_RESPONSE;
        } else {
            throw new IllegalStateException("No responses");
        }
    }

    @Override
    public List<AccessCertificationResponseType> getOutcomesToStopOn() {
        return Arrays.asList(ACCEPT);
    }
}
