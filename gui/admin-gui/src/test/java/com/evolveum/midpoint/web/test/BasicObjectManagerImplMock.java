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
package com.evolveum.midpoint.web.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.model.ObjectManager;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

import java.util.Collection;
import java.util.Set;

/**
 * 
 * @author lazyman
 * 
 */
public class BasicObjectManagerImplMock implements ObjectManager<ObjectType> {

	@Override
	public Collection<ObjectType> list(PagingType paging) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public Collection<ObjectType> list() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public ObjectType get(String oid, PropertyReferenceListType resolve) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public ObjectType create() {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public String add(ObjectType newObject) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public Set<PropertyChange> submit(ObjectType changedObject, Task task, OperationResult result) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public void delete(String oid) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
