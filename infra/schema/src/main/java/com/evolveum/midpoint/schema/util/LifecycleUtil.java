/**
 * Copyright (c) 2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.VirtualAssignmenetSpecification;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
	
	public static <R extends AbstractRoleType> VirtualAssignmenetSpecification<R> getForcedAssignmentSpecification(LifecycleStateModelType lifecycleStateModel, 
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
        	
        VirtualAssignmenetSpecification<R> virtualAssignmenetSpecification = new VirtualAssignmenetSpecification();
        virtualAssignmenetSpecification.setType(targetClass);
        
        
        ObjectFilter objectFilter = prismContext.getQueryConverter().parseFilter(filter, targetClass);
        virtualAssignmenetSpecification.setFilter(objectFilter);
        
        return virtualAssignmenetSpecification;
	}
	
	
	
//	public static <T extends AbstractRoleType> Collection<T> getListOfForcedRoles(LifecycleStateModelType lifecycleModel, 
//			String targetLifecycleState, PrismContext prismContext, ObjectResolver resolver, Task task, OperationResult result)  {
//        ObjectFilter filter = getForcedAssignmentFilter(lifecycleModel, targetLifecycleState, prismContext);
//        	
//        if (filter == null) {
//        	return null;
//        }
//        	
//        Collection<T> forcedRoles = new HashSet<>();
//        ResultHandler<T> handler = (object, parentResult)  -> {
//        	return forcedRoles.add(object.asObjectable());
//        };
//			
//        	
//        resolver.searchIterative(AbstractRoleType.class, 
//       		ObjectQuery.createObjectQuery(filter), null, handler, task, result);
//        
//	}

}
