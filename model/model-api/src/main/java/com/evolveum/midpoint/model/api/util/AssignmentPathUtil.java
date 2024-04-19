/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerable;
import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

public class AssignmentPathUtil {

    // works with native assignment path (AssignmentPath)
    public static ExtensionType collectExtensions(AssignmentPath path, int startAt) throws SchemaException {
        ExtensionType rv = new ExtensionType();
        PrismContainerValue<?> pcv = rv.asPrismContainerValue();
        for (int i = startAt; i < path.getSegments().size(); i++) {
            AssignmentPathSegment segment = path.getSegments().get(i);
            AssignmentType assignment = segment.getAssignmentAny();
            if (assignment != null && assignment.getExtension() != null) {
                ObjectTypeUtil.mergeExtension(pcv, assignment.getExtension().asPrismContainerValue());
            }
            if (segment.getTarget() != null && segment.getTarget().getExtension() != null) {
                ObjectTypeUtil.mergeExtension(pcv, segment.getTarget().getExtension().asPrismContainerValue());
            }
        }
        return rv;
    }

    // works with externalized assignment path (AssignmentPathType)
    public static ExtensionType collectExtensions(AssignmentPathType path, int startAt, ModelService modelService, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        ExtensionType rv = new ExtensionType(modelService.getPrismContext());
        PrismContainerValue<?> pcv = rv.asPrismContainerValue();
        PrismObject<? extends ObjectType> lastTarget = null;           // used for caching
        for (int i = startAt; i < path.getSegment().size(); i++) {
            AssignmentPathSegmentType segment = path.getSegment().get(i);
            AssignmentType assignment = getAssignment(segment, lastTarget, modelService, task, result);
            if (assignment != null && assignment.getExtension() != null) {
                ObjectTypeUtil.mergeExtension(pcv, assignment.getExtension().asPrismContainerValue());
            }
            PrismObject<? extends ObjectType> target = getAssignmentTarget(segment, modelService, task, result);
            if (target != null && target.getExtension() != null) {
                ObjectTypeUtil.mergeExtension(pcv, target.getExtensionContainerValue());
            }
            lastTarget = target;
        }
        return rv;
    }

    private static PrismObject<? extends ObjectType> getAssignmentTarget(AssignmentPathSegmentType segment,
            ModelService modelService, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (segment.getTargetRef() == null || segment.getTargetRef().getOid() == null) {
            return null;
        }
        return getObject(segment.getTargetRef(), modelService, task, result);
    }

    private static AssignmentType getAssignment(AssignmentPathSegmentType segment,
            PrismObject<? extends ObjectType> candidate, ModelService modelService, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        if (segment.getSourceRef() == null || segment.getSourceRef().getOid() == null || segment.getAssignmentId() == null) {
            return null;
        }
        PrismObject<? extends ObjectType> source;
        if (candidate != null && segment.getSourceRef().getOid().equals(candidate.getOid()) && candidate.asObjectable() instanceof FocusType) {
            source = candidate;
        } else {
            source = getObject(segment.getSourceRef(), modelService, task, result);
        }
        PrismContainer<AssignmentType> assignmentContainer = source.findContainer(FocusType.F_ASSIGNMENT);
        if (assignmentContainer == null) {
            return null;
        }
        return asContainerable(assignmentContainer.findValue(segment.getAssignmentId()));
    }

    private static PrismObject<? extends ObjectType> getObject(ObjectReferenceType reference,
            ModelService modelService,
            Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        String oid = reference.getOid();
        QName typeName = reference.getType() != null ? reference.getType() : ObjectType.COMPLEX_TYPE;
        Class<? extends ObjectType> typeClass = ObjectTypes.getObjectTypeClass(typeName);
        return modelService.getObject(typeClass, oid, createNoFetchCollection(), task, result);
    }
}
