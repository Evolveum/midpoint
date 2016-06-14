package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author mederly
 */
public class VisualizationContext {

	private static final Trace LOGGER = TraceManager.getTrace(VisualizationContext.class);

	@NotNull private final PrismContext prismContext;
	@NotNull private final Set<DataItem> dataItems = new HashSet<>();
	@NotNull private final Map<String,PrismObject<ResourceType>> resources = new HashMap<>();
	@NotNull private final List<Relation> relations = new ArrayList<>();

	private boolean subgraphsForResources = false;
	private boolean showUnusedItems = false;

	public VisualizationContext(@NotNull PrismContext prismContext) {
		this.prismContext = prismContext;
	}

	@NotNull
	public List<Relation> getRelations() {
		return relations;
	}

	@NotNull
	public Set<DataItem> getDataItems() {
		return dataItems;
	}

	public void registerResource(PrismObject<ResourceType> resource) {
		Validate.notNull(resource.getOid());
		resources.put(resource.getOid(), resource);
	}

	public void registerDataItem(ResourceDataItem item) {
		dataItems.add(item);
	}

	public RefinedResourceSchema getRefinedResourceSchema(String resourceOid) {
		PrismObject resource = resources.get(resourceOid);
		if (resource == null) {
			return null;
		}
		try {
			return RefinedResourceSchema.getRefinedSchema(resource, prismContext);
		} catch (SchemaException e) {
			throw new SystemException("Unexpected exception: " + e.getMessage(), e);
		}
	}

	public ResourceDataItem findResourceItem(@NotNull String resourceOid, @Nullable ShadowKindType kind, @Nullable String intent, @NotNull ItemPath path) {
		kind = DataModelVisualizerImpl.def(kind);
		intent = DataModelVisualizerImpl.def(intent);
		QName name = path.asSingleName();
		if (name == null) {
			LOGGER.warn("Unexpected path to an attribute: {}", path);
			return null;
		}
		for (ResourceDataItem item : getResourceDataItems()) {
			if (item.matches(resourceOid, kind, intent, name)) {
				return item;
			}
		}
		LOGGER.warn("Unknown resource data item: resource={}, kind={}, intent={}, name={}", resourceOid, kind, intent, name);
		return null;
	}

	private List<ResourceDataItem> getResourceDataItems() {
		List<ResourceDataItem> rv = new ArrayList<>();
		for (DataItem item : dataItems) {
			if (item instanceof ResourceDataItem) {
				rv.add((ResourceDataItem) item);
			}
		}
		return rv;
	}

	private List<RepositoryDataItem> getRepositoryDataItems() {
		List<RepositoryDataItem> rv = new ArrayList<>();
		for (DataItem item : dataItems) {
			if (item instanceof RepositoryDataItem) {
				rv.add((RepositoryDataItem) item);
			}
		}
		return rv;
	}

	public RepositoryDataItem resolveRepositoryItem(Class<? extends ObjectType> aClass, ItemPath path) {
		QName typeName = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(aClass).getTypeName();
		for (RepositoryDataItem item : getRepositoryDataItems()) {
			if (item.matches(typeName, path)) {
				return item;
			}
		}
		RepositoryDataItem item = new RepositoryDataItem(typeName, path);
		dataItems.add(item);
		return item;
	}

	public void registerMappingRelation(@NotNull List<DataItem> sources, @Nullable DataItem target, @NotNull MappingType mapping) {
		LOGGER.info("Adding relation: {} -> {}", sources, target);
		MappingRelation relation = new MappingRelation(sources, target, mapping);
		relations.add(relation);
	}

	public String exportDot() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph G {\n");

		int clusterNumber = 1;
		for (PrismObject<ResourceType> resource : resources.values()) {
			if (subgraphsForResources) {
				sb.append("  subgraph cluster_").append(clusterNumber++).append(" {");
				sb.append("    label=\"").append(resource.getName()).append("\";\n");
			}
			RefinedResourceSchema schema = getRefinedResourceSchema(resource.getOid());
			for (RefinedObjectClassDefinition def : schema.getRefinedDefinitions()) {
				StringBuilder sb1 = new StringBuilder();
				sb1.append("    subgraph cluster_").append(clusterNumber++).append(" {");
				String typeName = "";
				if (!subgraphsForResources && resources.size() > 1) {
					typeName = PolyString.getOrig(resource.getName()) + ": ";
				}
				typeName += getObjectTypeName(def);
				sb1.append("      label=\"").append(typeName).append("\";\n");
				String previousNodeName = null;
				for (ResourceAttributeDefinition attrDef : def.getAttributeDefinitions()) {
					if (attrDef.isIgnored()) {
						continue;
					}
					ResourceDataItem item = findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(attrDef.getName()));
					if (showUnusedItems || isUsed(item)) {
						String nodeName = addLabelForItem(sb1, item);
						addHiddenEdge(sb1, previousNodeName, nodeName);
						previousNodeName = nodeName;
					}
				}
				for (RefinedAssociationDefinition assocDef : def.getAssociations()) {
					if (assocDef.isIgnored()) {
						continue;
					}
					ResourceDataItem item = findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(assocDef.getName()));
					if (showUnusedItems || isUsed(item)) {
						String nodeName = addLabelForItem(sb1, item);
						addHiddenEdge(sb1, previousNodeName, nodeName);
						previousNodeName = nodeName;
					}
				}
				sb1.append("    }\n");
				if (previousNodeName != null) {
					sb.append(sb1.toString());
				}
			}
			if (subgraphsForResources) {
				sb.append("  }\n");
			}
		}
		sb.append("\n");

		int mappingNode = 1;
		for (Relation relation : relations) {
			if (relation.getSources().size() == 1 && relation.getTarget() != null) {
				sb.append("  ").append(relation.getSources().get(0).getNodeName());
				sb.append(" -> ").append(relation.getTarget().getNodeName());
				sb.append(" [label=\"").append(relation.getEdgeLabel()).append("\"];").append("\n");
			} else {
				String mappingName = "m" + (mappingNode++);
				String nodeLabel = relation.getNodeLabel(mappingName);
				if (nodeLabel != null) {
					sb.append("  ").append(mappingName).append(" [label=\"").append(nodeLabel).append("\"];\n");
				}
				for (DataItem src : relation.getSources()) {
					sb.append("  ").append(src.getNodeName()).append(" -> ").append(mappingName).append("\n");
				}
				if (relation.getTarget() != null) {
					sb.append("  ").append(mappingName).append(" -> ").append(relation.getTarget().getNodeName()).append("\n");
				}
			}
		}

		sb.append("}");

		return sb.toString();
	}

	private boolean isUsed(DataItem item) {
		for (Relation relation : relations) {
			if (relation.getSources().contains(item) || relation.getTarget() == item) {
				return true;
			}
		}
		return false;
	}

	private void addHiddenEdge(StringBuilder sb, String previousNodeName, String nodeName) {
		if (previousNodeName == null) {
			return;
		}
		sb.append(previousNodeName).append(" -> ").append(nodeName).append(" [style=invis];\n");
	}

	private String addLabelForItem(StringBuilder sb, ResourceDataItem item) {
		String nodeName = item.getNodeName();
		sb.append("      ").append(nodeName).append(" [label=\"").append(item.getItemName().getLocalPart()).append("\"];\n");
		return nodeName;
	}

	@NotNull
	public PrismObject<ResourceType> getResource(@NotNull String resourceOid) {
		return resources.get(resourceOid);
	}

	@NotNull
	String getObjectTypeName(RefinedObjectClassDefinition refinedObjectClassDefinition) {
		if (refinedObjectClassDefinition == null) {
			return "?";
		}
		if (refinedObjectClassDefinition.getDisplayName() != null) {
			return refinedObjectClassDefinition.getDisplayName();
		}
		return refinedObjectClassDefinition.getObjectClassDefinition().getTypeName().getLocalPart() + "/" + refinedObjectClassDefinition.getKind() + "/" + refinedObjectClassDefinition.getIntent();
	}


}
