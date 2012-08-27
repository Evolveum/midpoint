package com.evolveum.midpoint.schema.util;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;

public class ObjectQueryUtil {

	
	public static ObjectQuery createResourceAndAccountQuery(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(objectClass, "Object class to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		AndFilter and = AndFilter.createAnd(
				EqualsFilter.createReferenceEqual(AccountShadowType.class, AccountShadowType.F_RESOURCE_REF, prismContext, resourceOid), 
				EqualsFilter.createEqual(
				AccountShadowType.class, prismContext, AccountShadowType.F_OBJECT_CLASS, objectClass));
		return ObjectQuery.createObjectQuery(and);
	}
	
}
