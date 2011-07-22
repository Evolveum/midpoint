package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.SystemConfigurationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;

public class SystemManagerImpl extends ObjectManagerImpl<SystemConfigurationType, SystemConfigurationDto> {

	private static final long serialVersionUID = 7510934216789096238L;

	@Override
	public Collection<SystemConfigurationDto> list(PagingType paging) {
		SystemConfigurationType config = get(SystemObjectsType.SYSTEM_CONFIGURATION.value(),
				new PropertyReferenceListType(), SystemConfigurationType.class);
		Collection<SystemConfigurationDto> collection = new ArrayList<SystemConfigurationDto>();
		if (config != null) {
			collection.add(createObject(config));
		}

		return collection;
	}

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return SystemConfigurationType.class;
	}

	@Override
	protected SystemConfigurationDto createObject(SystemConfigurationType objectType) {
		return new SystemConfigurationDto(objectType);
	}

	@Override
	public Set<PropertyChange> submit(SystemConfigurationDto changedObject) {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

}
