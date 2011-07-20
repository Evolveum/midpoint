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
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.web.model.UserManager2;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.UserDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 *
 */
public class UserManagerImpl2 extends ObjectManagerImpl2<UserType, UserDto> implements UserManager2 {

	private static final long serialVersionUID = -3457278299468312767L;

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return UserType.class;
	}

	@Override
	protected UserDto createObject(UserType objectType) {
		return new UserDto(null);
	}

	@Override
	public Collection<UserDto> list(PagingType paging) {
		return list(paging, ObjectTypes.USER);
	}

	@Override
	public Set<PropertyChange> submit(UserDto changedObject) throws WebModelException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AccountShadowDto addAccount(UserDto userDto, String resourceOid) throws WebModelException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserDto> search(QueryType search, PagingType paging, OperationResult result)
			throws WebModelException {
		// TODO Auto-generated method stub
		return null;
	}
}
