/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.VirtualAssignmentSpecification;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualAssignmentSpecificationType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Objects;

/**
 * @author semancik
 *
 */
public class LifecycleUtil {

    public static LifecycleStateType findStateDefinition(
            @Nullable LifecycleStateModelType lifecycleStateModel, @Nullable String stateName) {
        if (lifecycleStateModel == null) {
            return null;
        }
        var explicitStateName = Objects.requireNonNullElse(stateName, SchemaConstants.LIFECYCLE_ACTIVE);
        for (LifecycleStateType stateBean: lifecycleStateModel.getState()) {
            if (explicitStateName.equals(stateBean.getName())) {
                return stateBean;
            }
        }
        return null;
    }

    public static <R extends AbstractRoleType> VirtualAssignmentSpecification<R> getForcedAssignmentSpecification(
            LifecycleStateModelType lifecycleStateModel, String targetLifecycleState)
            throws SchemaException {
        LifecycleStateType stateDefinition = findStateDefinition(lifecycleStateModel, targetLifecycleState);
        if (stateDefinition == null) {
            return null;
        }

        VirtualAssignmentSpecificationType specification = stateDefinition.getForcedAssignment();
        if (specification == null) {
            return null;
        }

        SearchFilterType filter = specification.getFilter();
        if (filter == null) {
            return null;
        }

        QName targetType = specification.getTargetType();
        Class<R> targetClass;
        if (targetType != null) {
            //noinspection unchecked
            targetClass = (Class<R>) PrismContext.get().getSchemaRegistry().getCompileTimeClassForObjectTypeRequired(targetType);
        } else {
            //noinspection unchecked
            targetClass = (Class<R>) AbstractRoleType.class;
        }

        return new VirtualAssignmentSpecification<>(
                targetClass,
                PrismContext.get().getQueryConverter().parseFilter(filter, targetClass));
    }
}
