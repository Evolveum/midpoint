/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.task.api.Task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CaseTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.primary.ApprovalMetadataHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpGeneralHelper;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Helps with managing LensContext objects for approved changes execution.
 */
@Component
public class LensContextHelper {

    private static final Trace LOGGER = TraceManager.getTrace(LensContextHelper.class);

    @Autowired private MiscHelper miscHelper;
    @Autowired private PcpGeneralHelper pcpGeneralHelper;
    @Autowired private ApprovalMetadataHelper approvalMetadataHelper;

    LensContext<?> collectApprovedDeltasToModelContext(CaseType rootCase, List<CaseType> subcases, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {
        LensContext<?> rootContext = (LensContext<?>) miscHelper.getModelContext(rootCase, task, result);
        List<ObjectTreeDeltas> deltasToMerge = new ArrayList<>();

        for (CaseType subcase : subcases) {
            if (!CaseTypeUtil.isClosed(subcase)) {
                throw new IllegalStateException("Child case " + subcase + " is not in CLOSED state; its state is " + subcase.getState());
            }
            ObjectTreeDeltas<?> deltas = pcpGeneralHelper.retrieveResultingDeltas(subcase);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Child case {} has {} resulting deltas", subcase, deltas != null ? deltas.getDeltaList().size() : 0);
            }
            if (deltas != null) {
                ObjectDelta focusChange = deltas.getFocusChange();
                if (focusChange != null) {
                    approvalMetadataHelper.addAssignmentApprovalMetadata(focusChange, subcase, task, result);
                }
                if (focusChange != null && focusChange.isAdd()) {
                    deltasToMerge.add(0, deltas);   // "add" must go first
                } else {
                    deltasToMerge.add(deltas);
                }
            }
        }
        mergeDeltasToModelContext(rootContext, deltasToMerge);
        return rootContext;
    }

    void mergeDeltasToModelContext(LensContext<?> rootContext, List<ObjectTreeDeltas> deltasToMerge)
            throws SchemaException {
        for (ObjectTreeDeltas deltaToMerge : deltasToMerge) {
            LensFocusContext<?> focusContext = rootContext.getFocusContext();
            ObjectDelta focusDelta = deltaToMerge.getFocusChange();
            if (focusDelta != null) {
                LOGGER.trace("Adding delta to root model context; delta = {}", focusDelta.debugDumpLazily());
                if (focusContext.getPrimaryDelta() != null && !focusContext.getPrimaryDelta().isEmpty()) {
                    //noinspection unchecked
                    focusContext.addPrimaryDelta(focusDelta);
                } else {
                    //noinspection unchecked
                    focusContext.setPrimaryDelta(focusDelta);
                }
            }
            //noinspection unchecked
            Set<Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> entries = deltaToMerge.getProjectionChangeMapEntries();
            for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : entries) {
                LOGGER.trace("Adding projection delta to root model context; rsd = {}, delta = {}", entry.getKey(),
                        entry.getValue().debugDumpLazily());
                ModelProjectionContext projectionContext = rootContext.findProjectionContext(entry.getKey());
                if (projectionContext == null) {
                    // TODO more liberal treatment?
                    throw new IllegalStateException("No projection context for " + entry.getKey());
                }
                if (projectionContext.getPrimaryDelta() != null && !projectionContext.getPrimaryDelta().isEmpty()) {
                    projectionContext.addPrimaryDelta(entry.getValue());
                } else {
                    projectionContext.setPrimaryDelta(entry.getValue());
                }
            }
        }
    }
}
