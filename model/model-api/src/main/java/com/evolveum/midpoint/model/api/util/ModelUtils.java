/**
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.model.api.util;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class ModelUtils {
	
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
