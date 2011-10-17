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
package com.evolveum.midpoint.model.controller;

import java.util.Collection;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public interface ModelController extends ModelService {

	// Constants for OperationResult
	String CLASS_NAME_WITH_DOT = ModelController.class.getName() + ".";
	String SEARCH_OBJECTS_IN_REPOSITORY = CLASS_NAME_WITH_DOT + "searchObjectsInRepository";
	String SEARCH_OBJECTS_IN_PROVISIONING = CLASS_NAME_WITH_DOT + "searchObjectsInProvisioning";
	String ADD_OBJECT_WITH_EXCLUSION = CLASS_NAME_WITH_DOT + "addObjectWithExclusion";
	String MODIFY_OBJECT_WITH_EXCLUSION = CLASS_NAME_WITH_DOT + "modifyObjectWithExclusion";
	String CHANGE_ACCOUNT = CLASS_NAME_WITH_DOT + "changeAccount";

	<T extends ObjectType> void modifyObjectWithExclusion(Class<T> type, ObjectModificationType change,
			Collection<String> excludedResourceOids, OperationResult result) throws ObjectNotFoundException;

	/**
	 * TODO: document
	 * 
	 * @param <T>
	 * @param object
	 * @param accountOid
	 * @param parentResult
	 * @return
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotFoundException
	 * @throws SchemaException
	 */
	<T extends ObjectType> String addObjectWithExclusion(T object, String accountOid,
			OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException,
			SchemaException;

	/**
	 * This method is used when we wan't to add user with different user
	 * template than system template (e.g. during synchronization)
	 * 
	 * @param user
	 * @param userTemplate
	 * @param result
	 * @return
	 * @throws ObjectAlreadyExistsException
	 * @throws ObjectNotFoundException
	 */
	String addUser(UserType user, UserTemplateType userTemplate, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException;

}