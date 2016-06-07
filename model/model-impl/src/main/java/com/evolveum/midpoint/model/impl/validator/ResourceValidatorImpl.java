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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.validator.Issue;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

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

	private static final ItemPath ITEM_PATH_SYNCHRONIZATION = new ItemPath(ResourceType.F_SYNCHRONIZATION, SynchronizationType.F_OBJECT_SYNCHRONIZATION);

	@Autowired
	private MatchingRuleRegistry matchingRuleRegistry;
	
	@Autowired
	private PrismContext prismContext;
	
	private class ResourceValidationContext {
		@NotNull final PrismObject<ResourceType> resourceObject;
		@NotNull final ObjectReferenceType resourceRef;
		@NotNull final Scope scope;
		@NotNull final Task task;
		@NotNull final ValidationResult validationResult;
		final ResourceSchema resourceSchema;

		public ResourceValidationContext(
				@NotNull PrismObject<ResourceType> resourceObject,
				@NotNull Scope scope, @NotNull Task task,
				@NotNull ValidationResult validationResult, ResourceSchema resourceSchema) {
			this.resourceObject = resourceObject;
			this.resourceRef = ObjectTypeUtil.createObjectRef(resourceObject);
			this.scope = scope;
			this.task = task;
			this.validationResult = validationResult;
			this.resourceSchema = resourceSchema;
		}
	}


	@NotNull
	@Override
	public ValidationResult validate(@NotNull PrismObject<ResourceType> resourceObject, @NotNull Scope scope, @NotNull Task task,
			@NotNull OperationResult result) {

		final ResourceType resource = resourceObject.asObjectable();
		final ValidationResult vr = new ValidationResult();

		ResourceSchema resourceSchema = null;
		try {
			resourceSchema = RefinedResourceSchema.getResourceSchema(resourceObject, prismContext);
		} catch (Throwable t) {
			vr.add(Issue.Severity.WARNING, CAT_SCHEMA, C_NO_SCHEMA,
					getString(CLASS_DOT + C_NO_SCHEMA, t.getMessage()),
					ObjectTypeUtil.createObjectRef(resourceObject), ItemPath.EMPTY_PATH);
		}
		
		ResourceValidationContext ctx = new ResourceValidationContext(resourceObject, scope, task, vr, resourceSchema);
		
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		if (schemaHandling != null) {
			checkSchemaHandlingDuplicateObjectTypes(ctx, schemaHandling);
			checkSchemaHandlingMoreDefaults(ctx, schemaHandling);
			checkSchemaHandlingObjectTypes(ctx, schemaHandling);
		}
		SynchronizationType synchronization = resource.getSynchronization();
		if (synchronization != null) {
			checkSynchronizationDuplicateObjectTypes(ctx, synchronization);
		}
		checkSynchronizationExistence(ctx);
		return ctx.validationResult;
	}

	private void checkSchemaHandlingObjectTypes(ResourceValidationContext ctx, SchemaHandlingType schemaHandling) {
		int i = 0;
		for (ResourceObjectTypeDefinitionType objectType : schemaHandling.getObjectType()) {
			ItemPath path = new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE, i);
			checkSchemaHandlingObjectType(ctx, path, objectType);
			i++;
		}
	}

	private void checkSchemaHandlingObjectType(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType) {
		checkDuplicateItems(ctx, path, objectType);
		if (objectType.getObjectClass() == null) {
			ctx.validationResult.add(Issue.Severity.ERROR,
					CAT_SCHEMA_HANDLING, C_MISSING_OBJECT_CLASS,
					getString(CLASS_DOT + C_MISSING_OBJECT_CLASS, getName(objectType)),
					ctx.resourceRef, path.append(ResourceObjectTypeDefinitionType.F_OBJECT_CLASS));
		}
		ObjectClassComplexTypeDefinition ocdef = null;
		if (ctx.resourceSchema != null && objectType.getObjectClass() != null) {
			ocdef = ctx.resourceSchema.findObjectClassDefinition(objectType.getObjectClass());
			if (ocdef == null) {
				ctx.validationResult.add(Issue.Severity.WARNING,
						CAT_SCHEMA_HANDLING, C_UNKNOWN_OBJECT_CLASS,
						getString(CLASS_DOT + C_UNKNOWN_OBJECT_CLASS, getName(objectType), objectType.getObjectClass()),
						ctx.resourceRef, path.append(ResourceObjectTypeDefinitionType.F_OBJECT_CLASS));
			}
		}
		int i = 0;
		for (ResourceAttributeDefinitionType attributeDef : objectType.getAttribute()) {
			checkSchemaHandlingAttribute(ctx, ocdef, path.append(new ItemPath(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, i)), objectType, attributeDef);
			i++;
		}
		i = 0;
		for (ResourceObjectAssociationType associationDef : objectType.getAssociation()) {
			checkSchemaHandlingAssociation(ctx, ocdef, path.append(new ItemPath(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, i)), objectType, associationDef);
			i++;
		}
	}

	private void checkDuplicateItems(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType) {
		Set<QName> existingNames = new HashSet<>();
		Set<QName> duplicates = new HashSet<>();
		for (ItemRefinedDefinitionType def : objectType.getAttribute()) {
			registerName(ctx, existingNames, duplicates, def.getRef());
		}
		for (ItemRefinedDefinitionType def : objectType.getAssociation()) {
			registerName(ctx, existingNames, duplicates, def.getRef());
		}
		if (!duplicates.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.ERROR,
					CAT_SCHEMA_HANDLING, C_MULTIPLE_ITEMS,
					getString(CLASS_DOT + C_MULTIPLE_ITEMS, getName(objectType), PrettyPrinter.prettyPrint(duplicates)),
					ctx.resourceRef, path);
		}
	}

	private void registerName(ResourceValidationContext ctx, Set<QName> existingNames, Set<QName> duplicates, ItemPathType ref) {
		QName name = itemRefToName(ref);
		if (name != null) {
			if (name.getNamespaceURI() == null) {
				name = new QName(ResourceTypeUtil.getResourceNamespace(ctx.resourceObject), name.getLocalPart());			// TODO is this correct?
			}
			if (!existingNames.add(name)) {
				duplicates.add(name);
			}
		}
	}

	private void checkSchemaHandlingAttribute(ResourceValidationContext ctx, ObjectClassComplexTypeDefinition ocdef,
			ItemPath path,
			ResourceObjectTypeDefinitionType objectType, ResourceAttributeDefinitionType attributeDef) {
		QName ref = itemRefToName(attributeDef.getRef());
		checkSchemaHandlingItem(ctx, path, attributeDef);
		ResourceAttributeDefinition<?> rad = null;
		if (ocdef != null) {
			if (ref != null) {
				rad = ocdef.findAttributeDefinition(ref, ResourceTypeUtil.isCaseIgnoreAttributeNames(ctx.resourceObject.asObjectable()));
				if (rad == null) {
					ctx.validationResult.add(Issue.Severity.ERROR,
							CAT_SCHEMA_HANDLING, C_UNKNOWN_ATTRIBUTE_NAME,
							getString(CLASS_DOT + C_UNKNOWN_ATTRIBUTE_NAME, getName(objectType), ref, objectType.getObjectClass()),
							ctx.resourceRef, path.append(ResourceItemDefinitionType.F_REF));
				}
			}
		}
		checkItemRef(ctx, path, objectType, attributeDef, C_NO_ATTRIBUTE_REF);
		checkMatchingRule(ctx, path, objectType, attributeDef, ref, rad);
	}

	@Nullable
	private QName itemRefToName(ItemPathType ref) {
		return ItemPath.asSingleName(ref != null ? ref.getItemPath() : null);
	}

	private void checkMatchingRule(ResourceValidationContext ctx, ItemPath path,
			ResourceObjectTypeDefinitionType objectType, ResourceAttributeDefinitionType attributeDef, QName ref, ResourceAttributeDefinition<?> rad) {
		QName matchingRule = attributeDef.getMatchingRule();
		if (matchingRule == null) {
			return;
		}
		try {
			matchingRuleRegistry.getMatchingRule(matchingRule, rad != null ? rad.getTypeName() : null);
		} catch (Throwable t) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SCHEMA_HANDLING, C_WRONG_MATCHING_RULE,
					getString(CLASS_DOT + C_WRONG_MATCHING_RULE, getName(objectType), PrettyPrinter.prettyPrint(ref), t.getMessage()),
					ctx.resourceRef, path.append(ResourceItemDefinitionType.F_MATCHING_RULE));
		}
	}

	private void checkSchemaHandlingAssociation(ResourceValidationContext ctx, ObjectClassComplexTypeDefinition ocdef, ItemPath path,
			ResourceObjectTypeDefinitionType objectType, ResourceObjectAssociationType associationDef) {
		checkSchemaHandlingItem(ctx, path, associationDef);
		checkItemRef(ctx, path, objectType, associationDef, C_NO_ASSOCIATION_NAME);
		checkNotEmpty(ctx, path, objectType, associationDef, associationDef.getKind(), ResourceObjectAssociationType.F_KIND, C_MISSING_ASSOCIATION_TARGET_KIND);
		checkNotEmpty(ctx, path, objectType, associationDef, associationDef.getIntent(), ResourceObjectAssociationType.F_INTENT, C_MISSING_ASSOCIATION_TARGET_INTENT);
		checkNotEmpty(ctx, path, objectType, associationDef, associationDef.getDirection(), ResourceObjectAssociationType.F_DIRECTION, C_MISSING_ASSOCIATION_DIRECTION);
		checkNotEmpty(ctx, path, objectType, associationDef, associationDef.getAssociationAttribute(), ResourceObjectAssociationType.F_ASSOCIATION_ATTRIBUTE, C_MISSING_ASSOCIATION_ASSOCIATION_ATTRIBUTE);
		checkNotEmpty(ctx, path, objectType, associationDef, associationDef.getValueAttribute(), ResourceObjectAssociationType.F_VALUE_ATTRIBUTE, C_MISSING_ASSOCIATION_VALUE_ATTRIBUTE);
		// TODO checking matching rule for associations?
		QName ref = itemRefToName(associationDef.getRef());
		if (ocdef != null) {
			if (ref != null) {
				if (ocdef.findAttributeDefinition(ref, ResourceTypeUtil.isCaseIgnoreAttributeNames(ctx.resourceObject.asObjectable())) != null) {
					ctx.validationResult.add(Issue.Severity.ERROR,
							CAT_SCHEMA_HANDLING, C_COLLIDING_ASSOCIATION_NAME,
							getString(CLASS_DOT + C_COLLIDING_ASSOCIATION_NAME, getName(objectType), PrettyPrinter.prettyPrint(ref)),
							ctx.resourceRef, path.append(ResourceItemDefinitionType.F_REF));
				}
			}
		}

	}

	private void checkNotEmpty(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			ResourceObjectAssociationType associationDef, Object object, QName name, String errorCode) {
		if (object == null) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, errorCode,
					getString(CLASS_DOT + errorCode, getName(objectType), String.valueOf(associationDef.getRef())), ctx.resourceRef, new ItemPath(path, name));
		}
	}

	private void checkNotEmpty(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			ResourceObjectAssociationType associationDef, Collection<?> values, QName name, String errorCode) {
		if (values == null || values.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, errorCode,
					getString(CLASS_DOT + errorCode, getName(objectType), String.valueOf(associationDef.getRef())), ctx.resourceRef, new ItemPath(path, name));
		}
	}

	private void checkItemRef(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			ResourceItemDefinitionType itemDef, String noRefKey) {
		ItemPath refPath = itemDef.getRef() != null ? itemDef.getRef().getItemPath() : null;
		if (ItemPath.isNullOrEmpty(refPath)) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, noRefKey,
					getString(CLASS_DOT + noRefKey, getName(objectType)),
					ctx.resourceRef, path.append(ItemRefinedDefinitionType.F_REF));
		} else if (refPath.size() > 1 || !(refPath.getSegments().get(0) instanceof NameItemPathSegment)) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, C_WRONG_ITEM_NAME,
					getString(CLASS_DOT + C_WRONG_ITEM_NAME, getName(objectType), refPath.toString()),
					ctx.resourceRef, path.append(ItemRefinedDefinitionType.F_REF));
		} else if (StringUtils.isBlank(refPath.asSingleName().getNamespaceURI())) {
			ctx.validationResult.add(Issue.Severity.WARNING, CAT_SCHEMA_HANDLING, C_NO_ITEM_NAMESPACE,
					getString(CLASS_DOT + C_NO_ITEM_NAMESPACE, getName(objectType), refPath.toString()),
					ctx.resourceRef, path.append(ItemRefinedDefinitionType.F_REF));
		}
	}

	private void checkSchemaHandlingItem(ResourceValidationContext ctx, ItemPath path, ResourceItemDefinitionType itemDef) {
	}

	private void checkSchemaHandlingDuplicateObjectTypes(ResourceValidationContext ctx, SchemaHandlingType schemaHandling) {
		DuplicateObjectTypeDetector detector = new DuplicateObjectTypeDetector(schemaHandling);
		if (detector.hasDuplicates()) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SCHEMA_HANDLING, C_MULTIPLE_SCHEMA_HANDLING_DEFINITIONS,
					getString(CLASS_DOT + C_MULTIPLE_SCHEMA_HANDLING_DEFINITIONS, detector.getDuplicatesList()),
					ctx.resourceRef, new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
		}
	}

	private void checkSchemaHandlingMoreDefaults(ResourceValidationContext ctx, SchemaHandlingType schemaHandling) {
		int defAccount = 0, defEntitlement = 0, defGeneric = 0;
		for (ResourceObjectTypeDefinitionType def : schemaHandling.getObjectType()) {
			if (Boolean.TRUE.equals(def.isDefault())) {
				switch (ResourceTypeUtil.fillDefault(def.getKind())) {
					case ACCOUNT:
						defAccount++;
						break;
					case ENTITLEMENT:
						defEntitlement++;
						break;
					case GENERIC:
						defGeneric++;
						break;
					default:
						throw new IllegalStateException();
				}
			}
		}
		checkMultipleDefaultDefinitions(ctx, ShadowKindType.ACCOUNT, defAccount);
		checkMultipleDefaultDefinitions(ctx, ShadowKindType.ENTITLEMENT, defEntitlement);
		checkMultipleDefaultDefinitions(ctx, ShadowKindType.GENERIC, defGeneric);
	}

	private void checkMultipleDefaultDefinitions(ResourceValidationContext ctx, ShadowKindType kind, int count) {
		if (count > 1) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SCHEMA_HANDLING, C_MULTIPLE_SCHEMA_HANDLING_DEFAULT_DEFINITIONS,
					getString(CLASS_DOT + C_MULTIPLE_SCHEMA_HANDLING_DEFAULT_DEFINITIONS, kind),
					ctx.resourceRef, new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
		}
	}

	private void checkSynchronizationDuplicateObjectTypes(ResourceValidationContext ctx, SynchronizationType synchronization) {
		DuplicateObjectTypeDetector detector = new DuplicateObjectTypeDetector(synchronization);
		if (detector.hasDuplicates()) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SYNCHRONIZATION, C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS,
					getString(CLASS_DOT + C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS, detector.getDuplicatesList()),
					ctx.resourceRef, ITEM_PATH_SYNCHRONIZATION);
		}
	}

	private void checkSynchronizationExistence(ResourceValidationContext ctx) {
		ResourceType resource = ctx.resourceObject.asObjectable();
		Set<ObjectTypeRecord> schemaHandlingFor = new HashSet<>(ObjectTypeRecord.extractFrom(resource.getSchemaHandling()));
		Collection<ObjectTypeRecord> synchronizationFor = ObjectTypeRecord.extractFrom(resource.getSynchronization());
		schemaHandlingFor.removeAll(synchronizationFor);
		if (!schemaHandlingFor.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.INFO, CAT_SYNCHRONIZATION, C_NO_SYNCHRONIZATION_DEFINITION,
					getString(CLASS_DOT + C_NO_SYNCHRONIZATION_DEFINITION, ObjectTypeRecord.asFormattedList(schemaHandlingFor)),
					ctx.resourceRef, ITEM_PATH_SYNCHRONIZATION);
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
