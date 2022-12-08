/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.VirtualAssignmentSpecification;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualAssignmentSpecificationType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class LifecycleUtil {

    public static LifecycleStateType findStateDefinition(LifecycleStateModelType lifecycleStateModel, String targetLifecycleState) {
        if (lifecycleStateModel == null) {
            return null;
        }
        if (targetLifecycleState == null) {
            targetLifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
        }
        for (LifecycleStateType stateType: lifecycleStateModel.getState()) {
            if (targetLifecycleState.equals(stateType.getName())) {
                return stateType;
            }
        }
        return null;
    }

    public static <R extends AbstractRoleType> VirtualAssignmentSpecification<R> getForcedAssignmentSpecification(LifecycleStateModelType lifecycleStateModel,
            String targetLifecycleState, PrismContext prismContext) throws SchemaException {
        LifecycleStateType stateDefinition = findStateDefinition(lifecycleStateModel, targetLifecycleState);
        if (stateDefinition == null) {
            return null;
        }

        VirtualAssignmentSpecificationType virtualAssignmentSpecificationType = stateDefinition.getForcedAssignment();
        if (virtualAssignmentSpecificationType == null) {
            return null;
        }

        SearchFilterType filter = virtualAssignmentSpecificationType.getFilter();
        if (filter == null) {
            return null;
        }

        QName targetType = virtualAssignmentSpecificationType.getTargetType();
        Class<R> targetClass = (Class<R>) AbstractRoleType.class;
        if (targetType != null) {
            targetClass = (Class<R>) prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(targetType);
        }

        VirtualAssignmentSpecification<R> virtualAssignmentSpecification = new VirtualAssignmentSpecification();
        virtualAssignmentSpecification.setType(targetClass);


        ObjectFilter objectFilter = prismContext.getQueryConverter().parseFilter(filter, targetClass);
        virtualAssignmentSpecification.setFilter(objectFilter);

        return virtualAssignmentSpecification;
    }

    /**
     * Returns true if the specified configuration item (e.g. resource, object class, object type, item, mapping, ...)
     * is in "production" lifecycle state.
     *
     * The idea is that configuration items in `active` and `deprecated` states will have a different behavior
     * than the ones in `proposed` state. (The behavior of other states is going to be determined later.)
     *
     * TODO Preliminary code.
     */
    @Experimental
    public static boolean isInProduction(String lifecycleState) {
        return lifecycleState == null
                || SchemaConstants.LIFECYCLE_ACTIVE.equals(lifecycleState)
                || SchemaConstants.LIFECYCLE_DEPRECATED.equals(lifecycleState);
    }

//    public static <T extends AbstractRoleType> Collection<T> getListOfForcedRoles(LifecycleStateModelType lifecycleModel,
//            String targetLifecycleState, PrismContext prismContext, ObjectResolver resolver, Task task, OperationResult result)  {
//        ObjectFilter filter = getForcedAssignmentFilter(lifecycleModel, targetLifecycleState, prismContext);
//
//        if (filter == null) {
//            return null;
//        }
//
//        Collection<T> forcedRoles = new HashSet<>();
//        ResultHandler<T> handler = (object, parentResult)  -> {
//            return forcedRoles.add(object.asObjectable());
//        };
//
//
//        resolver.searchIterative(AbstractRoleType.class,
//               ObjectQuery.createObjectQuery(filter), null, handler, task, result);
//
//    }

}
