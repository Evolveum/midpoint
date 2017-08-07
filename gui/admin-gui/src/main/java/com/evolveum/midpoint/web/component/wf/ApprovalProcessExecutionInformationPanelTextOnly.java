/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.input.TextAreaPanel;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalProcessExecutionInformationDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApprovalStageExecutionInformationDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ApproverEngagementDto;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * TEMPORARY IMPLEMENTATION. Replace with something graphically nice.
 *
 * @author mederly
 */
public class ApprovalProcessExecutionInformationPanelTextOnly extends BasePanel<ApprovalProcessExecutionInformationDto> {

    private static final String ID_TEXT = "text";

    // todo options to select which columns will be shown
    public ApprovalProcessExecutionInformationPanelTextOnly(String id, IModel<ApprovalProcessExecutionInformationDto> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
    	add(new TextAreaPanel<>(ID_TEXT, new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                ApprovalProcessExecutionInformationDto processInfo = getModelObject();
                if (processInfo == null) {
                    return null;
                }
                int currentStageNumber = processInfo.getCurrentStageNumber();
                int numberOfStages = processInfo.getNumberOfStages();
                StringBuilder sb = new StringBuilder();
                List<ApprovalStageExecutionInformationDto> stages = processInfo.getStages();
                for (ApprovalStageExecutionInformationDto stageInfo : stages) {
                    if (stageInfo.getStageNumber() == currentStageNumber) {
                        sb.append("====> ");
                    }
                    sb.append(stageInfo.getNiceStageName(numberOfStages));
                    sb.append(" ");
                    if (stageInfo.getAutomatedOutcome() != null) {
                        sb.append("[").append(stageInfo.getAutomatedOutcome()).append("] because of ")
                                .append(stageInfo.getAutomatedCompletionReason());      // the reason is localizable
                    } else {
                        for (ApproverEngagementDto engagement : stageInfo.getApproverEngagements()) {
                            sb.append("[").append(WebComponentUtil.getDisplayNameOrName(engagement.getApproverRef())).append("] ");
                            if (engagement.getOutput() != null) {
                                sb.append("(").append(ApprovalUtils.fromUri(engagement.getOutput().getOutcome()));
                                if (engagement.getCompletedBy() != null && !ObjectTypeUtil.matchOnOid(engagement.getApproverRef(), engagement.getCompletedBy())) {
                                    sb.append(" by ").append(WebComponentUtil.getDisplayNameOrName(engagement.getCompletedBy()));
                                }
                                sb.append(") ");
                            } else {
                                sb.append("(?) ");
                            }
                        }
                    }
                    sb.append("\n");
                }
                return sb.toString();
            }
        }, 8));
    }

}
