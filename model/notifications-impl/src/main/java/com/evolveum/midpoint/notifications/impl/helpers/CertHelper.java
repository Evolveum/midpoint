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

package com.evolveum.midpoint.notifications.impl.helpers;

import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.notifications.api.events.AccessCertificationEvent;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class CertHelper {

    private static final Trace LOGGER = TraceManager.getTrace(CertHelper.class);

    // if not present, CertificationListener will not be enabled, so these events will not be generated
    @Autowired(required = false)
    private CertificationManager certificationManager;

    public String formatStatus(AccessCertificationEvent event) {
        return String.valueOf(event.getStatus());       // TODO
    }

    public String formatState(AccessCertificationEvent event) {
        AccessCertificationCampaignType campaign = event.getCampaign();

        switch(campaign.getState()) {
            case CREATED:
                return "Created";
            case IN_REVIEW_STAGE:
                return "In review stage " + formatStage(campaign);
            case REVIEW_STAGE_DONE:
                return "Done review stage " + formatStage(campaign);
            case IN_REMEDIATION:
                return "Remediation in progress";
            case CLOSED:
                return "Closed";
            default:
                return "";      // should not occur
        }
    }

    private String formatStage(AccessCertificationCampaignType campaign) {
        String rv = campaign.getStageNumber() + "/" + CertCampaignTypeUtil.getNumberOfStages(campaign);

        AccessCertificationStageType stage = CertCampaignTypeUtil.findCurrentStage(campaign);
        if (StringUtils.isNotEmpty(stage.getName())) {
            rv += " (" + stage.getName() + ")";
        }
        return rv;
    }

    public String getStageShortName(AccessCertificationCampaignType campaign) {
        if (campaign.getState() == AccessCertificationCampaignStateType.IN_REMEDIATION) {
            return "remediation stage";
        } else {
            return "stage " + campaign.getStageNumber() + "/" + CertCampaignTypeUtil.getNumberOfStages(campaign);
        }
    }

    public String getCampaignNameAndOid(AccessCertificationEvent event) {
        return event.getCampaignName() + " (oid " + event.getCampaign().getOid() + ")";
    }

    public void appendStatistics(StringBuilder sb, AccessCertificationCampaignType campaign, Task task, OperationResult result) {

        AccessCertificationCasesStatisticsType stat;
        try {
            stat = certificationManager.getCampaignStatistics(campaign.getOid(), false, task, result);
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | ObjectAlreadyExistsException | ExpressionEvaluationException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get campaign statistics", e);
            sb.append("Couldn't get campaign statistics because of ").append(e);
            return;
        }
        int all = stat.getMarkedAsAccept() + stat.getMarkedAsRevoke() + stat.getMarkedAsReduce() + stat.getMarkedAsNotDecide() +
                stat.getWithoutResponse();
        sb.append("Number of cases:\t").append(all);
        sb.append("\nMarked as ACCEPT:\t").append(stat.getMarkedAsAccept());
        sb.append("\nMarked as REVOKE:\t").append(stat.getMarkedAsRevoke())
                .append(" (remedied: ").append(stat.getMarkedAsRevokeAndRemedied()).append(")");
        sb.append("\nMarked as REDUCE:\t").append(stat.getMarkedAsReduce())
                .append(" (remedied: ").append(stat.getMarkedAsReduceAndRemedied()).append(")");
        sb.append("\nMarked as NOT DECIDED:\t").append(stat.getMarkedAsNotDecide());
        sb.append("\nNo response:\t\t").append(stat.getWithoutResponse());
    }
}
