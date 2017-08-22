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

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            subTypes = FocusTypeUtil.determineSubTypes(object);
        }
		return determineObjectPolicyConfiguration(object.getCompileTimeClass(), subTypes, systemConfigurationType);
	}
	
	public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(Class<O> objectClass, List<String> objectSubtypes, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
		ObjectPolicyConfigurationType applicablePolicyConfigurationType = null;
		for (ObjectPolicyConfigurationType aPolicyConfigurationType: systemConfigurationType.getDefaultObjectPolicyConfiguration()) {
			QName typeQName = aPolicyConfigurationType.getType();
			if (typeQName == null) {
				continue;       // TODO implement correctly (using 'applicable policies' perhaps)
			}
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
			if (typeQName == null) {
				continue;
			}
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

	// from the most to least appropriate
	@NotNull
	public static <O extends ObjectType> List<ObjectPolicyConfigurationType> getApplicablePolicies(
			Class<O> objectClass, List<String> objectSubtypes, SystemConfigurationType systemConfigurationType)
			throws ConfigurationException {
		List<ObjectPolicyConfigurationType> rv = new ArrayList<>();
		List<ObjectPolicyConfigurationType> typeNoSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> typeWithSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> noTypeNoSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> noTypeWithSubtype = new ArrayList<>();
		List<ObjectPolicyConfigurationType> all = new ArrayList<>();

		all.addAll(systemConfigurationType.getDefaultObjectPolicyConfiguration());
		all.addAll(systemConfigurationType.getObjectTemplate());        // deprecated
		if (objectClass == UserType.class) {
			// Deprecated method to specify user template. For compatibility only
			ObjectReferenceType templateRef = systemConfigurationType.getDefaultUserTemplateRef();
			if (templateRef != null) {
				all.add(new ObjectPolicyConfigurationType().objectTemplateRef(templateRef.clone()));
			}
		}

		for (ObjectPolicyConfigurationType aPolicyConfigurationType: all) {
			QName typeQName = aPolicyConfigurationType.getType();
			if (typeQName != null) {
				ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(typeQName);
				if (objectType == null) {
					throw new ConfigurationException(
							"Unknown type " + typeQName + " in default object policy definition or object template definition in system configuration");
				}
				if (objectType.getClassDefinition() == objectClass) {
					String aSubType = aPolicyConfigurationType.getSubtype();
					if (aSubType == null) {
						typeNoSubtype.add(aPolicyConfigurationType);
					} else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
						typeWithSubtype.add(aPolicyConfigurationType);
					}
				}
			} else {
				String aSubType = aPolicyConfigurationType.getSubtype();
				if (aSubType == null) {
					noTypeNoSubtype.add(aPolicyConfigurationType);
				} else if (objectSubtypes != null && objectSubtypes.contains(aSubType)) {
					noTypeWithSubtype.add(aPolicyConfigurationType);
				}
			}
		}
		rv.addAll(typeWithSubtype);
		rv.addAll(typeNoSubtype);
		rv.addAll(noTypeWithSubtype);
		rv.addAll(noTypeNoSubtype);
		return rv;
	}

	@NotNull
	public static <F extends ObjectType> List<ObjectPolicyConfigurationType> getApplicablePolicies(LensContext<F> context) {
		PrismObject<SystemConfigurationType> config = context.getSystemConfiguration();
		if (config == null) {
			return Collections.emptyList();
		}
		PrismObject<F> object = context.getFocusContext() != null ? context.getFocusContext().getObjectAny() : null;
		List<String> subTypes = FocusTypeUtil.determineSubTypes(object);
		List<ObjectPolicyConfigurationType> relevantPolicies;
		try {
			relevantPolicies = ModelUtils.getApplicablePolicies(context.getFocusContext().getObjectTypeClass(), subTypes,
					config.asObjectable());
		} catch (ConfigurationException e) {
			throw new SystemException("Couldn't get relevant object policies", e);
		}
		LOGGER.trace("Relevant policies: {}", relevantPolicies);
		return relevantPolicies;
	}

	public static <F extends ObjectType> ConflictResolutionType getConflictResolution(LensContext<F> context) {
		for (ObjectPolicyConfigurationType p : ModelUtils.getApplicablePolicies(context)) {
			if (p.getConflictResolution() != null) {
				return p.getConflictResolution();
			}
		}
		return null;
	}

}
