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
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;

/**
 * 
 * @author lazyman
 * 
 */
public class RoleManagerImpl extends ObjectManagerImpl<RoleType, RoleDto> implements RoleManager {

	private static final long serialVersionUID = -5937335028068858595L;

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return RoleType.class;
	}

	@Override
	protected RoleDto createObject(RoleType objectType) {
		return new RoleDto(objectType);
	}

	@Override
	public Collection<RoleDto> list(PagingType paging) {
		return list(paging, ObjectTypes.ROLE);
	}

	@Override
	public Set<PropertyChange> submit(RoleDto changedObject) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
