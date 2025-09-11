/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract from ApprovalSchemaExecutionInformationType that could be directly displayed via the GUI as "approval process preview"
 * (either for the whole process or only the future stages).
 */
public class ApprovalProcessExecutionInformationDto implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    public static final String F_PROCESS_NAME = "processName";
    public static final String F_TARGET_NAME = "targetName";
    public static final String F_STAGES = "stages";
    public static final String F_TRIGGERS = "triggers";

    private final boolean wholeProcess;                                   // do we represent whole process or only the future stages?
    private final int currentStageNumber;                                 // current stage number (0 if there's no current stage, i.e. process has not started yet)
    private final int numberOfStages;
    private final String processName;
    private final String targetName;
    private final List<ApprovalStageExecutionInformationDto> stages = new ArrayList<>();
    private final EvaluatedTriggerGroupDto triggers;
    private final boolean running;

    private ApprovalProcessExecutionInformationDto(boolean wholeProcess, int currentStageNumber, int numberOfStages,
            String processName, String targetName,
            EvaluatedTriggerGroupDto triggers, boolean running) {
        this.wholeProcess = wholeProcess;
        this.currentStageNumber = currentStageNumber;
        this.numberOfStages = numberOfStages;
        this.processName = processName;
        this.targetName = targetName;
        this.triggers = triggers;
        this.running = running;
    }

    @NotNull
    public static ApprovalProcessExecutionInformationDto createFrom(ApprovalSchemaExecutionInformationType info,
            boolean wholeProcess, Task opTask,
            OperationResult result, PageAdminLTE parentPage) {
        int currentStageNumber = info.getCurrentStageNumber() != null ? info.getCurrentStageNumber() : 0;
        int numberOfStages = info.getStage().size();
        String processName = ApprovalContextUtil.getProcessName(info);
        String targetName = ApprovalContextUtil.getTargetName(info);
        CaseType aCase = ApprovalContextUtil.getCase(info);
        boolean running = aCase != null && !SchemaConstants.CASE_STATE_CLOSED.equals(aCase.getState());
        EvaluatedTriggerGroupDto triggers = EvaluatedTriggerGroupDto.initializeFromRules(ApprovalContextUtil.getAllRules(info.getPolicyRules()), false, new EvaluatedTriggerGroupDto.UniquenessFilter());
        ApprovalProcessExecutionInformationDto rv =
                new ApprovalProcessExecutionInformationDto(wholeProcess, currentStageNumber, numberOfStages, processName,
                        targetName, triggers, running);
        int startingStageNumber = wholeProcess ? 1 : currentStageNumber;
        boolean reachable = true;
        for (int i = startingStageNumber; i <= numberOfStages; i++) {
            ApprovalStageExecutionInformationDto stage =
                    ApprovalStageExecutionInformationDto.createFrom(info, i, opTask, result, parentPage);
            stage.setReachable(reachable);
            rv.stages.add(stage);
            if (stage.getOutcome() == ApprovalLevelOutcomeType.REJECT) {
                reachable = false;      // for following stages
            }
        }
        return rv;
    }

    public boolean isWholeProcess() {
        return wholeProcess;
    }

    public int getCurrentStageNumber() {
        return currentStageNumber;
    }

    public int getNumberOfStages() {
        return numberOfStages;
    }

    public List<ApprovalStageExecutionInformationDto> getStages() {
        return stages;
    }

    public String getProcessName() {
        return processName;
    }

    public String getTargetName() {
        return targetName;
    }

    public boolean isRunning() {
        return running;
    }

    public EvaluatedTriggerGroupDto getTriggers() {
        return triggers;
    }
}
