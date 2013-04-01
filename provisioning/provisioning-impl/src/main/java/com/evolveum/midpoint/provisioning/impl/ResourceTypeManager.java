package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;

@Component
public class ResourceTypeManager {

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ResourceSchemaCache resourceSchemaCache;
	@Autowired(required = true)
	private ConnectorTypeManager connectorTypeManager;
	@Autowired(required = true)
	private PrismContext prismContext;

//	private PrismObjectDefinition<ResourceType> resourceTypeDefinition = null;

	private static final Trace LOGGER = TraceManager.getTrace(ResourceTypeManager.class);

	
	public ResourceTypeManager() {
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
	 * @param parentResult
	 * 
	 * @return completed resource
	 * @throws ObjectNotFoundException
	 *             connector instance was not found
	 * @throws SchemaException
	 * @throws CommunicationException
	 *             cannot fetch resource schema
	 * @throws ConfigurationException
	 */
	public ResourceType completeResource(ResourceType resource, ResourceSchema resourceSchema,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException {

		// do not add as a subresult..it will be added later, if the completing
		// of resource will be successfull.if not, it will be only set as a
		// fetch result in the resource..
		OperationResult result = parentResult.createMinorSubresult(ResourceTypeManager.class.getName() + ".completeResource");
		applyConnectorSchemaToResource(resource, result);

		// Check presence of a schema
		XmlSchemaType xmlSchemaType = resource.getSchema();
		if (xmlSchemaType == null) {
			xmlSchemaType = new XmlSchemaType();
			resource.setSchema(xmlSchemaType);
		}
		Element xsdElement = ResourceTypeUtil.getResourceXsdSchema(resource);

		ResourceType newResource = null;
		ConnectorInstance connector = null;
		try {
			connector = getConnectorInstance(resource, false, result);
		} catch (ObjectNotFoundException e) {
			String message = "Error resolving connector reference in " + resource
								+ ": Error creating connector instace: " + e.getMessage();
			// Catch the exceptions. There are not critical. We need to catch them all because the connector may
			// throw even undocumented runtime exceptions.
			// Even non-complete resource may still be usable. The fetchResult indicates that there was an error
			result.recordPartialError(message, e);
			return resource;
		}

		if (xsdElement == null) {
			// There is no schema, we need to pull it from the resource
			
			completeSchema(resource, resourceSchema, connector, result);

			newResource = resourceSchemaCache.put(resource);
		}

		if (newResource == null) {
			// try to fetch schema from cache
			newResource = resourceSchemaCache.get(resource);
		}

		try {

			completeCapabilities(newResource, connector, result);
			
		} catch (Exception ex) {
			// Catch the exceptions. There are not critical. We need to catch them all because the connector may
			// throw even undocumented runtime exceptions.
			// Even non-complete resource may still be usable. The fetchResult indicates that there was an error
			result.recordPartialError("Cannot add native capabilities to resource object. Resource object returned without native capabilities. Cause: "+ex.getMessage(), ex);
		}
		result.recordSuccessIfUnknown();

		parentResult.recordSuccess();
		return newResource;
	}

	private void completeSchema(ResourceType resource, ResourceSchema resourceSchema,
			ConnectorInstance connector, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (resourceSchema == null) { // unless it has been already pulled
			LOGGER.trace("Fetching resource schema for " + ObjectTypeUtil.toShortString(resource));
			
			try {
				// Fetch schema from connector, UCF will convert it to
				// Schema Processor format and add all necessary annotations
				resourceSchema = connector.getResourceSchema(result);

			} catch (Exception ex) {
				// Catch the exceptions. There are not critical. We need to catch them all because the connector may
				// throw even undocumented runtime exceptions.
				
				LOGGER.error("Unable to complete {}: {}", new Object[]{resource, ex.getMessage(), ex});
				
				// Even non-complete resource may still be usable. The result indicates that there was an error
				result.recordPartialError("Cannot add native capabilities to resource object. Resource object returned without native capabilities. Cause: "+ex.getMessage(), ex);
				
				// Otherwise ignore the error. The resource is not complete but the upper layer code should deal with that
				// Throwing an error will effectively break any operation with the resource (including delete).
				return;
			}
			if (resourceSchema == null) {
				LOGGER.warn("No resource schema generated for {}", resource);
			} else {
				LOGGER.debug("Generated resource schema for " + ObjectTypeUtil.toShortString(resource) + ": "
					+ resourceSchema.getDefinitions().size() + " definitions");
			}
		}
		
		if (resourceSchema == null) {
			// No not even bother to put this in the cache
			return;
		}

		adjustSchemaForCapabilities(resource, resourceSchema);

		Document xsdDoc = null;
		try {
			// Convert to XSD
			LOGGER.trace("Serializing XSD resource schema for {} to DOM",
					ObjectTypeUtil.toShortString(resource));

			xsdDoc = resourceSchema.serializeToXsd();

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Serialized XSD resource schema for {}:\n{}",
						ObjectTypeUtil.toShortString(resource), DOMUtil.serializeDOMToString(xsdDoc));
			}

		} catch (SchemaException e) {
			throw new SchemaException("Error processing resource schema for "
					+ ObjectTypeUtil.toShortString(resource) + ": " + e.getMessage(), e);
		}

		Element xsdElement = DOMUtil.getFirstChildElement(xsdDoc);
		if (xsdElement == null) {
			throw new SchemaException("No schema was generated for " + resource);
		}
		CachingMetadataType cachingMetadata = MiscSchemaUtil.generateCachingMetadata();

		// Store generated schema into repository (modify the original
		// Resource)
		LOGGER.info("Storing generated schema in resource " + ObjectTypeUtil.toShortString(resource));

		ContainerDelta<XmlSchemaType> schemaContainerDelta = ContainerDelta.createDelta(prismContext,
				ResourceType.class, ResourceType.F_SCHEMA);
		PrismContainerValue<XmlSchemaType> cval = new PrismContainerValue<XmlSchemaType>();
		schemaContainerDelta.setValueToReplace(cval);
		PrismProperty<CachingMetadataType> cachingMetadataProperty = cval
				.createProperty(XmlSchemaType.F_CACHING_METADATA);
		cachingMetadataProperty.setRealValue(cachingMetadata);
		PrismProperty<Element> definitionProperty = cval.createProperty(XmlSchemaType.F_DEFINITION);
		ObjectTypeUtil.setXsdSchemaDefinition(definitionProperty, xsdElement);

		Collection<ItemDelta<?>> modifications = new ArrayList<ItemDelta<?>>(1);
		modifications.add(schemaContainerDelta);

        try {
			repositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }

		// Store generated schema into the resource (this will be kept in
		// the in-memory cache)
		ResourceTypeUtil.setResourceXsdSchema(resource, xsdElement);
		resource.getSchema().setCachingMetadata(cachingMetadata);
		// Note: do not switch order of the operations. Storing schema in
		// repo must happend first. The same
		// DOM element is used here. Its ownership must remain with the
		// in-memory resource

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Putting resource in cache:\n{}", resource.asPrismObject().dump());
			LOGGER.trace("Schema:\n{}",
					DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(resource)));
		}
	}
	
	/**
	 * Add native capabilities to the provided resource. The native capabilities
	 * are either reused from the cache or retrieved from the resource. Also make sure that the
	 * capabilities are cached in the repo.
	 */
	private void completeCapabilities(ResourceType resource, ConnectorInstance connector,
			OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException {

		// This is not really clean now. We need to add caching metadata and
		// things like that
		// FIXME
		if (resource.getCapabilities() != null && resource.getCapabilities().getNative() != null) {
			return;
		}

		Collection<Object> capabilities = null;
		try {

			if (connector == null) {
				connector = getConnectorInstance(resource, false, result);
			}
			capabilities = connector.getCapabilities(result);

		} catch (CommunicationException ex) {
			throw new CommunicationException("Cannot fetch resource native capabilities: " + ex.getMessage(),
					ex);
		} catch (GenericFrameworkException ex) {
			throw new GenericConnectorException("Generic error in connector " + connector + ": "
					+ ex.getMessage(), ex);
		} catch (ConfigurationException ex) {
			throw new GenericConnectorException("Configuration error in connector " + connector + ": "
					+ ex.getMessage(), ex);
		}
		
		CapabilitiesType capType = resource.getCapabilities();
		if (capType == null) {
			capType = new CapabilitiesType();
			resource.setCapabilities(capType);
		}
		
		if (capabilities != null) {
			CapabilityCollectionType nativeCapType = new CapabilityCollectionType();
			capType.setNative(nativeCapType);
			nativeCapType.getAny().addAll(capabilities);
		}
		CachingMetadataType cachingMetadata = MiscSchemaUtil.generateCachingMetadata();
		capType.setCachingMetadata(cachingMetadata);
		
		// Make sure this is stored in repo
		ObjectDelta<ResourceType> capabilitiesReplaceDelta = ObjectDelta.createModificationReplaceProperty(ResourceType.class, resource.getOid(), 
				ResourceType.F_CAPABILITIES, prismContext, capType);
		
        try {
			repositoryService.modifyObject(ResourceType.class, resource.getOid(), capabilitiesReplaceDelta.getModifications(), result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
		
	}

	/**
	 * Apply proper definition (connector schema) to the resource.
	 */
	private void applyConnectorSchemaToResource(ResourceType resource, OperationResult result)
			throws SchemaException, ObjectNotFoundException {
		
		ConnectorType connectorType = connectorTypeManager.getConnectorType(resource, result);
		PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefintion = ConnectorTypeUtil
				.findConfigurationContainerDefintion(connectorType, prismContext);
		if (configurationContainerDefintion == null) {
			throw new SchemaException("No configuration container definition in " + connectorType);
		}
		PrismContainer<Containerable> configurationContainer = ResourceTypeUtil
				.getConfigurationContainer(resource);
		if (configurationContainer == null) {
			throw new SchemaException("No configuration container in " + resource);
		}
		configurationContainer.applyDefinition(configurationContainerDefintion, true);
	}

	public void testConnection(ResourceType resourceType, OperationResult parentResult) {

		// === test INITIALIZATION ===

		OperationResult initResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_INITIALIZATION.getOperation());
		ConnectorInstance connector;
		try {

			connector = getConnectorInstance(resourceType, true, initResult);
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
			initResult.recordFatalError("Unexpected runtime error", e);
			return;
		} catch (CommunicationException e) {
			initResult.recordFatalError("Communication error", e);
			return;
		} catch (ConfigurationException e) {
			initResult.recordFatalError("Configuration error", e);
			return;
		}
		LOGGER.debug("Testing connection to the resource with oid {}", resourceType.getOid());

		// === test CONFIGURATION ===

		OperationResult configResult = parentResult
				.createSubresult(ConnectorTestOperation.CONFIGURATION_VALIDATION.getOperation());

		try {
			connector.configure(resourceType.asPrismObject().findContainer(ResourceType.F_CONNECTOR_CONFIGURATION)
					.getValue(), configResult);
			configResult.recordSuccess();
		} catch (CommunicationException e) {
			configResult.recordFatalError("Communication error", e);
			return;
		} catch (GenericFrameworkException e) {
			configResult.recordFatalError("Generic error", e);
			return;
		} catch (SchemaException e) {
			configResult.recordFatalError("Schema error", e);
			return;
		} catch (ConfigurationException e) {
			configResult.recordFatalError("Configuration error", e);
			return;
		} catch (RuntimeException e) {
			configResult.recordFatalError("Unexpected runtime error", e);
			return;
		}

		// === test CONNECTION ===

		// delegate the main part of the test to the connector
		connector.test(parentResult);

		parentResult.computeStatus();
		if (!parentResult.isAcceptable()) {
			modifyResourceAvailabilityStatus(resourceType, AvailabilityStatusType.DOWN, parentResult);
			// No point in going on. Following tests will fail anyway, they will
			// just produce misleading
			// messages.
			return;
		} else {
			modifyResourceAvailabilityStatus(resourceType, AvailabilityStatusType.UP, parentResult);
		}

		// === test SCHEMA ===

		OperationResult schemaResult = parentResult.createSubresult(ConnectorTestOperation.CONNECTOR_SCHEMA
				.getOperation());

		ResourceSchema schema = null;
		try {
			// Try to fetch schema from the connector. The UCF will convert it
			// to Schema Processor
			// format, so it is already structured
			schema = connector.getResourceSchema(schemaResult);
		} catch (CommunicationException e) {
			schemaResult.recordFatalError("Communication error: " + e.getMessage(), e);
			return;
		} catch (GenericFrameworkException e) {
			schemaResult.recordFatalError("Generic error: " + e.getMessage(), e);
			return;
		} catch (ConfigurationException e) {
			schemaResult.recordFatalError("Configuration error: " + e.getMessage(), e);
			return;
		}

		if (schema == null || schema.isEmpty()) {
			// Resource does not support schema
			// If there is a static schema in resource definition this may still be OK
			try {
				schema = RefinedResourceSchema.getResourceSchema(resourceType, prismContext);
			} catch (SchemaException e) {
				schemaResult.recordFatalError(e);
				return;
			}
			
			if (schema == null || schema.isEmpty()) {
				schemaResult.recordFatalError("Connector does not support schema and no static schema available");
				return;
			}
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
					"Object not found (unexpected error, probably a bug): " + e.getMessage(), e);
			return;
		} catch (SchemaException e) {
			schemaResult.recordFatalError(
					"Schema processing error (probably connector bug): " + e.getMessage(), e);
			return;
		} catch (CommunicationException e) {
			schemaResult.recordFatalError("Communication error: " + e.getMessage(), e);
			return;
		} catch (ConfigurationException e) {
			schemaResult.recordFatalError("Configuration error: " + e.getMessage(), e);
			return;
		}

		schemaResult.recordSuccess();

		// TODO: connector sanity (e.g. at least one account type, identifiers
		// in schema, etc.)

	}
	
	public void modifyResourceAvailabilityStatus(ResourceType resource, AvailabilityStatusType status, OperationResult result){
			
			if (resource.getOperationalState() == null || resource.getOperationalState().getLastAvailabilityStatus() == null || resource.getOperationalState().getLastAvailabilityStatus() != status) {
				List<PropertyDelta<?>> modifications = new ArrayList<PropertyDelta<?>>();
				PropertyDelta<?> statusDelta = PropertyDelta.createModificationReplaceProperty(OperationalStateType.F_LAST_AVAILABILITY_STATUS, resource.asPrismObject().getDefinition(), status);
				modifications.add(statusDelta);
				statusDelta.setParentPath(new ItemPath(ResourceType.F_OPERATIONAL_STATE));
				
				try{
				repositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
				} catch(SchemaException ex){
					throw new SystemException(ex);
				} catch(ObjectAlreadyExistsException ex){
					throw new SystemException(ex);
				} catch(ObjectNotFoundException ex){
					throw new SystemException(ex);
				}
			}
			if (resource.getOperationalState() == null){
				OperationalStateType operationalState = new OperationalStateType();
				operationalState.setLastAvailabilityStatus(status);
				resource.setOperationalState(operationalState);
			} else{
				resource.getOperationalState().setLastAvailabilityStatus(status);
			}
		}

	/**
	 * Adjust scheme with respect to capabilities. E.g. disable attributes that
	 * are used for special purpose (such as account activation simulation).
	 */
	private void adjustSchemaForCapabilities(ResourceType resource, ResourceSchema resourceSchema) {
		if (resource.getCapabilities() == null || resource.getCapabilities().getConfigured() == null) {
			return;
		}
		ActivationCapabilityType activationCapability = ResourceTypeUtil.getCapability(resource
				.getCapabilities().getConfigured().getAny(), ActivationCapabilityType.class);
		if (activationCapability != null && activationCapability.getEnableDisable() != null) {
			QName attributeName = activationCapability.getEnableDisable().getAttribute();
			if (attributeName != null) {
				// The attribute used for enable/disable simulation should be
				// ignored in the schema
				// otherwise strange things may happen, such as changing the
				// same attribute both from
				// activation/enable and from the attribute using its native
				// name.
				for (ObjectClassComplexTypeDefinition objectClassDefinition : resourceSchema
						.getDefinitions(ObjectClassComplexTypeDefinition.class)) {
					ResourceAttributeDefinition attributeDefinition = objectClassDefinition
							.findAttributeDefinition(attributeName);
					if (attributeDefinition != null) {
						attributeDefinition.setIgnored(true);
					} else {
						// simulated activation attribute points to something
						// that
						// is not in the schema
						// technically, this is an error. But it looks to be
						// quite
						// common in connectors.
						// The enable/disable is using operational attributes
						// that
						// are not exposed in the
						// schema, but they work if passed to the connector.
						// Therefore we don't want to break anything. We could
						// log
						// an warning here, but the
						// warning would be quite frequent. Maybe a better place
						// to
						// warn user would be import
						// of the object.
						LOGGER.debug("Simulated activation attribute "
								+ attributeName
								+ " for objectclass "
								+ objectClassDefinition.getTypeName()
								+ " in "
								+ ObjectTypeUtil.toShortString(resource)
								+ " does not exist in the resource schema. This may work well, but it is not clean. Connector exposing such schema should be fixed.");
					}
				}
			}
		}
	}

	public ResourceSchema getResourceSchema(ResourceType resource, OperationResult parentResult) throws SchemaException, CommunicationException,
			ConfigurationException {

		ResourceSchema schema = null;
		try {

			// Make sure that the schema is retrieved from the resource
			// this will also retrieve the schema from cache and/or parse it if
			// needed
			ResourceType completeResource = completeResource(resource, null, parentResult);
			schema = RefinedResourceSchema.getResourceSchema(completeResource, prismContext);

		} catch (SchemaException e) {
			parentResult.recordFatalError("Unable to parse resource schema: " + e.getMessage(), e);
			throw new SchemaException("Unable to parse resource schema: " + e.getMessage(), e);
		} catch (ObjectNotFoundException e) {
			// this really should not happen
			parentResult.recordFatalError("Unexpected ObjectNotFoundException: " + e.getMessage(), e);
			throw new SystemException("Unexpected ObjectNotFoundException: " + e.getMessage(), e);
		} catch (ConfigurationException e) {
			parentResult.recordFatalError("Unable to parse resource schema: " + e.getMessage(), e);
			throw new ConfigurationException("Unable to parse resource schema: " + e.getMessage(), e);
		}
		
		if (schema == null) {
			return null;
		}

		checkSchema(schema);

		return schema;
	}

	private void checkSchema(PrismSchema schema) throws SchemaException {
		// This is resource schema, it should contain only
		// ResourceObjectDefintions
		for (Definition def : schema.getDefinitions()) {
			if (def instanceof ComplexTypeDefinition) {
				// This is OK
			} else if (def instanceof ResourceAttributeContainerDefinition) {
				checkResourceObjectDefinition((ResourceAttributeContainerDefinition) def);
			} else {
				throw new SchemaException("Unexpected definition in resource schema: " + def);
			}
		}
	}

	private void checkResourceObjectDefinition(ResourceAttributeContainerDefinition rod)
			throws SchemaException {
		for (ItemDefinition def : rod.getDefinitions()) {
			if (!(def instanceof ResourceAttributeDefinition)) {
				throw new SchemaException("Unexpected definition in resource schema object " + rod + ": "
						+ def);
			}
		}
	}

	private ConnectorInstance getConnectorInstance(ResourceType resource, boolean forceFresh, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		return connectorTypeManager.getConfiguredConnectorInstance(resource, forceFresh, parentResult);
	}

	public void applyDefinition(ObjectDelta<ResourceType> delta, OperationResult objectResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		
		if (delta.isAdd()) {
			PrismObject<ResourceType> resource = delta.getObjectToAdd();
			applyConnectorSchemaToResource(resource.asObjectable(), objectResult);
			return;
			
		} else if (delta.isModify()) {
			// Go on
			
		} else {
			return;
		}
		
		String resourceOid = delta.getOid();
		Validate.notNull(resourceOid, "Resource oid not specified in the object delta. Could not apply definition.");
		PrismObject<ResourceType>resource = getResource(resourceOid, objectResult);
		ResourceType resourceType = resource.asObjectable();
//		ResourceType resourceType = completeResource(resource.asObjectable(), null, objectResult);
		//TODO TODO TODO FIXME FIXME FIXME copied from ObjectImprted..union this two cases
		PrismContainer<Containerable> configurationContainer = ResourceTypeUtil.getConfigurationContainer(resourceType);
        if (configurationContainer == null || configurationContainer.isEmpty()) {
            // Nothing to check
            objectResult.recordWarning("The resource has no configuration");
            return;
        }

        // Check the resource configuration. The schema is in connector, so fetch the connector first
        String connectorOid = resourceType.getConnectorRef().getOid();
        if (StringUtils.isBlank(connectorOid)) {
            objectResult.recordFatalError("The connector reference (connectorRef) is null or empty");
            return;
        }

        PrismObject<ConnectorType> connector = null;
        ConnectorType connectorType = null;
        try {
            connector = getRepositoryService().getObject(ConnectorType.class, connectorOid, objectResult);
            connectorType = connector.asObjectable();
        } catch (ObjectNotFoundException e) {
            // No connector, no fun. We can't check the schema. But this is referential integrity problem.
            // Mark the error ... there is nothing more to do
            objectResult.recordFatalError("Connector (OID:" + connectorOid + ") referenced from the resource is not in the repository", e);
            return;
        } catch (SchemaException e) {
            // Probably a malformed connector. To be kind of robust, lets allow the import.
            // Mark the error ... there is nothing more to do
            objectResult.recordPartialError("Connector (OID:" + connectorOid + ") referenced from the resource has schema problems: " + e.getMessage(), e);
            LOGGER.error("Connector (OID:{}) referenced from the imported resource \"{}\" has schema problems: {}", new Object[]{connectorOid, resourceType.getName(), e.getMessage(), e});
            return;
        }
        
        Element connectorSchemaElement = ConnectorTypeUtil.getConnectorXsdSchema(connector);
        PrismSchema connectorSchema = null;
        if (connectorSchemaElement == null) {
        	// No schema to validate with
        	return;
        }
		try {
			connectorSchema = PrismSchema.parse(connectorSchemaElement, "schema for " + connector, prismContext);
		} catch (SchemaException e) {
			objectResult.recordFatalError("Error parsing connector schema for " + connector + ": "+e.getMessage(), e);
			return;
		}
        QName configContainerQName = new QName(connectorType.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
		PrismContainerDefinition<?> configContainerDef = connectorSchema.findContainerDefinitionByElementName(configContainerQName);
		if (configContainerDef == null) {
			objectResult.recordFatalError("Definition of configuration container " + configContainerQName + " not found in the schema of of " + connector);
            return;
		}
        
        try {
			configurationContainer.applyDefinition(configContainerDef);
		} catch (SchemaException e) {
			objectResult.recordFatalError("Configuration error in " + resource + ": "+e.getMessage(), e);
            return;
		}
     
        resourceType.asPrismObject().findContainer(ResourceType.F_CONNECTOR_CONFIGURATION).applyDefinition(configContainerDef);
 
        for (ItemDelta<?> itemDelta : delta.getModifications()){
        	if (itemDelta.getParentPath() == null){
        		LOGGER.trace("No parent path defined for item delta {}", itemDelta);
        		continue;
        	}
        	
        	QName first = ItemPath.getName(itemDelta.getParentPath().first());
        	
        	if (first == null){
        		continue;
        	}
        	
        	if (itemDelta.getDefinition() == null && (ResourceType.F_CONNECTOR_CONFIGURATION.equals(first) || ResourceType.F_SCHEMA.equals(first))){
        		ItemPath path = itemDelta.getPath().rest();
        		ItemDefinition itemDef = configContainerDef.findItemDefinition(path);
				itemDelta.applyDefinition(itemDef);
        		
        	}
        }
	}

	private PrismObject<ResourceType> getResource(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException{
		PrismObject<ResourceType> prismResource = getRepositoryService().getObject(ResourceType.class, oid, parentResult);
		return prismResource;
	}

	public void applyDefinition(PrismObject<ResourceType> resource, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		applyConnectorSchemaToResource(resource.asObjectable(), parentResult);
	}
}
