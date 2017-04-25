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

package com.evolveum.midpoint.model.impl.dataModel.model;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.dataModel.DataModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class ResourceDataItem extends DataItem {

	@NotNull private final DataModel ctx;
	@NotNull private final String resourceOid;
	@NotNull private final ShadowKindType kind;
	@NotNull private final String intent;				// TODO or more intents?
	@NotNull private final ItemPath itemPath;
	private final boolean hasItemDefinition;

	private RefinedResourceSchema refinedResourceSchema;
	private RefinedObjectClassDefinition refinedObjectClassDefinition;
	private RefinedAttributeDefinition<?> refinedAttributeDefinition;

	public ResourceDataItem(@NotNull DataModel ctx, @NotNull String resourceOid, @NotNull ShadowKindType kind,
			@NotNull String intent, RefinedResourceSchema refinedResourceSchema,
			RefinedObjectClassDefinition refinedDefinition, @NotNull QName itemName) {
		this.ctx = ctx;
		this.resourceOid = resourceOid;
		this.kind = kind;
		this.intent = intent;
		this.itemPath = new ItemPath(itemName);
		this.hasItemDefinition = true;
		this.refinedResourceSchema = refinedResourceSchema;
		this.refinedObjectClassDefinition = refinedDefinition;
	}

	public ResourceDataItem(@NotNull DataModel ctx, @NotNull String resourceOid, @NotNull ShadowKindType kind,
			@NotNull String intent, RefinedResourceSchema refinedResourceSchema,
			RefinedObjectClassDefinition refinedDefinition, @NotNull ItemPath itemPath) {
		this.ctx = ctx;
		this.resourceOid = resourceOid;
		this.kind = kind;
		this.intent = intent;
		this.itemPath = itemPath;
		if (itemPath.lastNamed() == null) {
			throw new IllegalArgumentException("Wrong itemPath (must end with a named segment): " + itemPath);
		}
		this.hasItemDefinition = itemPath.size() == 1;			// TODO
		this.refinedResourceSchema = refinedResourceSchema;
		this.refinedObjectClassDefinition = refinedDefinition;
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
	public QName getLastItemName() {
		return itemPath.lastNamed().getName();
	}

	@NotNull
	public ItemPath getItemPath() {
		return itemPath;
	}

	public boolean isHasItemDefinition() {
		return hasItemDefinition;
	}

	public RefinedResourceSchema getRefinedResourceSchema() {
		if (refinedResourceSchema == null) {
			refinedResourceSchema = ctx.getRefinedResourceSchema(resourceOid);
		}
		return refinedResourceSchema;
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
			if (def != null && hasItemDefinition) {
				refinedAttributeDefinition = def.findAttributeDefinition(getLastItemName());
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
				", path=" + itemPath +
				'}';
	}

	public QName getObjectClassName() {
		return refinedObjectClassDefinition != null ? refinedObjectClassDefinition.getTypeName() : null;
	}

	public boolean matches(String resourceOid, ShadowKindType kind, String intent, QName objectClassName,
			ItemPath path) {
		return this.resourceOid.equals(resourceOid)
				&& this.kind == kind
				&& ObjectUtils.equals(this.intent, intent)
				&& QNameUtil.match(getObjectClassName(), objectClassName)
				&& this.itemPath.equivalent(path);
	}

}
