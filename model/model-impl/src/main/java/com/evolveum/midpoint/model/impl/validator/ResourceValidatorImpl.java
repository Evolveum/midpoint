/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.model.impl.validator;

import com.evolveum.midpoint.model.api.validator.Issue;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
@Component(value = "resourceValidator")
public class ResourceValidatorImpl implements ResourceValidator {

	public static final String CLASS_DOT = ResourceValidator.class.getSimpleName() + ".";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
			SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH);

	@NotNull
	@Override
	public ValidationResult validate(@NotNull PrismObject<ResourceType> resourceObject, @NotNull Scope scope, @NotNull Task task,
			@NotNull OperationResult result) {
		final ValidationResult vr = new ValidationResult();
		final ResourceType resource = resourceObject.asObjectable();
		final ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(resourceObject);
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		if (schemaHandling != null) {
			checkSchemaHandlingDuplicates(vr, ref, schemaHandling);
			checkMissingAccountObjectClasses(vr, ref, schemaHandling);
		}
		SynchronizationType synchronization = resource.getSynchronization();
		if (synchronization != null) {
			checkSynchronizationDuplicates(vr, ref, synchronization);
		}
		return vr;
	}

	private void checkMissingAccountObjectClasses(ValidationResult vr, ObjectReferenceType ref, SchemaHandlingType schemaHandling) {
		int i = 0;
		for (ResourceObjectTypeDefinitionType objectType : schemaHandling.getObjectType()) {
			if (objectType.getObjectClass() == null) {
				vr.add(Issue.Severity.ERROR,
						CAT_SCHEMA_HANDLING, C_MISSING_OBJECT_CLASS,
						getString(CLASS_DOT + C_MISSING_OBJECT_CLASS, getName(objectType)),
						ref, new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE, i, ResourceObjectTypeDefinitionType.F_OBJECT_CLASS));
			}
			i++;
		}
	}

	private void checkSchemaHandlingDuplicates(ValidationResult vr, ObjectReferenceType ref, SchemaHandlingType schemaHandling) {
		DuplicateObjectTypeDetector detector = new DuplicateObjectTypeDetector();
		for (ResourceObjectTypeDefinitionType objectType : schemaHandling.getObjectType()) {
			detector.add(objectType);
		}
		if (detector.hasDuplicates()) {
			vr.add(Issue.Severity.WARNING,
					CAT_SCHEMA_HANDLING, C_MULTIPLE_SCHEMA_HANDLING_DEFINITIONS,
					getString(CLASS_DOT + C_MULTIPLE_SCHEMA_HANDLING_DEFINITIONS, detector.getDuplicatesList()),
					ref, new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
		}
	}

	private void checkSynchronizationDuplicates(ValidationResult vr, ObjectReferenceType ref, SynchronizationType synchronization) {
		DuplicateObjectTypeDetector detector = new DuplicateObjectTypeDetector();
		for (ObjectSynchronizationType syncType : synchronization.getObjectSynchronization()) {
			detector.add(syncType);
		}
		if (detector.hasDuplicates()) {
			vr.add(Issue.Severity.WARNING,
					CAT_SYNCHRONIZATION, C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS,
					getString(CLASS_DOT + C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS, detector.getDuplicatesList()),
					ref, new ItemPath(ResourceType.F_SYNCHRONIZATION, SynchronizationType.F_OBJECT_SYNCHRONIZATION));
		}
	}

	private String getName(ResourceObjectTypeDefinitionType objectType) {
		StringBuilder sb = new StringBuilder();
		if (objectType.getDisplayName() != null) {
			sb.append(objectType.getDisplayName());
			sb.append(" (");
		}
		sb.append("kind: ");
		sb.append(objectType.getKind() != null ? objectType.getKind() : ShadowKindType.ACCOUNT);
		sb.append(", intent: ");
		sb.append(objectType.getIntent() != null ? objectType.getIntent() : SchemaConstants.INTENT_DEFAULT);
		if (objectType.getDisplayName() != null) {
			sb.append(")");
		}
		return sb.toString();
	}


	@NotNull
	private String getString(String key, Object... parameters) {
		final String resolvedKey;
		if (key != null) {
			if (RESOURCE_BUNDLE.containsKey(key)) {
				resolvedKey = RESOURCE_BUNDLE.getString(key);
			} else {
				resolvedKey = key;
			}
		} else {
			resolvedKey = "";
		}
		final MessageFormat format = new MessageFormat(resolvedKey, RESOURCE_BUNDLE.getLocale());
		return format.format(parameters);
	}

}
