/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.actions;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.cases.impl.engine.CaseEngineOperationImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Closes the stage:
 *
 * - either right after it has been opened (if auto-completion was ordered),
 * - or after the work items are done.
 */
class CloseStageAction extends InternalAction {

    private static final Trace LOGGER = TraceManager.getTrace(CloseStageAction.class);

    @Nullable private final StageClosingResult autoClosingInformation;

    CloseStageAction(@NotNull CaseEngineOperationImpl operation, @Nullable StageClosingResult autoClosingInformation) {
        super(operation, LOGGER);
        this.autoClosingInformation = autoClosingInformation;
    }

    @Override
    public @Nullable Action executeInternal(OperationResult result) {

        StageClosingResult closingInformation;
        if (autoClosingInformation != null) {
            recordAutoCompletionToCaseHistory();
            closingInformation = autoClosingInformation;
        } else {
            closingInformation = getEngineExtension().processStageClosing(operation, result);
        }

        beans.triggerHelper.removeAllStageTriggersForWorkItem(getCurrentCase());

        if (closingInformation.shouldCaseProcessingContinue()
                && operation.getCurrentStageNumber() < operation.getExpectedNumberOfStages()) {
            return new OpenStageAction(operation);
        } else {
            // Result of the last stage is the result of the whole case
            return new CloseCaseAction(operation, closingInformation.getCaseOutcomeUri());
        }
    }

    private void recordAutoCompletionToCaseHistory() {
        assert autoClosingInformation != null;
        StageCompletionEventType event = new StageCompletionEventType();
        event.setTimestamp(beans.clock.currentTimeXMLGregorianCalendar());
        event.setStageNumber(operation.getCurrentStageNumber());
        event.setAutomatedDecisionReason(autoClosingInformation.getAutomatedStageCompletionReason());
        event.setOutcome(autoClosingInformation.getStageOutcomeUri());
        operation.addCaseHistoryEvent(event);
    }
}
