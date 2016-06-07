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
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.text.MessageFormat;
import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

/**
 * EXPERIMENTAL
 *
 * TODO:
 *  - existence of dependent kind/intent/resource (in thorough scope)
 *  - checking references (thorough)
 *  - mapping: unknown channel / except-channel
 *  - mapping: invalid source, invalid target
 *  - empty mapping (?)
 *  - iteration tokens
 *  - invalid objectclass in synchronization
 *  - invalid focus type in synchronization
 *  - empty correlation, correlation condition?
 *  - empty confirmation condition?
 *  - empty synchronization condition?
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
			int i = 1;
			for (ObjectSynchronizationType objectSync : resource.getSynchronization().getObjectSynchronization()) {
				checkObjectSynchronization(ctx, new ItemPath(ResourceType.F_SYNCHRONIZATION, SynchronizationType.F_OBJECT_SYNCHRONIZATION, i), objectSync);
				i++;
			}
		}
		checkSynchronizationExistence(ctx);
		return ctx.validationResult;
	}

	private void checkSchemaHandlingObjectTypes(ResourceValidationContext ctx, SchemaHandlingType schemaHandling) {
		int i = 1;
		for (ResourceObjectTypeDefinitionType objectType : schemaHandling.getObjectType()) {
			ItemPath path = new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE, i);
			checkSchemaHandlingObjectType(ctx, path, objectType);
			i++;
		}
	}

	private void checkSchemaHandlingObjectType(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType) {
		checkDuplicateItems(ctx, path, objectType);
		checkObjectClass(ctx, path, objectType);
		ObjectClassComplexTypeDefinition ocdef = null;
		if (ctx.resourceSchema != null && objectType.getObjectClass() != null) {
			ocdef = ctx.resourceSchema.findObjectClassDefinition(objectType.getObjectClass());
			checkObjectClassDefinition(ctx, path, objectType, ocdef);
		}
		int i = 1;
		for (ResourceAttributeDefinitionType attributeDef : objectType.getAttribute()) {
			checkSchemaHandlingAttribute(ctx, ocdef, path.append(new ItemPath(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, i)), objectType, attributeDef);
			i++;
		}
		i = 1;
		for (ResourceObjectAssociationType associationDef : objectType.getAssociation()) {
			checkSchemaHandlingAssociation(ctx, ocdef, path.append(new ItemPath(ResourceObjectTypeDefinitionType.F_ATTRIBUTE, i)), objectType, associationDef);
			i++;
		}

		if (objectType.getActivation() != null) {
			ItemPath actPath = path.append(ResourceObjectTypeDefinitionType.F_ACTIVATION);
			checkBidirectionalMapping(ctx, actPath, ResourceActivationDefinitionType.F_ADMINISTRATIVE_STATUS, objectType, objectType.getActivation().getAdministrativeStatus());
			checkBidirectionalMapping(ctx, actPath, ResourceActivationDefinitionType.F_EXISTENCE, objectType, objectType.getActivation().getExistence());
			checkBidirectionalMapping(ctx, actPath, ResourceActivationDefinitionType.F_LOCKOUT_STATUS, objectType, objectType.getActivation().getLockoutStatus());
			checkBidirectionalMapping(ctx, actPath, ResourceActivationDefinitionType.F_VALID_FROM, objectType, objectType.getActivation().getValidFrom());
			checkBidirectionalMapping(ctx, actPath, ResourceActivationDefinitionType.F_VALID_TO, objectType, objectType.getActivation().getValidTo());
		}
		if (objectType.getCredentials() != null) {
			ItemPath credPath = path.append(ResourceObjectTypeDefinitionType.F_CREDENTIALS);
			checkPasswordMapping(ctx, credPath, ResourceCredentialsDefinitionType.F_PASSWORD, objectType, objectType.getCredentials().getPassword());
		}
		checkDependencies(ctx, path, objectType);
	}

	private void checkBidirectionalMapping(ResourceValidationContext ctx, ItemPath path, QName itemName, ResourceObjectTypeDefinitionType objectType,
			@Nullable ResourceBidirectionalMappingType bidirectionalMapping) {
		if (bidirectionalMapping == null) {
			return;
		}
		ItemPath itemPath = path.append(itemName);
		int i = 1;
		for (MappingType inbound : bidirectionalMapping.getInbound()) {
			checkMapping(ctx, itemPath.append(ResourceBidirectionalMappingType.F_INBOUND), objectType, itemName, inbound, false, i, true);
			i++;
		}
		i = 1;
		for (MappingType outbound : bidirectionalMapping.getOutbound()) {
			checkMapping(ctx, itemPath.append(ResourceBidirectionalMappingType.F_OUTBOUND), objectType, itemName, outbound, true, i, true);
			i++;
		}
	}

	private void checkPasswordMapping(ResourceValidationContext ctx, ItemPath path, QName itemName, ResourceObjectTypeDefinitionType objectType,
			@Nullable ResourcePasswordDefinitionType passwordDefinition) {
		if (passwordDefinition == null) {
			return;
		}
		ItemPath itemPath = path.append(itemName);
		int i = 1;
		for (MappingType inbound : passwordDefinition.getInbound()) {
			checkMapping(ctx, itemPath.append(ResourcePasswordDefinitionType.F_INBOUND), objectType, itemName, inbound, false, i, true);
			i++;
		}
		if (passwordDefinition.getOutbound() != null) {
			checkMapping(ctx, itemPath.append(ResourcePasswordDefinitionType.F_OUTBOUND), objectType, itemName, passwordDefinition.getOutbound(), true, 0, true);
		}
	}

	private void checkDependencies(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType) {
		for (ResourceObjectTypeDependencyType dependency : objectType.getDependency()) {
			if (dependency.getResourceRef() == null ||
					(ctx.resourceObject.getOid() != null && ctx.resourceObject.getOid().equals(dependency.getResourceRef().getOid()))) {
				if (ResourceTypeUtil.findObjectTypeDefinition(ctx.resourceObject, dependency.getKind(), dependency.getIntent()) == null) {
					ctx.validationResult.add(Issue.Severity.WARNING,
							CAT_SCHEMA_HANDLING, C_DEPENDENT_OBJECT_TYPE_DOES_NOT_EXIST,
							getString(CLASS_DOT + C_DEPENDENT_OBJECT_TYPE_DOES_NOT_EXIST, getName(objectType),
									ResourceTypeUtil.fillDefault(dependency.getKind()) + "/" + ResourceTypeUtil.fillDefault(dependency.getIntent())),
							ctx.resourceRef, path.append(ResourceObjectTypeDefinitionType.F_DEPENDENCY));
				}
			} else {
				// check in 'remote' resource only if thorough validation is required
			}
		}
	}

	private void checkObjectClassDefinition(ResourceValidationContext ctx, ItemPath path,
			ResourceObjectTypeDefinitionType objectType, ObjectClassComplexTypeDefinition ocdef) {
		if (ocdef == null) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SCHEMA_HANDLING, C_UNKNOWN_OBJECT_CLASS,
					getString(CLASS_DOT + C_UNKNOWN_OBJECT_CLASS, getName(objectType), objectType.getObjectClass()),
					ctx.resourceRef, path.append(ResourceObjectTypeDefinitionType.F_OBJECT_CLASS));
		}
	}

	private void checkObjectClass(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType) {
		if (objectType.getObjectClass() == null) {
			ctx.validationResult.add(Issue.Severity.ERROR,
					CAT_SCHEMA_HANDLING, C_MISSING_OBJECT_CLASS,
					getString(CLASS_DOT + C_MISSING_OBJECT_CLASS, getName(objectType)),
					ctx.resourceRef, path.append(ResourceObjectTypeDefinitionType.F_OBJECT_CLASS));
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
		checkSchemaHandlingItem(ctx, path, objectType, attributeDef);
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

	private void checkMapping(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			QName itemName, MappingType mapping, boolean outbound, int index, boolean implicitSourceOrTarget) {
		String inOut = outbound ? getString("ResourceValidator.outboundMapping") : getString("ResourceValidator.inboundMapping", index);
		String itemNameText = PrettyPrinter.prettyPrint(itemName);
		if (outbound && mapping.getTarget() != null) {
			ctx.validationResult.add(Issue.Severity.INFO,
					CAT_SCHEMA_HANDLING, C_SUPERFLUOUS_MAPPING_TARGET,
					getString(CLASS_DOT + C_SUPERFLUOUS_MAPPING_TARGET, getName(objectType),
							inOut, itemNameText, format(mapping.getTarget())),
					ctx.resourceRef, path);
		}
		if (!implicitSourceOrTarget && (mapping.getExpression() == null || mapping.getExpression().getExpressionEvaluator().isEmpty() ||
				(mapping.getExpression().getExpressionEvaluator().size() == 1 &&
						QNameUtil.match(mapping.getExpression().getExpressionEvaluator().get(0).getName(), SchemaConstantsGenerated.C_AS_IS)))) {
			if ((outbound && (mapping.getSource() == null || mapping.getSource().isEmpty())) || (!outbound && mapping.getTarget() == null)) {
				String code = outbound ? C_MISSING_MAPPING_SOURCE : C_MISSING_MAPPING_TARGET;
				ctx.validationResult.add(Issue.Severity.WARNING,
						CAT_SCHEMA_HANDLING, code,
						getString(CLASS_DOT + code, getName(objectType), inOut, itemNameText),
						ctx.resourceRef, path);
			}
		}
		for (MappingSourceDeclarationType source : mapping.getSource()) {
			checkItemPath(ctx, path, objectType, itemName, mapping, outbound, index, source.getPath());
		}
		if (mapping.getTarget() != null) {
			checkItemPath(ctx, path, objectType, itemName, mapping, outbound, index, mapping.getTarget().getPath());
		}
	}

	private void checkItemPath(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			QName itemDef, MappingType mapping, boolean outbound, int index, ItemPathType mappingPath) {
		// TODO
	}

	private String format(MappingTargetDeclarationType target) {
		return target != null ? format(target.getPath()) : "";
	}

	private String format(ItemPathType path) {
		return path != null ? path.toString() : "";
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
		checkSchemaHandlingItem(ctx, path, objectType, associationDef);
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
		for (String intent : associationDef.getIntent()) {
			checkAssociationTargetIntent(ctx, path, objectType, associationDef, ref, intent);
		}
	}

	private void checkAssociationTargetIntent(ResourceValidationContext ctx, ItemPath path,
			ResourceObjectTypeDefinitionType objectType, ResourceObjectAssociationType associationDef, QName ref, String intent) {
		if (ResourceTypeUtil.findObjectTypeDefinition(ctx.resourceObject, associationDef.getKind(), intent) == null) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SCHEMA_HANDLING, C_TARGET_OBJECT_TYPE_DOES_NOT_EXIST,
					getString(CLASS_DOT + C_TARGET_OBJECT_TYPE_DOES_NOT_EXIST, getName(objectType),
							ResourceTypeUtil.fillDefault(associationDef.getKind()) + "/" + ResourceTypeUtil.fillDefault(intent),
							PrettyPrinter.prettyPrint(ref)),
					ctx.resourceRef, path);
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

	private void checkSchemaHandlingItem(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			ResourceItemDefinitionType itemDef) {
		QName itemName = itemRefToName(itemDef.getRef());
		if (itemDef.getOutbound() != null) {
			checkMapping(ctx, path.append(ResourceItemDefinitionType.F_OUTBOUND), objectType, itemName, itemDef.getOutbound(), true, 0, false);
		}
		int i = 1;
		for (MappingType inbound : itemDef.getInbound()) {
			checkMapping(ctx, path.append(new ItemPath(ResourceItemDefinitionType.F_INBOUND, i)), objectType, itemName, inbound, false, i, false);
			i++;
		}
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

	private void checkObjectSynchronization(ResourceValidationContext ctx, ItemPath path, ObjectSynchronizationType objectSync) {
		Map<SynchronizationSituationType,Integer> counts = new HashMap<>();
		for (SynchronizationReactionType reaction : objectSync.getReaction()) {
			if (reaction.getSituation() == null) {
				ctx.validationResult.add(Issue.Severity.WARNING, CAT_SYNCHRONIZATION, C_NO_SITUATION,
						getString(CLASS_DOT + C_NO_SITUATION, getName(objectSync)),
						ctx.resourceRef, path);
			} else {
				Integer c = counts.get(reaction.getSituation());
				counts.put(reaction.getSituation(), c != null ? c+1 : 1);
			}
		}
		checkMissingReactions(ctx, path, objectSync, counts, Arrays.asList(UNLINKED, UNMATCHED));
		checkDuplicateReactions(ctx, path, objectSync, counts);
	}

	private void checkDuplicateReactions(ResourceValidationContext ctx, ItemPath path, ObjectSynchronizationType objectSync,
			Map<SynchronizationSituationType, Integer> counts) {
		List<SynchronizationSituationType> duplicates = new ArrayList<>();
		for (Map.Entry<SynchronizationSituationType, Integer> entry : counts.entrySet()) {
			if (entry.getValue() > 1) {
				duplicates.add(entry.getKey());
			}
		}
		if (!duplicates.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.WARNING, CAT_SYNCHRONIZATION, C_DUPLICATE_REACTIONS,
					getString(CLASS_DOT + C_DUPLICATE_REACTIONS, getName(objectSync), String.valueOf(duplicates)),
					ctx.resourceRef, path);
		}
	}

	private void checkMissingReactions(ResourceValidationContext ctx, ItemPath path, ObjectSynchronizationType objectSync,
			Map<SynchronizationSituationType, Integer> counts, Collection<SynchronizationSituationType> situations) {
		List<SynchronizationSituationType> missing = new ArrayList<>(situations);
		missing.removeAll(counts.keySet());
		if (!missing.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.WARNING, CAT_SYNCHRONIZATION, C_NO_REACTION,
					getString(CLASS_DOT + C_NO_REACTION, getName(objectSync), String.valueOf(missing)),
					ctx.resourceRef, path);
		}
	}

	private String getName(ObjectSynchronizationType objectSync) {
		StringBuilder sb = new StringBuilder();
		if (objectSync.getName() != null) {
			sb.append(objectSync.getName());
			sb.append(" (");
		}
		sb.append("kind: ");
		sb.append(ResourceTypeUtil.fillDefault(objectSync.getKind()));
		sb.append(", intent: ");
		sb.append(ResourceTypeUtil.fillDefault(objectSync.getIntent()));
		if (objectSync.getName() != null) {
			sb.append(")");
		}
		return sb.toString();
	}

	private String getName(ResourceObjectTypeDefinitionType objectType) {
		StringBuilder sb = new StringBuilder();
		if (objectType.getDisplayName() != null) {
			sb.append(objectType.getDisplayName());
			sb.append(" (");
		}
		sb.append("kind: ");
		sb.append(ResourceTypeUtil.fillDefault(objectType.getKind()));
		sb.append(", intent: ");
		sb.append(ResourceTypeUtil.fillDefault(objectType.getIntent()));
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
