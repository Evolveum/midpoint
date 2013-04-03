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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;

public class ObjectQueryUtil {

	
	public static ObjectQuery createResourceAndAccountQuery(String resourceOid, QName objectClass, PrismContext prismContext) throws SchemaException {
		Validate.notNull(resourceOid, "Resource where to search must not be null.");
		Validate.notNull(objectClass, "Object class to search must not be null.");
		Validate.notNull(prismContext, "Prism context must not be null.");
		AndFilter and = AndFilter.createAnd(
				RefFilter.createReferenceEqual(ResourceObjectShadowType.class, ResourceObjectShadowType.F_RESOURCE_REF, prismContext, resourceOid), 
				EqualsFilter.createEqual(
						ResourceObjectShadowType.class, prismContext, ResourceObjectShadowType.F_OBJECT_CLASS, objectClass));
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
