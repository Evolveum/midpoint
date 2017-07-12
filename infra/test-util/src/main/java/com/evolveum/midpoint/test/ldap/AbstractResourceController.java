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
package com.evolveum.midpoint.test.ldap;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
	
	public ResourceType getResourceType() {
		return resource.asObjectable();
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
