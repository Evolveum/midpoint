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
package com.evolveum.midpoint.web.model.impl;

import java.util.Collection;
import java.util.Set;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.model.dto.ObjectDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author lazyman
 * 
 */
public class BasicObjectManagerImpl extends ObjectManagerImpl<ObjectType, ObjectDto<ObjectType>> {

	private static final long serialVersionUID = 3586749968888686380L;

	@Override
	public Collection<ObjectDto<ObjectType>> list(PagingType paging) {
		return list(paging, ObjectTypes.OBJECT);
	}

	@Override
	public Set<PropertyChange> submit(ObjectDto<ObjectType> changedObject, Task task, OperationResult parentResult) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return ObjectType.class;
	}

	@Override
	protected ObjectDto<ObjectType> createObject(ObjectType objectType) {
		return new ObjectDto<ObjectType>(objectType);
	}
}
