/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections.CollectionUtils;

import javax.xml.datatype.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.evolveum.midpoint.model.impl.tasks.scanner.FocusValidityScannerTaskExecution.QueryScope.*;

import static java.util.Collections.singleton;

/**
 * An execution of a focus validity scanner task.
 *
 * Deals with creating parts (object/assignment processing).
 * Maintains specific fields related to custom item timestamp checking.
 */
public class FocusValidityScannerTaskExecution
        extends AbstractScannerTaskExecution<FocusValidityScannerTaskHandler, FocusValidityScannerTaskExecution> {

    /** Validity constraint defining what item and what time interval we watch for. */
    final TimeValidityPolicyConstraintType validityConstraint;

    /** Do we have notification actions to invoke when validity constraint is met? */
    private final boolean notificationActionsPresent;

    /** TODO move to the gatekeeper */
    private final Set<String> processedOids = ConcurrentHashMap.newKeySet();

    public FocusValidityScannerTaskExecution(FocusValidityScannerTaskHandler taskHandler,
            RunningTask localCoordinatorTask,
            WorkBucketType workBucket,
            TaskPartitionDefinitionType partDefinition,
            TaskWorkBucketProcessingResult previousRunResult) {
        super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        this.validityConstraint = getValidityPolicyConstraintFromTask();
        this.notificationActionsPresent = areNotificationActionsPresentInTask();
    }

    @Override
    public List<FocusValidityScannerTaskPartExecution> createPartExecutions() {
        List<FocusValidityScannerTaskPartExecution> partExecutions = new ArrayList<>();
        Collection<QueryScope> queryScopes = getQueryScopes();
        if (queryScopes.contains(OBJECTS)) {
            partExecutions.add(new FocusValidityScannerTaskPartExecution(this, OBJECTS));
        }
        if (queryScopes.contains(ASSIGNMENTS)) {
            partExecutions.add(new FocusValidityScannerTaskPartExecution(this, ASSIGNMENTS));
        }
        if (queryScopes.contains(COMBINED)) {
            partExecutions.add(new FocusValidityScannerTaskPartExecution(this, COMBINED));
        }
        return partExecutions;
    }

    private Collection<QueryScope> getQueryScopes() {
        String handlerUri = localCoordinatorTask.getHandlerUri();
        if (ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_1.equals(handlerUri)) {
            return singleton(OBJECTS);
        } else if (ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_2.equals(handlerUri)) {
            return singleton(ASSIGNMENTS);
        } else if (ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI.equals(handlerUri)) {
            return Arrays.asList(OBJECTS, ASSIGNMENTS);
        } else {
            return singleton(COMBINED);
        }
    }

    private TimeValidityPolicyConstraintType getValidityPolicyConstraintFromTask() {
        PolicyRuleType policyRule = localCoordinatorTask.getPolicyRule();
        if (policyRule == null || policyRule.getPolicyConstraints() == null) {
            return null;
        }

        List<TimeValidityPolicyConstraintType> timeValidityConstraints = policyRule.getPolicyConstraints().getObjectTimeValidity();
        if (CollectionUtils.isEmpty(timeValidityConstraints)) {
            return null;
        }
        return timeValidityConstraints.iterator().next();
    }

    private boolean areNotificationActionsPresentInTask() {
        PolicyRuleType policyRule = localCoordinatorTask.getPolicyRule();
        return policyRule != null && policyRule.getPolicyActions() != null &&
                !policyRule.getPolicyActions().getNotification().isEmpty();
    }

    TimeValidityPolicyConstraintType getValidityConstraint() {
        return validityConstraint;
    }

    // Should be called only when really doing custom validity check.
    // (But returns meaningful value even if not.)
    public Duration getNegativeActivationOffset() {
        if (validityConstraint != null && validityConstraint.getActivateOn() != null) {
            return validityConstraint.getActivateOn().negate();
        } else {
            return XmlTypeConverter.createDuration(0);
        }
    }

    /**
     * @return True if the task does not do standard validity checking, but uses custom validity constraint
     * and notification actions.
     */
    boolean doCustomValidityCheck() {
        return validityConstraint != null && notificationActionsPresent;
    }

    boolean oidAlreadySeen(String objectOid) {
        return !processedOids.add(objectOid);
    }

    public enum QueryScope {
        /** The task will search for objects with validity changes. */
        OBJECTS,

        /** The task will search for assignments with validity changes. */
        ASSIGNMENTS,

        /** The task will search for both objects and assignments with validity changes. (In a single query.) */
        COMBINED
    }
}
