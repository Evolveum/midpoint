package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
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
public class DataModelVisualizer {

	private static final Trace LOGGER = TraceManager.getTrace(DataModelVisualizer.class);

	@Autowired
	private ModelController modelController;

	@Autowired
	private PrismContext prismContext;

    public String visualize(Collection<String> resourceOids, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException {

		LOGGER.info("Starting data model visualization");

		VisualizationContext ctx = new VisualizationContext(prismContext);

		ObjectQuery resourceQuery;
		if (resourceOids != null) {
			resourceQuery = QueryBuilder.queryFor(ResourceType.class, prismContext)
					.id(resourceOids.toArray(new String[0]))
					.build();
		} else {
			resourceQuery = null;
		}
		List<PrismObject<ResourceType>> resources = modelController.searchObjects(ResourceType.class, resourceQuery, null, task, result);

		createDataItems(ctx, resources);
		processResourceMappings(ctx, resources);

		return ctx.exportDot();
    }

	@SuppressWarnings("unchecked")
    public String visualize(ResourceType resource, Task task, OperationResult result)
			throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException {

		LOGGER.info("Starting data model visualization for {}", ObjectTypeUtil.toShortString(resource));

		VisualizationContext ctx = new VisualizationContext(prismContext);

		List<PrismObject<ResourceType>> resources = new ArrayList<>();
		resources.add(resource.clone().asPrismObject());

		createDataItems(ctx, resources);
		processResourceMappings(ctx, resources);

		return ctx.exportDot();
    }

	private void processResourceMappings(VisualizationContext ctx, List<PrismObject<ResourceType>> resources) throws SchemaException {
		for (PrismObject<ResourceType> resource : resources) {
			LOGGER.info("Processing {}", ObjectTypeUtil.toShortString(resource));
			RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
			if (refinedResourceSchema == null) {
				LOGGER.info("Refined resource schema is null, skipping the resource.");
				continue;
			}
			List<? extends RefinedObjectClassDefinition> refinedDefinitions = refinedResourceSchema.getRefinedDefinitions();
			for (RefinedObjectClassDefinition refinedDefinition : refinedDefinitions) {
				LOGGER.debug("Processing refined definition {}", refinedDefinition);
				Collection<? extends RefinedAttributeDefinition<?>> attributeDefinitions = refinedDefinition.getAttributeDefinitions();
				for (RefinedAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
					if (attributeDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Processing refined attribute definition for {}", attributeDefinition.getName());
					ResourceDataItem attrItem = ctx.findResourceItem(resource.getOid(), def(refinedDefinition.getKind()), def(refinedDefinition.getIntent()), new ItemPath(attributeDefinition.getName()));
					if (attributeDefinition.getOutboundMappingType() != null) {
						processOutboundMapping(ctx, attrItem, attributeDefinition.getOutboundMappingType());
					}
					processInboundMappings(ctx, attrItem, attributeDefinition.getInboundMappingTypes());
				}
				Collection<RefinedAssociationDefinition> associationDefinitions = refinedDefinition.getAssociations();
				for (RefinedAssociationDefinition associationDefinition : associationDefinitions) {
					if (associationDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Processing refined association definition for {}", associationDefinition.getName());
					ResourceDataItem assocItem = ctx.findResourceItem(resource.getOid(), def(refinedDefinition.getKind()), def(refinedDefinition.getIntent()), new ItemPath(associationDefinition.getName()));
					if (associationDefinition.getOutboundMappingType() != null) {
						processOutboundMapping(ctx, assocItem, associationDefinition.getOutboundMappingType());
					}
//					if (associationDefinition.getAssociationTarget() != null) {
//						RefinedObjectClassDefinition target = associationDefinition.getAssociationTarget();
//						boolean objectToSubject = associationDefinition.getResourceObjectAssociationType().getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT;
//						associationDefinition.getResourceObjectAssociationType().getAssociationAttribute()
//					}
				}
			}
		}
	}

	private void createDataItems(VisualizationContext ctx, List<PrismObject<ResourceType>> resources) throws SchemaException {
		LOGGER.debug("createDataItems starting");
		for (PrismObject<ResourceType> resource : resources) {
			final ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
			if (resourceSchema == null) {
				LOGGER.info("Resource schema is null, skipping the resource.");
				continue;
			}
			RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resource);
			if (refinedResourceSchema == null) {
				LOGGER.info("Refined resource schema is null, skipping the resource.");		// actually shouldn't be null if resource schema exists
				continue;
			}

			ctx.registerResource(resource);

			List<? extends RefinedObjectClassDefinition> refinedDefinitions = refinedResourceSchema.getRefinedDefinitions();
			for (RefinedObjectClassDefinition refinedDefinition : refinedDefinitions) {
				LOGGER.debug("Processing refined definition {} in {}", refinedDefinition, resource);
				Collection<? extends RefinedAttributeDefinition<?>> attributeDefinitions = refinedDefinition.getAttributeDefinitions();
				//Collection<? extends ResourceAttributeDefinition> rawAttributeDefinitions = refinedDefinition.getObjectClassDefinition().getAttributeDefinitions();
				for (RefinedAttributeDefinition<?> attributeDefinition : attributeDefinitions) {
					if (attributeDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Registering refined attribute definition for {}", attributeDefinition.getName());
					ResourceDataItem attrItem = new ResourceDataItem(ctx, resource.getOid(), def(refinedDefinition.getKind()), def(refinedDefinition.getIntent()), attributeDefinition.getName());
					attrItem.setRefinedResourceSchema(refinedResourceSchema);
					attrItem.setRefinedObjectClassDefinition(refinedDefinition);
					attrItem.setRefinedAttributeDefinition(attributeDefinition);
					// TODO check the name
					ctx.registerDataItem(attrItem);
				}
				// TODO check attributes not mentioned in schema handling
				Collection<RefinedAssociationDefinition> associationDefinitions = refinedDefinition.getAssociations();
				for (RefinedAssociationDefinition associationDefinition : associationDefinitions) {
					if (associationDefinition.isIgnored()) {
						continue;
					}
					LOGGER.debug("Registering refined association definition for {}", associationDefinition.getName());
					ResourceDataItem assocItem = new ResourceDataItem(ctx, resource.getOid(), def(refinedDefinition.getKind()), def(refinedDefinition.getIntent()), associationDefinition.getName());
					ctx.registerDataItem(assocItem);
				}
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

	private void processInboundMappings(VisualizationContext ctx, ResourceDataItem item, List<MappingType> mappings) {
		if (mappings == null) {
			return;
		}
		for (MappingType mapping : mappings) {
			processInboundMapping(ctx, item, mapping);
		}
	}

	private void processInboundMapping(@NotNull VisualizationContext ctx, @NotNull ResourceDataItem sourceItem, @NotNull MappingType mapping) {
		LOGGER.info("Processing inbound mapping: {} for {}", mapping, sourceItem);
		List<DataItem> sources = new ArrayList<>();
		for (MappingSourceDeclarationType sourceDecl : mapping.getSource()) {
			LOGGER.debug(" - src: {}", sourceDecl.getPath());
			DataItem explicitSourceItem = resolveSourceItem(ctx, sourceItem, mapping, sourceDecl, null);
			sources.add(explicitSourceItem);
		}
		if (!sources.contains(sourceItem)) {
			sources.add(sourceItem);
		}
		DataItem targetItem = null;
		MappingTargetDeclarationType targetDecl = mapping.getTarget();
		if (mapping.getTarget() != null) {
			LOGGER.debug(" - target: {}", targetDecl.getPath());
			targetItem = resolveTargetItem(ctx, sourceItem, mapping, targetDecl, ExpressionConstants.VAR_FOCUS);
		}
		ctx.registerMappingRelation(sources, targetItem, mapping);
	}

	// for outbound (but sometimes also inbound) mappings
	@NotNull
	private DataItem resolveSourceItem(@NotNull VisualizationContext ctx, @NotNull ResourceDataItem currentItem,
			@NotNull MappingType mapping, @NotNull MappingSourceDeclarationType sourceDecl, @Nullable QName defaultVariable) {
		// todo from the description
		ItemPath path = sourceDecl.getPath().getItemPath();
		if (!(path.first() instanceof NameItemPathSegment)) {
			LOGGER.warn("Probably incorrect path ({}) - does not start with a name - skipping", path);
			return createAdHocDataItem(ctx, path);
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
				return createAdHocDataItem(ctx, path);
			}
			varName = defaultVariable;
			itemPath = path;
		}

		if (QNameUtil.match(ExpressionConstants.VAR_ACCOUNT, varName)) {
			return resolveResourceItem(ctx, currentItem, itemPath);
		} else if (QNameUtil.match(ExpressionConstants.VAR_USER, varName)) {
			return ctx.resolveRepositoryItem(UserType.class, itemPath);
		} else if (QNameUtil.match(ExpressionConstants.VAR_ACTOR, varName)) {
			return ctx.resolveRepositoryItem(UserType.class, itemPath);			// TODO
		} else if (QNameUtil.match(ExpressionConstants.VAR_FOCUS, varName)) {
			Class<? extends ObjectType> guessedClass = guessFocusClass(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent());
			DataItem item = ctx.resolveRepositoryItem(guessedClass, itemPath);
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
		return createAdHocDataItem(ctx, path);
	}

	private DataItem createAdHocDataItem(VisualizationContext ctx, ItemPath path) {
		return new AdHocDataItem(path);
	}

	// currently for inbounds only
	@NotNull
	private DataItem resolveTargetItem(@NotNull VisualizationContext ctx, @NotNull ResourceDataItem currentItem,
			@NotNull MappingType mapping, @NotNull MappingTargetDeclarationType targetDecl, @Nullable QName defaultVariable) {
		// todo from the description
		ItemPath path = targetDecl.getPath().getItemPath();
		if (!(path.first() instanceof NameItemPathSegment)) {
			LOGGER.warn("Probably incorrect path ({}) - does not start with a name - skipping", path);
			return createAdHocDataItem(ctx, path);
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
				return createAdHocDataItem(ctx, path);
			}
			varName = defaultVariable;
			itemPath = path;
		}

		if (QNameUtil.match(ExpressionConstants.VAR_ACCOUNT, varName)) {
			return resolveResourceItem(ctx, currentItem, itemPath);				// does make sense?
		} else if (QNameUtil.match(ExpressionConstants.VAR_USER, varName)) {
			return ctx.resolveRepositoryItem(UserType.class, itemPath);
		} else if (QNameUtil.match(ExpressionConstants.VAR_ACTOR, varName)) {
			return ctx.resolveRepositoryItem(UserType.class, itemPath);			// TODO
		} else if (QNameUtil.match(ExpressionConstants.VAR_FOCUS, varName)) {
			Class<? extends ObjectType> guessedClass = guessFocusClass(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent());
			DataItem item = ctx.resolveRepositoryItem(guessedClass, itemPath);
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
		return createAdHocDataItem(ctx, path);
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

	private ResourceDataItem resolveResourceItem(VisualizationContext ctx, ResourceDataItem currentItem, ItemPath path) {
		return ctx.findResourceItem(currentItem.getResourceOid(), currentItem.getKind(), currentItem.getIntent(), path);
	}

	private void processOutboundMapping(@NotNull VisualizationContext ctx, @NotNull ResourceDataItem targetItem, @NotNull MappingType mapping) {
		LOGGER.info("Processing outbound mapping: {} for {}", mapping, targetItem);
		List<DataItem> sources = new ArrayList<>();
		for (MappingSourceDeclarationType sourceDecl : mapping.getSource()) {
			LOGGER.info(" - src: {}", sourceDecl.getPath());
			DataItem sourceItem = resolveSourceItem(ctx, targetItem, mapping, sourceDecl, ExpressionConstants.VAR_FOCUS);
			sources.add(sourceItem);
		}
		MappingTargetDeclarationType targetDecl = mapping.getTarget();
		if (targetDecl != null) {
			LOGGER.warn(" - ignoring target (mapping is outbound): {}; using {} instead", targetDecl.getPath(), targetItem);
		}
		ctx.registerMappingRelation(sources, targetItem, mapping);
	}

}
