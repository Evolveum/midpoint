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
import java.util.Random;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.object.ResourceObjectShadowUtil;
import com.evolveum.midpoint.common.object.ResourceTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.AttributeModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorManager;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ConnectorTestOperation;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.schema.processor.PropertyModification.ModificationType;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeDeletionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptOrderType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;
import com.sun.xml.bind.v2.schemagen.xmlschema.SchemaTop;

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

	private static final Trace LOGGER = TraceManager
			.getTrace(ShadowCache.class);

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
	public ResourceObjectShadowType getShadow(String oid,
			ResourceObjectShadowType repositoryShadow,
			OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException {

		Validate.notNull(oid, "Object id must not be null.");

		LOGGER.debug("Start getting object with oid {}", oid);

		// We are using parent result directly, not creating subresult.
		// We want to hide the existence of shadow cache from the user.

		// Get the shadow from repository. There are identifiers that we need
		// for accessing the object by UCF.
		// Later, the repository object may have a fully cached object from.
		if (repositoryShadow == null) {
			repositoryShadow = (ResourceObjectShadowType) getRepositoryService()
					.getObject(oid, null, parentResult);
			LOGGER.debug("Found shadow object: {}",
					JAXBUtil.silentMarshalWrap(repositoryShadow));
		}

		// Sanity check
		if (!oid.equals(repositoryShadow.getOid())) {
			parentResult
					.recordFatalError("Provided OID is not equal to OID of repository shadow");
			throw new IllegalArgumentException(
					"Provided OID is not equal to OID of repository shadow");
		}

		ResourceType resource = getResource(
				ResourceObjectShadowUtil.getResourceOid(repositoryShadow),
				parentResult);

		LOGGER.debug("Getting fresh object from ucf.");
		// Get the fresh object from UCF
		ConnectorInstance connector = getConnectorInstance(resource,
				parentResult);
		Schema schema = getResourceSchema(resource, connector, parentResult);

		QName objectClass = repositoryShadow.getObjectClass();
		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		if (rod == null) {
			// Unknown objectclass
			SchemaException ex = new SchemaException(
					"Object class "
							+ objectClass
							+ " defined in the repository shadow is not known in schema of resource "
							+ ObjectTypeUtil.toShortString(resource));
			parentResult
					.recordFatalError(
							"Object class "
									+ objectClass
									+ " defined in the repository shadow is not known in resource schema",
							ex);
			throw ex;
		}

		// Let's get all the identifiers from the Shadow <attributes> part
		Set<ResourceObjectAttribute> identifiers = rod
				.parseIdentifiers(repositoryShadow.getAttributes().getAny());

		if (identifiers == null || identifiers.isEmpty()) {
			// No identifiers found
			SchemaException ex = new SchemaException(
					"No identifiers found in the respository shadow "
							+ ObjectTypeUtil.toShortString(repositoryShadow)
							+ " with respect to resource "
							+ ObjectTypeUtil.toShortString(resource));
			parentResult.recordFatalError(
					"No identifiers found in the respository shadow "
							+ ObjectTypeUtil.toShortString(repositoryShadow),
					ex);
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
			parentResult.recordFatalError(
					"Object " + identifiers + "not found on the Resource "
							+ ObjectTypeUtil.toShortString(resource), ex);
			throw new ObjectNotFoundException("Object " + identifiers
					+ " not found on the Resource "
					+ ObjectTypeUtil.toShortString(resource), ex);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector. Reason: "
							+ ex.getMessage(), ex);
			throw new CommunicationException(
					"Error communicating with the connector", ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError(
					"Generic error in connector. Reason: " + ex.getMessage(),
					ex);
			throw new GenericConnectorException("Generic error in connector "
					+ connector + ": " + ex.getMessage(), ex);
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
			parentResult.recordFatalError(
					"Schema error. Reason: " + ex.getMessage(), ex);
			throw new SchemaException("Schema error: " + ex.getMessage(), ex);
		}
		repositoryShadow.getAttributes().getAny().clear();
		repositoryShadow.getAttributes().getAny().addAll(xmlAttributes);

		LOGGER.debug("Fresh object from ucf {}",
				JAXBUtil.silentMarshalWrap(repositoryShadow));

		parentResult.recordSuccess();
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
	public void listShadows(ResourceType resource, QName objectClass,
			final ShadowHandler handler, final OperationResult parentResult)
			throws CommunicationException, ObjectNotFoundException,
			SchemaException {

		Validate.notNull(objectClass);
		if (resource == null) {
			parentResult.recordFatalError("Resource must not be null");
			throw new IllegalArgumentException("Resource must not be null.");
		}

		LOGGER.debug(
				"Start listing objects on resource with oid {} with object class {} ",
				resource.getOid(), objectClass);

		ConnectorInstance connector = getConnectorInstance(resource,
				parentResult);

		Schema schema = getResourceSchema(resource, connector, parentResult);

		if (schema == null) {
			parentResult.recordFatalError("Can't get resource schema.");
			throw new IllegalArgumentException("Can't get resource schema.");
		}

		ResourceObjectDefinition resourceDef = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		if (resourceDef == null) {
			// Unknown objectclass
			SchemaException ex = new SchemaException(
					"Object class "
							+ objectClass
							+ " defined in the repository shadow is not known in schema of resource "
							+ ObjectTypeUtil.toShortString(resource));
			parentResult
					.recordFatalError(
							"Object class "
									+ objectClass
									+ " defined in the repository shadow is not known in resource schema",
							ex);
			throw ex;
		}

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
			connector.search(objectClass, resourceDef, resultHandler,
					parentResult);
			LOGGER.debug("Finished listing obejcts.");
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			parentResult.recordFatalError(
					"Error communicationg with the connector " + connector
							+ ". Reason: " + e.getMessage(), e);
			throw new CommunicationException(
					"Error communicationg with the connector " + connector
							+ ". Reason: " + e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError(
					"Generic error in connector. Reason: " + e.getMessage(), e);
			throw new GenericConnectorException(
					"Generic error in connector. Reason: " + e.getMessage(), e);
		}
		parentResult.recordSuccess();
	}

	public String addShadow(ObjectType object, ScriptsType scripts,
			ResourceType resource, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException,
			ObjectAlreadyExistsException, SchemaException,
			ObjectNotFoundException {

		Validate.notNull(object, "Object to add must not be null.");

		LOGGER.debug("Start adding shadow object {}.",
				JAXBUtil.silentMarshalWrap(object));

		if (object instanceof AccountShadowType) {

			AccountShadowType resourceObjectShadow = (AccountShadowType) object;

			if (resource == null) {
				resource = getResource(
						ResourceObjectShadowUtil
								.getResourceOid(resourceObjectShadow),
						parentResult);
			}

			ConnectorInstance connector = getConnectorInstance(resource,
					parentResult);
			Schema schema = getResourceSchema(resource, connector, parentResult);

			// convert xml attributes to ResourceObject
			ResourceObject resourceObject = convertFromXml(
					resourceObjectShadow, schema, parentResult);
			String result = null;
			Set<ResourceObjectAttribute> resourceAttributes = null;
			// add object using connector, setting new properties to the
			// resourceObject

			try {
				Set<Operation> scriptOperations = null;
				if (scripts != null) {
					scriptOperations = createExecuteScriptOperation(
							OperationTypeType.ADD, scripts);
				}
				resourceAttributes = connector.addObject(resourceObject,
						scriptOperations, parentResult);

				LOGGER.debug("Added object: {}",
						DebugUtil.prettyPrint(resourceAttributes));
				resourceObject.getProperties().addAll(resourceAttributes);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
				parentResult.recordFatalError(
						"Error communitacing with the connector " + connector
								+ ": " + ex.getMessage(), ex);
				throw new CommunicationException(
						"Error communitacing with the connector " + connector
								+ ": " + ex.getMessage(), ex);
			} catch (GenericFrameworkException ex) {
				parentResult.recordFatalError("Generic error in connector: "
						+ ex.getMessage(), ex);
				throw new GenericConnectorException(
						"Generic error in connector: " + ex.getMessage(), ex);
			}

			// create account shadow from resource object identifiers. This
			// account shadow consisted
			// of the identifiers added to the repo
			LOGGER.debug("Setting identifier of added obejct to the repository object");

			resourceObjectShadow = (AccountShadowType) createResourceShadow(
					resourceObject.getIdentifiers(), resourceObjectShadow);

			if (resourceObjectShadow == null) {
				parentResult
						.recordFatalError("Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
				throw new IllegalStateException(
						"Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
			}
			LOGGER.debug("Adding object with identifiers to the repository.");

			try {
				result = getRepositoryService().addObject(resourceObjectShadow,
						parentResult);
			} catch (ObjectAlreadyExistsException ex) {
				parentResult
						.recordFatalError(
								"Can't add shadow object to the repository. Shadow object already exist. Reason: "
										+ ex.getMessage(), ex);
				throw new ObjectAlreadyExistsException(
						"Can't add shadow object to the repository. Shadow object already exist. Reason: "
								+ ex.getMessage(), ex);
			}
			parentResult.recordSuccess();
			return result;
		}
		return null;

	}

	private Set<Operation> createExecuteScriptOperation(OperationTypeType type,
			ScriptsType scripts) {
		Set<Operation> scriptOperations = new HashSet<Operation>();
		for (ScriptType script : scripts.getScript()) {
			if (type.equals(script.getOperation())) {
				ExecuteScriptOperation scriptOperation = new ExecuteScriptOperation();

				for (ScriptArgumentType argument : script.getArgument()) {
					JAXBElement<ValueConstructionType.Value> value = argument
							.getValue();
					ExecuteScriptArgument arg = new ExecuteScriptArgument(
							argument.getName(), value.getValue().getContent());
					scriptOperation.getArgument().add(arg);
				}

				scriptOperation.setLanguage(script.getLanguage());
				scriptOperation.setTextCode(script.getCode());

				scriptOperation.setScriptOrder(script.getOrder());

				if (script.getHost().equals(ScriptHostType.CONNECTOR)) {
					scriptOperation.setConnectorHost(true);
					scriptOperation.setResourceHost(false);
				}
				if (script.getHost().equals(ScriptHostType.RESOURCE)) {
					scriptOperation.setConnectorHost(false);
					scriptOperation.setResourceHost(true);
				}

				scriptOperations.add(scriptOperation);
			}
		}
		return scriptOperations;
	}

	public void deleteShadow(ObjectType objectType, ScriptsType scripts,
			ResourceType resource, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException,
			ObjectNotFoundException, SchemaException {

		Validate.notNull(objectType, "Object to delete must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		if (objectType instanceof AccountShadowType) {

			AccountShadowType accountShadow = (AccountShadowType) objectType;

			if (resource == null) {
				resource = getResource(
						ResourceObjectShadowUtil.getResourceOid(accountShadow),
						parentResult);
			}

			LOGGER.debug("Deleting obejct  {} from the resource {}.",
					ObjectTypeUtil.toShortString(objectType),
					ObjectTypeUtil.toShortString(resource));

			ConnectorInstance connector = getConnectorInstance(resource,
					parentResult);

			Schema schema = getResourceSchema(resource, connector, parentResult);

			ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
					.findContainerDefinitionByType(accountShadow
							.getObjectClass());

			LOGGER.debug("Getting object identifiers");
			Set<ResourceObjectAttribute> identifiers = rod
					.parseIdentifiers(accountShadow.getAttributes().getAny());
			Set<Operation> executeScriptOperations = null;
			if (scripts != null) {
				executeScriptOperations = createExecuteScriptOperation(
						OperationTypeType.DELETE, scripts);
			}
			try {
				connector.deleteObject(accountShadow.getObjectClass(),
						executeScriptOperations, identifiers, parentResult);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't delete object "
						+ ObjectTypeUtil.toShortString(accountShadow)
						+ ". Reason: " + ex.getMessage(), ex);
				throw new ObjectNotFoundException(
						"An error occured while deleting resource object "
								+ accountShadow + "whith identifiers "
								+ identifiers + ": " + ex.getMessage(), ex);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
				parentResult.recordFatalError(
						"Error communicating with the connector " + connector
								+ ": " + ex.getMessage(), ex);
				throw new CommunicationException(
						"Error communitacing with the connector " + connector
								+ ": " + ex.getMessage(), ex);
			} catch (GenericFrameworkException ex) {
				parentResult.recordFatalError("Generic error in connector: "
						+ ex.getMessage(), ex);
				throw new GenericConnectorException(
						"Generic error in connector: " + ex.getMessage(), ex);
			}

			LOGGER.debug("Detele object with oid {} form repository.",
					accountShadow.getOid());
			try {
				getRepositoryService().deleteObject(accountShadow.getOid(),
						parentResult);
			} catch (ObjectNotFoundException ex) {
				parentResult.recordFatalError("Can't delete object "
						+ ObjectTypeUtil.toShortString(accountShadow)
						+ ". Reason: " + ex.getMessage(), ex);
				throw new ObjectNotFoundException(
						"An error occured while deleting resource object "
								+ accountShadow + "whith identifiers "
								+ identifiers + ": " + ex.getMessage(), ex);
			}
			parentResult.recordSuccess();
		}
	}

	public void modifyShadow(ObjectType objectType, ResourceType resource,
			ObjectModificationType objectChange, ScriptsType scripts,
			OperationResult parentResult) throws CommunicationException,
			GenericFrameworkException, ObjectNotFoundException, SchemaException {

		Validate.notNull(objectType, "Object to modify must not be null.");
		Validate.notNull(objectChange, "Object change must not be null.");

		if (objectType instanceof AccountShadowType) {
			AccountShadowType accountType = (AccountShadowType) objectType;
			if (resource == null) {
				resource = getResource(
						ResourceObjectShadowUtil.getResourceOid(accountType),
						parentResult);
			}

			LOGGER.debug("Modifying object {} on resource with oid {}",
					JAXBUtil.silentMarshalWrap(accountType), resource.getOid());

			ConnectorInstance connector = getConnectorInstance(resource,
					parentResult);

			Schema schema = getResourceSchema(resource, connector, parentResult);

			ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
					.findContainerDefinitionByType(accountType.getObjectClass());
			Set<ResourceObjectAttribute> identifiers = rod
					.parseIdentifiers(accountType.getAttributes().getAny());

			Set<Operation> executeScriptOperation = null;
			if (scripts != null) {
				executeScriptOperation = createExecuteScriptOperation(
						OperationTypeType.MODIFY, scripts);
			}
			Set<Operation> changes = getAttributeChanges(objectChange, rod);
			if (executeScriptOperation != null) {
				changes.addAll(executeScriptOperation);
			}
			LOGGER.debug("Applying change: {}",
					JAXBUtil.silentMarshalWrap(objectChange));
			try {
				connector.modifyObject(accountType.getObjectClass(),
						identifiers, changes, parentResult);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
				parentResult.recordFatalError(
						"Object to modify not found. Reason: "
								+ ex.getMessage(), ex);
				throw new ObjectNotFoundException(
						"Object to modify not found. " + ex.getMessage(), ex);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
				parentResult.recordFatalError(
						"Error communicationg with the connector " + connector
								+ ": " + ex.getMessage(), ex);
				throw new CommunicationException(
						"Error comminicationg with connector " + connector
								+ ": " + ex.getMessage(), ex);
			}
			parentResult.recordSuccess();
		}
	}

	public void testConnection(ResourceType resourceType,
			OperationResult parentResult) {

		// === test INITIALIZATION ===

		OperationResult initResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTION_INITIALIZATION
						.getOperation());
		ConnectorInstance connector;
		try {

			connector = getConnectorInstance(resourceType, parentResult);
			initResult.recordSuccess();
		} catch (ObjectNotFoundException e) {
			// The connector was not found. The resource definition is either
			// wrong or the connector is not
			// installed.
			initResult.recordFatalError("The connector was not found", e);
			return;
		}
		LOGGER.debug("Testing connection to the resource with oid {}",
				resourceType.getOid());

		// === test CONNECTION ===

		// delegate the main part of the test to the connector
		connector.test(parentResult);

		// === test SCHEMA ===

		OperationResult schemaResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_SCHEMA
						.getOperation());

		Schema schema = null;
		try {
			// Try to fetch schema from the connector. The UCF will convert it
			// to Schema Processor
			// format, so it is already structured
			schema = connector.fetchResourceSchema(schemaResult);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			schemaResult.recordFatalError(
					"Communication error: " + e.getMessage(), e);
			return;
		} catch (GenericFrameworkException e) {
			schemaResult
					.recordFatalError("Generic error: " + e.getMessage(), e);
			return;
		}

		if (schema == null || schema.isEmpty()) {
			schemaResult.recordFatalError("Empty schema returned");
			return;
		}

		// Invoke completeResource(). This will store the fetched schema to the
		// ResourceType
		// if there is no <schema> definition already. Therefore the
		// testResource() can be used to
		// generate the resource schema - until we have full schema caching
		// capability.
		try {
			completeResource(resourceType, schema, schemaResult);
		} catch (ObjectNotFoundException e) {
			schemaResult.recordFatalError(
					"Object not found (unexpected error, probably a bug): "
							+ e.getMessage(), e);
			return;
		} catch (SchemaException e) {
			schemaResult.recordFatalError(
					"Schema processing error (probably connector bug): "
							+ e.getMessage(), e);
			return;
		} catch (CommunicationException e) {
			schemaResult.recordFatalError(
					"Communication error: " + e.getMessage(), e);
			return;
		}

		schemaResult.recordSuccess();

		// TODO: connector sanity (e.g. at least one account type, identifiers
		// in schema, etc.)

	}

	public void searchObjectsIterative(final QName objectClass,
			final ResourceType resourceType, final ShadowHandler handler,
			final DiscoveryHandler discoveryHandler,
			final OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.debug(
				"Searching objects iterative with obejct class {}, resource: {}.",
				objectClass, ObjectTypeUtil.toShortString(resourceType));

		ConnectorInstance connector = getConnectorInstance(resourceType,
				parentResult);

		final Schema schema = getResourceSchema(resourceType, connector,
				parentResult);

		if (schema == null) {
			parentResult.recordFatalError("Can't get resource schema.");
			throw new IllegalArgumentException("Can't get resource schema.");
		}

		ResourceObjectDefinition resourceDef = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);

		ResultHandler resultHandler = new ResultHandler() {

			@Override
			public boolean handle(ResourceObject object) {
				ResourceObjectShadowType shadow;
				LOGGER.debug("Found resource object {}",
						DebugUtil.prettyPrint(object));
				try {

					// Try to find shadow that corresponds to the resource
					// object
					shadow = lookupShadow(object, parentResult);

					if (shadow == null) {
						LOGGER.trace(
								"Shadow object (in repo) to the resource object {} (on the resource) not found.",
								DebugUtil.prettyPrint(object));

						// TODO: make sure that the resource object has
						// appropriate definition
						// (use objectClass and schema)

						// The resource object obviously exists on the resource,
						// but appropriate shadow does not exist in the
						// repository
						// we need to create the shadow to align repo state to
						// the reality (resource)
						shadow = createResourceShadow(object, resourceType,
								parentResult);

						// And notify about the change we have discovered (if
						// requested to do so)
						if (discoveryHandler != null) {
							discoveryHandler.discovered(shadow, parentResult);
						}
					} else {
						LOGGER.trace(
								"Found shadow object in the repository {}",
								DebugUtil.prettyPrint(shadow));
					}

				} catch (SchemaProcessorException e) {
					// TODO: better error handling
					// TODO log it?
					parentResult.recordFatalError("Schema processor error: "
							+ e.getMessage(), e);
					LOGGER.error("Schema processor error: {}", e.getMessage(),
							e);
					return false;
				} catch (SchemaException e) {
					// TODO: better error handling
					// TODO log it?
					parentResult.recordFatalError(
							"Schema error: " + e.getMessage(), e);
					LOGGER.error("Schema error: {}", e.getMessage(), e);
					return false;
				}

				// TODO: if shadow does not exists, create it now

				return handler.handle(shadow);
			}

		};

		try {

			connector.search(objectClass, resourceDef, resultHandler,
					parentResult);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError("Generic error in the connector: "
					+ e.getMessage(), e);
			throw new CommunicationException("Generic error in the connector: "
					+ e.getMessage(), e);

		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector
							+ ": " + ex.getMessage(), ex);
			throw new CommunicationException(
					"Error communicating with the connector " + connector
							+ ": " + ex.getMessage(), ex);
		}

		parentResult.recordSuccess();

	}

	public Property fetchCurrentToken(ResourceType resourceType,
			OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, SchemaException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		LOGGER.debug("Getting last token");
		ConnectorInstance connector = getConnectorInstance(resourceType,
				parentResult);
		QName objectClass = new QName(resourceType.getNamespace(),
				"AccountObjectClass");
		Property lastToken = null;
		try {
			lastToken = connector.fetchCurrentToken(objectClass, parentResult);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError("Generic error in the connector: "
					+ e.getMessage(), e);
			throw new CommunicationException("Generic error in the connector: "
					+ e.getMessage(), e);

		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Error communicating with the connector " + connector
							+ ": " + ex.getMessage(), ex);
			throw new CommunicationException(
					"Error communicating with the connector " + connector
							+ ": " + ex.getMessage(), ex);
		}

		LOGGER.debug("Got last token: {}", DebugUtil.prettyPrint(lastToken));
		parentResult.recordSuccess();
		return lastToken;
	}

	public List<Change> fetchChanges(ResourceType resourceType,
			Property lastToken, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException,
			GenericFrameworkException, SchemaException {

		Validate.notNull(resourceType, "Resource must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");
		Validate.notNull(lastToken, "Token property must not be null.");

		LOGGER.debug("Shadow cache, fetch changes");
		ConnectorInstance connector = getConnectorInstance(resourceType,
				parentResult);

		QName objectClass = new QName(resourceType.getNamespace(),
				"AccountObjectClass");

		// get changes from the connector
		List<Change> changes = null;
		try {
			changes = connector.fetchChanges(objectClass, lastToken,
					parentResult);

			for (Change change : changes) {
				// search objects in repository
				AccountShadowType newAccount = findOrCreateAccount(connector,
						resourceType, change, parentResult);
				change.setOldShadow(newAccount);
			}
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Schema error: " + ex.getMessage(),
					ex);
			throw new SchemaException("Schema error: " + ex.getMessage(), ex);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			parentResult.recordFatalError(
					"Communication error: " + ex.getMessage(), ex);
			throw new CommunicationException("Communication error: "
					+ ex.getMessage(), ex);
		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError(
					"Object not found. Reason: " + ex.getMessage(), ex);
			throw new ObjectNotFoundException("Object not found. Reason: "
					+ ex.getMessage(), ex);
		} catch (GenericFrameworkException ex) {
			parentResult.recordFatalError("Generic error: " + ex.getMessage(),
					ex);
			throw new GenericFrameworkException("Generic error: "
					+ ex.getMessage(), ex);
		}
		parentResult.recordSuccess();
		return changes;
	}

	private AccountShadowType findOrCreateAccount(ConnectorInstance connector,
			ResourceType resource, Change change, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException,
			CommunicationException, GenericFrameworkException {

		ObjectListType accountList = searchAccountByUid(
				change.getIdentifiers(), parentResult);

		if (accountList.getObject().size() > 1) {
			parentResult
					.recordFatalError("Found more than one account with the identifier "
							+ change.getIdentifiers() + ".");
			throw new IllegalArgumentException(
					"Found more than one account with the identifier "
							+ change.getIdentifiers() + ".");
		}

		AccountShadowType newAccount = null;
		// if object doesn't exist, create it now
		if (accountList.getObject().isEmpty()) {

			if (!(change.getChange() instanceof ObjectChangeDeletionType)) {
				newAccount = createNewAccount(change, resource, connector,
						parentResult);
				LOGGER.debug("Create account shadow object: {}",
						ObjectTypeUtil.toShortString(newAccount));
			}
			// if exist, set the old shadow to the change
		} else {
			for (ObjectType obj : accountList.getObject()) {
				if (!(obj instanceof ResourceObjectShadowType)) {
					parentResult
							.recordFatalError("Object type must be one of the resource object shadow.");
					throw new IllegalStateException(
							"Object type must be one of the resource object shadow.");
				}
				newAccount = (AccountShadowType) obj;
				// if the fetched chande was one of the deletion type, delete
				// corresponding account from repo now
				if (change.getChange() instanceof ObjectChangeDeletionType) {
					try {
						getRepositoryService().deleteObject(
								newAccount.getOid(), parentResult);
					} catch (ObjectNotFoundException ex) {
						parentResult.recordFatalError(
								"Object with oid " + newAccount.getOid()
										+ " not found in repo. Reason: "
										+ ex.getMessage(), ex);
						throw new ObjectNotFoundException("Object with oid "
								+ newAccount.getOid()
								+ " not found in repo. Reason: "
								+ ex.getMessage(), ex);
					}
				}
			}
		}

		return newAccount;
	}

	private ObjectListType searchAccountByUid(Set<Property> identifiers,
			OperationResult parentResult) throws SchemaException {
		XPathSegment xpathSegment = new XPathSegment(
				SchemaConstants.I_ATTRIBUTES);
		Document doc = DOMUtil.getDocument();
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathType xpath = new XPathType(xpathSegments);
		List<Element> values = new ArrayList<Element>();
		try {
			for (Property identifier : identifiers) {
				values.addAll(identifier.serializeToDom(doc));
			}
		} catch (SchemaProcessorException ex) {
			throw new SchemaException(
					"Error serializing identifiers to dom. Reason: "
							+ ex.getMessage(), ex);
		}
		Element filter = QueryUtil.createAndFilter(doc, QueryUtil
				.createTypeFilter(doc, QNameUtil
						.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE)),
				QueryUtil.createEqualFilter(doc, xpath, values));

		QueryType query = new QueryType();
		query.setFilter(filter);

		ObjectListType accountList = null;
		try {
			accountList = getRepositoryService().searchObjects(query,
					new PagingType(), parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError(
					"Failed to search account according to the identifiers: "
							+ identifiers + ". Reason: " + ex.getMessage(), ex);
			throw new SchemaException(
					"Failed to search account according to the identifiers: "
							+ identifiers + ". Reason: " + ex.getMessage(), ex);
		}
		return accountList;
	}

	private AccountShadowType createNewAccount(Change change,
			ResourceType resourceType, ConnectorInstance connector,
			OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException,
			GenericFrameworkException {

		AccountShadowType newAccount = null;
		try {
			newAccount = (AccountShadowType) createResourceShadow(
					change.getIdentifiers(), new AccountShadowType());
		} catch (SchemaException ex) {
			parentResult
					.recordFatalError("Can't create account shadow from identifiers: "
							+ change.getIdentifiers());
			throw new SchemaException(
					"Can't create account shadow from identifiers: "
							+ change.getIdentifiers());
		}
		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(resourceType.getOid());
		newAccount.setResourceRef(ref);

		QName objectClass = new QName(resourceType.getNamespace(),
				"AccountObjectClass");
		newAccount.setObjectClass(objectClass);

		// set name for new account
		ResourceObject resourceObject = fetchResourceObject(
				change.getIdentifiers(), connector, resourceType, parentResult);
		String accountName = determineShadowName(resourceObject);
		newAccount.setName(resourceType.getName() + "-" + accountName);

		try {
			getRepositoryService().addObject(newAccount, parentResult);
		} catch (ObjectAlreadyExistsException e) {
			parentResult.recordFatalError(
					"Can't add account " + DebugUtil.prettyPrint(newAccount)
							+ " to the repository. Reason: " + e.getMessage(),
					e);
			throw new IllegalStateException(e.getMessage(), e);
		}

		return newAccount;
	}

	private ResourceObject fetchResourceObject(Set<Property> identifiers,
			ConnectorInstance connector, ResourceType resource,
			OperationResult parentResult) throws ObjectNotFoundException,
			CommunicationException, GenericFrameworkException, SchemaException {

		Set<ResourceObjectAttribute> roIdentifiers = new HashSet<ResourceObjectAttribute>();
		for (Property p : identifiers) {
			ResourceObjectAttribute roa = new ResourceObjectAttribute(
					p.getName(), p.getDefinition(), p.getValues());
			roIdentifiers.add(roa);
		}

		try {
			Schema schema = getResourceSchema(resource, connector, parentResult);
			ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
					.findContainerDefinitionByType(new QName(resource
							.getNamespace(), "AccountObjectClass"));
			ResourceObject resourceObject = connector.fetchObject(rod,
					roIdentifiers, parentResult);
			return resourceObject;
		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException e) {
			parentResult.recordFatalError("Object not found. Identifiers: "
					+ roIdentifiers + ". Reason: " + e.getMessage(), e);
			throw new ObjectNotFoundException("Object not found. Identifiers: "
					+ roIdentifiers + ". Reason: " + e.getMessage(), e);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			parentResult.recordFatalError(
					"Error communication with the connector " + connector
							+ ". Reason: " + e.getMessage(), e);
			throw new CommunicationException(
					"Error communication with the connector " + connector
							+ ". Reason: " + e.getMessage(), e);
		} catch (GenericFrameworkException e) {
			parentResult.recordFatalError("Generic error in the connector "
					+ connector + ". Reason: " + e.getMessage(), e);
			throw new CommunicationException("Generic error in the connector "
					+ connector + ". Reason: " + e.getMessage(), e);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Can't get resource schema. Reason: "
					+ ex.getMessage(), ex);
			throw new SchemaException("Can't get resource schema. Reason: "
					+ ex.getMessage(), ex);
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
	private ResourceObjectShadowType lookupShadow(
			ResourceObject resourceObject, OperationResult parentResult)
			throws SchemaProcessorException, SchemaException {

		QueryType query = createSearchShadowQuery(resourceObject);
		PagingType paging = new PagingType();

		// TODO: check for errors
		ObjectListType results;

		results = getRepositoryService().searchObjects(query, paging,
				parentResult);

		if (results.getObject().size() == 0) {
			return null;
		}
		if (results.getObject().size() > 1) {
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for "
					+ resourceObject);
		}

		return (ResourceObjectShadowType) results.getObject().get(0);
	}

	private QueryType createSearchShadowQuery(ResourceObject resourceObject)
			throws SchemaProcessorException {

		// We are going to query for attributes, so setup appropriate
		// XPath for the filter
		XPathSegment xpathSegment = new XPathSegment(
				SchemaConstants.I_ATTRIBUTES);
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
			throw new IllegalArgumentException(
					"More than one identifier value is not supported");
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
				QueryUtil.createTypeFilter(doc, QNameUtil
						.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE)),
				QueryUtil.createEqualFilter(doc, xpath,
						identifier.serializeToDom(doc)));

		QueryType query = new QueryType();
		query.setFilter(filter);

		System.out.println("created query " + DOMUtil.printDom(filter));

		return query;
	}

	// UTILITY METHODS

	private ConnectorInstance getConnectorInstance(ResourceType resource,
			OperationResult result) throws ObjectNotFoundException {
		// TODO: Add caching later
		try {
			return getConnectorManager().createConnectorInstance(resource);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException e) {
			result.recordFatalError(e.getMessage(), e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		}
	}

	private Schema getResourceSchema(ResourceType resource,
			ConnectorInstance connector, OperationResult parentResult)
			throws CommunicationException, SchemaException {

		// TODO: Need to add some form of memory caching here.

		Schema schema = null;

		// Parse schema from resource definition (if available)
		Element resourceXsdSchema = ResourceTypeUtil
				.getResourceXsdSchema(resource);
		if (resourceXsdSchema != null) {

			try {
				schema = Schema.parse(resourceXsdSchema);
			} catch (SchemaProcessorException e) {
				parentResult
						.recordFatalError("Unable to parse resource schema: "
								+ e.getMessage(), e);
				throw new SchemaException("Unable to parse resource schema: "
						+ e.getMessage(), e);
			}

		} else {
			// Otherwise try to fetch schema from connector

			try {
				schema = connector.fetchResourceSchema(parentResult);
			} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
				parentResult.recordFatalError(
						"Error communicating with the connector " + connector,
						ex);
				throw new CommunicationException(
						"Error communicating with the connector " + connector,
						ex);
			} catch (GenericFrameworkException ex) {
				parentResult.recordFatalError("Generic error in connector "
						+ connector + ": " + ex.getMessage(), ex);
				throw new GenericConnectorException(
						"Generic error in connector " + connector + ": "
								+ ex.getMessage(), ex);
			}

			if (schema == null) {
				parentResult
						.recordFatalError("Unable to fetch schema from the resource.");
				throw new SchemaException(
						"Unable to fetch schema from the resource");
			}

			// TODO: store fetched schema in the resource for future (and
			// offline) use

		}

		return schema;
	}

	private ResourceType getResource(String oid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		// TODO: add some caching
		return (ResourceType) getRepositoryService().getObject(oid, null,
				parentResult);
	}

	/**
	 * convert resource object shadow to the resource object according to given
	 * schema
	 * 
	 * @param resourceObjectShadow
	 *            object from which attributes are converted
	 * @param schema
	 * @return resourceObject
	 * @throws SchemaException
	 *             Object class definition was not found
	 */
	private ResourceObject convertFromXml(
			ResourceObjectShadowType resourceObjectShadow, Schema schema,
			OperationResult parentResult) throws SchemaException {
		QName objectClass = resourceObjectShadow.getObjectClass();

		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(schema, "Resource schema must not be null.");

		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);
		if (rod == null) {
			parentResult.recordFatalError("Schema definition for object class "
					+ objectClass + " was not found");
			throw new SchemaException("Schema definition for object class "
					+ objectClass + " was not found");
		}
		ResourceObject resourceObject = rod.instantiate();

		List<Element> attributes = resourceObjectShadow.getAttributes()
				.getAny();

		if (attributes == null) {
			throw new IllegalArgumentException(
					"Attributes for the account was not defined.");
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
	private ResourceObjectShadowType createResourceShadow(
			Set<Property> identifiers,
			ResourceObjectShadowType resourceObjectShadow)
			throws SchemaException {

		List<Element> identifierElements = new ArrayList<Element>();
		Document doc = DOMUtil.getDocument();
		for (Property p : identifiers) {
			try {
				List<Element> eList = p.serializeToDom(doc);
				identifierElements.addAll(eList);
			} catch (SchemaProcessorException e) {
				throw new SchemaException(
						"An error occured while serializing property " + p
								+ " to DOM");
			}
		}

		if (resourceObjectShadow.getAttributes() != null) {
			resourceObjectShadow.getAttributes().getAny().clear();
		} else {
			resourceObjectShadow
					.setAttributes(new ResourceObjectShadowType.Attributes());
		}
		resourceObjectShadow.getAttributes().getAny()
				.addAll(identifierElements);

		return resourceObjectShadow;
	}

	/**
	 * Create shadow based on the resource object that we have got
	 * 
	 * This method expects that the provided resource shadow is properly
	 * associated with the schema (has a definition).
	 * 
	 * @param resourceObject
	 *            resource object found on the resource
	 * @return shadow object created in the repository
	 * @throws SchemaException
	 */
	private ResourceObjectShadowType createResourceShadow(
			ResourceObject resourceObject, ResourceType resource,
			OperationResult parentResult) throws SchemaException {

		ResourceObjectShadowType shadow = null;

		// Determine correct type for the shadow
		if (resourceObject.isAccountType()) {
			shadow = new AccountShadowType();
		} else {
			shadow = new ResourceObjectShadowType();
		}

		shadow.setObjectClass(resourceObject.getDefinition().getTypeName());
		shadow.setName(determineShadowName(resourceObject));
		shadow.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
		Attributes attributes = new Attributes();
		shadow.setAttributes(attributes);

		Document doc = DOMUtil.getDocument();

		// Add identifiers to the shadow
		Set<Property> identifiers = resourceObject.getIdentifiers();
		for (Property p : identifiers) {
			try {
				List<Element> eList = p.serializeToDom(doc);
				shadow.getAttributes().getAny().addAll(eList);
			} catch (SchemaProcessorException e) {
				throw new SchemaException(
						"An error occured while serializing property " + p
								+ " to DOM");
			}
		}

		// Store shadow in the repository
		String oid = null;
		try {

			oid = getRepositoryService().addObject(shadow, parentResult);

		} catch (ObjectAlreadyExistsException e) {
			// This should not happen. The OID is not supplied and it is
			// generated by the repo
			// If it happens, it must be a repo bug. Therefore it is safe to
			// convert to runtime exception
			LOGGER.error("Unexpected repository behavior: "
					+ e.getClass().getSimpleName() + ": " + e.getMessage(), e);
			throw new IllegalStateException("Unexpected repository behavior: "
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		shadow.setOid(oid);

		// Add all attributes to the shadow
		shadow.getAttributes().getAny().clear();
		for (ResourceObjectAttribute attr : resourceObject.getAttributes()) {
			try {
				List<Element> eList = attr.serializeToDom(doc);
				shadow.getAttributes().getAny().addAll(eList);
			} catch (SchemaProcessorException e) {
				throw new SchemaException(
						"An error occured while serializing attribute " + attr
								+ " to DOM");
			}
		}

		return shadow;
	}

	private String determineShadowName(ResourceObject resourceObject)
			throws SchemaException {
		if (resourceObject.getNamingAttribute() == null) {
			// No naming attribute defined. Try to fall back to identifiers.
			Set<Property> identifiers = resourceObject.getIdentifiers();
			// We can use only single identifiers (not composite)
			if (identifiers.size() == 1) {
				Property identifier = identifiers.iterator().next();
				// Only single-valued identifiers
				Set<Object> values = identifier.getValues();
				if (values.size() == 1) {
					Object value = values.iterator().next();
					// and only strings
					if (value instanceof String) {
						return (String) value;
					}
				}
			}
			// Identifier is not usable as name
			// TODO: better identification of a problem
			throw new SchemaException(
					"No naming attribute defined (and identifier not usable)");
		}
		// TODO: Error handling
		return resourceObject.getNamingAttribute().getValue(String.class);
	}

	private Set<Operation> getAttributeChanges(
			ObjectModificationType objectChange, ResourceObjectDefinition rod) {
		Set<Operation> changes = new HashSet<Operation>();
		for (PropertyModificationType modification : objectChange
				.getPropertyModification()) {

			if (modification.getPath() == null) {
				throw new IllegalArgumentException(
						"Path to modificated attributes is null.");
			}

			if (modification.getPath().getTextContent()
					.contains(SchemaConstants.I_ATTRIBUTES.getLocalPart())) {

				Set<Property> changedProperties = rod
						.parseProperties(modification.getValue().getAny());
				for (Property p : changedProperties) {

					AttributeModificationOperation attributeModification = new AttributeModificationOperation();
					attributeModification.setChangeType(modification
							.getModificationType());
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

	private QueryType createSearchQuery(Set<Property> identifiers)
			throws SchemaException {
		XPathSegment xpathSegment = new XPathSegment(
				SchemaConstants.I_ATTRIBUTES);
		Document doc = DOMUtil.getDocument();
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathType xpath = new XPathType(xpathSegments);
		List<Element> values = new ArrayList<Element>();
		try {
			for (Property identifier : identifiers) {
				values.addAll(identifier.serializeToDom(doc));
			}
		} catch (SchemaProcessorException ex) {
			throw new SchemaException(
					"Error serializing identifiers to dom. Reason: "
							+ ex.getMessage(), ex);
		}
		Element filter = QueryUtil.createAndFilter(doc, QueryUtil
				.createTypeFilter(doc, QNameUtil
						.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE)),
				QueryUtil.createEqualFilter(doc, xpath, values));

		QueryType query = new QueryType();
		query.setFilter(filter);
		return query;
	}

	/**
	 * Make sure that the resource is complete.
	 * 
	 * It will check if the resource has a sufficiently fresh schema, etc.
	 * 
	 * Returned resource may be the same or may be a different instance, but it
	 * is guaranteed that it will be "fresher" and will correspond to the
	 * repository state (assuming that the provided resource also corresponded
	 * to the repository state).
	 * 
	 * The connector schema that was fetched before can be supplied to this
	 * method. This is just an optimization. It comes handy e.g. in test
	 * connection case.
	 * 
	 * Note: This is not really the best place for this method. Need to figure
	 * out correct place later.
	 * 
	 * @param resource
	 *            Resource to check
	 * @param resourceSchema
	 *            schema that was freshly pre-fetched (or null)
	 * @param result
	 *            completed resource
	 * @return
	 * @throws ObjectNotFoundException
	 *             connector instance was not found
	 * @throws SchemaException
	 * @throws CommunicationException
	 *             cannot fetch resource schema
	 */
	public ResourceType completeResource(ResourceType resource,
			Schema resourceSchema, OperationResult result)
			throws ObjectNotFoundException, SchemaException,
			CommunicationException {

		// Check presence of a schema
		XmlSchemaType xmlSchemaType = resource.getSchema();
		if (xmlSchemaType == null) {
			xmlSchemaType = new XmlSchemaType();
			resource.setSchema(xmlSchemaType);
		}
		Element xsdElement = findXsdElement(xmlSchemaType);

		if (xsdElement == null) {
			// There is no schema, we need to pull it from the resource

			if (resourceSchema == null) { // unless it has been already pulled
				LOGGER.trace("Fetching resource schema for "
						+ ObjectTypeUtil.toShortString(resource));
				ConnectorInstance connector = getConnectorInstance(resource,
						result);
				try {
					// Fetch schema from connector, UCF will convert it to
					// Schema Processor format and add all
					// necessary annotations
					resourceSchema = connector.fetchResourceSchema(result);

				} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
					throw new CommunicationException(
							"Cannot fetch resource schema: " + ex.getMessage(),
							ex);
				} catch (GenericFrameworkException ex) {
					throw new GenericConnectorException(
							"Generic error in connector " + connector + ": "
									+ ex.getMessage(), ex);
				}
			}
			LOGGER.debug("Generated resource schema for "
					+ ObjectTypeUtil.toShortString(resource) + ": "
					+ resourceSchema.getDefinitions().size() + " definitions");
			Document xsdDoc = null;
			try {
				// Convert to XSD
				LOGGER.trace("Generating XSD resource schema for "
						+ ObjectTypeUtil.toShortString(resource));
				xsdDoc = resourceSchema.serializeToXsd();
			} catch (SchemaProcessorException e) {
				throw new SchemaException(
						"Error processing resource schema for "
								+ ObjectTypeUtil.toShortString(resource) + ": "
								+ e.getMessage(), e);
			}
			// Store into repository (modify ResourceType)
			LOGGER.info("Storing generated schema in resource "
					+ ObjectTypeUtil.toShortString(resource));
			xsdElement = DOMUtil.getFirstChildElement(xsdDoc);
			xmlSchemaType.getAny().add(xsdElement);
			ObjectModificationType objectModificationType = ObjectTypeUtil
					.createModificationReplaceProperty(resource.getOid(),
							SchemaConstants.I_SCHEMA, xmlSchemaType);
			repositoryService.modifyObject(objectModificationType, result);
		}

		return resource;
	}

	private Element findXsdElement(XmlSchemaType xmlSchemaType) {
		List<Element> schemaElements = xmlSchemaType.getAny();
		for (Element e : schemaElements) {
			if (QNameUtil.compareQName(SchemaConstants.XSD_SCHEMA_ELEMENT, e)) {
				return e;
			}
		}
		return null;
	}
}
