/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author semancik
 *
 */
public class ShadowDiscriminatorObjectDelta<T extends Objectable> extends ObjectDelta<T> {

	private ResourceShadowDiscriminator discriminator;

	public ShadowDiscriminatorObjectDelta(Class<T> objectTypeClass, ChangeType changeType, PrismContext prismContext) {
		super(objectTypeClass, changeType, prismContext);
	}

	public ResourceShadowDiscriminator getDiscriminator() {
		return discriminator;
	}

	public void setDiscriminator(ResourceShadowDiscriminator discriminator) {
		this.discriminator = discriminator;
	}

	@Override
	protected void checkIdentifierConsistence(boolean requireOid) {
		if (requireOid && discriminator.getResourceOid() == null) {
    		throw new IllegalStateException("Null resource oid in delta "+this);
    	}
	}

	/**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method.
     */
    public static <O extends Objectable, X> ShadowDiscriminatorObjectDelta<O> createModificationReplaceProperty(Class<O> type,
    		String resourceOid, ShadowKindType kind, String intent, ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ShadowDiscriminatorObjectDelta<O> objectDelta = new ShadowDiscriminatorObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setDiscriminator(new ResourceShadowDiscriminator(resourceOid, kind, intent));
    	fillInModificationReplaceProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }

	@Override
	protected String debugName() {
		return "ShadowDiscriminatorObjectDelta";
	}

	@Override
	protected String debugIdentifiers() {
		return discriminator == null ? "null" : discriminator.toString();
	}

}
