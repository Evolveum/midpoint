/*
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
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * The callback from some of the object utilities to resolve objects.
 * 
 * The classes implementing this will most likely fetch the objects from the
 * repository or from some kind of object cache.
 * 
 * @author Radovan Semancik
 */
public interface ObjectResolver {
	
	/**
	 * Resolve the provided reference to object (ObjectType).
	 * 
	 * Note: The reference is used instead of just OID because the reference
	 * also contains object type. This speeds up the repository operations.
	 * 
	 * @param ref object reference to resolve
	 * @param contextDescription short description of the context of resolution, e.g. "executing expression FOO". Used in error messages.
	 * @return resolved object
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws SchemaException
	 *             error dealing with storage schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 */
	public ObjectType resolve(ObjectReferenceType ref, String contextDescription, OperationResult result) throws ObjectNotFoundException, SchemaException;
	
}
