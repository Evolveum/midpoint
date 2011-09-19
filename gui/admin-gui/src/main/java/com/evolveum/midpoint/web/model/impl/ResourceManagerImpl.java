package com.evolveum.midpoint.web.model.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.ConnectorHostDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

public class ResourceManagerImpl extends ObjectManagerImpl<ResourceType, GuiResourceDto> implements
		ResourceManager {

	private static final long serialVersionUID = -4183063295869675058L;
	private static final Trace LOGGER = TraceManager.getTrace(ResourceManagerImpl.class);

	@Override
	protected Class<? extends ObjectType> getSupportedObjectClass() {
		return ResourceType.class;
	}

	@Override
	protected GuiResourceDto createObject(ResourceType objectType) {
		return new GuiResourceDto(objectType);
	}

	@Override
	public Set<PropertyChange> submit(GuiResourceDto changedObject) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<GuiResourceDto> list(PagingType paging) {
		return list(paging, ObjectTypes.RESOURCE);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<ConnectorDto> listConnectors() {
		Collection<ConnectorType> connectors = (Collection<ConnectorType>) list(null,
				ObjectTypes.CONNECTOR.getClassDefinition());

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
			result = new OperationResult(ResourceManager.TEST_CONNECTION);
		}

		printResults(LOGGER, result);
		return result;
	}

	@Override
	public void importFromResource(String resourceOid, QName objectClass) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object class must not be null.");
		LOGGER.debug("Launching import from resource with oid {} and object class {}.", new Object[] {
				resourceOid, objectClass });

		// TODO: correct task setup

		OperationResult result = new OperationResult(ResourceManager.IMPORT_FROM_RESOURCE);
		TaskType taskType = new TaskType();
		taskType.setResult(result.createOperationResultType());

		Task task = null;
		try {
			getModel().importAccountsFromResource(resourceOid, objectClass, task, result);

			result = OperationResult.createOperationResult(taskType.getResult());
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't launch import on resource {} and object class {}",
					ex, resourceOid, objectClass);
			result.recordFatalError(ex);
		}

		printResults(LOGGER, result);
	}

	@Override
	public Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> listResourceObjects(
			String resourceOid, QName objectClass, PagingType paging) {
		Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
		Validate.notNull(objectClass, "Object class must not be null.");
		LOGGER.debug("Listing resource objects from resource with oid {} and object class {}.", new Object[] {
				resourceOid, objectClass });

		OperationResult result = new OperationResult(ResourceManager.LIST_RESOURCE_OBJECTS);
		Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> collection = new ArrayList<ResourceObjectShadowDto<ResourceObjectShadowType>>();
		try {
			ObjectListType list = getModel().listResourceObjects(resourceOid, objectClass, paging, result);
			if (list != null) {
				for (ObjectType objectType : list.getObject()) {
					collection.add(new ResourceObjectShadowDto<ResourceObjectShadowType>(
							(ResourceObjectShadowType) objectType));
				}
			}
			result.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get import status from resource {}", ex, resourceOid);
			result.recordFatalError("Couldn't get import status from resource '" + resourceOid + "'.", ex);
		} finally {
			result.computeStatus("", "");
		}

		printResults(LOGGER, result);

		return collection;
	}

	@Override
	public ConnectorDto getConnector(String oid) {
		ConnectorType connector = null;
		try {
			connector = get(ConnectorType.class, oid, new PropertyReferenceListType());
		} catch (Exception ex) {
			// TODO: error handling
			throw new SystemException(ex);
		}

		if (connector == null) {
			return null;
		}

		return new ConnectorDto(connector);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<ConnectorHostDto> listConnectorHosts() {
		Collection<ConnectorHostDto> collection = new ArrayList<ConnectorHostDto>();

		OperationResult result = new OperationResult(ResourceManager.LIST_CONNECTOR_HOSTS);
		try {
			Collection<ConnectorHostType> connectors = (Collection<ConnectorHostType>) list(null,
					ObjectTypes.CONNECTOR_HOST.getClassDefinition());
			if (connectors != null) {
				for (ConnectorHostType connector : connectors) {
					collection.add(new ConnectorHostDto(connector));
				}
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list connector hosts", ex);
		} finally {
			result.computeStatus("Couldn't list connector hosts.",
					"Some problem occured during connector hosts listing.");
		}

		printResults(LOGGER, result);

		return collection;
	}

	@Override
	public void discoverConnectorsOnHost(ConnectorHostDto connectorHost) {
		Validate.notNull(connectorHost, "Connector host dto must not be null.");

		OperationResult result = new OperationResult(ResourceManager.DISCOVER_CONNECTORS_ON_HOST);
		try {
			getModel().discoverConnectors(connectorHost.getXmlObject(), result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't discover connectors on connector host {}", ex,
					connectorHost.getName());
		} finally {
			result.computeStatus("Couldn't discover connectors on connector host '" + connectorHost.getName()
					+ "'.");
		}

		printResults(LOGGER, result);
	}
}
