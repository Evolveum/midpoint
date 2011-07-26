package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.w3c.dom.Document;

import net.sf.saxon.s9api.QName;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.model.SystemManager;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.SystemConfigurationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

public class SystemManagerImpl extends ObjectManagerImpl<SystemConfigurationType, SystemConfigurationDto>
		implements SystemManager {

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

	@Override
	public boolean updateLoggingConfiguration(LoggingConfigurationType configuration) {
		boolean updated =false;
		OperationResult result = new OperationResult(UPDATE_LOGGING_CONFIGURATION);
		try {
			String xml = JAXBUtil.marshalWrap(configuration, SchemaConstants.LOGGING);
			Document document = DOMUtil.parseDocument(xml);
			ObjectModificationType change = ObjectTypeUtil.createModificationReplaceProperty(
					SystemObjectsType.SYSTEM_CONFIGURATION.value(), SchemaConstants.LOGGING,
					document.getDocumentElement());

			getModel().modifyObject(change, result);
			updated = true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return updated;
	}
}
