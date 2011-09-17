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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;

/**
 * 
 * @author lazyman
 *
 */
public class RoleManagerImplMock implements RoleManager {

	@Override
	public Collection<RoleDto> list(PagingType paging) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<RoleDto> list() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RoleDto get(String oid, PropertyReferenceListType resolve) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RoleDto create() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String add(RoleDto newObject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<PropertyChange> submit(RoleDto changedObject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(String oid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
		// TODO Auto-generated method stub
		return null;
	}
}
