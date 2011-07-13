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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.object.ResourceObjectShadowUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.AttributeModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 * This class manages the "cache" of ResourceObjectShadows in the repository.
 * 
 * In short, this class takes care of aligning the shadow objects in repository
 * with the real state of the resource.
 * 
 * The repository content is considered a "cache" when it comes to Shadow
 * objects. That's why they are called "shadow" objects after all. When a new
 * state (values) of the resource object is detected, the shadow in the
 * repository should be updated. No matter if that was detected by
 * synchronization, reconciliation or an ordinary get from resource. This class
 * is supposed to do that.
 * 
 * Therefore all operations that deal with "shadows" should pass through this
 * class. It forms yet another layer of the provisioning subsystem.
 * 
 * Current implementation assumes we are only storing primary identifier in the
 * repository. That should be made configurable later. It also only support
 * Account objects now.
 * 
 * This is WORK IN PROGRESS ...
 * 
 * @author Radovan Semancik
 */
@Component
public class ShadowCache {

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private ConnectorManager connectorManager;

	public ShadowCache() {
		repositoryService = null;
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

	public ConnectorManager getConnectorManager() {
		return connectorManager;
	}

	/**
	 * Set the value of connector manager.
	 * 
	 * Expected to be injected.
	 * 
	 * @param connectorManager
	 */
	public void setConnectorManager(ConnectorManager connectorManager) {
		this.connectorManager = connectorManager;
	}

	/**
	 * Gets the shadow with specified OID
	 * 
	 * The shadow will be read from the repository and missing information will
	 * be fetched from the resource.
	 * 
	 * If no repositoryShadow is specified, the shadow will be retrieved from
	 * the repository. This is just an optimization if the object was already
	 * fetched (which is a usual case).
	 * 
	 * This method is using identification by OID. This is intended for normal
	 * usage. Method that uses native identification will be provided later.
	 * 
	 * @param oid
	 *            OID of shadow to get.
	 * @param repositoryShadow
	 *            shadow that was read from the repository
	 * @return retrieved shadow (merged attributes from repository and resource)
	 * @throws ObjectNotFoundException
	 *             shadow was not found or object was not found on the resource
	 * @throws CommunicationException
	 *             problem communicating with the resource
	 * @throws SchemaException
	 *             problem processing schema or schema violation
	 */
	public ResourceObjectShadowType getShadow(String oid, ResourceObjectShadowType repositoryShadow,
			OperationResult parentResult) throws ObjectNotFoundException, CommunicationException,
			SchemaException {

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.

		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.
		// Later, the repository object may have a fully cached object from.
		if (repositoryShadow == null) {
			repositoryShadow = (ResourceObjectShadowType) getRepositoryService().getObject(oid, null,
					parentResult);
		}

		// Sanity check
		if (!oid.equals(repositoryShadow.getOid())) {
			throw new IllegalArgumentException("Provided OID is not equal to OID of repository shadow");
		}

		ResourceType resource = getResource(ResourceObjectShadowUtil.getResourceOid(repositoryShadow),
				parentResult);

		// Get the fresh object from UCF
		ConnectorInstance connector = getConnectorInstance(resource);
		Schema schema = null;
		schema = getResourceSchema(resource, connector, parentResult);
		
		

		QName objectClass = repositoryShadow.getObjectClass();
		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		if (rod == null) {
			// Unknown objectclass
			SchemaException ex = new SchemaException("Object class " + objectClass
					+ " defined in the repository shadow is not known in schema of resource "
					+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError("Object class " + objectClass
					+ " defined in the repository shadow is not known in resource schema", ex);
			throw ex;
		}

		// Let's get all the identifiers from the Shadow <attributes> part
		Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(repositoryShadow.getAttributes()
				.getAny());

		if (identifiers == null || identifiers.isEmpty()) {
			// No identifiers found
			SchemaException ex = new SchemaException("No identifiers found in the respository shadow "
					+ ObjectTypeUtil.toShortString(repositoryShadow) + " with respect to resource "
					+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError(
					"No identifiers found in the respository shadow "
							+ ObjectTypeUtil.toShortString(repositoryShadow), ex);
			throw ex;
		}

		ResourceObject ro = null;

		try {
			// Passing ResourceObjectDefinition instead object class. The
			// returned
			// ResourceObject will have a proper links to the schema.
			ro = connector.fetchObject(rod, identifiers, parentResult);

		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
			// TODO: Discovery
			throw new ObjectNotFoundException("Object " + identifiers + " not found on the Resource "
					+ ObjectTypeUtil.toShortString(resource), ex);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			throw new CommunicationException("Error communicating with the connector", ex);
		} catch (GenericFrameworkException ex) {
			throw new GenericConnectorException("Generic error in connector " + connector + ": "
					+ ex.getMessage(), ex);
		}

		// Let's replace the attribute values fetched from repository with the
		// ResourceObject content fetched from resource. The resource is more
		// fresh and the attributes more complete.
		// TODO: Discovery
		Element firstElement = repositoryShadow.getAttributes().getAny().get(0);
		Document doc = firstElement.getOwnerDocument();
		// TODO: Optimize the use of XML namespaces
		List<Element> xmlAttributes;
		try {
			xmlAttributes = ro.serializePropertiesToDom(doc);
		} catch (SchemaProcessorException ex) {
			throw new SchemaException("Schema error: " + ex.getMessage(), ex);
		}
		repositoryShadow.getAttributes().getAny().clear();
		repositoryShadow.getAttributes().getAny().addAll(xmlAttributes);

		return repositoryShadow;
	}

	/**
	 * List all shadow objects of specified objectClass.
	 * 
	 * Not used now. Will be used in import. Only provided for demonstration how
	 * to map ResourceObject to shadow.
	 * 
	 * !!! NOT TESTED !!!
	 * 
	 * @param resource
	 * @param objectClass
	 * @param handler
	 * @param parentResult
	 * @throws CommunicationException
	 * @throws ObjectNotFoundException
	 *             the connector object was not found
	 */
	public void listShadows(ResourceType resource, QName objectClass, final ShadowHandler handler,
			final OperationResult parentResult) throws CommunicationException, ObjectNotFoundException {

		ConnectorInstance connector = getConnectorInstance(resource);

		if (resource == null) {
			throw new IllegalArgumentException("Resource must not be null.");
		}

		Schema schema = getResourceSchema(resource, connector, parentResult);

		if (schema == null) {
			throw new IllegalArgumentException("Can't get resource schema.");
		}

		ResourceObjectDefinition resourceDef = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		ResultHandler resultHandler = new ResultHandler() {

			@Override
			public boolean handle(ResourceObject object) {

				ResourceObjectShadowType shadow;
				try {
					shadow = lookupShadow(object, parentResult);
				} catch (SchemaProcessorException e) {
					// TODO: better error handling
					// TODO log it?
					return false;
				} catch (SchemaException e) {
					// TODO: better error handling
					// TODO log it?
					return false;
				}

				// TODO: if shadow does not exists, create it now

				return handler.handle(shadow);
			}
		};

		try {
			connector.search(objectClass, resourceDef, resultHandler, parentResult);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			throw new CommunicationException(e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			throw new GenericConnectorException(e.getMessage(), e);
		}
	}

	public String addShadow(ObjectType object, ScriptsType scripts, ResourceType resource,
			OperationResult parentResult) throws CommunicationException, GenericFrameworkException,
			ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {

		if (object == null) {
			parentResult.recordFatalError("Object to add must not be null.");
			throw new IllegalArgumentException("Object to add must not be null.");
		}

		if (object instanceof AccountShadowType) {
			AccountShadowType resourceObjectShadow = (AccountShadowType) object;

			if (resource == null) {
				resource = getResource(ResourceObjectShadowUtil.getResourceOid(resourceObjectShadow),
						parentResult);
			}

			ConnectorInstance connector = getConnectorInstance(resource);
			Schema schema = getResourceSchema(resource, connector, parentResult);

			// convert xml attributes to ResourceObject
			ResourceObject resourceObject = convertFromXml(resourceObjectShadow, schema);
			String result = null;
			Set<ResourceObjectAttribute> resourceAttributes = null;
			// add object using connector, setting new properties to the
			// resourceObject
			try {
				resourceAttributes = connector.addObject(resourceObject, null, parentResult);
				resourceObject.getProperties().addAll(resourceAttributes);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
				parentResult.recordFatalError("Error communitacing with the connector " + connector + ": "
						+ ex.getMessage(), ex);
				throw new CommunicationException("Error communitacing with the connector " + connector + ": "
						+ ex.getMessage(), ex);
			} catch (GenericFrameworkException ex) {
				parentResult.recordFatalError(ex.getMessage(), ex);
				throw new GenericConnectorException(ex.getMessage(), ex);
			}

			// create account shadow from resource object identifiers. This
			// account shadow consisted
			// of the identifiers added to the repo
			resourceObjectShadow = (AccountShadowType) createResourceShadow(resourceObject.getIdentifiers(),
					resourceObjectShadow);
			
			if (resourceObjectShadow == null){
				parentResult.recordFatalError("Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
				throw new IllegalStateException("Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
			}
			
			result = getRepositoryService().addObject(resourceObjectShadow, parentResult);
			parentResult.recordSuccess();
			return result;
		}
		return null;

	}

	public void deleteShadow(ObjectType objectType, ResourceType resource, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
			SchemaException {

		if (objectType instanceof AccountShadowType) {

			AccountShadowType accountShadow = (AccountShadowType) objectType;

			if (resource == null) {
				resource = getResource(ResourceObjectShadowUtil.getResourceOid(accountShadow), parentResult);
			}

			ConnectorInstance connector = getConnectorInstance(resource);

			Schema schema = getResourceSchema(resource, connector, parentResult);

			ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
					.findContainerDefinitionByType(accountShadow.getObjectClass());

			Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(accountShadow.getAttributes()
					.getAny());

			try {
				connector.deleteObject(accountShadow.getObjectClass(), identifiers, parentResult);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
				throw new ObjectNotFoundException("An error occured while deleting resource object "
						+ accountShadow + "whith identifiers " + identifiers + ": " + ex.getMessage(), ex);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
				throw new CommunicationException("Error communitacing with the connector " + connector + ": "
						+ ex.getMessage(), ex);
			}

			getRepositoryService().deleteObject(accountShadow.getOid(), parentResult);
		}
	}

	public void modifyShadow(ObjectType objectType, ResourceType resource,
			ObjectModificationType objectChange, ScriptsType scripts, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
			SchemaException {

		if (objectType instanceof AccountShadowType) {
			AccountShadowType accountType = (AccountShadowType) objectType;
			if (resource == null) {

				resource = getResource(ResourceObjectShadowUtil.getResourceOid(accountType), parentResult);

			}

			ConnectorInstance connector = getConnectorInstance(resource);

			Schema schema = getResourceSchema(resource, connector, parentResult);

			ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
					.findContainerDefinitionByType(accountType.getObjectClass());
			Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(accountType.getAttributes()
					.getAny());

			Set<Operation> changes = getAttributeChanges(objectChange, rod);// new
																			// HashSet<Operation>();

			try {
				connector.modifyObject(accountType.getObjectClass(), identifiers, changes, parentResult);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
				throw new ObjectNotFoundException("Object to modify not found. " + ex.getMessage(), ex);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
				throw new CommunicationException("Error comminicationg with connector " + connector + ": "
						+ ex.getMessage(), ex);
			}
		}
	}

	public void testConnection(ResourceType resourceType, OperationResult parentResult) {

		OperationResult initResult = parentResult
				.createSubresult(ProvisioningService.TEST_CONNECTION_CONNECTOR_INIT_OPERATION);
		ConnectorInstance connector;
		try {
			connector = getConnectorInstance(resourceType);
			initResult.recordSuccess();
		} catch (ObjectNotFoundException e) {
			// The connector was not found. The resource definition is either
			// wrong or the connector is not
			// installed.
			initResult.recordFatalError("The connector was not found", e);
			return;
		}
		connector.test(parentResult);
	}

	public void searchObjectsIterative(QName objectClass, ResourceType resourceType,
			final ShadowHandler handler, final OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException {

		ConnectorInstance connector = getConnectorInstance(resourceType);

		if (resourceType == null) {
			throw new IllegalArgumentException("Resource must not be null.");
		}

		Schema schema = getResourceSchema(resourceType, connector, parentResult);

		if (schema == null) {
			throw new IllegalArgumentException("Can't get resource schema.");
		}

		ResourceObjectDefinition resourceDef = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		ResultHandler resultHandler = new ResultHandler() {

			@Override
			public boolean handle(ResourceObject object) {
				ResourceObjectShadowType shadow;
				System.out.println();
				try {
					shadow = lookupShadow(object, parentResult);

				} catch (SchemaProcessorException e) {
					// TODO: better error handling
					// TODO log it?
					return false;
				} catch (SchemaException e) {
					// TODO: better error handling
					// TODO log it?
					return false;
				}

				// TODO: if shadow does not exists, create it now

				return handler.handle(shadow);
			}
		};

		try {

			connector.search(objectClass, resourceDef, resultHandler, parentResult);
		} catch (GenericFrameworkException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			// throw new CommunicationException(ex.getMessage(), ex);
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}

	}

	// TODO: methods with native identification (Set<Attribute> identifier)
	// instead of OID.

	// OLD METHODS
	// TODO: refactor to current needs

	/**
	 * Locates the appropriate Shadow in repository that corresponds to the
	 * provided resource object.
	 * 
	 * @param parentResult
	 * 
	 * @return current unchanged shadow object that corresponds to provided
	 *         resource object or null if the object does not exist
	 * @throws SchemaProcessorException
	 * @throws SchemaException
	 */
	private ResourceObjectShadowType lookupShadow(ResourceObject resourceObject, OperationResult parentResult)
			throws SchemaProcessorException, SchemaException {

		QueryType query = createSearchShadowQuery(resourceObject);
		PagingType paging = new PagingType();

		// TODO: check for errors
		ObjectListType results;

		results = getRepositoryService().searchObjects(query, paging, parentResult);

		if (results.getObject().size() == 0) {
			return null;
		}
		if (results.getObject().size() > 1) {
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for " + resourceObject);
		}

		return (ResourceObjectShadowType) results.getObject().get(0);
	}

	private QueryType createSearchShadowQuery(ResourceObject resourceObject) throws SchemaProcessorException {

		// We are going to query for attributes, so setup appropriate
		// XPath for the filter
		XPathSegment xpathSegment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathType xpath = new XPathType(xpathSegments);

		// Now we need to determine what is the identifer and set corrent
		// value for it in the filter
		Property identifier = resourceObject.getIdentifier();

		Set<Object> idValues = identifier.getValues();
		// Only one value is supported for an identifier
		if (idValues.size() > 1) {
			// TODO: This should probably be switched to checked exception later
			throw new IllegalArgumentException("More than one identifier value is not supported");
		}
		if (idValues.size() < 1) {
			// TODO: This should probably be switched to checked exception later
			throw new IllegalArgumentException("The identifier has no value");
		}

		// We have all the data, we can construct the filter now
		Document doc = DOMUtil.getDocument();
		Element filter = QueryUtil.createAndFilter(
				doc,
				// TODO: The account type is hardcoded now, it should determined
				// from the shcema later, or maybe we can make it entirelly
				// generic (use ResourceObjectShadowType instead).
				QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE)),
				QueryUtil.createEqualFilter(doc, xpath, identifier.serializeToDom(doc)));

		QueryType query = new QueryType();
		query.setFilter(filter);

		System.out.println("created query " + DOMUtil.printDom(filter));

		return query;
	}

	// UTILITY METHODS

	private ConnectorInstance getConnectorInstance(ResourceType resource) throws ObjectNotFoundException {
		// TODO: Add caching later
		try {
			return getConnectorManager().createConnectorInstance(resource);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException e) {
			throw new ObjectNotFoundException(e.getMessage(), e);
		}
	}

	private Schema getResourceSchema(ResourceType resource, ConnectorInstance connector,
			OperationResult parentResult) throws CommunicationException {

		// TEMPORARY HACK: Fetch schema from connector

		try {
			return connector.fetchResourceSchema(parentResult);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			throw new CommunicationException("Error communicating with the connector " + connector, ex);
		} catch (GenericFrameworkException ex) {
			throw new GenericConnectorException("Generic error in connector " + connector + ": "
					+ ex.getMessage(), ex);
		}

		// Need to add some form of caching here.
		// For now just parse it from the resource definition.

		// Element schemaElement =
		// ResourceTypeUtil.getResourceXsdSchema(resource);
		// if (schemaElement==null) {
		// throw new
		// SchemaException("No schema found in definition of resource "+ObjectTypeUtil.toShortString(resource));
		// }
		// return Schema.parse(schemaElement);
	}

	private ResourceType getResource(String oid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		// TODO: add some caching
		return (ResourceType) getRepositoryService().getObject(oid, null, parentResult);
	}

	/**
	 * convert resource object shadow to the resource object according to given
	 * schema
	 * 
	 * @param resourceObjectShadow
	 *            object from which attributes are converted
	 * @param schema
	 * @return resourceObject
	 */
	private ResourceObject convertFromXml(ResourceObjectShadowType resourceObjectShadow, Schema schema) {
		QName objectClass = resourceObjectShadow.getObjectClass();

		if (objectClass == null) {
			throw new IllegalArgumentException("Object class is not defined.");
		}

		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);
		ResourceObject resourceObject = rod.instantiate();

		List<Element> attributes = resourceObjectShadow.getAttributes().getAny();
		if (attributes == null) {
			throw new IllegalArgumentException("Attributes for the account was not defined.");
		}

		Set<ResourceObjectAttribute> resAttr = rod.parseAttributes(attributes);
		resourceObject.getAttributes().addAll(resAttr);

		return resourceObject;
	}

	/**
	 * create resource object shadow from identifiers
	 * 
	 * @param identifiers
	 *            properties of the resourceObject. This properties describes
	 *            created resource object shadow attributes
	 * @param resourceObjectShadow
	 * @return resourceObjectShadow
	 * @throws SchemaException
	 */
	private ResourceObjectShadowType createResourceShadow(Set<Property> identifiers,
			ResourceObjectShadowType resourceObjectShadow) throws SchemaException {

		List<Element> identifierElements = new ArrayList<Element>();
		Document doc = DOMUtil.getDocument();
		for (Property p : identifiers) {
			try {
				List<Element> eList = p.serializeToDom(doc);
				identifierElements.addAll(eList);
			} catch (SchemaProcessorException e) {
				throw new SchemaException("An error occured while serializing property " + p + " to DOM");
			}
		}

		resourceObjectShadow.getAttributes().getAny().clear();
		resourceObjectShadow.getAttributes().getAny().addAll(identifierElements);

		return resourceObjectShadow;
	}

	private Set<Operation> getAttributeChanges(ObjectModificationType objectChange,
			ResourceObjectDefinition rod) {
		Set<Operation> changes = new HashSet<Operation>();
		for (PropertyModificationType modification : objectChange.getPropertyModification()) {

			if (modification.getPath() == null) {
				throw new IllegalArgumentException("Path to modificated attributes is null.");
			}

			if (modification.getPath().getTextContent().contains(SchemaConstants.I_ATTRIBUTES.getLocalPart())) {

				Set<Property> changedProperties = rod.parseProperties(modification.getValue().getAny());
				for (Property p : changedProperties) {

					AttributeModificationOperation attributeModification = new AttributeModificationOperation();
					attributeModification.setChangeType(modification.getModificationType());
					attributeModification.setNewAttribute(p);
					changes.add(attributeModification);
				}
			} else {
				throw new IllegalArgumentException("Wrong path value: "
						+ modification.getPath().getTextContent());
			}
		}
		return changes;
	}
}
