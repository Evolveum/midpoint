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
package com.evolveum.midpoint.common.object;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * The callback from some of the object utilities to resolve objects.
 * 
 * The classes implementing this will most likely fetch the objects from the
 * repository or from some kind of object cache.
 * 
 * This is EXPERIMENTAL feature. Let's see if it can simplify the system or
 * whether it only complicates things ...
 * 
 * @author Radovan Semancik
 */
public interface ObjectResolver {
	
	public ObjectType resolve(String oid);
	
}
