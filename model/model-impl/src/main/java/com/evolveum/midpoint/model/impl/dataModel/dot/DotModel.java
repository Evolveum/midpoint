/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.model.impl.dataModel.DataModel;
import com.evolveum.midpoint.model.impl.dataModel.DataModelVisualizerImpl;
import com.evolveum.midpoint.model.impl.dataModel.model.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

public class DotModel {

    private static final Trace LOGGER = TraceManager.getTrace(DotModel.class);

    public static final String LF = "&#10;";

    private final DataModel dataModel;

    @NotNull private final Map<DataItem, DotDataItem> dataItemsMap = new HashMap<>();
    @NotNull private final Map<Relation, DotRelation> relationsMap = new HashMap<>();

    public DotModel(DataModel dataModel) {
        this.dataModel = dataModel;
        for (DataItem dataItem : dataModel.getDataItems()) {
            DotDataItem ddi;
            if (dataItem instanceof RepositoryDataItem) {
                ddi = new DotRepositoryDataItem((RepositoryDataItem) dataItem);
            } else if (dataItem instanceof ResourceDataItem) {
                ddi = new DotResourceDataItem((ResourceDataItem) dataItem, this);
            } else if (dataItem instanceof AdHocDataItem) {
                ddi = new DotAdHocDataItem((AdHocDataItem) dataItem);
            } else {
                throw new AssertionError("Wrong data item: " + dataItem);
            }
            dataItemsMap.put(dataItem, ddi);
        }
        for (Relation relation : dataModel.getRelations()) {
            DotRelation dr;
            if (relation instanceof MappingRelation) {
                dr = new DotMappingRelation((MappingRelation) relation);
            } else {
                dr = new DotOtherRelation(relation);
            }
            relationsMap.put(relation, dr);
        }
    }

    private boolean subgraphsForResources = false;
    private boolean showUnusedItems = false;

    public String exportDot() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph G {\n");

        Set<DataItem> itemsShown = new HashSet<>();

        int clusterNumber = 1;
        int indent = 1;
        for (PrismObject<ResourceType> resource : dataModel.getResources().values()) {
            if (subgraphsForResources) {
                sb.append(indent(indent)).append("subgraph cluster_").append(clusterNumber++).append(" {\n");
                sb.append(indent(indent+1)).append("label=\"").append(resource.getName()).append("\";\n");
                // TODO style for resource label
                indent++;
            }
            ResourceSchema schema = dataModel.getRefinedResourceSchema(resource.getOid());

            for (ResourceObjectTypeDefinition def : schema.getObjectTypeDefinitions()) {
                StringBuilder sb1 = new StringBuilder();

                sb1.append(indent(indent)).append("subgraph cluster_").append(clusterNumber++).append(" {\n");
                String typeName = "";
                if (!subgraphsForResources && dataModel.getResources().size() > 1) {
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
                    ResourceDataItem item = dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), getObjectClassName(def), attrDef.getItemName());
                    previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName, item);
                }
                for (ResourceAssociationDefinition assocDef : def.getAssociationDefinitions()) {
                    if (assocDef.isIgnored()) {
                        continue;
                    }
                    ResourceDataItem item = dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(),
                            getObjectClassName(def), assocDef.getName());
                    previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName, item);
                }
                previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
                        dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), getObjectClassName(def),
                                ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS)));
                previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
                        dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), getObjectClassName(def),
                                ItemPath.create(ShadowType.F_ACTIVATION, DataModelVisualizerImpl.ACTIVATION_EXISTENCE)));
                previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
                        dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), getObjectClassName(def),
                                ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_VALID_FROM)));
                previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
                        dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), getObjectClassName(def),
                                ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_VALID_TO)));
                previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
                        dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), getObjectClassName(def),
                                ItemPath.create(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS)));
                previousNodeName = addResourceItem(itemsShown, indent, sb1, previousNodeName,
                        dataModel.findResourceItem(resource.getOid(), def.getKind(), def.getIntent(), getObjectClassName(def),
                                ItemPath.create(ShadowType.F_CREDENTIALS, CredentialsType.F_PASSWORD)));

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
        for (Relation relation : dataModel.getRelations()) {
            showNodesIfNeeded(sb, indent, itemsShown, relation.getSources());
            showNodeIfNeeded(sb, indent, itemsShown, relation.getTarget());
            DotRelation dotRelation = getDotRelation(relation);
            if (relation.getSources().size() == 1 && relation.getTarget() != null) {
                DataItem sourceItem = relation.getSources().get(0);
                DataItem targetItem = relation.getTarget();
                DotDataItem dotSourceItem = getDotDataItem(sourceItem);
                DotDataItem dotTargetItem = getDotDataItem(targetItem);
                sb.append(indent(indent)).append(dotSourceItem.getNodeName());
                sb.append(" -> ").append(dotTargetItem.getNodeName());
                sb.append(" [label=\"").append(dotRelation.getEdgeLabel()).append("\"");
                sb.append(", style=").append(dotRelation.getEdgeStyle());
                sb.append(", tooltip=\"").append(dotRelation.getEdgeTooltip()).append("\"");
                sb.append(", labeltooltip=\"").append(dotRelation.getEdgeTooltip()).append("\"");
                sb.append("];").append("\n");
            } else {
                String mappingName = "m" + (mappingNode++);
                String nodeLabel = dotRelation.getNodeLabel(mappingName);
                if (nodeLabel != null) {
                    sb.append(indent(indent)).append(mappingName).append(" [label=\"").append(nodeLabel).append("\"");
                    String styles = dotRelation.getNodeStyleAttributes();
                    if (StringUtils.isNotEmpty(styles)) {
                        sb.append(", ").append(styles);
                    }
                    sb.append(", tooltip=\"").append(dotRelation.getNodeTooltip()).append("\"");
                    sb.append("];\n");
                }
                for (DataItem src : relation.getSources()) {
                    DotDataItem dotSrc = getDotDataItem(src);
                    sb.append(indent(indent)).append(dotSrc.getNodeName()).append(" -> ").append(mappingName)
                            .append(" [style=").append(dotRelation.getEdgeStyle()).append("]\n");
                }
                if (relation.getTarget() != null) {
                    DotDataItem dotTarget = getDotDataItem(relation.getTarget());
                    sb.append(indent(indent)).append(mappingName).append(" -> ").append(dotTarget.getNodeName())
                            .append(" [style=").append(dotRelation.getEdgeStyle()).append("]\n");
                }
            }
        }

        sb.append("}");

        String dot = sb.toString();
        LOGGER.debug("Resulting DOT:\n{}", dot);
        return dot;
    }

    private QName getObjectClassName(ResourceObjectTypeDefinition def) {
        return def != null ? def.getTypeName() : null;
    }

    private String addResourceItem(Set<DataItem> itemsShown, int indent, StringBuilder sb1, String previousNodeName, ResourceDataItem item) {
        if (showUnusedItems || isUsed(item)) {
            showNodeIfNeeded(sb1, indent, itemsShown, item);
            String nodeName = getDotDataItem(item).getNodeName();
            addHiddenEdge(sb1, indent, previousNodeName, nodeName);
            previousNodeName = nodeName;
        }
        return previousNodeName;
    }

    private void showNodesIfNeeded(StringBuilder sb, int indent, Set<DataItem> itemsShown, List<DataItem> items) {
        for (DataItem item : items) {
            if (item != null) {
                showNodeIfNeeded(sb, indent, itemsShown, item);
            } else {
                // a warning was probably already issued
            }
        }
    }

    private void showNodeIfNeeded(StringBuilder sb, int indent, Set<DataItem> itemsShown, DataItem item) {
        if (itemsShown.add(item)) {
            DotDataItem dotDataItem = getDotDataItem(item);
            sb.append(indent(indent)).append(dotDataItem.getNodeName()).append(" [");
            sb.append("label=\"").append(dotDataItem.getNodeLabel()).append("\" ").append(dotDataItem.getNodeStyleAttributes());
            sb.append("]\n");
        }
    }

    private boolean isUsed(DataItem item) {
        for (Relation relation : dataModel.getRelations()) {
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
    public String getObjectTypeName(ResourceObjectDefinition definition, boolean formatted) {
        if (definition == null) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        if (definition.getDisplayName() != null) {
            sb.append(definition.getDisplayName());
            sb.append(formatted ? LF : "/");
        }
        var typeDefinition = definition.getTypeDefinition();
        if (typeDefinition != null) {
            sb.append(typeDefinition.getKind());
            sb.append(formatted ? LF : "/");
            sb.append(typeDefinition.getIntent());
            sb.append(formatted ? LF : "/");
        } else {
            sb.append(definition.getObjectClassName().getLocalPart());
            sb.append(formatted ? LF : "/");
        }
        sb.append("(");
        sb.append(definition.getObjectClassName().getLocalPart());
        sb.append(")");
        return sb.toString();
    }

    public String indent(int i) {
        return StringUtils.repeat("  ", i);
    }

    public DataModel getDataModel() {
        return dataModel;
    }

    public DotRelation getDotRelation(Relation relation) {
        DotRelation rv = relationsMap.get(relation);
        if (rv != null) {
            return rv;
        } else {
            throw new IllegalStateException("No dot relation for " + relation);
        }
    }

    public DotDataItem getDotDataItem(DataItem dataItem) {
        DotDataItem rv = dataItemsMap.get(dataItem);
        if (rv != null) {
            return rv;
        } else {
            throw new IllegalStateException("No dot data item for " + dataItem);
        }
    }
}
