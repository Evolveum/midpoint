/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces.operations;

import com.evolveum.midpoint.schema.traces.OpNodeTreeBuilder;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@Experimental
public class ResolutionUtil {

    private static final String COULD_NOT_RESOLVE = ResolutionUtil.class.getName() + ".couldNotResolve";

    public static void resolveReferenceName(ObjectReferenceType reference, OpNodeTreeBuilder.NameResolver nameResolver) {
        if (nameResolver != null && reference != null && reference.getOid() != null && reference.getTargetName() == null) {
            if (reference.asReferenceValue().getUserData(COULD_NOT_RESOLVE) != null) {
                return;
            }
            PolyStringType targetName = nameResolver.getName(reference.getOid());
            if (targetName != null) {
                reference.setTargetName(targetName);
            } else {
                reference.asReferenceValue().setUserData(COULD_NOT_RESOLVE, true);
            }
        }
    }

    public static void resolveAssignmentReferenceNames(AssignmentType assignment, OpNodeTreeBuilder.NameResolver nameResolver) {
        if (nameResolver != null && assignment != null) {
            resolveReferenceName(assignment.getTargetRef(), nameResolver);
            if (assignment.getConstruction() != null) {
                resolveReferenceName(assignment.getConstruction().getResourceRef(), nameResolver);
            }
        }
    }
}
