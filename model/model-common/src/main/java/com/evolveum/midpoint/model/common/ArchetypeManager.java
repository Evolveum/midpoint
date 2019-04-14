/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.model.common;

import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Component that can efficiently determine archetypes for objects.
 * It is backed by caches, therefore this is supposed to be a low-overhead service that can be
 * used in many places.
 * 
 * @author Radovan Semancik
 */
@Component
public class ArchetypeManager {
	
	private static final Trace LOGGER = TraceManager.getTrace(ArchetypeManager.class);
	
	@Autowired private SystemObjectCache systemObjectCache;
	
	public PrismObject<ArchetypeType> getArchetype(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
		// TODO: make this efficient (use cache)
		return systemObjectCache.getArchetype(oid, result);
	}
	
	public <O extends AssignmentHolderType> PrismObject<ArchetypeType> determineArchetype(PrismObject<O> assignmentHolder, OperationResult result) throws SchemaException, ConfigurationException {
		if (assignmentHolder == null) {
			return null;
		}
		if (!assignmentHolder.canRepresent(AssignmentHolderType.class)) {
			return null;
		}
		List<ObjectReferenceType> archetypeRefs = ((AssignmentHolderType)assignmentHolder.asObjectable()).getArchetypeRef();
		if (archetypeRefs == null || archetypeRefs.isEmpty()) {
			return null;
		}
		if (archetypeRefs.size() > 1) {
			throw new SchemaException("Only a single archetype for an object is supported: "+assignmentHolder);
		}
		ObjectReferenceType archetypeRef = archetypeRefs.get(0);

		PrismObject<ArchetypeType> archetype;
		try {
			archetype = systemObjectCache.getArchetype(archetypeRef.getOid(), result);
		} catch (ObjectNotFoundException e) {
			LOGGER.warn("Archetype {} for object {} cannot be found", archetypeRef.getOid(), assignmentHolder);
			return null;
		}
		return archetype;
	}

	public <O extends ObjectType> ArchetypePolicyType determineArchetypePolicy(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
		if (object == null) {
			return null;
		}
		if (object.canRepresent(AssignmentHolderType.class)) {
			PrismObject<ArchetypeType> archetype = determineArchetype((PrismObject<? extends AssignmentHolderType>) object, result);
			if (archetype != null) {
				return archetype.asObjectable().getArchetypePolicy();
			}
		}
		// No archetype for this object. Try to find appropriate system configuration section for this object.
		return determineObjectPolicyConfiguration(object, result);
	}
	
	public <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
		if (object == null) {
			return null;
		}
		PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
		if (systemConfiguration == null) {
			return null;
		}
		return determineObjectPolicyConfiguration(object, systemConfiguration.asObjectable());
	}
	
	public <O extends ObjectType> ExpressionProfile determineExpressionProfile(PrismObject<O> object, OperationResult result) throws SchemaException, ConfigurationException {
		ArchetypePolicyType archetypePolicy = determineArchetypePolicy(object, result);
		if (archetypePolicy == null) {
			return null;
		}
		String expressionProfileId = archetypePolicy.getExpressionProfile();
		return systemObjectCache.getExpressionProfile(expressionProfileId, result);
	}
	
	/**
	 * This has to remain static due to use from LensContext. Hopefully it will get refactored later.
	 */
	public static <O extends ObjectType> ObjectPolicyConfigurationType determineObjectPolicyConfiguration(PrismObject<O> object, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
		List<String> subTypes = FocusTypeUtil.determineSubTypes(object);
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

	public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(PrismObject<O> object, PrismObject<SystemConfigurationType> systemConfiguration) throws ConfigurationException {
		if (systemConfiguration == null) {
			return null;
		}
		return determineLifecycleModel(object, systemConfiguration.asObjectable());
	}
	
	public static <O extends ObjectType> LifecycleStateModelType determineLifecycleModel(PrismObject<O> object, SystemConfigurationType systemConfigurationType) throws ConfigurationException {
		ObjectPolicyConfigurationType objectPolicyConfiguration = determineObjectPolicyConfiguration(object, systemConfigurationType);
		if (objectPolicyConfiguration == null) {
			return null;
		}
		return objectPolicyConfiguration.getLifecycleStateModel();
	}
}
