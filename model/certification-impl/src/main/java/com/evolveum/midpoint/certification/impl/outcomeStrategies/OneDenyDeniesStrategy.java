/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl.outcomeStrategies;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.Arrays;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType.ONE_DENY_DENIES;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NOT_DECIDED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.REVOKE;

/**
 * @author mederly
 */
@Component
public class OneDenyDeniesStrategy extends BaseOutcomeStrategy {

    @PostConstruct
    public void init() {
        register(ONE_DENY_DENIES);
    }

    @Override
    public AccessCertificationResponseType computeOutcome(ResponsesSummary sum) {
        if (sum.has(REVOKE)) {
            return REVOKE;
        } else if (sum.has(REDUCE)) {
            return REDUCE;
        } else if (sum.has(ACCEPT)) {
            return ACCEPT;
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
        return Arrays.asList(REDUCE, REVOKE);
    }
}
