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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.web.repo;

import java.util.List;

import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author lazyman
 * 
 */
public interface RepositoryManager {

	String CLASS_NAME = RepositoryManager.class.getName();
	String LIST_OBJECTS = CLASS_NAME + "listObjects";
	String SEARCH_OBJECTS = CLASS_NAME + "searchObjects";
	String GET_OBJECT = CLASS_NAME + "getObject";
	String SAVE_OBJECT = CLASS_NAME + "saveObject";
	String DELETE_OBJECT = CLASS_NAME + "deleteObject";
	String ADD_OBJECT = CLASS_NAME + "addObject";

	<T extends ObjectType> List<T>  listObjects(Class<T> objectType, int offset, int count);

	List<? extends ObjectType> searchObjects(String name);

	ObjectType getObject(String oid);

	boolean saveObject(ObjectType object);

	boolean deleteObject(String oid);

	String addObject(ObjectType object) throws ObjectAlreadyExistsException;
}
