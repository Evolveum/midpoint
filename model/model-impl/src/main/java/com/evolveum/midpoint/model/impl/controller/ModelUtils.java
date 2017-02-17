/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.impl.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class ModelUtils {

	private static final Trace LOGGER = TraceManager.getTrace(ModelUtils.class);

	public static void validatePaging(ObjectPaging paging) {
		if (paging == null) {
			return;
		}

		if (paging.getMaxSize() != null && paging.getMaxSize().longValue() < 0) {
			throw new IllegalArgumentException("Paging max size must be more than 0.");
		}
		if (paging.getOffset() != null && paging.getOffset().longValue() < 0) {
			throw new IllegalArgumentException("Paging offset index must be more than 0.");
		}
	}
	
	public static void recordFatalError(OperationResult result, Throwable e) {
		recordFatalError(result, e.getMessage(), e);
	}

	public static void recordFatalError(OperationResult result, String message, Throwable e) {
		// Do not log at ERROR level. This is too harsh. Especially in object not found case.
		// What model considers an error may be just a normal situation for the code is using model API.
		// If this is really an error then it should be logged by the invoking code.
		LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
		result.recordFatalError(message, e);
		result.cleanupResult(e);
	}
	
	public static void recordPartialError(OperationResult result, Throwable e) {
		recordPartialError(result, e.getMessage(), e);
	}

	public static void recordPartialError(OperationResult result, String message, Throwable e) {
		// Do not log at ERROR level. This is too harsh. Especially in object not found case.
		// What model considers an error may be just a normal situation for the code is using model API.
		// If this is really an error then it should be logged by the invoking code.
		LoggingUtils.logExceptionOnDebugLevel(LOGGER, message, e);
		result.recordPartialError(message, e);
		result.cleanupResult(e);
	}
	
	public static <O extends ObjectType> String getOperationUrlFromDelta(ObjectDelta<O> delta) {
		if (delta == null) {
			return null;
		}
		if (delta.isAdd()) {
			return ModelAuthorizationAction.ADD.getUrl();
		}
		if (delta.isModify()) {
			return ModelAuthorizationAction.MODIFY.getUrl();
		}
		if (delta.isDelete()) {
			return ModelAuthorizationAction.DELETE.getUrl();
		}
		throw new IllegalArgumentException("Unknown delta type "+delta);
	}

	public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(PrismObject<O> object, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
		List<String> subTypes;
        if (object.getOid() == null){
            subTypes = new ArrayList<>();
        } else {
            subTypes = determineSubTypes(object);
        }
		return determineObjectPolicyConfiguration(object.getCompileTimeClass(), subTypes, systemConfigurationType);
	}
	
	public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(Class<O> objectClass, List<String> objectSubtypes, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
		ObjectPolicyConfigurationType applicablePolicyConfigurationType = null;
		for (ObjectPolicyConfigurationType aPolicyConfigurationType: systemConfigurationType.getDefaultObjectPolicyConfiguration()) {
			QName typeQName = aPolicyConfigurationType.getType();
			ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
			if (objectType == null) {
				throw new ConfigurationException("Unknown type "+typeQName+" in default object policy definition in system configuration");
			}
			if (objectType.getClassDefinition() == objectClass) {
				String aSubType = aPolicyConfigurationType.getSubtype();
				if (aSubType == null) {
					if (applicablePolicyConfigurationType == null) {
						applicablePolicyConfigurationType = aPolicyConfigurationType;
					}
				} else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
					applicablePolicyConfigurationType = aPolicyConfigurationType;
				}
			}
		}
		if (applicablePolicyConfigurationType != null) {
			return applicablePolicyConfigurationType;
		}

		// Deprecated
		for (ObjectPolicyConfigurationType aPolicyConfigurationType: systemConfigurationType.getObjectTemplate()) {
			QName typeQName = aPolicyConfigurationType.getType();
			ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
			if (objectType == null) {
				throw new ConfigurationException("Unknown type "+typeQName+" in object template definition in system configuration");
			}
			if (objectType.getClassDefinition() == objectClass) {
				return aPolicyConfigurationType;
			}
		}
		
		// Deprecated method to specify user template. For compatibility only
		if (objectClass == UserType.class) {
			ObjectReferenceType templateRef = systemConfigurationType.getDefaultUserTemplateRef();
			if (templateRef == null) {
				return null;
			}
			ObjectPolicyConfigurationType policy = new ObjectPolicyConfigurationType();
			policy.setObjectTemplateRef(templateRef.clone());
			return policy;
		}
		
		return null;
	}
	
	public static <O extends ObjectType> List<String> determineSubTypes(PrismObject<O> object) {
		if (object == null) {
			return null;
		}
		
		// TODO: get subType (from ObjectType)
		
		if (object.canRepresent(UserType.class)) {
			return (((UserType)object.asObjectable()).getEmployeeType());
		}
		if (object.canRepresent(OrgType.class)) {
			return (((OrgType)object.asObjectable()).getOrgType());
		}
		if (object.canRepresent(RoleType.class)) {
			List<String> roleTypes = new ArrayList<>(1);
			roleTypes.add((((RoleType)object.asObjectable()).getRoleType()));
			return roleTypes;
		}
		if (object.canRepresent(ServiceType.class)) {
			return (((ServiceType)object.asObjectable()).getServiceType());
		}
		return null;
	}

	// If the object was retrieved as "not readonly", it was cloned on retrieval from cache.
	// So it's not necessary to clone it now (before applying security and schema).
	//
	// However, if it was retrieved as readonly, it was _not_ cloned after retrieving from cache.
	// So let's clone it now.
	public static <T extends ObjectType> PrismObject<T> cloneIfReadOnly(PrismObject<T> object,
			Collection<SelectorOptions<GetOperationOptions>> options) {
		return cloneIfReadOnly(object, SelectorOptions.findRootOptions(options));
	}

	public static <O extends ObjectType> PrismObject<O> cloneIfReadOnly(PrismObject<O> object, GetOperationOptions options) {
		if (GetOperationOptions.isReadOnly(options)) {
			return object.clone();
		} else {
			return object;
		}
	}
}
