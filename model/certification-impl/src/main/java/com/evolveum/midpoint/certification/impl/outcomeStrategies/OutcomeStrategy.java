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

import java.util.List;

/**
 * @author mederly
 */
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
