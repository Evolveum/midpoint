/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.refinery;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;

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
    		String resourceOid, String intent, PropertyPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ShadowDiscriminatorObjectDelta<O> objectDelta = new ShadowDiscriminatorObjectDelta<O>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setDiscriminator(new ResourceShadowDiscriminator(resourceOid, intent));
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
