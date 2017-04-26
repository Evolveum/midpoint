/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import org.jetbrains.annotations.NotNull;

/**
 * DTO representing a particular workItem.
 *
 * TODO cleanup a bit
 *
 * @author mederly
 */
public class CertWorkItemDto extends CertCaseOrWorkItemDto {

    public static final String F_COMMENT = "comment";
    @SuppressWarnings("unused")
	public static final String F_RESPONSE = "response";

    @NotNull private final AccessCertificationWorkItemType workItem;

    CertWorkItemDto(@NotNull AccessCertificationWorkItemType workItem, @NotNull PageBase page) {
        //noinspection ConstantConditions
        super(CertCampaignTypeUtil.getCase(workItem), page);
        this.workItem = workItem;
    }

    public String getComment() {
        return WorkItemTypeUtil.getComment(workItem);
    }

    public void setComment(String value) {
        if (workItem.getOutput() == null) {
            workItem.beginOutput().comment(value);
        } else {
            workItem.getOutput().comment(value);
        }
    }

    public AccessCertificationResponseType getResponse() {
        return OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem));
    }

    public long getWorkItemId() {
        return workItem.getId();
    }

	public Integer getEscalationLevelNumber() {
        int n = WfContextUtil.getEscalationLevelNumber(workItem);
        return n != 0 ? n : null;
    }

    public String getEscalationLevelInfo() {
        return WfContextUtil.getEscalationLevelInfo(workItem);
    }
}
