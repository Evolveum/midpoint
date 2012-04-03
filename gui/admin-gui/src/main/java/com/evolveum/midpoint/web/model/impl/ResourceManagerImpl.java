package com.evolveum.midpoint.web.model.impl;

import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.*;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
    public Set<PropertyChange> submit(GuiResourceDto changedObject, Task task, OperationResult parentResult) {
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
            List<PrismObject<T>> list = getModel().listResourceObjectShadows(oid, resourceObjectShadowType, result);
            for (PrismObject<T> object : list) {
                ResourceObjectShadowDto<T> dto = new ResourceObjectShadowDto<T>(object.asObjectable());
                resourceObjectShadowDtoList.add(dto);
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't list object shadows for oid {}, type {}", ex,
                    new Object[]{oid, resourceObjectShadowType});
            result.recordFatalError("Couldn't list object shadows for oid '" + oid + "', type '"
                    + resourceObjectShadowType.getClass() + "'.", ex);
        }

        ControllerUtil.printResults(LOGGER, result, null);

        return resourceObjectShadowDtoList;
    }

    @Override
    public OperationResult testConnection(String resourceOid) {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        LOGGER.debug("Testing resource with oid {}.", new Object[]{resourceOid});

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

        ControllerUtil.printResults(LOGGER, result, null);
        return result;
    }

    @Override
    public void importFromResource(String resourceOid, QName objectClass) {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(objectClass, "Object class must not be null.");
        LOGGER.debug("Launching import from resource with oid {} and object class {}.", new Object[]{
                resourceOid, objectClass});

        Task task = getTaskManager().createTaskInstance(ResourceManager.IMPORT_FROM_RESOURCE);

        SecurityUtils security = new SecurityUtils();
        PrincipalUser principal = security.getPrincipalUser();
        task.setOwner(principal.getUser().asPrismObject());

        OperationResult result = task.getResult();
        try {

            getModel().importAccountsFromResource(resourceOid, objectClass, task, result);

            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't launch import on resource {} and object class {}",
                    ex, resourceOid, objectClass);
            result.recordFatalError(ex);
        }

        ControllerUtil.printResults(LOGGER, result, null);
    }

    @Override
    public Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> listResourceObjects(
            String resourceOid, QName objectClass, PagingType paging) {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(objectClass, "Object class must not be null.");
        LOGGER.debug("Listing resource objects from resource with oid {} and object class {}.", new Object[]{
                resourceOid, objectClass});

        OperationResult result = new OperationResult(ResourceManager.LIST_RESOURCE_OBJECTS);
        Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> collection = new ArrayList<ResourceObjectShadowDto<ResourceObjectShadowType>>();
        try {
            List<PrismObject<? extends ResourceObjectShadowType>> list = getModel().listResourceObjects(resourceOid, objectClass, paging, result);
            if (list != null) {
                for (PrismObject<? extends ResourceObjectShadowType> object : list) {
                    collection.add(new ResourceObjectShadowDto<ResourceObjectShadowType>(object.asObjectable()));
                }
            }
            result.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get import status from resource {}", ex, resourceOid);
            result.recordFatalError("Couldn't get import status from resource '" + resourceOid + "'.", ex);
        } finally {
            result.computeStatus("Error while listing resource objects.", "");
        }

        ControllerUtil.printResults(LOGGER, result, null);

        return collection;
    }

    @Override
    public ConnectorDto getConnector(String oid) {
        ConnectorType connector;
        try {
            PrismObject<ConnectorType> object = get(ConnectorType.class, oid, new PropertyReferenceListType());
            connector = object.asObjectable();
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

        ControllerUtil.printResults(LOGGER, result, null);

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

        ControllerUtil.printResults(LOGGER, result, null);
    }
}
