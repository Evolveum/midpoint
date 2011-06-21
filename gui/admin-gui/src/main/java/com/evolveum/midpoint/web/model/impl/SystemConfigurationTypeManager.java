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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.evolveum.midpoint.web.model.SystemConfigurationManager;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.SystemConfigurationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;

public class SystemConfigurationTypeManager extends SystemConfigurationManager {
	
	private static final long serialVersionUID = 7510934216789096238L;

	@Override
	public Collection<SystemConfigurationDto> list(PagingType paging) {
		SystemConfigurationDto config = get("", new PropertyReferenceListType());
		Collection<SystemConfigurationDto> collection = new ArrayList<SystemConfigurationDto>();
		if (config != null) {
			collection.add(config);
		}

		return collection;
	}

	@Override
	public SystemConfigurationDto create() {
		return new SystemConfigurationDto();
	}

	@Override
	public Set<PropertyChange> submit(SystemConfigurationDto changedObject) throws WebModelException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
