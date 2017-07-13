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
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
public class ResourceObjectDiscriminator {
	
	private QName objectClass;
	private Collection<? extends ResourceAttribute<?>> primaryIdentifiers;
	
	public ResourceObjectDiscriminator(QName objectClass, Collection<? extends ResourceAttribute<?>> primaryIdentifiers) {
		super();
		this.objectClass = objectClass;
		this.primaryIdentifiers = primaryIdentifiers;
	}
	
	public QName getObjectClass() {
		return objectClass;
	}

	public Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiers() {
		return primaryIdentifiers;
	}
	
	public boolean matches(PrismObject<ShadowType> shadow) {
		ShadowType shadowType = shadow.asObjectable();
		if (!objectClass.equals(shadowType.getObjectClass())) {
			return false;
		}
		Collection<ResourceAttribute<?>> shadowIdentifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
		return PrismProperty.compareCollectionRealValues(primaryIdentifiers, shadowIdentifiers);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((primaryIdentifiers == null) ? 0 : primaryIdentifiers.hashCode());
		result = prime * result + ((objectClass == null) ? 0 : objectClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceObjectDiscriminator other = (ResourceObjectDiscriminator) obj;
		if (primaryIdentifiers == null) {
			if (other.primaryIdentifiers != null)
				return false;
		} else if (!primaryIdentifiers.equals(other.primaryIdentifiers))
			return false;
		if (objectClass == null) {
			if (other.objectClass != null)
				return false;
		} else if (!objectClass.equals(other.objectClass))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ResourceObjectDiscriminator(" + objectClass + ": " + primaryIdentifiers + ")";
	}

}
