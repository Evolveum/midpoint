/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

/**
 * @author skublik
 */

public class AuthenticationEvaluatorUtil {

    public static boolean checkRequiredAssignment(List<AssignmentType> assignments, List<ObjectReferenceType> requireAssignments) {
        if (requireAssignments == null || requireAssignments.isEmpty()){
            return true;
        }
        if (assignments == null || assignments.isEmpty()) {
            return false;
        }

        for (ObjectReferenceType requiredAssignment : requireAssignments) {
            if (requiredAssignment == null) {
                throw new IllegalStateException("Required assignment is null");
            }
            if (requiredAssignment.getOid() == null){
                throw new IllegalStateException("Oid of required assignment is null");
            }
            boolean match = false;
            for (AssignmentType assignment : assignments) {
                ObjectReferenceType targetRef = assignment.getTargetRef();
                if (targetRef != null) {
                    if (targetRef.getOid() != null && targetRef.getOid().equals(requiredAssignment.getOid())) {
                        match = true;
                        break;
                    }
                } else if (assignment.getConstruction() != null && requiredAssignment.getType() != null
                        && QNameUtil.match(requiredAssignment.getType(), ResourceType.COMPLEX_TYPE)) {
                    if (assignment.getConstruction().getResourceRef() != null &&
                            assignment.getConstruction().getResourceRef().getOid() != null &&
                            assignment.getConstruction().getResourceRef().getOid().equals(requiredAssignment.getOid())) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

    public static AuthenticationBehavioralDataType getBehavior(FocusType focus) {

        if (focus.getBehavior() == null){
            focus.setBehavior(new BehaviorType());
        }
        if (focus.getBehavior().getAuthentication() == null){
            focus.getBehavior().setAuthentication(new AuthenticationBehavioralDataType());
        }
        return focus.getBehavior().getAuthentication();
    }
}
