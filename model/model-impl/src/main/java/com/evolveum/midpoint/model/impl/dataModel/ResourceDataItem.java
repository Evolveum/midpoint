package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class ResourceDataItem extends DataItem {

	@NotNull private final VisualizationContext ctx;
	@NotNull private final String resourceOid;
	@NotNull private final ShadowKindType kind;
	@NotNull private final String intent;				// TODO or more intents?
	@NotNull private final QName itemName;

	private RefinedResourceSchema refinedResourceSchema;
	private RefinedObjectClassDefinition refinedObjectClassDefinition;
	private RefinedAttributeDefinition<?> refinedAttributeDefinition;

	public ResourceDataItem(@NotNull VisualizationContext ctx, @NotNull String resourceOid, @NotNull ShadowKindType kind, @NotNull String intent, @NotNull QName itemName) {
		this.ctx = ctx;
		this.resourceOid = resourceOid;
		this.kind = kind;
		this.intent = intent;
		this.itemName = itemName;
	}

	@NotNull
	public String getResourceOid() {
		return resourceOid;
	}

	@NotNull
	public ShadowKindType getKind() {
		return kind;
	}

	@NotNull
	public String getIntent() {
		return intent;
	}

	@NotNull
	public QName getItemName() {
		return itemName;
	}

	public RefinedResourceSchema getRefinedResourceSchema() {
		if (refinedResourceSchema == null) {
			refinedResourceSchema = ctx.getRefinedResourceSchema(resourceOid);
		}
		return refinedResourceSchema;
	}

	public void setRefinedResourceSchema(RefinedResourceSchema refinedResourceSchema) {
		this.refinedResourceSchema = refinedResourceSchema;
	}

	public void setRefinedObjectClassDefinition(RefinedObjectClassDefinition refinedObjectClassDefinition) {
		this.refinedObjectClassDefinition = refinedObjectClassDefinition;
	}

	public RefinedObjectClassDefinition getRefinedObjectClassDefinition() {
		if (refinedObjectClassDefinition == null) {
			RefinedResourceSchema schema = getRefinedResourceSchema();
			if (schema != null) {
				refinedObjectClassDefinition = schema.getRefinedDefinition(kind, intent);
			}
		}
		return refinedObjectClassDefinition;
	}

	public RefinedAttributeDefinition<?> getRefinedAttributeDefinition() {
		if (refinedAttributeDefinition == null) {
			RefinedObjectClassDefinition def = getRefinedObjectClassDefinition();
			if (def != null) {
				refinedAttributeDefinition = def.findAttributeDefinition(itemName);
			}
		}
		return refinedAttributeDefinition;
	}

	public void setRefinedAttributeDefinition(RefinedAttributeDefinition<?> refinedAttributeDefinition) {
		this.refinedAttributeDefinition = refinedAttributeDefinition;
	}

	@Override
	public String toString() {
		return "ResourceDataItem{" +
				"resourceOid='" + resourceOid + '\'' +
				", kind=" + kind +
				", intent='" + intent + '\'' +
				", name=" + itemName +
				'}';
	}

	public boolean matches(String resourceOid, ShadowKindType kind, String intent, QName name) {
		if (!this.resourceOid.equals(resourceOid)) {
			return false;
		}
		if (this.kind != kind) {
			return false;
		}
		if (!ObjectUtils.equals(this.intent, intent)) {
			return false;
		}
		return QNameUtil.match(this.itemName, name);
	}

	@Override
	public String getNodeName() {
		return "\"" + getResourceName() + ":" + ctx.getObjectTypeName(getRefinedObjectClassDefinition()) + ":" + itemName.getLocalPart() + "\"";
	}

	@Override
	public String getNodeLabel() {
		return "TODO";
	}

	@Override
	public String getNodeStyleAttributes() {
		return "TODO";
	}

	@NotNull
	public String getResourceName() {
		PolyString name = ctx.getResource(resourceOid).getName();
		return name != null ? name.getOrig() : resourceOid;
	}
}
