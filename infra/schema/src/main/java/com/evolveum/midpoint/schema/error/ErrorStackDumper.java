/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.error;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * PoC implementation of dumping a "logical" stack, consisting of a sequence of nested {@link OperationResult} objects.
 *
 * Should produce something like this:
 *
 * ----
 * Already evaluated: EvaluatedPolicyRuleImpl(create-children-on-new-project-creation)
 *   at com.evolveum.midpoint.model.api.ModelService.executeChanges (UNKNOWN)
 *       [p]options=null
 *   at com.evolveum.midpoint.model.impl.lens.Clockwork.run (FATAL_ERROR)
 *   at com.evolveum.midpoint.model.impl.lens.Clockwork.runWithConflictDetection (FATAL_ERROR)
 *   at com.evolveum.midpoint.model.impl.lens.Clockwork.click (FATAL_ERROR)
 *       [c]task=Task(id:1691493171681-1613-1, name:null, oid:null)
 *       [c]context=LensContext(s=SECONDARY, W(e=1,p=1): LensFocusContext(OrgType:28cc620b-3122-4d74-954c-93f5ea0d8c00), [])
 *   at com.evolveum.midpoint.model.impl.lens.projector.Projector.project (FATAL_ERROR)
 *       [p]fromStart=true
 *       [c]projectionWave=1
 *   at com.evolveum.midpoint.model.impl.lens.projector.Projector.focus (FATAL_ERROR)
 *   at com.evolveum.midpoint.model.impl.lens.projector.Projector.focusPolicyRules (FATAL_ERROR)
 *   at com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluator.evaluateRule (FATAL_ERROR)
 *       [p]policyRule=create-children-on-new-project-creation:(omod)->(executeX)
 *       [p]policyRuleId=4d3280a1-6514-4984-ac2c-7e56c05af258:3
 *       [c]context=org: ariane5 (OID:28cc620b-3122-4d74-954c-93f5ea0d8c00) / AFTER
 * -----
 */
@Experimental
public class ErrorStackDumper {

    public static String dump(@NotNull Throwable throwable, @NotNull OperationResult result) {
        for (var cause : ExceptionUtil.getCausesFromBottomUp(throwable)) {
            var path = findInResult(result, cause);
            if (path != null) {
                return dump(path);
            }
        }
        return "(no dump available)";
    }

    private static String dump(List<OperationResult> path) {
        StringBuilder sb = new StringBuilder();
        var message = getMessage(path);
        if (message != null) {
            sb.append(message).append("\n");
        }
        for (OperationResult result : path) {
            result.dumpBasicInfo(sb, "at ", 1);
        }
        return sb.toString();
    }

    private static String getMessage(List<OperationResult> path) {
        for (OperationResult result : path) {
            var msg = result.getMessage();
            if (msg != null) {
                return msg;
            }
        }
        return null;
    }

    private static List<OperationResult> findInResult(@NotNull OperationResult root, @NotNull Throwable cause) {
        List<OperationResult> workingPath = new ArrayList<>();
        if (tryFinding(workingPath, root, cause)) {
            return workingPath;
        } else {
            return null;
        }
    }

    private static boolean tryFinding(List<OperationResult> workingPath, OperationResult result, Throwable cause) {
        workingPath.add(result);
        for (OperationResult child : result.getSubresults()) {
            if (tryFinding(workingPath, child, cause)) {
                return true; // return immediately - we have found the path in a child
            }
        }
        if (matches(result, cause)) {
            return true; // not in child, but maybe in us?
        }
        // nope, not in us nor in our children
        workingPath.remove(workingPath.size() - 1);
        return false;
    }

    private static boolean matches(OperationResult result, Throwable cause) {
        return result.getCause() == cause;
    }
}
