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
package com.evolveum.midpoint.common;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.schema.processor.ResourceAttribute;

/**
 * @author semancik
 *
 */
public class ResourceObjectPattern {
	
	private Collection<ResourceAttribute<?>> identifiers;
	
	public Collection<ResourceAttribute<?>> getIdentifiers() {
		if (identifiers == null) {
			identifiers = new ArrayList<ResourceAttribute<?>>();
		}
		return identifiers;
	}
	
	public void addIdentifier(ResourceAttribute<?> identifier) {
		getIdentifiers().add(identifier);
	}

	public static boolean matches(Collection<? extends ResourceAttribute<?>> attributesToMatch,
			Collection<ResourceObjectPattern> protectedAccountPatterns) {
		for (ResourceObjectPattern pattern: protectedAccountPatterns) {
			if (pattern.matches(attributesToMatch)) {
				return true;
			}
		}
		return false;
	}

	public boolean matches(Collection<? extends ResourceAttribute<?>> attributesToMatch) {
		for (ResourceAttribute<?> identifier: identifiers) {
			if (!matches(identifier, attributesToMatch)) {
				return false;
			}
		}
		return true;
	}

	private static boolean matches(ResourceAttribute<?> identifier, Collection<? extends ResourceAttribute<?>> attributesToMatch) {
		for (ResourceAttribute<?> attributeToMatch: attributesToMatch) {
			if (matches(identifier, attributeToMatch)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matches(ResourceAttribute<?> identifier, ResourceAttribute<?> attributeToMatch) {
		return identifier.equalsRealValue(attributeToMatch);
	}

}
