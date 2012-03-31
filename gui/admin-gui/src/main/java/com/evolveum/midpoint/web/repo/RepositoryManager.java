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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author lazyman
 * 
 */
public interface RepositoryManager {

	String CLASS_NAME_WITH_DOT = RepositoryManager.class.getName() + ".";
	String LIST_OBJECTS = CLASS_NAME_WITH_DOT + "listObjects";
	String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + "searchObjects";
	String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";
	String SAVE_OBJECT = CLASS_NAME_WITH_DOT + "saveObject";
	String DELETE_OBJECT = CLASS_NAME_WITH_DOT + "deleteObject";
	String ADD_OBJECT = CLASS_NAME_WITH_DOT + "addObject";

	<T extends ObjectType> List<PrismObject<T>>  listObjects(Class<T> objectType, int offset, int count);

    ResultList<PrismObject<ObjectType>> searchObjects(String name);

    <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid);

	boolean saveObject(PrismObject<?> object, String xml);

	<T extends ObjectType> boolean deleteObject(Class<T> type, String oid);

	String addObject(PrismObject<?> object) throws ObjectAlreadyExistsException;
}
