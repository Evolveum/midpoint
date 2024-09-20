/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import java.util.*;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.ApprovalBeans;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LevelEvaluationStrategyType;

public class CaseStageClosing extends AbstractCaseStageProcessing {

    private static final Trace LOGGER = TraceManager.getTrace(CaseStageClosing.class);

    /** Work items for the current stage. */
    private final List<CaseWorkItemType> currentWorkItems;

    // The following is filled-in by analyzeOutcomes method

    private boolean allApproved;
    private final List<CaseWorkItemType> answeredWorkItems = new ArrayList<>();
    private final Set<String> differentOutcomes = new HashSet<>();

    public CaseStageClosing(@NotNull CaseEngineOperation operation, ApprovalBeans beans) {
        super(operation, beans);
        this.currentWorkItems = getWorkItemsForCurrentStage();
    }

    private List<CaseWorkItemType> getWorkItemsForCurrentStage() {
        return currentCase.getWorkItem().stream()
                .filter(wi -> wi.getStageNumber() != null && wi.getStageNumber() == currentStageNumber)
                .collect(Collectors.toList());
    }

    public StageClosingResult process() {

        logInput();

        analyzeOutcomes();
        boolean shouldContinue = determineIfShouldContinue();

        logOutput(shouldContinue);

        return new StageClosingResultImpl(
                shouldContinue,
                ApprovalUtils.toUri(shouldContinue),
                ApprovalUtils.toUri(shouldContinue),
                null);
    }

    private void analyzeOutcomes() {
        allApproved = true;
        for (CaseWorkItemType workItem : currentWorkItems) {
            allApproved &= ApprovalUtils.isApproved(workItem.getOutput());
            if (workItem.getCloseTimestamp() != null && workItem.getPerformerRef() != null) {
                answeredWorkItems.add(workItem);
                differentOutcomes.add(
                        workItem.getOutput() != null ? workItem.getOutput().getOutcome() : null);
            }
        }
    }

    private boolean determineIfShouldContinue() {
        if (stageDef.getEvaluationStrategy() == LevelEvaluationStrategyType.FIRST_DECIDES) {
            if (differentOutcomes.size() > 1) {
                LOGGER.warn(
                        "Ambiguous outcome with firstDecides strategy in {}: {} response(s), providing outcomes of {}",
                        ApprovalContextUtil.getBriefDiagInfo(operation.getCurrentCase()),
                        answeredWorkItems.size(), differentOutcomes);
                answeredWorkItems.sort(
                        Comparator.comparing(
                                item -> XmlTypeConverter.toMillis(item.getCloseTimestamp()),
                                Comparator.nullsLast(Comparator.naturalOrder())));
                CaseWorkItemType first = answeredWorkItems.get(0);
                boolean shouldContinue = ApprovalUtils.isApproved(first.getOutput());
                LOGGER.warn("Possible race condition, so taking the first one: {} ({})", shouldContinue, first);
                return shouldContinue;
            } else if (differentOutcomes.size() == 1) {
                return ApprovalUtils.isApproved(differentOutcomes.iterator().next()) && !differentOutcomes.isEmpty();
            } else {
                throw new IllegalStateException("No outcomes?");
            }
        } else {
            return allApproved && !answeredWorkItems.isEmpty();
        }
    }

    private void logInput() {
        if (!LOGGER.isTraceEnabled()) {
            return;
        }
        LOGGER.trace("*** Summarizing decisions in stage {} (stage evaluation strategy = {}): ",
                stageDef.getName(), stageDef.getEvaluationStrategy());
        for (CaseWorkItemType workItem : currentWorkItems) {
            LOGGER.trace(" - {}", workItem);
        }
    }

    private void logOutput(boolean shouldContinue) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        LOGGER.debug("Closing the stage for approval case {}, stage {}: result of this stage: {}",
                currentCase, ApprovalContextUtil.getStageDiagName(stageDef), shouldContinue);
    }
}
