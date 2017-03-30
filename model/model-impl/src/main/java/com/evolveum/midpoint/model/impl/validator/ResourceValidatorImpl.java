/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.api.validator.Issue;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.model.impl.util.DataModelUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
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

import static com.evolveum.midpoint.schema.util.ResourceTypeUtil.fillDefault;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.*;

/**
 * EXPERIMENTAL
 *
 * TODO:
 *  - existence of dependent kind/intent/resource (in thorough scope)
 *  - checking references (thorough)
 *  - mapping: unknown channel / except-channel
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

	private static final ItemPath ITEM_PATH_SYNCHRONIZATION = new ItemPath(ResourceType.F_SYNCHRONIZATION, SynchronizationType.F_OBJECT_SYNCHRONIZATION);
	private static final ItemPath ITEM_PATH_SCHEMA_HANDLING = new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE);

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
		@NotNull final ResourceBundle bundle;
		final ResourceSchema resourceSchema;

		public ResourceValidationContext(
				@NotNull PrismObject<ResourceType> resourceObject,
				@NotNull Scope scope, @NotNull Task task,
				@NotNull ValidationResult validationResult, ResourceSchema resourceSchema, ResourceBundle bundle) {
			this.resourceObject = resourceObject;
			this.resourceRef = ObjectTypeUtil.createObjectRef(resourceObject);
			this.scope = scope;
			this.task = task;
			this.validationResult = validationResult;
			this.resourceSchema = resourceSchema;
			this.bundle = bundle;
		}
	}


	@NotNull
	@Override
	public ValidationResult validate(@NotNull PrismObject<ResourceType> resourceObject, @NotNull Scope scope,
			@Nullable Locale locale, @NotNull Task task, @NotNull OperationResult result) {

		final ResourceType resource = resourceObject.asObjectable();
		final ValidationResult vr = new ValidationResult();

		ResourceBundle bundle = ResourceBundle.getBundle(
				SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH,
				locale != null ? locale : Locale.getDefault());

		ResourceSchema resourceSchema = null;
		try {
			resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resourceObject, prismContext);
		} catch (Throwable t) {
			vr.add(Issue.Severity.WARNING, CAT_SCHEMA, C_NO_SCHEMA,
					getString(bundle, CLASS_DOT + C_NO_SCHEMA, t.getMessage()),
					ObjectTypeUtil.createObjectRef(resourceObject), ItemPath.EMPTY_PATH);
		}

		ResourceValidationContext ctx = new ResourceValidationContext(resourceObject, scope, task, vr, resourceSchema, bundle);
		
		SchemaHandlingType schemaHandling = resource.getSchemaHandling();
		if (schemaHandling != null) {
			checkSchemaHandlingDuplicateObjectTypes(ctx, schemaHandling);
			checkSchemaHandlingDefaults(ctx, schemaHandling);
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
		checkSynchronizationExistenceForSchemaHandlingObjectTypes(ctx);
		checkSchemaHandlingExistenceForSynchronizationObjectTypes(ctx);
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
		i = 1;
		for (MappingType outbound : passwordDefinition.getOutbound()) {
			checkMapping(ctx, itemPath.append(ResourcePasswordDefinitionType.F_OUTBOUND), objectType, itemName, outbound, true, i, true);
			i++;
		}
	}

	private void checkDependencies(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType) {
		for (ResourceObjectTypeDependencyType dependency : objectType.getDependency()) {
			if (dependency.getResourceRef() == null ||
					(ctx.resourceObject.getOid() != null && ctx.resourceObject.getOid().equals(dependency.getResourceRef().getOid()))) {
				if (ResourceTypeUtil.findObjectTypeDefinition(ctx.resourceObject, dependency.getKind(), dependency.getIntent()) == null) {
					ctx.validationResult.add(Issue.Severity.WARNING,
							CAT_SCHEMA_HANDLING, C_DEPENDENT_OBJECT_TYPE_DOES_NOT_EXIST,
							getString(ctx.bundle, CLASS_DOT + C_DEPENDENT_OBJECT_TYPE_DOES_NOT_EXIST, getName(objectType),
									fillDefault(dependency.getKind()) + "/" + fillDefault(dependency.getIntent())),
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
					getString(ctx.bundle, CLASS_DOT + C_UNKNOWN_OBJECT_CLASS, getName(objectType), objectType.getObjectClass()),
					ctx.resourceRef, path.append(ResourceObjectTypeDefinitionType.F_OBJECT_CLASS));
		}
	}

	private void checkObjectClass(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType) {
		if (objectType.getObjectClass() == null) {
			ctx.validationResult.add(Issue.Severity.ERROR,
					CAT_SCHEMA_HANDLING, C_MISSING_OBJECT_CLASS,
					getString(ctx.bundle, CLASS_DOT + C_MISSING_OBJECT_CLASS, getName(objectType)),
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
					getString(ctx.bundle, CLASS_DOT + C_MULTIPLE_ITEMS, getName(objectType), prettyPrintUsingStandardPrefix(duplicates)),
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
		// TODO rewrite using CompositeRefinedObjectClassDefinition
		if (ref != null) {
			boolean caseIgnoreAttributeNames = ResourceTypeUtil.isCaseIgnoreAttributeNames(ctx.resourceObject.asObjectable());
			if (ocdef != null) {
				rad = ocdef.findAttributeDefinition(ref, caseIgnoreAttributeNames);
			}
			if (rad == null) {
				for (QName auxOcName : objectType.getAuxiliaryObjectClass()) {
					ObjectClassComplexTypeDefinition auxOcDef = ctx.resourceSchema.findObjectClassDefinition(auxOcName);
					if (auxOcDef != null) {
						rad = auxOcDef.findAttributeDefinition(ref, caseIgnoreAttributeNames);
						if (rad != null) {
							break;
						}
					}
				}
			}
			if (rad == null) {
				ctx.validationResult.add(Issue.Severity.ERROR,
						CAT_SCHEMA_HANDLING, C_UNKNOWN_ATTRIBUTE_NAME,
						getString(ctx.bundle, CLASS_DOT + C_UNKNOWN_ATTRIBUTE_NAME, getName(objectType), ref, objectType.getObjectClass()),
						ctx.resourceRef, path.append(ResourceItemDefinitionType.F_REF));
			}
		}
		checkItemRef(ctx, path, objectType, attributeDef, C_NO_ATTRIBUTE_REF);
		checkMatchingRule(ctx, path, objectType, attributeDef, ref, rad);
	}

	private void checkMapping(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			QName itemName, MappingType mapping, boolean outbound, int index, boolean implicitSourceOrTarget) {
		String inOut = outbound ? getString(ctx.bundle, "ResourceValidator.outboundMapping") : getString(ctx.bundle, "ResourceValidator.inboundMapping", index);
		String itemNameText = prettyPrintUsingStandardPrefix(itemName);
		if (outbound && mapping.getTarget() != null) {
			ctx.validationResult.add(Issue.Severity.INFO,
					CAT_SCHEMA_HANDLING, C_SUPERFLUOUS_MAPPING_TARGET,
					getString(ctx.bundle, CLASS_DOT + C_SUPERFLUOUS_MAPPING_TARGET, getName(objectType),
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
						getString(ctx.bundle, CLASS_DOT + code, getName(objectType), inOut, itemNameText),
						ctx.resourceRef, path);
			}
		}
		for (VariableBindingDefinitionType source : mapping.getSource()) {
			checkItemPath(ctx, path, objectType, itemName, mapping, outbound, itemNameText, true, index, source.getPath());
		}
		if (mapping.getTarget() != null) {
			checkItemPath(ctx, path, objectType, itemName, mapping, outbound, itemNameText, false, index, mapping.getTarget().getPath());
		}
	}

	private void checkItemPath(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			QName itemDef, MappingType mapping, boolean outbound, String itemNameText, boolean isSource, int index, @Nullable ItemPathType mappingPath) {
		if (mappingPath == null) {
			return;
		}
		DataModelUtil.PathResolutionContext pctx = new DataModelUtil.ResourceResolutionContext(prismContext, ExpressionConstants.VAR_FOCUS,
				ctx.resourceObject.asObjectable(), fillDefault(objectType.getKind()), fillDefault(objectType.getIntent()));
		DataModelUtil.PathResolutionResult result = DataModelUtil.resolvePath(mappingPath.getItemPath(), pctx);
		if (result == null) {
			// i.e. not implemented -> no reports
		} else if (result.getDefinition() != null) {
			// definition found => OK (ignoring any potential issues found)
		} else {
			String inOut = outbound ? getString(ctx.bundle, "ResourceValidator.outboundMapping") : getString(ctx.bundle, "ResourceValidator.inboundMapping", index);
			Issue.Severity severity = Issue.getSeverity(result.getIssues());
			if (severity == null) {
				severity = Issue.Severity.INFO;
			}
			String code;
			if (severity != Issue.Severity.INFO) {
				code = isSource ? C_INVALID_MAPPING_SOURCE : C_INVALID_MAPPING_TARGET;
			} else {
				code = isSource ? C_SUSPICIOUS_MAPPING_SOURCE : C_SUSPICIOUS_MAPPING_TARGET;
			}
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (Issue issue : result.getIssues()) {
				if (first) first = false; else sb.append("; ");
				sb.append(issue.getText());
			}
			ctx.validationResult.add(severity, CAT_SCHEMA_HANDLING, code,
					getString(ctx.bundle, CLASS_DOT + code, getName(objectType), inOut, itemNameText, sb.toString()),
					ctx.resourceRef, path);
		}
	}

	private String format(VariableBindingDefinitionType target) {
		return target != null ? format(target.getPath()) : "";
	}

	private String format(ItemPathType path) {
		return path != null ? path.toString() : "";
	}

	private String format(List<?> items) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Object o : items) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(o);
		}
		return sb.toString();
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
					getString(ctx.bundle, CLASS_DOT + C_WRONG_MATCHING_RULE, getName(objectType), prettyPrintUsingStandardPrefix(ref), t.getMessage()),
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
							getString(ctx.bundle, CLASS_DOT + C_COLLIDING_ASSOCIATION_NAME, getName(objectType), prettyPrintUsingStandardPrefix(ref)),
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
					getString(ctx.bundle, CLASS_DOT + C_TARGET_OBJECT_TYPE_DOES_NOT_EXIST, getName(objectType),
							fillDefault(associationDef.getKind()) + "/" + fillDefault(intent),
							prettyPrintUsingStandardPrefix(ref)),
					ctx.resourceRef, path);
		}
	}

	private void checkNotEmpty(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			ResourceObjectAssociationType associationDef, Object object, QName name, String errorCode) {
		if (object == null) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, errorCode,
					getString(ctx.bundle, CLASS_DOT + errorCode, getName(objectType), String.valueOf(associationDef.getRef())), ctx.resourceRef, new ItemPath(path, name));
		}
	}

	private void checkNotEmpty(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			ResourceObjectAssociationType associationDef, Collection<?> values, QName name, String errorCode) {
		if (values == null || values.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, errorCode,
					getString(ctx.bundle, CLASS_DOT + errorCode, getName(objectType), String.valueOf(associationDef.getRef())), ctx.resourceRef, new ItemPath(path, name));
		}
	}

	private void checkItemRef(ResourceValidationContext ctx, ItemPath path, ResourceObjectTypeDefinitionType objectType,
			ResourceItemDefinitionType itemDef, String noRefKey) {
		ItemPath refPath = itemDef.getRef() != null ? itemDef.getRef().getItemPath() : null;
		if (ItemPath.isNullOrEmpty(refPath)) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, noRefKey,
					getString(ctx.bundle, CLASS_DOT + noRefKey, getName(objectType)),
					ctx.resourceRef, path.append(ItemRefinedDefinitionType.F_REF));
		} else if (refPath.size() > 1 || !(refPath.getSegments().get(0) instanceof NameItemPathSegment)) {
			ctx.validationResult.add(Issue.Severity.ERROR, CAT_SCHEMA_HANDLING, C_WRONG_ITEM_NAME,
					getString(ctx.bundle, CLASS_DOT + C_WRONG_ITEM_NAME, getName(objectType), refPath.toString()),
					ctx.resourceRef, path.append(ItemRefinedDefinitionType.F_REF));
		} else if (StringUtils.isBlank(refPath.asSingleName().getNamespaceURI())) {
			ctx.validationResult.add(Issue.Severity.WARNING, CAT_SCHEMA_HANDLING, C_NO_ITEM_NAMESPACE,
					getString(ctx.bundle, CLASS_DOT + C_NO_ITEM_NAMESPACE, getName(objectType), refPath.toString()),
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
					getString(ctx.bundle, CLASS_DOT + C_MULTIPLE_SCHEMA_HANDLING_DEFINITIONS, detector.getDuplicatesList()),
					ctx.resourceRef, new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
		}
	}

	private void checkSchemaHandlingDefaults(ResourceValidationContext ctx, SchemaHandlingType schemaHandling) {
		int defAccount = 0, defEntitlement = 0, defGeneric = 0;
		int totalAccount = 0;
		for (ResourceObjectTypeDefinitionType def : schemaHandling.getObjectType()) {
			if (Boolean.TRUE.equals(def.isDefault())) {
				switch (fillDefault(def.getKind())) {
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
			if (fillDefault(def.getKind()) == ShadowKindType.ACCOUNT) {
				totalAccount++;
			}
		}
		checkMultipleDefaultDefinitions(ctx, ShadowKindType.ACCOUNT, defAccount);
		checkMultipleDefaultDefinitions(ctx, ShadowKindType.ENTITLEMENT, defEntitlement);
		checkMultipleDefaultDefinitions(ctx, ShadowKindType.GENERIC, defGeneric);
		if (totalAccount > 0 && defAccount == 0) {
			ctx.validationResult.add(Issue.Severity.INFO,
					CAT_SCHEMA_HANDLING, C_NO_DEFAULT_ACCOUNT_SCHEMA_HANDLING_DEFAULT_DEFINITION,
					getString(ctx.bundle, CLASS_DOT + C_NO_DEFAULT_ACCOUNT_SCHEMA_HANDLING_DEFAULT_DEFINITION),
					ctx.resourceRef, new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
		}
	}

	private void checkMultipleDefaultDefinitions(ResourceValidationContext ctx, ShadowKindType kind, int count) {
		if (count > 1) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SCHEMA_HANDLING, C_MULTIPLE_SCHEMA_HANDLING_DEFAULT_DEFINITIONS,
					getString(ctx.bundle, CLASS_DOT + C_MULTIPLE_SCHEMA_HANDLING_DEFAULT_DEFINITIONS, kind),
					ctx.resourceRef, new ItemPath(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
		}
	}

	private void checkSynchronizationDuplicateObjectTypes(ResourceValidationContext ctx, SynchronizationType synchronization) {
		DuplicateObjectTypeDetector detector = new DuplicateObjectTypeDetector(synchronization);
		if (detector.hasDuplicates()) {
			ctx.validationResult.add(Issue.Severity.WARNING,
					CAT_SYNCHRONIZATION, C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS,
					getString(ctx.bundle, CLASS_DOT + C_MULTIPLE_SYNCHRONIZATION_DEFINITIONS, detector.getDuplicatesList()),
					ctx.resourceRef, ITEM_PATH_SYNCHRONIZATION);
		}
	}

	private void checkSynchronizationExistenceForSchemaHandlingObjectTypes(ResourceValidationContext ctx) {
		ResourceType resource = ctx.resourceObject.asObjectable();
		Set<ObjectTypeRecord> schemaHandlingFor = new HashSet<>(ObjectTypeRecord.extractFrom(resource.getSchemaHandling()));
		Collection<ObjectTypeRecord> synchronizationFor = ObjectTypeRecord.extractFrom(resource.getSynchronization());
		schemaHandlingFor.removeAll(synchronizationFor);
		if (!schemaHandlingFor.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.INFO, CAT_SYNCHRONIZATION, C_NO_SYNCHRONIZATION_DEFINITION,
					getString(ctx.bundle, CLASS_DOT + C_NO_SYNCHRONIZATION_DEFINITION, ObjectTypeRecord.asFormattedList(schemaHandlingFor)),
					ctx.resourceRef, ITEM_PATH_SYNCHRONIZATION);
		}
	}

	private void checkSchemaHandlingExistenceForSynchronizationObjectTypes(ResourceValidationContext ctx) {
		ResourceType resource = ctx.resourceObject.asObjectable();
		Set<ObjectTypeRecord> synchronizationFor = new HashSet<>(ObjectTypeRecord.extractFrom(resource.getSynchronization()));
		Collection<ObjectTypeRecord> schemaHandlingFor = ObjectTypeRecord.extractFrom(resource.getSchemaHandling());
		synchronizationFor.removeAll(schemaHandlingFor);
		if (!synchronizationFor.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.INFO, CAT_SCHEMA_HANDLING, C_NO_SCHEMA_HANDLING_DEFINITION,
					getString(ctx.bundle, CLASS_DOT + C_NO_SCHEMA_HANDLING_DEFINITION, ObjectTypeRecord.asFormattedList(synchronizationFor)),
					ctx.resourceRef, ITEM_PATH_SCHEMA_HANDLING);
		}
	}

	private void checkObjectSynchronization(ResourceValidationContext ctx, ItemPath path, ObjectSynchronizationType objectSync) {
		Map<SynchronizationSituationType,Integer> counts = new HashMap<>();
		for (SynchronizationReactionType reaction : objectSync.getReaction()) {
			if (reaction.getSituation() == null) {
				ctx.validationResult.add(Issue.Severity.WARNING, CAT_SYNCHRONIZATION, C_NO_SITUATION,
						getString(ctx.bundle, CLASS_DOT + C_NO_SITUATION, getName(objectSync)),
						ctx.resourceRef, path);
			} else {
				Integer c = counts.get(reaction.getSituation());
				counts.put(reaction.getSituation(), c != null ? c+1 : 1);
			}
		}
		checkMissingReactions(ctx, path, objectSync, counts, Collections.singletonList(UNLINKED));
		checkDuplicateReactions(ctx, path, objectSync, counts);
		if (objectSync.getCorrelation().isEmpty()) {
			ctx.validationResult.add(Issue.Severity.WARNING, CAT_SYNCHRONIZATION, C_NO_CORRELATION_RULE,
					getString(ctx.bundle, CLASS_DOT + C_NO_CORRELATION_RULE, getName(objectSync)),
					ctx.resourceRef, path);
		}
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
					getString(ctx.bundle, CLASS_DOT + C_DUPLICATE_REACTIONS, getName(objectSync), format(duplicates)),
					ctx.resourceRef, path);
		}
	}

	private void checkMissingReactions(ResourceValidationContext ctx, ItemPath path, ObjectSynchronizationType objectSync,
			Map<SynchronizationSituationType, Integer> counts, Collection<SynchronizationSituationType> situations) {
		List<SynchronizationSituationType> missing = new ArrayList<>(situations);
		missing.removeAll(counts.keySet());
		if (!missing.isEmpty()) {
			ctx.validationResult.add(Issue.Severity.INFO, CAT_SYNCHRONIZATION, C_NO_REACTION,
					getString(ctx.bundle, CLASS_DOT + C_NO_REACTION, getName(objectSync), format(missing)),
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
		sb.append(fillDefault(objectSync.getKind()));
		sb.append(", intent: ");
		sb.append(fillDefault(objectSync.getIntent()));
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
		sb.append(fillDefault(objectType.getKind()));
		sb.append(", intent: ");
		sb.append(fillDefault(objectType.getIntent()));
		if (objectType.getDisplayName() != null) {
			sb.append(")");
		}
		return sb.toString();
	}


	@NotNull
	private String getString(ResourceBundle bundle, String key, Object... parameters) {
		final String resolvedKey;
		if (key != null) {
			if (bundle.containsKey(key)) {
				resolvedKey = bundle.getString(key);
			} else {
				resolvedKey = key;
			}
		} else {
			resolvedKey = "";
		}
		final MessageFormat format = new MessageFormat(resolvedKey, bundle.getLocale());
		return format.format(parameters);
	}

	// PrettyPrinter output is too verbose/confusing
	// TODO move to some standard class (but PrettyPrinter is questionable)

	public static String prettyPrintUsingStandardPrefix(Collection<QName> names) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (QName name : names) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			sb.append(prettyPrintUsingStandardPrefix(name));
		}
		return sb.toString();
	}

	// TODO check if there's not any standard list of usual prefixes (I'm quite sure there is)
	public static String prettyPrintUsingStandardPrefix(QName name) {
		if (name == null) {
			return null;
		}
		String ns = name.getNamespaceURI();
		if (SchemaConstants.NS_C.equals(ns)) {
			return "c:" + name.getLocalPart();
		} else if (MidPointConstants.NS_RI.equals(ns)) {
			return "ri:" + name.getLocalPart();
		} else if (SchemaConstantsGenerated.NS_ICF_SCHEMA.equals(ns)) {
			return "icfs:" + name.getLocalPart();
		} else if (SchemaConstantsGenerated.NS_ICF_SCHEMA.equals(ns)) {
			return "icfs:" + name.getLocalPart();
		} else {
			return null;
		}
	}

}
