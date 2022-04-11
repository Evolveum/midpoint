/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.cases.impl.helpers;

import static com.evolveum.midpoint.cases.impl.CaseManagerImpl.OP_CLEANUP_CASES;

import static java.util.Collections.singleton;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;

import java.util.List;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;

import org.springframework.stereotype.Component;

/**
 * Implements case cleanup functionality.
 */
@Component
public class CaseCleaner {

    private static final Trace LOGGER = TraceManager.getTrace(CaseCleaner.class);

    private static final String OP_STATISTICS = OP_CLEANUP_CASES + ".statistics";

    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;

    public void cleanupCases(
            @NotNull CleanupPolicyType policy,
            @NotNull RunningTask executionTask,
            @NotNull OperationResult result)
            throws CommonException {

        DeletionCounters counters = new DeletionCounters();
        TimeBoundary timeBoundary = TimeBoundary.compute(policy.getMaxAge());
        XMLGregorianCalendar deleteCasesClosedUpTo = timeBoundary.boundary;

        LOGGER.debug("Starting cleanup for closed cases deleting up to {} (duration '{}').", deleteCasesClosedUpTo,
                timeBoundary.positiveDuration);

        ObjectQuery obsoleteCasesQuery = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_STATE).eq(SchemaConstants.CASE_STATE_CLOSED)
                .and().item(CaseType.F_CLOSE_TIMESTAMP).le(deleteCasesClosedUpTo)
                .and().item(CaseType.F_PARENT_REF).isNull()
                .build();
        List<PrismObject<CaseType>> rootObsoleteCases =
                modelService.searchObjects(CaseType.class, obsoleteCasesQuery, null, executionTask, result);

        LOGGER.debug("Found {} case tree(s) to be cleaned up", rootObsoleteCases.size());

        boolean interrupted = false;
        for (PrismObject<CaseType> rootObsoleteCase : rootObsoleteCases) {
            if (!executionTask.canRun()) {
                result.recordWarning("Interrupted");
                LOGGER.warn("Task cleanup was interrupted.");
                interrupted = true;
                break;
            }

            IterativeOperationStartInfo startInfo = new IterativeOperationStartInfo(
                    new IterationItemInformation(rootObsoleteCase));
            startInfo.setSimpleCaller(true);
            Operation op = executionTask.recordIterativeOperationStart(startInfo);
            try {
                if (ObjectTypeUtil.isIndestructible(rootObsoleteCase)) {
                    LOGGER.trace("Not deleting root case {} because it's marked as indestructible", rootObsoleteCase);
                    op.skipped();
                } else {
                    deleteCaseWithChildren(rootObsoleteCase, counters, executionTask, result);
                    op.succeeded();
                }
            } catch (Throwable t) {
                op.failed(t);
                LoggingUtils.logException(LOGGER, "Couldn't delete children cases for {}", t, rootObsoleteCase);
            }
            executionTask.incrementLegacyProgressAndStoreStatisticsIfTimePassed(result);
        }

        LOGGER.info("Case cleanup procedure " + (interrupted ? "was interrupted" : "finished")
                + ". Successfully deleted {} cases; there were problems with deleting {} cases.", counters.deleted, counters.problems);
        String suffix = interrupted ? " Interrupted." : "";
        if (counters.problems == 0) {
            result.createSubresult(OP_STATISTICS)
                    .recordStatus(SUCCESS, "Successfully deleted " + counters.deleted + " case(s)." + suffix);
        } else {
            result.createSubresult(OP_STATISTICS)
                    .recordPartialError("Successfully deleted " + counters.deleted + " case(s), "
                            + "there was problems with deleting " + counters.problems + " cases." + suffix);
        }
    }

    /**
     * Assuming that the case is not indestructible. Unlike in tasks, here the indestructible children do not
     * prevent the root nor their siblings from deletion.
     */
    private void deleteCaseWithChildren(PrismObject<CaseType> parentCase, DeletionCounters counters,
            Task task, OperationResult result) {

        // get all children cases
        ObjectQuery childrenCasesQuery = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF)
                .ref(parentCase.getOid())
                .build();

        List<PrismObject<CaseType>> childrenCases;
        try {
            childrenCases = modelService.searchObjects(CaseType.class, childrenCasesQuery, null, task, result);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't look for children of {} - continuing", e, parentCase);
            counters.problems++;
            return;
        }

        LOGGER.trace("Removing case {} along with its {} children.", parentCase, childrenCases.size());

        for (PrismObject<CaseType> caseToDelete : childrenCases) {
            if (ObjectTypeUtil.isIndestructible(caseToDelete)) {
                LOGGER.trace("Not deleting sub-case {} as it is indestructible", caseToDelete);
            } else {
                deleteCaseWithChildren(caseToDelete, counters, task, result);
            }
        }
        deleteCase(parentCase, counters, task, result);
    }

    private void deleteCase(PrismObject<CaseType> caseToDelete, DeletionCounters counters, Task task, OperationResult result) {
        try {
            ObjectDelta<CaseType> deleteDelta = prismContext.deltaFactory().object().create(CaseType.class, ChangeType.DELETE);
            deleteDelta.setOid(caseToDelete.getOid());
            modelService.executeChanges(singleton(deleteDelta), ModelExecuteOptions.create().raw(), task, result);
            counters.deleted++;
        } catch (CommonException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete case {} - continuing with the others", e, caseToDelete);
            counters.problems++;
        }
    }

    //todo move to one place from TaskManagerQuartzImpl
    private static class TimeBoundary {
        private final Duration positiveDuration;
        private final XMLGregorianCalendar boundary;

        private TimeBoundary(Duration positiveDuration, XMLGregorianCalendar boundary) {
            this.positiveDuration = positiveDuration;
            this.boundary = boundary;
        }

        private static TimeBoundary compute(Duration rawDuration) {
            Duration positiveDuration = rawDuration.getSign() > 0 ? rawDuration : rawDuration.negate();
            XMLGregorianCalendar boundary = XmlTypeConverter.createXMLGregorianCalendar();
            boundary.add(positiveDuration.negate());
            return new TimeBoundary(positiveDuration, boundary);
        }
    }

    private static class DeletionCounters {
        private int deleted;
        private int problems;
    }
}
