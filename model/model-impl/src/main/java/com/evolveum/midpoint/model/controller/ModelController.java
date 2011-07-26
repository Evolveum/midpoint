package com.evolveum.midpoint.model.controller;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;

public interface ModelController extends ModelService {

	String CLASS_NAME = ModelController.class.getName() + ".";
	String ADD_OBJECT = CLASS_NAME + "addObject";
	String GET_OBJECT = CLASS_NAME + "getObject";

	ObjectListType searchObjectsInProvisioning(QueryType query, PagingType paging, OperationResult result);

	void modifyObjectWithExclusion(ObjectModificationType change, String accountOid, OperationResult result)
			throws ObjectNotFoundException;

	<T extends ObjectType> T getObject(String oid, PropertyReferenceListType resolve, OperationResult result,
			Class<T> clazz, boolean fromProvisioning) throws ObjectNotFoundException;
}