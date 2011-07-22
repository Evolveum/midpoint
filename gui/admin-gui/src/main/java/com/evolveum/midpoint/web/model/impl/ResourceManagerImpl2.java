package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.model.ResourceManager2;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskStatusType;

public class ResourceManagerImpl2 extends ObjectManagerImpl2<ResourceType, ResourceDto> implements
		ResourceManager2 {

	private static final long serialVersionUID = -4183063295869675058L;
	private static final Trace LOGGER = TraceManager.getTrace(ResourceManagerImpl2.class);

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return ResourceType.class;
	}

	@Override
	protected ResourceDto createObject(ResourceType objectType) {
		return new ResourceDto(objectType);
	}

	@Override
	public Set<PropertyChange> submit(ResourceDto changedObject) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<ResourceDto> list(PagingType paging) {
		return list(paging, ObjectTypes.RESOURCE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<ConnectorDto> listConnectors() {
		Collection<ConnectorType> connectors = (Collection<ConnectorType>) list(
				PagingTypeFactory.createListAllPaging(), ObjectTypes.CONNECTOR.getClassDefinition());

		Collection<ConnectorDto> list = new ArrayList<ConnectorDto>();
		for (ConnectorType connector : connectors) {
			list.add(new ConnectorDto(connector));
		}

		return list;
	}

	@Override
	public <T extends ResourceObjectShadowType> List<ResourceObjectShadowDto<T>> listObjectShadows(
			String oid, Class<T> resourceObjectShadowType) {
		Validate.notNull(oid, "Oid must not be null.");
		Validate.notNull(resourceObjectShadowType, "Resource object shadow type class must not be null.");

		OperationResult result = new OperationResult(LIST_OBJECT_SHADOWS);
		List<ResourceObjectShadowDto<T>> resourceObjectShadowDtoList = new ArrayList<ResourceObjectShadowDto<T>>();
		try {
			List<T> list = getModel().listResourceObjectShadows(oid, resourceObjectShadowType, result);
			for (ResourceObjectShadowType object : list) {
				ResourceObjectShadowDto<T> dto = new ResourceObjectShadowDto<T>(object);
				resourceObjectShadowDtoList.add(dto);
			}
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list object shadows for oid {}, type {}", ex,
					new Object[] { oid, resourceObjectShadowType });
			result.recordFatalError("Couldn't list object shadows for oid '" + oid + "', type '"
					+ resourceObjectShadowType.getClass() + "'.", ex);
		}

		printResults(LOGGER, result);

		return resourceObjectShadowDtoList;
	}

	@Override
	public OperationResult testConnection(String resourceOid) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		LOGGER.debug("Testing resource with oid {}.", new Object[] { resourceOid });

		OperationResult result = null;
		try {
			result = getModel().testResource(resourceOid);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't test resource {}", ex, resourceOid);
			result.recordFatalError("Couldn't test resource '" + resourceOid + "'.", ex);
		}

		if (result == null) {
			result = new OperationResult(ResourceManager2.TEST_CONNECTION);
		}

		printResults(LOGGER, result);
		return result;
	}

	@Override
	public void launchImportFromResource(String resourceOid, QName objectClass) {
		// TODO Auto-generated method stub

	}

	@Override
	public TaskStatusType getImportStatus(String resourceOid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> listResourceObjects(
			String resourceOid, QName objectClass, PagingType paging) {
		// TODO Auto-generated method stub
		return null;
	}
}
