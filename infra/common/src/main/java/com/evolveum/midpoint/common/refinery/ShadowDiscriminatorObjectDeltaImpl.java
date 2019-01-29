/*
 * Copyright (c) 2010-2019 Evolveum
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
import com.evolveum.midpoint.prism.impl.delta.ObjectDeltaImpl;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;

/**
 * @author semancik
 *
 */
public class ShadowDiscriminatorObjectDeltaImpl<T extends Objectable> extends ObjectDeltaImpl<T> implements ShadowDiscriminatorObjectDelta<T> {
	private static final long serialVersionUID = 1L;

	private ResourceShadowDiscriminator discriminator;

	ShadowDiscriminatorObjectDeltaImpl(Class<T> objectTypeClass, ChangeType changeType, PrismContext prismContext) {
		super(objectTypeClass, changeType, prismContext);
	}

	@Override
	public ResourceShadowDiscriminator getDiscriminator() {
		return discriminator;
	}

	@Override
	public void setDiscriminator(ResourceShadowDiscriminator discriminator) {
		this.discriminator = discriminator;
	}

	@Override
	protected void checkIdentifierConsistence(boolean requireOid) {
		if (requireOid && discriminator.getResourceOid() == null) {
    		throw new IllegalStateException("Null resource oid in delta "+this);
    	}
	}

	@Override
	protected String debugName() {
		return "ShadowDiscriminatorObjectDelta";
	}

	@Override
	protected String debugIdentifiers() {
		return discriminator == null ? "null" : discriminator.toString();
	}
	
	@Override
	public ShadowDiscriminatorObjectDeltaImpl<T> clone() {
		ShadowDiscriminatorObjectDeltaImpl<T> clone = new ShadowDiscriminatorObjectDeltaImpl<>(this.getObjectTypeClass(), this.getChangeType(), this.getPrismContext());
        copyValues(clone);
        return clone;
    }
    
    protected void copyValues(ShadowDiscriminatorObjectDeltaImpl<T> clone) {
		super.copyValues(clone);
        clone.discriminator = this.discriminator;
    }
}
