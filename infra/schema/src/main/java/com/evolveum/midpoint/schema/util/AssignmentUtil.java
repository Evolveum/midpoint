/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AssignmentUtil {

    @Experimental
    public static String getAssignmentInfo(AssignmentType assignment) {
        if (assignment == null) {
            return null;
        }
        List<String> infos = new ArrayList<>();
        CollectionUtils.addIgnoreNull(infos, getReferenceInfo(assignment.getTargetRef()));
        CollectionUtils.addIgnoreNull(infos, getConstructionInfo(assignment.getConstruction()));
        infos.addAll(getMappingsInfo(assignment.getFocusMappings()));
        CollectionUtils.addIgnoreNull(infos, PolicyRuleTypeUtil.toShortString(assignment.getPolicyRule()));

        return String.join("; ", infos);
    }

    private static List<String> getMappingsInfo(MappingsType focusMappings) {
        if (focusMappings != null) {
            return focusMappings.getMapping().stream()
                    .map(mapping -> "m:" + MappingUtil.getShortInfo(mapping))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    private static String getConstructionInfo(ConstructionType construction) {
        if (construction != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("c:")
                    .append(getReferenceInfo(construction.getResourceRef()));
            if (construction.getKind() != null) {
                sb.append(", ").append(String.valueOf(construction.getKind()).toLowerCase());
            }
            if (construction.getIntent() != null) {
                sb.append(", ").append(construction.getIntent());
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    private static String getReferenceInfo(ObjectReferenceType targetRef) {
        if (targetRef != null) {
            StringBuilder sb = new StringBuilder();
            PolyStringType targetName = targetRef.getTargetName();
            if (targetName != null) {
                sb.append(targetName.getOrig());
            } else if (targetRef.getOid() != null) {
                sb.append(targetRef.getOid());
            } else if (targetRef.getFilter() != null) {
                sb.append(targetRef.getFilter()); // todo
            } else {
                sb.append("(empty)");
            }

            if (targetRef.getRelation() != null && !QNameUtil.match(targetRef.getRelation(), SchemaConstants.ORG_DEFAULT)) {
                sb.append(" [").append(targetRef.getRelation().getLocalPart()).append("]");
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    @Experimental
    public static String getSegmentInfo(AssignmentPathSegmentType segment) {
        if (segment == null) {
            return null;
        }
        if (segment.getAssignment() == null) {
            return "no assignment";
        }

        StringBuilder sb = new StringBuilder();
        if (Boolean.FALSE.equals(segment.isIsAssignment())) {
            sb.append("Inducement: ");
        } else {
            sb.append("Assignment: ");
        }
        sb.append(getReferenceInfo(segment.getSourceRef()));
        sb.append(" → ");
        sb.append(getAssignmentInfo(segment.getAssignment()));

        if (segment.getAssignment().getOrder() != null || !segment.getAssignment().getOrderConstraint().isEmpty()) {
            sb.append(" (").append(getOrderInfo(segment.getAssignment())).append(")");
        }

        if (Boolean.TRUE.equals(segment.isMatchingOrder())) {
            sb.append("*");
        }

        return sb.toString();
    }

    private static String getOrderInfo(AssignmentType assignment) {
        List<String> constraints = new ArrayList<>();
        if (assignment.getOrder() != null) {
            constraints.add("order " + assignment.getOrder());
        }
        for (OrderConstraintsType orderConstraint : assignment.getOrderConstraint()) {
            constraints.add(getOrderConstraintInfo(orderConstraint));
        }
        return String.join("; ", constraints);
    }

    private static String getOrderConstraintInfo(OrderConstraintsType constraint) {
        StringBuilder sb = new StringBuilder();
        if (constraint.getRelation() != null) {
            sb.append(constraint.getRelation().getLocalPart()).append("=");
        }
        if (constraint.getOrder() != null) {
            sb.append(constraint.getOrder());
        }
        if (constraint.getOrderMin() != null || constraint.getOrderMax() != null) {
            sb.append("<").append(constraint.getOrderMin()).append(",").append(constraint.getOrderMax()).append(">");
        }
        if (constraint.getResetOrder() != null) {
            sb.append("!").append(constraint.getResetOrder());
        }
        return sb.toString();
    }
}
