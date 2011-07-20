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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.XPathUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.schema.processor.PropertyModification.ModificationType;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * Implementation of provisioning service.
 * 
 * It is just a "dispatcher" that routes interface calls to appropriate places.
 * E.g. the operations regarding resource definitions are routed directly to the
 * repository, operations of shadow objects are routed to the shadow cache and
 * so on.
 * 
 * WORK IN PROGRESS
 * 
 * There be dragons. Beware the dog. Do not trespass.
 * 
 * @author Radovan Semancik
 */
@Service(value = "provisioningService")
public class ProvisioningServiceImpl implements ProvisioningService, ResourceObjectChangeNotificationManager {

	@Autowired
	private ShadowCache shadowCache;
	@Autowired
	private RepositoryService repositoryService;

	private List<ResourceObjectChangeListener> listeners = new ArrayList<ResourceObjectChangeListener>();

	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);

	private static final QName TOKEN_ELEMENT_QNAME = new QName(SchemaConstants.NS_PROVISIONING_LIVE_SYNC,
			"token");

	public ShadowCache getShadowCache() {
		return shadowCache;
	}

	public void setShadowCache(ShadowCache shadowCache) {
		this.shadowCache = shadowCache;
	}

	/**
	 * Get the value of repositoryService.
	 * 
	 * @return the value of repositoryService
	 */
	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	/**
	 * Set the value of repositoryService
	 * 
	 * Expected to be injected.
	 * 
	 * @param repositoryService
	 *            new value of repositoryService
	 */
	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Override
	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException {

		Validate.notNull(oid, "Oid of object to get must not be null.");
		Validate.notNull(resolve, "Property reference list type must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.debug("**PROVISIONING: Getting object with oid {}", oid);

		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".getObject");
		result.addParam("oid", oid);
		result.addParam("resolve", resolve);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ObjectType repositoryObject = null;

		// HACK: connector objects are not stored in the repo..
		if (oid.startsWith("icf1")) {
			ConnectorType connectorType = shadowCache.getConnectorManager().getConnector(oid);
			LOGGER.trace("**PROVISIONING: Got connector object {}", JAXBUtil.silentMarshalWrap(connectorType));
			return connectorType;
		}

		try {
			repositoryObject = getRepositoryService().getObject(oid, resolve, result);
			LOGGER.trace("**PROVISIONING: Got repository object {}",
					JAXBUtil.silentMarshalWrap(repositoryObject));
		} catch (ObjectNotFoundException e) {
			LOGGER.error("**PROVISIONING: Can't get obejct with oid {}. Reason {}", oid, e);
			result.record(e);
			throw e;
		}

		if (repositoryObject instanceof ResourceObjectShadowType) {
			// ResourceObjectShadowType shadow =
			// (ResourceObjectShadowType)object;
			// TODO: optimization needed: avoid multiple "gets" of the same
			// object

			ResourceObjectShadowType shadow = null;
			try {
				shadow = getShadowCache().getShadow(oid, (ResourceObjectShadowType) repositoryObject, result);
				LOGGER.trace("**PROVISIONING: Got shadow object {}", JAXBUtil.silentMarshalWrap(shadow));
			} catch (ObjectNotFoundException e) {
				LOGGER.error("**PROVISIONING: Can't get obejct with oid {}. Reason {}", oid, e);
				result.record(e);
				throw e;
			} catch (CommunicationException e) {
				LOGGER.error("**PROVISIONING: Can't get obejct with oid {}. Reason {}", oid, e);
				result.record(e);
				throw e;
			} catch (SchemaException e) {
				LOGGER.error("**PROVISIONING: Can't get obejct with oid {}. Reason {}", oid, e);
				result.record(e);
				throw e;
			}

			// TODO: object resolving

			LOGGER.debug("**PROVISIONING: Get object finished.");

			return shadow;
		} else {
			return repositoryObject;
		}

	}

	@Override
	public String addObject(ObjectType object, ScriptsType scripts, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException, CommunicationException,
			ObjectNotFoundException {
		// TODO

		Validate.notNull(object, "Object to add must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		
		LOGGER.debug("**PROVISIONING: Start to add object {}", object);

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".addObject");
		result.addParam("object", object);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		String addedShadow = null;

		try {
			addedShadow = getShadowCache().addShadow(object, scripts, null, parentResult);
			LOGGER.debug("**PROVISIONING: Added shadow object {}", JAXBUtil.silentMarshalWrap(addedShadow));
			result.recordSuccess();
		} catch (GenericFrameworkException ex) {
			LOGGER.error("**PROVISIONING: Can't add object {}. Reason {}", object, ex);
			result.recordFatalError("Failed to add shadow object: " + ex.getMessage(), ex);
			throw new CommunicationException(ex.getMessage(), ex);
		}

		LOGGER.debug("**PROVISIONING: Adding object finished.");
		return addedShadow;
	}

	@Override
	public int synchronize(String resourceOid, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException {

		
		Validate.notNull(resourceOid, "Resource oid must not be null.");
		Validate.notNull(task, "Task must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		
		
		
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".synchronize");
		result.addParam(OperationResult.PARAM_OID, resourceOid);
		result.addParam(OperationResult.PARAM_TASK, task);

		int processedChanges = 0;
		
		// Resolve resource
		ObjectType resourceObjectType = getObject(resourceOid, null, result);
		
		if (!(resourceObjectType instanceof ResourceType)){
			result.recordFatalError("Object to synchronize must be type of resource.");
			throw new IllegalArgumentException("Object to synchronize must be type of resource.");
		}
		
		ResourceType resourceType = (ResourceType) resourceObjectType;

		LOGGER.debug("**PROVISIONING: Start synchronization of resource {} ", DebugUtil.prettyPrint(resourceType));
		
		// getting token form task
		Property tokenProperty = task.getExtension().findProperty(TOKEN_ELEMENT_QNAME);

		LOGGER.trace("**PROVISIONING: Got token property: {} from the task extension.", DebugUtil.prettyPrint(tokenProperty));

		List<PropertyModification> modifications = new ArrayList<PropertyModification>();
		List<Change> changes = null;// new ArrayList<Change>();
		try {
			changes = getShadowCache().fetchChanges(resourceType, tokenProperty, result);
			for (Change change : changes) {

				ResourceObjectShadowChangeDescriptionType shadowChangeDescription = createResourceShadowChangeDescription(
						change, resourceType);
				LOGGER.trace("**PROVISIONING: Created resource object shadow change description {}", DebugUtil.prettyPrint(shadowChangeDescription));
				notifyResourceObjectChangeListeners(shadowChangeDescription, result);

				Property newToken = change.getToken();
				// TODO: create property modification from new token
				PropertyModification propertyModification = newToken.createModification(
						ModificationType.REPLACE, newToken.getValues());
				modifications.add(propertyModification);

				processedChanges++;

			}
			task.modifyExtension(modifications, result);
		} catch (ObjectNotFoundException e) {
		    result.recordFatalError(e.getMessage(), e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			result.recordFatalError("Error communicating with connector: "+ e.getMessage(), e);
			throw new CommunicationException(e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			result.recordFatalError(e.getMessage(), e);
			throw new CommunicationException(e.getMessage(), e);
		} catch (SchemaException e) {
			result.recordFatalError(e.getMessage(), e);
			throw new SchemaException(e.getMessage(), e);
		}

		result.recordSuccess();
		return processedChanges;

	}

	@Override
	public ObjectListType listObjects(Class<? extends ObjectType> objectType, PagingType paging,
			OperationResult parentResult) {

		Validate.notNull(objectType, "Object type to list must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.debug("**PROVISIONING: Start listing objects of type {}", objectType);
		// Result type for this operation
		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".listObjects");
		result.addParam("objectType", objectType);
		result.addParam("paging", paging);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ObjectListType objListType = null;

		if (ResourceObjectShadowType.class.isAssignableFrom(objectType)) {
			// Listing of shadows is not supported because this operation does
			// not specify resource
			// to search. Maybe we need another operation for this.

			result.recordFatalError("Listing of shadows is not supported");
			throw new NotImplementedException("Listing of shadows is not supported");

		} else {
			// TODO: delegate to repository
			objListType = getRepositoryService().listObjects(objectType, paging, parentResult);
			result.recordSuccess();
		}

		LOGGER.debug("**PROVISIONING: Finished listing object.");

		return objListType;

	}

	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException, CommunicationException {

		final ObjectListType objListType = new ObjectListType();

		final ResultHandler handler = new ResultHandler() {

			@Override
			public boolean handle(ObjectType object, OperationResult parentResult) {
				return objListType.getObject().add(object);
			}
		};

		searchObjectsIterative(query, paging, handler, parentResult);
		return objListType;
	}

	@Override
	public void modifyObject(ObjectModificationType objectChange, ScriptsType scripts,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException {

		Validate.notNull(objectChange, "Object change must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".modifyObject");
		result.addParam("objectChange", objectChange);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		LOGGER.debug("**PROVISIONING: Start to modify object.");
		LOGGER.trace("*PROVISIONING: Object change: {}", JAXBUtil.silentMarshalWrap(objectChange));

		if (objectChange == null || objectChange.getOid() == null) {
			result.recordFatalError("Object change or object change oid cannot be null");
			throw new IllegalArgumentException("Object change or object change oid cannot be null");
		}

		ObjectType objectType = getRepositoryService().getObject(objectChange.getOid(),
				new PropertyReferenceListType(), parentResult);

		LOGGER.debug("**PROVISIONING: Modifying object with oid {}", objectChange.getOid());
		LOGGER.trace("**PROVISIONING: Object to modify: {}.", JAXBUtil.silentMarshalWrap(objectType));

		try {
			getShadowCache().modifyShadow(objectType, null, objectChange, scripts, parentResult);
			result.recordSuccess();
		} catch (CommunicationException e) {
			// TODO Auto-generated catch block
			result.recordFatalError("Can't modify object with oid " + objectChange.getOid() + ". Reason: "
					+ e.getMessage(), e);
			throw new CommunicationException(e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			// TODO Auto-generated catch block
			result.recordFatalError("Can't modify object with oid " + objectChange.getOid() + ". Reason: "
					+ e.getMessage(), e);
			throw new CommunicationException(e.getMessage(), e);
		}

		LOGGER.debug("Finished modifying of object with oid {}", objectType.getOid());
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteObject(String oid, ScriptsType scripts, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, SchemaException {
		// TODO Auto-generated method stub

		Validate.notNull(oid, "Oid of object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.debug("**PROVISIONING: Start to delete object with oid {}", oid);

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".deleteObject");
		result.addParam("oid", oid);
		result.addParam("scripts", scripts);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		ObjectType objectType = null;
		try {
			objectType = getRepositoryService().getObject(oid, new PropertyReferenceListType(), parentResult);
			LOGGER.debug("**PROVISIONING: Object from repository to delete: {}",
					JAXBUtil.silentMarshalWrap(objectType));
		} catch (SchemaException e) {
			result.recordFatalError("Can't get object with oid " + oid + " from repository. Reason:  "
					+ e.getMessage() + " " + e);
			throw new ObjectNotFoundException(e.getMessage());
		}

		try {
			getShadowCache().deleteShadow(objectType, null, parentResult);
			result.recordSuccess();
		} catch (CommunicationException e) {
			result.recordFatalError(e.getMessage());
			throw new CommunicationException(e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			result.recordFatalError(e.getMessage());
			throw new CommunicationException(e.getMessage(), e);
		} catch (SchemaException e) {
			result.recordFatalError(e.getMessage());
			throw new SchemaException(e.getMessage(), e);
		}
		LOGGER.debug("**PROVISIONING: Finished deleting object.");

	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult parentResult)
			throws ObjectNotFoundException {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public OperationResult testResource(String resourceOid) throws ObjectNotFoundException {
		// We are not going to create parent result here. We don't want to
		// pollute the result with
		// implementation details, as this will be usually displayed in the
		// table of "test resource" results.

		Validate.notNull(resourceOid, "Resource OID to test is null.");

		LOGGER.debug("Start testing resource with oid {} ", resourceOid);

		OperationResult parentResult = new OperationResult(TEST_CONNECTION_OPERATION);
		parentResult.addParam("resourceOid", resourceOid);
		parentResult.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		try {
			ObjectType objectType = getRepositoryService().getObject(resourceOid,
					new PropertyReferenceListType(), parentResult);

			if (objectType instanceof ResourceType) {
				ResourceType resourceType = (ResourceType) objectType;
				getShadowCache().testConnection(resourceType, parentResult);
			} else {
				throw new IllegalArgumentException("Object with oid is not resource. OID: " + resourceOid);
			}
		} catch (ObjectNotFoundException ex) {
			throw new ObjectNotFoundException("Object with OID " + resourceOid + " not found");
		} catch (SchemaException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
		parentResult.computeStatus();

		LOGGER.debug("Finished testing resource with oid {} ", resourceOid);
		return parentResult;
	}

	@Override
	public ObjectListType listResourceObjects(String resourceOid, QName objectType, PagingType paging,
			OperationResult parentResult) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	@Override
	public void searchObjectsIterative(QueryType query, PagingType paging, final ResultHandler handler,
			final OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
			CommunicationException {

		Validate.notNull(query, "Search query must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.debug("Start to search object. Query {}", JAXBUtil.silentMarshalWrap(query));

		OperationResult result = parentResult.createSubresult(ProvisioningService.class.getName()
				+ ".searchObjectsIterative");
		result.addParam("query", query);
		result.addParam("paging", paging);
		result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, ProvisioningServiceImpl.class);

		Element filter = query.getFilter();
		NodeList list = filter.getChildNodes();
		String resourceOid = null;
		QName objectClass = null;

		if (QNameUtil.compareQName(SchemaConstants.C_FILTER_AND, filter)) {
			for (int i = 0; i < list.getLength(); i++) {
				if (QNameUtil.compareQName(SchemaConstants.C_FILTER_TYPE, list.item(i))) {
					String type = list.item(i).getAttributes().getNamedItem("uri").getNodeValue();
					if (type == null || "".equals(type)) {
						result.recordFatalError("Object type is not defined.");
						throw new IllegalArgumentException("Object type is not defined.");
					}

				} else if (QNameUtil.compareQName(SchemaConstants.C_FILTER_EQUAL, list.item(i))) {
					NodeList equealList = list.item(i).getChildNodes();

					for (int j = 0; j < equealList.getLength(); j++) {
						if (QNameUtil.compareQName(SchemaConstants.C_FILTER_VALUE, equealList.item(j))) {
							Node value = equealList.item(j).getFirstChild();
							if (QNameUtil.compareQName(SchemaConstants.I_RESOURCE_REF, value)) {
								resourceOid = value.getAttributes().getNamedItem("oid").getNodeValue();
								LOGGER.debug("**PROVISIONING: Search objects on resource with oid {}",
										resourceOid);

							} else if (QNameUtil.compareQName(SchemaConstants.I_OBJECT_CLASS, value)) {
								String textContent = value.getTextContent();
								String prefix = null;
								String namespace = null;
								String localPart = null;
								if (textContent.contains(":")) {
									prefix = textContent.substring(0, textContent.lastIndexOf(":"));
									namespace = value.lookupNamespaceURI(prefix);
									localPart = textContent.substring(textContent.lastIndexOf(":") + 1);
									objectClass = new QName(namespace, localPart);
									LOGGER.debug("**PROVISIONING: Object class to search: {}", objectClass);
								} else {
									result.recordFatalError("Object class was not defined.");
									throw new IllegalArgumentException("Object class was not defined.");
								}
							}
						}
					}
				}
			}
		}

		if (resourceOid == null) {
			throw new IllegalArgumentException("Resource not defined in a search query");
		}
		if (objectClass == null) {
			throw new IllegalArgumentException("Objectclass not defined in a search query");
		}

		ResourceType resource = null;
		try {
			resource = (ResourceType) getRepositoryService().getObject(resourceOid,
					new PropertyReferenceListType(), parentResult);

		} catch (ObjectNotFoundException e) {
			result.recordFatalError("Resource with oid " + resourceOid + "not found. Reason: " + e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		}

		final ShadowHandler shadowHandler = new ShadowHandler() {

			@Override
			public boolean handle(ResourceObjectShadowType shadow) {
				LOGGER.debug("Found shadow: {}", DebugUtil.prettyPrint(shadow));
				return handler.handle(shadow, parentResult);
			}
		};

		getShadowCache().searchObjectsIterative(objectClass, resource, shadowHandler, parentResult);
		result.recordSuccess();
	}

	@Override
	public synchronized void registerNotificationListener(ResourceObjectChangeListener listener) {
		if (listeners.contains(listener)) {
			LOGGER.warn(
					"Resource object change listener '{}' is already registered. Subsequent registration is ignored",
					listener);
		} else {
			listeners.add(listener);
		}

	}

	@Override
	public synchronized void unregisterNotificationListener(ResourceObjectChangeListener listener) {
		listeners.remove(listener);
	}

	private synchronized void notifyResourceObjectChangeListeners(
			ResourceObjectShadowChangeDescriptionType change, OperationResult parentResult) {

		Validate.notNull(change, "Change description of resource object shadow must not be null.");
		
		LOGGER.debug("Notifying change {} ", DebugUtil.prettyPrint(change));
		
		if ((null != listeners) && (!listeners.isEmpty())) {
			for (ResourceObjectChangeListener listener : listeners) {
				LOGGER.debug("Listener: {}", listener.getClass().getSimpleName());
				listener.notifyChange(change, parentResult);

			}
		}
	}

	private ResourceObjectShadowChangeDescriptionType createResourceShadowChangeDescription(Change change,
			ResourceType resourceType) {
		ResourceObjectShadowChangeDescriptionType shadowChangeDescription = new ResourceObjectShadowChangeDescriptionType();
		shadowChangeDescription.setObjectChange(change.getChange());
		shadowChangeDescription.setResource(resourceType);
		shadowChangeDescription.setShadow(change.getOldShadow());
		shadowChangeDescription.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));
		return shadowChangeDescription;

	}
}
