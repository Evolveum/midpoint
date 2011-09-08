package com.evolveum.midpoint.model.controller;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

public interface ModelController extends ModelService {

	// Constants for OperationResult
	String CLASS_NAME = ModelController.class.getName() + ".";
	String SEARCH_OBJECTS_IN_PROVISIONING = CLASS_NAME + "searchObjectsInProvisioning";
	String MODIFY_OBJECT_WITH_EXCLUSION = CLASS_NAME + "modifyObjectWithExclusion";

	<T extends ObjectType> void modifyObjectWithExclusion(Class<T> type, ObjectModificationType change,
			String accountOid, OperationResult result) throws ObjectNotFoundException;

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