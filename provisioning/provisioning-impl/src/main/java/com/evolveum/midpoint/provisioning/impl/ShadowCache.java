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
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.ucf.api.AttributeModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptArgument;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.ResultHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.processor.ComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.ItemDefinition;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.PropertyModification;
import com.evolveum.midpoint.schema.processor.PropertyModification.ModificationType;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.MiscUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
 * @author Radovan Semancik
 */
@Component
public class ShadowCache {

	@Autowired
	private RepositoryService repositoryService;
//	@Autowired
//	private ConnectorFactory connectorFactory;
	@Autowired
	private ConnectorTypeManager connectorTypeManager; 
	@Autowired
	private ResourceSchemaCache resourceSchemaCache;

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

//	public ConnectorFactory getConnectorFactory() {
//		return connectorFactory;
//	}
//
//	/**
//	 * Set the value of connector manager.
//	 * 
//	 * Expected to be injected.
//	 * 
//	 * @param connectorFactory
//	 */
//	public void setConnectorFactory(ConnectorFactory connectorFactory) {
//		this.connectorFactory = connectorFactory;
//	}

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
			repositoryShadow = getRepositoryService()
					.getObject(ResourceObjectShadowType.class, oid, null, parentResult);
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
		Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(repositoryShadow.getAttributes().getAny());

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

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow from repository:\n{}",ObjectTypeUtil.dump(repositoryShadow));
			LOGGER.trace("Resource object fetched from resource:\n{}",ro.dump());
		}
		
		// Complete the shadow by adding attributes from the resource object
		ResourceObjectShadowType resultShadow = assembleShadow(ro,repositoryShadow, parentResult);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow when assembled:\n",ObjectTypeUtil.dump(resultShadow));
		}
		
		parentResult.recordSuccess();
		return resultShadow;
	}
		
	/**
	 * Creates a shadow object from the supplied resource object.
	 * 
	 * If an optional resourceObject is specified, it will be used as a base for creating the shadow. In this case
	 * the same instance is returned, but it is enriched with attributes from the resource object.
	 *  
	 * @param resourceObject
	 * @param repositoryShadow
	 * @return
	 * @throws SchemaException 
	 */
	public ResourceObjectShadowType assembleShadow(ResourceObject resourceObject, ResourceObjectShadowType repositoryShadow, OperationResult parentResult) throws SchemaException {
		ResourceObjectShadowType resultShadow;
		Document doc;
		if (repositoryShadow!=null) {
			resultShadow = repositoryShadow;
			Object firstElement = resultShadow.getAttributes().getAny().get(0);
			doc = JAXBUtil.getDocument(firstElement);
		} else {
			// TODO: create specific subtypes
			resultShadow = new ResourceObjectShadowType();
			Attributes attributes = new Attributes();
			resultShadow.setAttributes(attributes);
			doc = DOMUtil.getDocument();
		}
		// Let's replace the attribute values fetched from repository with the
		// ResourceObject content fetched from resource. The resource is more
		// fresh and the attributes more complete.
		// TODO: Discovery
		// TODO: Optimize the use of XML namespaces
		List<Object> xmlAttributes;
		try {
			xmlAttributes = resourceObject.serializePropertiesToJaxb(doc);

		} catch (SchemaException ex) {
			parentResult.recordFatalError(ex.getMessage());
			throw ex;
		}
		resultShadow.getAttributes().getAny().clear();
		resultShadow.getAttributes().getAny().addAll(xmlAttributes);
		
		return resultShadow;
	}

	/**
	 * @param resource
	 * @param parentResult
	 * @return
	 * @throws SchemaException 
	 * @throws ObjectNotFoundException 
	 * @throws CommunicationException 
	 */
	private ConnectorInstance getConnectorInstance(ResourceType resource, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException {
		return connectorTypeManager.getConfiguredConnectorInstance(resource, parentResult);
	}

	/**
	 * List all shadow objects of specified objectClass.
	 * 
	 * Not used now. Will be used in import. Only provided for demonstration how
	 * to map ResourceObject to shadow.
	 * 
	 * @param resource
	 * @param objectClass
	 * @param handler
	 * @param parentResult
	 * @throws CommunicationException
	 * @throws ObjectNotFoundException
	 *             the connector object was not found
	 */
	public void listShadows(final ResourceType resource, final QName objectClass,
			final ShadowHandler handler, final boolean readFromRepository, final OperationResult parentResult)
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
				if (readFromRepository) {
					// Attached shadow (with OID)
					try {
						shadow = lookupShadow(object, parentResult);
					} catch (SchemaException e) {
						// TODO: better error handling
						LOGGER.error("Schema exception in resource object search on {} for {}: {}",new Object[]{ObjectTypeUtil.toShortString(resource),objectClass,e.getMessage(),e});
						return false;
					}
				} else {
					// Detached shadow (without OID)
					try {
						shadow = assembleShadow(object, null, parentResult);
					} catch (SchemaException e) {
						// TODO: better error handling
						LOGGER.error("Schema exception in resource object search on {} for {}: {}",new Object[]{ObjectTypeUtil.toShortString(resource),objectClass,e.getMessage(),e});
						return false;
					}
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

	public String addShadow(ResourceObjectShadowType shadow, ScriptsType scripts,
			ResourceType resource, OperationResult parentResult)
			throws CommunicationException, GenericFrameworkException,
			ObjectAlreadyExistsException, SchemaException,
			ObjectNotFoundException {

		Validate.notNull(shadow, "Object to add must not be null.");

		LOGGER.debug("Start adding shadow object {}.",
				JAXBUtil.silentMarshalWrap(shadow));

		if (resource == null) {
			resource = getResource(
					ResourceObjectShadowUtil
							.getResourceOid(shadow),
					parentResult);
		}

		ConnectorInstance connector = getConnectorInstance(resource,
				parentResult);
		Schema schema = getResourceSchema(resource, connector, parentResult);

		// convert xml attributes to ResourceObject
		ResourceObject resourceObject = convertResourceObjectFromXml(
				shadow, schema, parentResult);

		Set<ResourceObjectAttribute> resourceAttributes = null;
		// add object using connector, setting new properties to the
		// resourceObject

		try {
			Set<Operation> scriptOperations = null;

			scriptOperations = createExecuteScriptOperation(
					OperationTypeType.ADD, scripts, parentResult);

			resourceAttributes = connector.addObject(resourceObject,
					scriptOperations, parentResult);

			LOGGER.debug("Added object: {}",
					DebugUtil.prettyPrint(resourceAttributes));
			resourceObject.addAll(resourceAttributes);
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

		shadow = createShadow(resourceObject, resource, shadow);

		if (shadow == null) {
			parentResult
					.recordFatalError("Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
			throw new IllegalStateException(
					"Error while creating account shadow object to save in the reposiotory. AccountShadow is null.");
		}
		LOGGER.debug("Adding object with identifiers to the repository.");

		addShadowToRepository(shadow, resourceObject, parentResult);
				
		parentResult.recordSuccess();
		return shadow.getOid();

	}

	private Set<Operation> createExecuteScriptOperation(OperationTypeType type,
			ScriptsType scripts, OperationResult result) {
		if (scripts == null) {
			result.recordWarning("Skiping creating script operation to execute. Scripts was not defined.");
			return null;
		}
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

			executeScriptOperations = createExecuteScriptOperation(
					OperationTypeType.DELETE, scripts, parentResult);

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
				getRepositoryService().deleteObject(AccountShadowType.class, accountShadow.getOid(),
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
			Set<ResourceObjectAttribute> identifiers = rod.parseIdentifiers(accountType.getAttributes().getAny());

			Set<Operation> executeScriptOperation = null;

			executeScriptOperation = createExecuteScriptOperation(
					OperationTypeType.MODIFY, scripts, parentResult);

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
				.createSubresult(ConnectorTestOperation.CONNECTOR_INITIALIZATION
						.getOperation());
		ConnectorInstance connector;
		try {

			connector = getConnectorInstance(resourceType, initResult);
			initResult.recordSuccess();
		} catch (ObjectNotFoundException e) {
			// The connector was not found. The resource definition is either
			// wrong or the connector is not
			// installed.
			initResult.recordFatalError("The connector was not found", e);
			return;
		} catch (SchemaException e) {
			initResult.recordFatalError("Schema error while dealing with the connector definition", e);
			return;
		} catch (RuntimeException e) {
			initResult.recordFatalError("Unexpected runtime error",e);
			return;
		} catch (CommunicationException e) {
			initResult.recordFatalError("Communication error",e);
			return;
		}
		LOGGER.debug("Testing connection to the resource with oid {}",
				resourceType.getOid());
		
		// === test CONFIGURATION ===
		
		OperationResult configResult = parentResult
		.createSubresult(ConnectorTestOperation.CONFIGURATION_VALIDATION
				.getOperation());

		try {
			connector.configure(resourceType.getConfiguration(),configResult);
			configResult.recordSuccess();
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			configResult.recordFatalError("Communication error",e);
			return;
		} catch (GenericFrameworkException e) {
			configResult.recordFatalError("Generic error",e);
			return;
		} catch (SchemaException e) {
			configResult.recordFatalError("Schema error",e);
			return;
		} catch (RuntimeException e) {
			configResult.recordFatalError("Unexpected runtime error",e);
			return;
		}
		
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
								"Shadow object (in repo) corresponding to the resource object (on the resource) was not found. The repo shadow will be created. The resource object:\n{}",
								DebugUtil.prettyPrint(object));

						// TODO: make sure that the resource object has
						// appropriate definition
						// (use objectClass and schema)

						// The resource object obviously exists on the resource,
						// but appropriate shadow does not exist in the
						// repository
						// we need to create the shadow to align repo state to
						// the reality (resource)
						shadow = createShadow(object, resourceType, null);
						try {
							addShadowToRepository(shadow, object, parentResult);
						} catch (ObjectAlreadyExistsException e) {
							// This should not happen. We haven't supplied an OID so is should not conflict
							LOGGER.error("Unexpected repository behavior: Object already exists: {}",e.getMessage(),e);
							// but still go on ...
						}

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

				} catch (SchemaException e) {
					// TODO: better error handling
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
				ResourceObjectShadowType newShadow = findOrCreateShadowFromChange(connector,
						resourceType, change, parentResult);
				change.setOldShadow(newShadow);
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

	private ResourceObjectShadowType findOrCreateShadowFromChange(ConnectorInstance connector,
			ResourceType resource, Change change, OperationResult parentResult)
			throws SchemaException, ObjectNotFoundException,
			CommunicationException, GenericFrameworkException {

		List<AccountShadowType> accountList = searchAccountByUid(
				change.getIdentifiers(), parentResult);

		if (accountList.size() > 1) {
			parentResult
					.recordFatalError("Found more than one account with the identifier "
							+ change.getIdentifiers() + ".");
			throw new IllegalArgumentException(
					"Found more than one account with the identifier "
							+ change.getIdentifiers() + ".");
		}

		ResourceObjectShadowType newShadow = null;
		// if object doesn't exist, create it now
		if (accountList.isEmpty()) {

			if (!(change.getChange() instanceof ObjectChangeDeletionType)) {
				newShadow = createNewAccountFromChange(change, resource, connector,
						parentResult);
				LOGGER.debug("Create account shadow object: {}",
						ObjectTypeUtil.toShortString(newShadow));
			}
			// if exist, set the old shadow to the change
		} else {
			newShadow = accountList.get(0);
			// if the fetched change was one of the deletion type, delete
			// corresponding account from repo now
			if (change.getChange() instanceof ObjectChangeDeletionType) {
				try {
					getRepositoryService().deleteObject(AccountShadowType.class, 
							newShadow.getOid(), parentResult);
				} catch (ObjectNotFoundException ex) {
					parentResult.recordFatalError(
							"Object with oid " + newShadow.getOid()
									+ " not found in repo. Reason: "
									+ ex.getMessage(), ex);
					throw new ObjectNotFoundException("Object with oid "
							+ newShadow.getOid()
							+ " not found in repo. Reason: "
							+ ex.getMessage(), ex);
				}
			}
		}

		return newShadow;
	}

	private List<AccountShadowType> searchAccountByUid(Set<Property> identifiers,
			OperationResult parentResult) throws SchemaException {
		XPathSegment xpathSegment = new XPathSegment(
				SchemaConstants.I_ATTRIBUTES);
		Document doc = DOMUtil.getDocument();
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathHolder xpath = new XPathHolder(xpathSegments);
		List<Object> values = new ArrayList<Object>();
		for (Property identifier : identifiers) {
			values.addAll(identifier.serializeToJaxb(doc));
		}
		Element filter;
		try {
			filter = QueryUtil.createEqualFilter(doc, xpath, values);
		} catch (SchemaException e) {
			parentResult.recordFatalError(e);
			throw e;
		}

		QueryType query = new QueryType();
		query.setFilter(filter);

		List<AccountShadowType> accountList = null;
		try {
			accountList = getRepositoryService().searchObjects(AccountShadowType.class,query,
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

	private ResourceObjectShadowType createNewAccountFromChange(Change change,
			ResourceType resource, ConnectorInstance connector,
			OperationResult parentResult) throws SchemaException,
			ObjectNotFoundException, CommunicationException,
			GenericFrameworkException {

		ResourceObject resourceObject = fetchResourceObject(
				change.getIdentifiers(), connector, resource, parentResult);
		
		ResourceObjectShadowType shadow = null;
		try {
			shadow = createShadow(
					resourceObject, resource, null);
		} catch (SchemaException ex) {
			parentResult
					.recordFatalError("Can't create account shadow from identifiers: "
							+ change.getIdentifiers());
			throw new SchemaException(
					"Can't create account shadow from identifiers: "
							+ change.getIdentifiers());
		}
		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(resource.getOid());
		shadow.setResourceRef(ref);

		QName objectClass = new QName(resource.getNamespace(),
				"AccountObjectClass");
		shadow.setObjectClass(objectClass);

		// set name for new account
		String accountName = determineShadowName(resourceObject);
		shadow.setName(resource.getName() + "-" + accountName);

		try {
			getRepositoryService().addObject(shadow, parentResult);
		} catch (ObjectAlreadyExistsException e) {
			parentResult.recordFatalError(
					"Can't add account " + DebugUtil.prettyPrint(shadow)
							+ " to the repository. Reason: " + e.getMessage(),
					e);
			throw new IllegalStateException(e.getMessage(), e);
		}

		return shadow;
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
			throws SchemaException {

		QueryType query = createSearchShadowQuery(resourceObject);
		PagingType paging = new PagingType();

		// TODO: check for errors
		List<ResourceObjectShadowType> results;

		results = getRepositoryService().searchObjects(ResourceObjectShadowType.class, query, paging, parentResult);

		if (results.size() == 0) {
			return null;
		}
		if (results.size() > 1) {
			// TODO: Better error handling later
			throw new IllegalStateException("More than one shadows found for "
					+ resourceObject);
		}

		return results.get(0);
	}

	private QueryType createSearchShadowQuery(ResourceObject resourceObject)
			throws SchemaException {

		// We are going to query for attributes, so setup appropriate
		// XPath for the filter
		XPathSegment xpathSegment = new XPathSegment(
				SchemaConstants.I_ATTRIBUTES);
		List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
		xpathSegments.add(xpathSegment);
		XPathHolder xpath = new XPathHolder(xpathSegments);

		// Now we need to determine what is the identifier and set correct
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
		Element filter;
		try {
			filter = QueryUtil.createEqualFilter(doc, xpath,
					identifier.serializeToJaxb(doc));
		} catch (SchemaException e) {
			LOGGER.error("Schema error while creating search filter: {}",e.getMessage(),e);
			throw new SchemaException("Schema error while creating search filter: "+e.getMessage(),e);
		}

		QueryType query = new QueryType();
		query.setFilter(filter);

		System.out.println("created query " + DOMUtil.printDom(filter));

		return query;
	}

	// UTILITY METHODS

	private Schema getResourceSchema(ResourceType resource,
			ConnectorInstance connector, OperationResult parentResult)
			throws CommunicationException, SchemaException {
		
		Schema schema = null;
		try {
			
			// Make sure that the schema is retrieved from the resource
			// this will also retrieve the schema from cache and/or parse it if needed
			ResourceType completeResource = completeResource(resource, null, parentResult);
			schema = ResourceTypeUtil.getResourceSchema(completeResource);
			
		} catch (SchemaException e) {
			parentResult.recordFatalError("Unable to parse resource schema: "
							+ e.getMessage(), e);
			throw new SchemaException("Unable to parse resource schema: "
					+ e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			// this really should not happen
			parentResult.recordFatalError("Unexpected ObjectNotFoundException: "
					+ e.getMessage(), e);
			throw new SystemException("Unexpected ObjectNotFoundException: "
					+ e.getMessage(), e);
		}
		
		checkSchema(schema);

		return schema;
	}

	/**
	 * Schema sanity check
	 * @throws SchemaException 
	 */
	private void checkSchema(Schema schema) throws SchemaException {
		// This is resource schema, it should contain only ResourceObjectDefintions
		for (Definition def : schema.getDefinitions()) {
			if (def instanceof ComplexTypeDefinition) {
				// This is OK
			} else if (def instanceof ResourceObjectDefinition) {
				checkResourceObjectDefinition((ResourceObjectDefinition)def);
			} else {
				throw new SchemaException("Unexpected definition in resource schema: "+def);
			}
		}
	}

	/**
	 * Definition satinty check
	 * @throws SchemaException 
	 */
	private void checkResourceObjectDefinition(ResourceObjectDefinition rod) throws SchemaException {
		for (ItemDefinition def : rod.getDefinitions()) {
			if (!(def instanceof ResourceObjectAttributeDefinition)) {
				throw new SchemaException("Unexpected definition in resource schema object "+rod+": "+def);
			}
		}
	}

	private ResourceType getResource(String oid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		// TODO: add some caching
		return getRepositoryService().getObject(ResourceType.class, oid, null,
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
	private ResourceObject convertResourceObjectFromXml(
			ResourceObjectShadowType resourceObjectShadow, Schema schema,
			OperationResult parentResult) throws SchemaException {
		QName objectClass = resourceObjectShadow.getObjectClass();

		Validate.notNull(objectClass, "Object class must not be null.");
		Validate.notNull(schema, "Resource schema must not be null.");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow before conversion:\n{}",ObjectTypeUtil.dump(resourceObjectShadow));
		}
		
		ResourceObjectDefinition rod = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(objectClass);
		
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow resource object definition:\n{}",rod.dump());
		}
		
		if (rod == null) {
			parentResult.recordFatalError("Schema definition for object class "
					+ objectClass + " was not found");
			throw new SchemaException("Schema definition for object class "
					+ objectClass + " was not found");
		}
		ResourceObject resourceObject = rod.instantiate();

		List<Object> attributes = resourceObjectShadow.getAttributes()
				.getAny();

		if (attributes == null) {
			throw new IllegalArgumentException(
					"Attributes for the account was not defined.");
		}

		Set<ResourceObjectAttribute> resAttr = rod.parseAttributes(attributes);
		resourceObject.addAll(resAttr);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Shadow converted to resource object:\n{}",resourceObject.dump());
		}
		
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
	private ResourceObjectShadowType createShadow(Set<Property> identifiers,
			ResourceObjectShadowType resourceObjectShadow)
			throws SchemaException {

		List<Object> identifierElements = new ArrayList<Object>();
		Document doc = DOMUtil.getDocument();
		for (Property p : identifiers) {
			try {
				List<Object> eList = p.serializeToJaxb(doc);
				identifierElements.addAll(eList);
			} catch (SchemaException e) {
				throw new SchemaException(
						"An error occured while serializing property " + p
								+ " to DOM: "+e.getMessage(),e);
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
	private ResourceObjectShadowType createShadow(ResourceObject resourceObject, ResourceType resource, ResourceObjectShadowType shadow) throws SchemaException {

		if (shadow == null) {
			// Determine correct type for the shadow
			if (resourceObject.isAccountType()) {
				shadow = new AccountShadowType();
			} else {
				shadow = new ResourceObjectShadowType();
			}
		}

		if (shadow.getObjectClass()==null) {
			shadow.setObjectClass(resourceObject.getDefinition().getTypeName());
		}
		if (shadow.getName()==null) {
			shadow.setName(determineShadowName(resourceObject));
		}
		if (shadow.getResource()==null) {
			shadow.setResourceRef(ObjectTypeUtil.createObjectRef(resource));
		}
		if (shadow.getAttributes()==null) {
			Attributes attributes = new Attributes();
			shadow.setAttributes(attributes);
		}
		
		Document doc = DOMUtil.getDocument();
		
		// Add all attributes to the shadow
		shadow.getAttributes().getAny().clear();
		for (ResourceObjectAttribute attr : resourceObject.getAttributes()) {
			try {
				List<Object> eList = attr.serializeToJaxb(doc);
				shadow.getAttributes().getAny().addAll(eList);
			} catch (SchemaException e) {
				throw new SchemaException(
						"An error occured while serializing attribute " + attr
								+ " to DOM: "+e.getMessage(),e);
			}
		}
		
		return shadow;
	}
	
	/*
	 * 
	 * Stores only the attributes that need to go to the repository
	 * 
	 * ResoureObject is needed to determine the schema and identifier values.
	 * 
	 * The OID will be set back to the shadow
	 * 
	 */
	private void addShadowToRepository(ResourceObjectShadowType shadow, ResourceObject resourceObject, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException {

		// Replace original attributes with an empty set
		Attributes origAttrs = shadow.getAttributes();
		Attributes repoAttrs = new Attributes();
		shadow.setAttributes(repoAttrs);
		
		Document doc = DOMUtil.getDocument();

		// Add identifiers to the shadow
		Set<Property> identifiers = resourceObject.getIdentifiers();
		for (Property p : identifiers) {
			try {
				List<Object> eList = p.serializeToJaxb(doc);
				shadow.getAttributes().getAny().addAll(eList);
			} catch (SchemaException e) {
				throw new SchemaException(
						"An error occured while serializing property " + p
								+ " to DOM: "+e.getMessage(),e);
			}
		}

		// Store shadow in the repository
		String oid = null;
		try {

			oid = getRepositoryService().addObject(shadow, parentResult);

		} catch (ObjectAlreadyExistsException ex) {
			// This should not happen. The OID is not supplied and it is
			// generated by the repo
			// If it happens, it must be a repo bug. Therefore it is safe to
			// convert to runtime exception
			parentResult.recordFatalError("Can't add shadow object to the repository. Shadow object already exist. Reason: "
							+ ex.getMessage(), ex);
			throw new ObjectAlreadyExistsException("Can't add shadow object to the repository. Shadow object already exist. Reason: "
							+ ex.getMessage(), ex);
		}
		shadow.setOid(oid);
		shadow.setAttributes(origAttrs);
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
			ObjectModificationType objectChange, ResourceObjectDefinition rod) throws SchemaException {
		Set<Operation> changes = new HashSet<Operation>();
		for (PropertyModificationType modification : objectChange
				.getPropertyModification()) {

			if (modification.getPath() == null) {
				throw new IllegalArgumentException(
						"Path to modificated attributes is null.");
			}

			if (modification.getPath().getTextContent()
					.contains(SchemaConstants.I_ATTRIBUTES.getLocalPart())) {

				Set<ResourceObjectAttribute> changedProperties = rod
							.parseAttributes(modification.getValue().getAny());
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
		XPathHolder xpath = new XPathHolder(xpathSegments);
		List<Object> values = new ArrayList<Object>();
		try {
			for (Property identifier : identifiers) {
				values.addAll(identifier.serializeToJaxb(doc));
			}
		} catch (SchemaException ex) {
			throw new SchemaException(
					"Error serializing identifiers to dom. Reason: "
							+ ex.getMessage(), ex);
		}
		Element filter = QueryUtil.createEqualFilter(doc, xpath, values);

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
	 *            
	 * @return completed resource
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
		Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaType);

		ResourceType newResource = null;
		
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
				
			} catch (SchemaException e) {
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
			xmlSchemaType.setCachingMetadata(MiscUtil.generateCachingMetadata());
			
			ObjectModificationType objectModificationType = ObjectTypeUtil
					.createModificationReplaceProperty(resource.getOid(),
							SchemaConstants.I_SCHEMA, xmlSchemaType);
			
			repositoryService.modifyObject(resource.getClass(), objectModificationType, result);
			
			newResource = resourceSchemaCache.put(resource);
		}
		
		if (newResource == null) {
			// try to fetch schema from cache
			newResource = resourceSchemaCache.get(resource);
		}

		return newResource;
	}

}
