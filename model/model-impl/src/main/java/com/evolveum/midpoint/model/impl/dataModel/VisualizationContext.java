package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
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
	public static final String LF = "&#10;";

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
			return RefinedResourceSchemaImpl.getRefinedSchema(resource, prismContext);
		} catch (SchemaException e) {
			throw new SystemException("Unexpected exception: " + e.getMessage(), e);
		}
	}

	public ResourceDataItem findResourceItem(@NotNull String resourceOid, @Nullable ShadowKindType kind, @Nullable String intent, @NotNull ItemPath path) {
		kind = DataModelVisualizerImpl.def(kind);
		intent = DataModelVisualizerImpl.def(intent);
		for (ResourceDataItem item : getResourceDataItems()) {
			if (item.matches(resourceOid, kind, intent, path)) {
				return item;
			}
		}
		LOGGER.warn("Unknown resource data item: resource={}, kind={}, intent={}, path={}", resourceOid, kind, intent, path);
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
		LOGGER.debug("Adding relation: {} -> {}", sources, target);
		MappingRelation relation = new MappingRelation(sources, target, mapping);
		relations.add(relation);
	}

	public String indent(int i) {
		return StringUtils.repeat("  ", i);
	}

	public String exportDot() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph G {\n");

		Set<DataItem> itemsShown = new HashSet<>();

		int clusterNumber = 1;
		int indent = 1;
		for (PrismObject<ResourceType> resource : resources.values()) {
			if (subgraphsForResources) {
				sb.append(indent(indent)).append("subgraph cluster_").append(clusterNumber++).append(" {\n");
				sb.append(indent(indent+1)).append("label=\"").append(resource.getName()).append("\";\n");
				// TODO style for resource label
				indent++;
			}
			RefinedResourceSchema schema = getRefinedResourceSchema(resource.getOid());

			for (RefinedObjectClassDefinition def : schema.getRefinedDefinitions()) {
				StringBuilder sb1 = new StringBuilder();

				sb1.append(indent(indent)).append("subgraph cluster_").append(clusterNumber++).append(" {\n");
				String typeName = "";
				if (!subgraphsForResources && resources.size() > 1) {
					typeName = PolyString.getOrig(resource.getName()) + LF;
				}
				typeName += getObjectTypeName(def, true);
				sb1.append(indent(indent + 1)).append("label=\"").append(typeName).append("\";\n");
				sb1.append(indent(indent + 1)).append("fontname=\"times-bold\";\n\n");
				String previousNodeName = null;
				indent++;
				for (ResourceAttributeDefinition attrDef : def.getAttributeDefinitions()) {
					if (attrDef.isIgnored()) {
						continue;
					}
					ResourceDataItem item = findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(attrDef.getName()));
					previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName, item);
				}
				for (RefinedAssociationDefinition assocDef : def.getAssociationDefinitions()) {
					if (assocDef.isIgnored()) {
						continue;
					}
					ResourceDataItem item = findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(assocDef.getName()));
					previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName, item);
				}
				previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
						findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)));
				previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
						findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(ShadowType.F_ACTIVATION, DataModelVisualizerImpl.ACTIVATION_EXISTENCE)));
				previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
						findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALID_FROM)));
				previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
						findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALID_TO)));
				previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
						findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS)));
				previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
						findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), new ItemPath(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)));

				indent--;
				sb1.append(indent(indent)).append("}\n");
				if (previousNodeName != null) {
					sb.append(sb1.toString());
				}
			}
			if (subgraphsForResources) {
				sb.append(indent(indent)).append("}\n");
				indent--;
			}
		}
		sb.append("\n");

		int mappingNode = 1;
		for (Relation relation : relations) {
			showNodesIfNeeded(sb, indent, itemsShown, relation.getSources());
			showNodeIfNeeded(sb, indent, itemsShown, relation.getTarget());
			if (relation.getSources().size() == 1 && relation.getTarget() != null) {
				sb.append(indent(indent)).append(relation.getSources().get(0).getNodeName());
				sb.append(" -> ").append(relation.getTarget().getNodeName());
				sb.append(" [label=\"").append(relation.getEdgeLabel()).append("\"");
				sb.append(", style=").append(relation.getEdgeStyle());
				sb.append(", tooltip=\"").append(relation.getEdgeTooltip()).append("\"");
				sb.append(", labeltooltip=\"").append(relation.getEdgeTooltip()).append("\"");
				sb.append("];").append("\n");
			} else {
				String mappingName = "m" + (mappingNode++);
				String nodeLabel = relation.getNodeLabel(mappingName);
				if (nodeLabel != null) {
					sb.append(indent(indent)).append(mappingName).append(" [label=\"").append(nodeLabel).append("\"");
					String styles = relation.getNodeStyleAttributes();
					if (StringUtils.isNotEmpty(styles)) {
						sb.append(", ").append(styles);
					}
					sb.append(", tooltip=\"").append(relation.getNodeTooltip()).append("\"");
					sb.append("];\n");
				}
				for (DataItem src : relation.getSources()) {
					sb.append(indent(indent)).append(src.getNodeName()).append(" -> ").append(mappingName)
							.append(" [style=").append(relation.getEdgeStyle()).append("]\n");
				}
				if (relation.getTarget() != null) {
					sb.append(indent(indent)).append(mappingName).append(" -> ").append(relation.getTarget().getNodeName())
							.append(" [style=").append(relation.getEdgeStyle()).append("]\n");
				}
			}
		}

		sb.append("}");

		String dot = sb.toString();
		LOGGER.debug("Resulting DOT:\n{}", dot);
		return dot;
	}

	private String addResourceItem(Set<DataItem> itemsShown, int indent, StringBuilder sb1, String previousNodeName, ResourceDataItem item) {
		if (showUnusedItems || isUsed(item)) {
			showNodeIfNeeded(sb1, indent, itemsShown, item);
			String nodeName = item.getNodeName();
			addHiddenEdge(sb1, indent, previousNodeName, nodeName);
			previousNodeName = nodeName;
		}
		return previousNodeName;
	}

	private void showNodesIfNeeded(StringBuilder sb, int indent, Set<DataItem> itemsShown, List<DataItem> items) {
		for (DataItem item : items) {
			showNodeIfNeeded(sb, indent, itemsShown, item);
		}
	}

	private void showNodeIfNeeded(StringBuilder sb, int indent, Set<DataItem> itemsShown, DataItem item) {
		if (itemsShown.add(item)) {
			sb.append(indent(indent)).append(item.getNodeName()).append(" [");
			sb.append("label=\"").append(item.getNodeLabel()).append("\" ").append(item.getNodeStyleAttributes());
			sb.append("]\n");
		}
	}

	private boolean isUsed(DataItem item) {
		for (Relation relation : relations) {
			if (relation.getSources().contains(item) || relation.getTarget() == item) {
				return true;
			}
		}
		return false;
	}

	private void addHiddenEdge(StringBuilder sb, int indent, String previousNodeName, String nodeName) {
		if (previousNodeName == null) {
			return;
		}
		sb.append(indent(indent)).append(previousNodeName).append(" -> ").append(nodeName).append(" [style=invis];\n");
	}

	@NotNull
	public PrismObject<ResourceType> getResource(@NotNull String resourceOid) {
		return resources.get(resourceOid);
	}

	@NotNull
	String getObjectTypeName(RefinedObjectClassDefinition refinedObjectClassDefinition, boolean formatted) {
		if (refinedObjectClassDefinition == null) {
			return "?";
		}
		StringBuilder sb = new StringBuilder();
		if (refinedObjectClassDefinition.getDisplayName() != null) {
			sb.append(refinedObjectClassDefinition.getDisplayName());
			sb.append(formatted ? LF : "/");
		}
		sb.append(ResourceTypeUtil.fillDefault(refinedObjectClassDefinition.getKind()));
		sb.append(formatted ? LF : "/");
		sb.append(ResourceTypeUtil.fillDefault(refinedObjectClassDefinition.getIntent()));
		sb.append(formatted ? LF : "/");
		sb.append("(");
		sb.append(refinedObjectClassDefinition.getObjectClassDefinition().getTypeName().getLocalPart());
		sb.append(")");
		return sb.toString();
	}


}
