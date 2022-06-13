/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.ApprovalBeans;

import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LensContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Contains processing that occurs when an approval case is closed: either approved or rejected,
 * with either immediate or "after all approvals" execution.
 */
public class CaseClosing {

    private static final Trace LOGGER = TraceManager.getTrace(CaseClosing.class);

    @NotNull private final CaseEngineOperation operation;
    @NotNull private final ApprovalBeans beans;

    public CaseClosing(@NotNull CaseEngineOperation operation, @NotNull ApprovalBeans beans) {
        this.operation = operation;
        this.beans = beans;
    }

    public void finishCaseClosing(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        CaseType approvalCase = operation.getCurrentCase();

        ObjectTreeDeltas<?> approvedDeltas = prepareDeltaOut(approvalCase);
        beans.generalHelper.storeResultingDeltas(approvalCase, approvedDeltas, result);
        // note: resulting deltas are now _not_ stored in currentCase object (these are in repo only)

        // here we should execute the deltas, if appropriate!
        CaseType rootCase = beans.generalHelper.getRootCase(approvalCase, result);
        if (CaseTypeUtil.isClosed(rootCase)) {
            LOGGER.debug("Root case ({}) is already closed; not starting any execution tasks for {}", rootCase, approvalCase);
            beans.executionHelper.closeCaseInRepository(approvalCase, result);
        } else {
            LensContextType modelContextBean = rootCase.getModelContext();
            if (modelContextBean == null) {
                throw new IllegalStateException("No model context in root case " + rootCase);
            }
            if (isExecuteImmediately(modelContextBean)) {
                processImmediateExecution(approvalCase, approvedDeltas, result);
            } else {
                processDelayedExecution(approvalCase, rootCase, result);
            }
        }
    }

    private boolean isExecuteImmediately(LensContextType contextBean) {
        return contextBean.getOptions() != null
                && Boolean.TRUE.equals(contextBean.getOptions().isExecuteImmediatelyAfterApproval());
    }

    private void processImmediateExecution(CaseType currentCase, ObjectTreeDeltas<?> deltas, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        if (deltas != null) {
            LOGGER.debug("Case {} is approved with immediate execution -- let's start the process", currentCase);
            boolean waiting;
            if (!currentCase.getPrerequisiteRef().isEmpty()) {
                ObjectQuery query = PrismContext.get().queryFor(CaseType.class)
                        .id(getPrerequisiteOids(currentCase))
                        .and().not().item(CaseType.F_STATE).eq(SchemaConstants.CASE_STATE_CLOSED)
                        .build();
                SearchResultList<PrismObject<CaseType>> openPrerequisites =
                        beans.repositoryService.searchObjects(CaseType.class, query, null, result);
                waiting = !openPrerequisites.isEmpty();
                if (waiting) {
                    LOGGER.debug(
                            "Case {} cannot be executed now because of the following open prerequisites: {}"
                                    + " -- the execution task will be created in WAITING state",
                            currentCase, openPrerequisites);
                }
            } else {
                waiting = false;
            }
            beans.executionHelper.submitExecutionTask(currentCase, waiting, result);
        } else {
            LOGGER.debug("Case {} is rejected (with immediate execution) -- nothing to do here", currentCase);
            beans.executionHelper.closeCaseInRepository(currentCase, result);
            beans.executionHelper.checkDependentCases(currentCase.getParentRef().getOid(), result);
        }
    }

    private @NotNull String[] getPrerequisiteOids(CaseType currentCase) {
        return currentCase.getPrerequisiteRef().stream()
                .map(ObjectReferenceType::getOid)
                .toArray(String[]::new);
    }

    private void processDelayedExecution(CaseType currentCase, CaseType rootCase, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        LOGGER.debug("Approval case {} is completed; but execution is delayed so let's check other subcases of {}",
                currentCase, rootCase);
        beans.executionHelper.closeCaseInRepository(currentCase, result);
        List<CaseType> subcases = beans.miscHelper.getSubcases(rootCase, result);

        // TODO what about race conditions, i.e. what if remaining cases are closed "at once"?!
        if (subcases.stream().allMatch(CaseTypeUtil::isClosed)) {
            LOGGER.debug("All subcases of {} are closed, so let's execute the deltas", rootCase);
            beans.executionHelper.submitExecutionTaskIfNeeded(rootCase, subcases, operation.getTask(), result);
        } else {
            LOGGER.debug("Some subcases of {} are not closed yet. Delta execution is therefore postponed.", rootCase);
            for (CaseType subcase : subcases) {
                LOGGER.debug(" - {}: state={} (isClosed={})", subcase, subcase.getState(),
                        CaseTypeUtil.isClosed(subcase));
            }
        }
    }

    private ObjectTreeDeltas<?> prepareDeltaOut(CaseType aCase) throws SchemaException {
        ObjectTreeDeltas<?> deltaIn = beans.generalHelper.retrieveDeltasToApprove(aCase);
        if (ApprovalUtils.isApprovedFromUri(aCase.getOutcome())) {
            return deltaIn;
        } else {
            return null;
        }
    }
}
