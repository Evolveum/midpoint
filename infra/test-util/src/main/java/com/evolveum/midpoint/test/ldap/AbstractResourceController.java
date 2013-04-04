/**
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.test.ldap;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

/**
 * @author semancik
 *
 */
public abstract class AbstractResourceController {
	
	protected PrismObject<ResourceType> resource;
	
	public String getNamespace() {
		return ResourceTypeUtil.getResourceNamespace(resource);
	}

	public PrismObject<ResourceType> getResource() {
		return resource;
	}

	public void setResource(PrismObject<ResourceType> resource) {
		this.resource = resource;
	}

	public QName getAttributeQName(String attributeName) {
		return new QName(getNamespace(), attributeName);
	}
	
	public ItemPath getAttributePath(String attributeName) {
		return new ItemPath(
				ShadowType.F_ATTRIBUTES,
				getAttributeQName(attributeName));
	}

}
