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

import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;

/**
 * 
 * @author lazyman
 * 
 */
public class ConnectorTypeManager extends ObjectManagerImpl<ConnectorDto> {

	private static final long serialVersionUID = 2332102491422179112L;

	@Override
	public String add(ConnectorDto newObject) throws WebModelException {
		throw new UnsupportedOperationException("Not supported for this type.");
	}

	@Override
	public Set<PropertyChange> submit(ConnectorDto changedObject) throws WebModelException {
		throw new UnsupportedOperationException("Not supported for this type.");
	}

	@Override
	public void delete(String oid) throws WebModelException {
		throw new UnsupportedOperationException("Not supported for this type.");
	}

	@Override
	public ConnectorDto create() {
		return new ConnectorDto();
	}

	@Override
	public Collection<ConnectorDto> list(PagingType paging) {
		return list(paging, ObjectTypes.CONNECTOR);
	}
}
