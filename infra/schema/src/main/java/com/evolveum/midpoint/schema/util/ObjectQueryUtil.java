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

package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

public class ObjectQueryUtil {

	
	public static ObjectQuery createResourceAndAccountQuery(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(objectClass, "Object class to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		AndFilter and = AndFilter.createAnd(
				RefFilter.createReferenceEqual(ShadowType.class, ShadowType.F_RESOURCE_REF, prismContext, resourceOid), 
				EqualsFilter.createEqual(
						ShadowType.class, prismContext, ShadowType.F_OBJECT_CLASS, objectClass));
		return ObjectQuery.createObjectQuery(and);
	}
	
	public static <T extends ObjectType> ObjectQuery createNameQuery(Class<T> clazz, PrismContext prismContext, String name) throws SchemaException{
		PolyString namePolyString = new PolyString(name);
		namePolyString.recompute(prismContext.getDefaultPolyStringNormalizer());
		EqualsFilter equal = EqualsFilter.createEqual(clazz, prismContext, ObjectType.F_NAME, namePolyString);
		return ObjectQuery.createObjectQuery(equal);
	}
	
	public static ObjectQuery createRootOrgQuery(PrismContext prismContext) throws SchemaException {
		ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createRootOrg());
		return objectQuery;
	}
}
