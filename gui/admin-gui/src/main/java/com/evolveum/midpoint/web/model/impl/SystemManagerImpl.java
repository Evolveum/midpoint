package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.model.SystemManager;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.SystemConfigurationDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemObjectsType;

public class SystemManagerImpl extends ObjectManagerImpl<SystemConfigurationType, SystemConfigurationDto>
		implements SystemManager {

	private static final long serialVersionUID = 7510934216789096238L;

	@Override
	public Collection<SystemConfigurationDto> list(PagingType paging) {
		SystemConfigurationType config = null;
		try {
			config = get(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
					new PropertyReferenceListType());
		} catch (ObjectNotFoundException ex) {
			// TODO: error handling
			throw new SystemException(ex);
		}

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
		boolean updated = false;
		OperationResult result = new OperationResult(UPDATE_LOGGING_CONFIGURATION);
		try {
			String xml = JAXBUtil.marshalWrap(configuration, SchemaConstants.LOGGING);
			Document document = DOMUtil.parseDocument(xml);

			List<XPathSegment> segments = new ArrayList<XPathSegment>();
			XPathHolder xpath = new XPathHolder(segments);

			ObjectModificationType change = new ObjectModificationType();
			change.setOid(SystemObjectsType.SYSTEM_CONFIGURATION.value());
			change.getPropertyModification().add(
					ObjectTypeUtil.createPropertyModificationType(PropertyModificationTypeType.replace,
							xpath, document.getDocumentElement()));

			getModel().modifyObject(SystemConfigurationType.class, change, result);
			updated = true;
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return updated;
	}
}
