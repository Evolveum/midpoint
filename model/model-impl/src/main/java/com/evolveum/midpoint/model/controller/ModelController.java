package com.evolveum.midpoint.model.controller;

import java.util.List;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

public interface ModelController extends ModelService {

	String CLASS_NAME = ModelController.class.getName() + ".";	
	String SEARCH_OBJECTS_IN_PROVISIONING = CLASS_NAME + "searchObjectsInProvisioning";
	String MODIFY_OBJECT_WITH_EXCLUSION = CLASS_NAME + "modifyObjectWithExclusion";

	<T extends ObjectType> List<T> searchObjectsInProvisioning(Class<T> type, QueryType query, PagingType paging, OperationResult result);
	
	<T extends ObjectType> List<T> searchObjectsInRepository(Class<T> type, QueryType query, PagingType paging, OperationResult result);

	void modifyObjectWithExclusion(ObjectModificationType change, String accountOid, OperationResult result)
			throws ObjectNotFoundException;

	<T extends ObjectType> T getObject(Class<T> clazz, String oid, PropertyReferenceListType resolve, OperationResult result,
			boolean fromProvisioning) throws ObjectNotFoundException;
	
	/**
	 * TODO: Why this method?
	 */
	String addUser(UserType user, UserTemplateType userTemplate, OperationResult result)
			throws ObjectAlreadyExistsException, ObjectNotFoundException;

}