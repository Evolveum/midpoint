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
package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.dataModel.dot.DotModel;
import com.evolveum.midpoint.model.impl.dataModel.model.AdHocDataItem;
import com.evolveum.midpoint.model.impl.dataModel.model.DataItem;
import com.evolveum.midpoint.model.impl.dataModel.model.ResourceDataItem;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author pmederly
 */

@Component
public class DataModelVisualizerImpl implements DataModelVisualizer {

	private static final Trace LOGGER = TraceManager.getTrace(DataModelVisualizerImpl.class);
	public static final QName ACTIVATION_EXISTENCE = new QName(SchemaConstants.NS_C, "existence");

	@Autowired
	private ModelService modelService;

	@Autowired
	private PrismContext prismContext;

    @Override
	public String visualize(Collection<String> resourceOids, Target target, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException {

		LOGGER.debug("Starting data model visualization");

		DataModel model = new DataModel(prismContext);

		ObjectQuery resourceQuery;
		if (resourceOids != null) {
			resourceQuery = QueryBuilder.queryFor(ResourceType.class, prismContext)
					.id(resourceOids.toArray(new String[0]))
					.build();
		} else {
			resourceQuery = null;
		}
		List<PrismObject<ResourceType>> resources = modelService.searchObjects(ResourceType.class, resourceQuery, null, task, result);

		createDataItems(model, resources);
		processResourceMappings(model, resources);

		return export(model, target);
	}

	private String export(DataModel model, Target target) {
		if (target == null || target == Target.DOT) {
			return new DotModel(model).exportDot();
		} else {
			throw new UnsupportedOperationException("Not implemented yet.");
		}
	}

	@Override
	@SuppressWarnings("unchecked")
    public String visualize(ResourceType resource, Target target, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException {

		LOGGER.debug("Starting data model visualization for {}", ObjectTypeUtil.toShortString(resource));

		DataModel model = new DataModel(prismContext);

		List<PrismObject<ResourceType>> resources = new ArrayList<>();
		resources.add(resource.clone().asPrismObject());

		createDataItems(model, resources);
		processResourceMappings(model, resources);

		return export(model, target);
    }

	private void processResourceMappings(DataModel model, List<PrismObject<ResourceType>> resources) throws SchemaException {
		for (PrismObject<ResourceType> resource : resources) {
			LOGGER.debug("Processing {}", ObjectTypeUtil.toShortString(resource));
			RefinedResourceSchema refinedResourceSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
			if (refinedResourceSchema == null) {
				LOGGER.debug("Refined resource schema is null, skipping the resource.");
				continue;
			}
			List<? extends RefinedObjectClassDefinition> refinedDefinitions = refinedResourceSchema.getRefinedDefinitions();
			for (RefinedObjectClassDefinition refinedDefinition : refinedDefinitions) {
				LOGGER.debug("Processing refined definition {}", refinedDefinition);
				Collection<? extends RefinedAttributeDefinition<?>> attributeDefinitions = refinedDefinition.getAttributeDefinitions();
				final ShadowKindType kind = def(refinedDefinition.getKind());
				final String intent = def(refinedDefinition.getIntent());
				for (RefinedAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
					if (attributeDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Processing refined attribute definition for {}", attributeDefinition.getName());
					ResourceDataItem attrItem = model.findResourceItem(resource.getOid(), kind, intent, new ItemPath(attributeDefinition.getName()));
					if (attributeDefinition.getOutboundMappingType() != null) {
						processOutboundMapping(model, attrItem, attributeDefinition.getOutboundMappingType(), null);
					}
					processInboundMappings(model, attrItem, attributeDefinition.getInboundMappingTypes());
				}
				Collection<RefinedAssociationDefinition> associationDefinitions = refinedDefinition.getAssociationDefinitions();
				for (RefinedAssociationDefinition associationDefinition : associationDefinitions) {
					if (associationDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Processing refined association definition for {}", associationDefinition.getName());
					ResourceDataItem assocItem = model.findResourceItem(resource.getOid(), kind, intent, new ItemPath(associationDefinition.getName()));
					if (associationDefinition.getOutboundMappingType() != null) {
						processOutboundMapping(model, assocItem, associationDefinition.getOutboundMappingType(), null);
					}
//					if (associationDefinition.getAssociationTarget() != null) {
//						RefinedObjectClassDefinition target = associationDefinition.getAssociationTarget();
//						boolean objectToSubject = associationDefinition.getResourceObjectAssociationType().getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT;
//						associationDefinition.getResourceObjectAssociationType().getAssociationAttribute()
//					}
				}
				ResourceActivationDefinitionType actMapping = refinedDefinition.getActivationSchemaHandling();
				if (actMapping != null) {
					processBidirectionalMapping(model, resource.getOid(), kind, intent, new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), actMapping.getAdministrativeStatus());
					processBidirectionalMapping(model, resource.getOid(), kind, intent, new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM), actMapping.getValidFrom());
					processBidirectionalMapping(model, resource.getOid(), kind, intent, new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO), actMapping.getValidTo());
					processBidirectionalMapping(model, resource.getOid(), kind, intent, new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS), actMapping.getLockoutStatus());
					processBidirectionalMapping(model, resource.getOid(), kind, intent, new ItemPath(FocusType.F_ACTIVATION, ACTIVATION_EXISTENCE), actMapping.getExistence());
				}
				ResourcePasswordDefinitionType pwdDef = refinedDefinition.getPasswordDefinition();
				if (pwdDef != null) {
					final ItemPath pwdPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD);
					ResourceDataItem resourceDataItem = model.findResourceItem(resource.getOid(), kind, intent, pwdPath);
					if (resourceDataItem == null) {
						throw new IllegalStateException("No resource item for " + resource.getOid() + ":" + kind + ":" + intent + ":" + pwdPath);
					}
					if (pwdDef.getOutbound() != null) {
						for (MappingType outbound : pwdDef.getOutbound()) {
							processOutboundMapping(model, resourceDataItem, outbound, pwdPath);
						}
					}
					for (MappingType inbound : pwdDef.getInbound()) {
						processInboundMapping(model, resourceDataItem, inbound, pwdPath);
					}
				}
			}
		}
	}

	private void processBidirectionalMapping(DataModel model, String oid, ShadowKindType kind, String intent, ItemPath itemPath,
			ResourceBidirectionalMappingType mapping) {
		if (mapping == null) {
			return;
		}
		ResourceDataItem resourceDataItem = model.findResourceItem(oid, kind, intent, itemPath);
		if (resourceDataItem == null) {
			throw new IllegalStateException("No resource item for " + oid + ":" + kind + ":" + intent + ":" + itemPath);
		}
		for (MappingType outbound : mapping.getOutbound()) {
			processOutboundMapping(model, resourceDataItem, outbound, itemPath);
		}
		for (MappingType inbound : mapping.getInbound()) {
			processInboundMapping(model, resourceDataItem, inbound, itemPath);
		}
	}

	private void createDataItems(DataModel model, List<PrismObject<ResourceType>> resources) throws SchemaException {
		LOGGER.debug("createDataItems starting");
		for (PrismObject<ResourceType> resource : resources) {
			final ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
			if (resourceSchema == null) {
				LOGGER.debug("Resource schema is null, skipping the resource.");
				continue;
			}
			RefinedResourceSchema refinedResourceSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
			if (refinedResourceSchema == null) {
				LOGGER.debug("Refined resource schema is null, skipping the resource.");		// actually shouldn't be null if resource schema exists
				continue;
			}

			model.registerResource(resource);

			List<? extends RefinedObjectClassDefinition> refinedDefinitions = refinedResourceSchema.getRefinedDefinitions();
			for (RefinedObjectClassDefinition refinedDefinition : refinedDefinitions) {
				LOGGER.debug("Processing refined definition {} in {}", refinedDefinition, resource);
				Collection<? extends RefinedAttributeDefinition<?>> attributeDefinitions = refinedDefinition.getAttributeDefinitions();
				//Collection<? extends ResourceAttributeDefinition> rawAttributeDefinitions = refinedDefinition.getObjectClassDefinition().getAttributeDefinitions();
				final ShadowKindType kind = def(refinedDefinition.getKind());
				final String intent = def(refinedDefinition.getIntent());
				for (RefinedAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
					if (attributeDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Registering refined attribute definition for {}", attributeDefinition.getName());
					ResourceDataItem attrItem = new ResourceDataItem(model, resource.getOid(), kind, intent, attributeDefinition.getName());
					attrItem.setRefinedResourceSchema(refinedResourceSchema);
					attrItem.setRefinedObjectClassDefinition(refinedDefinition);
					attrItem.setRefinedAttributeDefinition(attributeDefinition);
					// TODO check the name
					model.registerDataItem(attrItem);
				}
				// TODO check attributes not mentioned in schema handling
				Collection<RefinedAssociationDefinition> associationDefinitions = refinedDefinition.getAssociationDefinitions();
				for (RefinedAssociationDefinition associationDefinition : associationDefinitions) {
					if (associationDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Registering refined association definition for {}", associationDefinition.getName());
					ResourceDataItem assocItem = new ResourceDataItem(model, resource.getOid(), kind, intent, associationDefinition.getName());
					model.registerDataItem(assocItem);
				}
				model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)));
				model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS)));
				model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALID_FROM)));
				model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALID_TO)));
				model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, new ItemPath(ShadowType.F_ACTIVATION, ACTIVATION_EXISTENCE)));
				model.registerDataItem(new ResourceDataItem(model, resource.getOid(), kind, intent, new ItemPath(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)));
			}
		}
//		createRepoDataItems(UserType.class);
//		createRepoDataItems(RoleType.class);
//		createRepoDataItems(OrgType.class);
//		createRepoDataItems(ServiceType.class);

		LOGGER.debug("createDataItems finished");
	}

	static ShadowKindType def(ShadowKindType kind) {
		return kind != null ? kind : ShadowKindType.ACCOUNT;
	}

	static String def(String intent) {
		return intent != null ? intent : "default";
	}

	private void processInboundMappings(DataModel model, ResourceDataItem item, List<MappingType> mappings) {
		if (mappings == null) {
			return;
		}
		for (MappingType mapping : mappings) {
			processInboundMapping(model, item, mapping, null);
		}
	}

	private void processInboundMapping(@NotNull DataModel model, @NotNull ResourceDataItem sourceItem, @NotNull MappingType mapping,
			@Nullable ItemPath defaultTargetItemPath) {
		LOGGER.debug("Processing inbound mapping: {} for {}", mapping, sourceItem);
		List<DataItem> sources = new ArrayList<>();
		for (VariableBindingDefinitionType sourceDecl : mapping.getSource()) {
			LOGGER.debug(" - src: {}", sourceDecl.getPath());
			DataItem explicitSourceItem = resolveSourceItem(model, sourceItem, mapping, sourceDecl, null);
			sources.add(explicitSourceItem);
		}
		if (!sources.contains(sourceItem)) {
			sources.add(sourceItem);
		}
		DataItem targetItem = null;
		VariableBindingDefinitionType targetDecl = mapping.getTarget();
		if (mapping.getTarget() != null) {
			LOGGER.debug(" - target: {}", targetDecl.getPath());
			targetItem = resolveTargetItem(model, sourceItem, mapping, targetDecl, ExpressionConstants.VAR_FOCUS);
		} else if (defaultTargetItemPath != null) {
			targetItem = resolveTargetItem(model, sourceItem, mapping, defaultTargetItemPath, ExpressionConstants.VAR_FOCUS);
		}
		model.registerMappingRelation(sources, targetItem, mapping);
	}

	private DataItem resolveSourceItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
			@NotNull MappingType mapping, @NotNull VariableBindingDefinitionType sourceDecl, @Nullable QName defaultVariable) {
		// todo from the description
		return resolveSourceItem(model, currentItem, mapping, sourceDecl.getPath().getItemPath(), defaultVariable);
	}

	// for outbound (but sometimes also inbound) mappings
	@NotNull
	private DataItem resolveSourceItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
			@NotNull MappingType mapping, @NotNull ItemPath path, @Nullable QName defaultVariable) {
		if (!(path.first() instanceof NameItemPathSegment)) {
			LOGGER.warn("Probably incorrect path ({}) - does not start with a name - skipping", path);
			return createAdHocDataItem(model, path);
		}
		QName varName;
		ItemPath itemPath;
		NameItemPathSegment firstNameSegment = (NameItemPathSegment) path.first();
		if (firstNameSegment.isVariable()) {
			varName = firstNameSegment.getName();
			itemPath = path.tail();
		} else {
			if (defaultVariable == null) {
				LOGGER.warn("No default variable for mapping source");
				return createAdHocDataItem(model, path);
			}
			varName = defaultVariable;
			itemPath = path;
		}

		if (QNameUtil.match(ExpressionConstants.VAR_ACCOUNT, varName)) {
			return resolveResourceItem(model, currentItem, itemPath);
		} else if (QNameUtil.match(ExpressionConstants.VAR_USER, varName)) {
			return model.resolveRepositoryItem(UserType.class, itemPath);
		} else if (QNameUtil.match(ExpressionConstants.VAR_ACTOR, varName)) {
			return model.resolveRepositoryItem(UserType.class, itemPath);			// TODO
		} else if (QNameUtil.match(ExpressionConstants.VAR_FOCUS, varName)) {
			Class<? extends ObjectType> guessedClass = guessFocusClass(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent());
			DataItem item = model.resolveRepositoryItem(guessedClass, itemPath);
			if (item != null) {
				return item;
			}
			// TODO guess e.g. by item existence in schema
			LOGGER.warn("Couldn't resolve {} in $focus", path);
		} else if (QNameUtil.match(ExpressionConstants.VAR_INPUT, varName)) {
			return currentItem;
		} else {
			LOGGER.warn("Unsupported variable {} in {}", varName, path);
		}
		return createAdHocDataItem(model, path);
	}

	private DataItem createAdHocDataItem(DataModel model, ItemPath path) {
		return new AdHocDataItem(path);
	}

	// currently for inbounds only
	@NotNull
	private DataItem resolveTargetItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
			@NotNull MappingType mapping, @NotNull VariableBindingDefinitionType targetDecl, @Nullable QName defaultVariable) {
		// todo from the description
		return resolveTargetItem(model, currentItem, mapping, targetDecl.getPath().getItemPath(), defaultVariable);
	}

	// currently for inbounds only
	@NotNull
	private DataItem resolveTargetItem(@NotNull DataModel model, @NotNull ResourceDataItem currentItem,
			@NotNull MappingType mapping, @NotNull ItemPath path, @Nullable QName defaultVariable) {
		if (!(path.first() instanceof NameItemPathSegment)) {
			LOGGER.warn("Probably incorrect path ({}) - does not start with a name - skipping", path);
			return createAdHocDataItem(model, path);
		}
		QName varName;
		ItemPath itemPath;
		NameItemPathSegment firstNameSegment = (NameItemPathSegment) path.first();
		if (firstNameSegment.isVariable()) {
			varName = firstNameSegment.getName();
			itemPath = path.tail();
		} else {
			if (defaultVariable == null) {
				LOGGER.warn("No default variable for mapping target");
				return createAdHocDataItem(model, path);
			}
			varName = defaultVariable;
			itemPath = path;
		}

		if (QNameUtil.match(ExpressionConstants.VAR_ACCOUNT, varName)) {
			return resolveResourceItem(model, currentItem, itemPath);				// does make sense?
		} else if (QNameUtil.match(ExpressionConstants.VAR_USER, varName)) {
			return model.resolveRepositoryItem(UserType.class, itemPath);
		} else if (QNameUtil.match(ExpressionConstants.VAR_ACTOR, varName)) {
			return model.resolveRepositoryItem(UserType.class, itemPath);			// TODO
		} else if (QNameUtil.match(ExpressionConstants.VAR_FOCUS, varName)) {
			Class<? extends ObjectType> guessedClass = guessFocusClass(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent());
			DataItem item = model.resolveRepositoryItem(guessedClass, itemPath);
			if (item != null) {
				return item;
			}
			// TODO guess e.g. by item existence in schema
			LOGGER.warn("Couldn't resolve {} in $focus", path);
		} else if (QNameUtil.match(ExpressionConstants.VAR_INPUT, varName)) {
			return currentItem;														// does make sense?
		} else {
			LOGGER.warn("Unsupported variable {} in {}", varName, path);
		}
		return createAdHocDataItem(model, path);
	}

	private Class<? extends ObjectType> guessFocusClass(@NotNull String resourceOid, @NotNull ShadowKindType kind, @NotNull String intent) {
		// TODO use synchronization as well
		switch (kind) {
			case ACCOUNT: return UserType.class;
			case ENTITLEMENT: return RoleType.class;
			case GENERIC: return OrgType.class;
		}
		throw new IllegalStateException();
	}

	private ResourceDataItem resolveResourceItem(DataModel model, ResourceDataItem currentItem, ItemPath path) {
		return model.findResourceItem(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent(), path);
	}

	private void processOutboundMapping(@NotNull DataModel model, @NotNull ResourceDataItem targetItem, @NotNull MappingType mapping,
			@Nullable ItemPath defaultSourceItemPath) {
		LOGGER.debug("Processing outbound mapping: {} for {}", mapping, targetItem);
		List<DataItem> sources = new ArrayList<>();
		for (VariableBindingDefinitionType sourceDecl : mapping.getSource()) {
			LOGGER.debug(" - src: {}", sourceDecl.getPath());
			DataItem sourceItem = resolveSourceItem(model, targetItem, mapping, sourceDecl, ExpressionConstants.VAR_FOCUS);
			sources.add(sourceItem);
		}
		if (defaultSourceItemPath != null) {
			DataItem defaultSource = resolveSourceItem(model, targetItem, mapping, defaultSourceItemPath, ExpressionConstants.VAR_FOCUS);
			if (!sources.contains(defaultSource)) {
				sources.add(defaultSource);
			}
		}
		VariableBindingDefinitionType targetDecl = mapping.getTarget();
		if (targetDecl != null) {
			LOGGER.warn(" - ignoring target (mapping is outbound): {}; using {} instead", targetDecl.getPath(), targetItem);
		}
		model.registerMappingRelation(sources, targetItem, mapping);
	}

}
