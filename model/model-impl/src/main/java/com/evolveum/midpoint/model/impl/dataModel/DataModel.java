package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.impl.dataModel.model.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author mederly
 */
public class DataModel {

	private static final Trace LOGGER = TraceManager.getTrace(DataModel.class);

	@NotNull private final PrismContext prismContext;
	@NotNull private final Map<String,PrismObject<ResourceType>> resources = new HashMap<>();
	@NotNull private final Set<DataItem> dataItems = new HashSet<>();
	@NotNull private final List<Relation> relations = new ArrayList<>();

	public DataModel(@NotNull PrismContext prismContext) {
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

	@NotNull
	public Map<String, PrismObject<ResourceType>> getResources() {
		return resources;
	}

	@NotNull
	public PrismObject<ResourceType> getResource(@NotNull String resourceOid) {
		return resources.get(resourceOid);
	}


}
