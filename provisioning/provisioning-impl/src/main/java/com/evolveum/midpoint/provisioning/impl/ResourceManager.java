/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ExecuteProvisioningScriptOperation;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaGenerationConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;

@Component
public class ResourceManager {

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private ResourceCache resourceCache;
	@Autowired(required = true)
	private ConnectorManager connectorTypeManager;
	@Autowired(required = true)
	private PrismContext prismContext;

	private static final Trace LOGGER = TraceManager.getTrace(ResourceManager.class);
	
	private static final String OPERATION_COMPLETE_RESOURCE = ResourceManager.class.getName() + ".completeResource";
	
	public PrismObject<ResourceType> getResource(PrismObject<ResourceType> repositoryObject, GetOperationOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException{
		InternalMonitor.getResourceCacheStats().recordRequest();
		
		PrismObject<ResourceType> cachedResource = resourceCache.get(repositoryObject, options);
		if (cachedResource != null) {
			InternalMonitor.getResourceCacheStats().recordHit();
			return cachedResource;
		}
		
		LOGGER.debug("Storing fetched resource {}, version {} to cache (previously cached version {})",
				new Object[]{ repositoryObject.getOid(), repositoryObject.getVersion(), resourceCache.getVersion(repositoryObject.getOid())});
		
		return loadAndCacheResource(repositoryObject, options, parentResult);
	}
	
	public PrismObject<ResourceType> getResource(String oid, GetOperationOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException{
		InternalMonitor.getResourceCacheStats().recordRequest();
		
		String version = repositoryService.getVersion(ResourceType.class, oid, parentResult);
		PrismObject<ResourceType> cachedResource = resourceCache.get(oid, version, options);
		if (cachedResource != null) {
			InternalMonitor.getResourceCacheStats().recordHit();
			if (LOGGER.isTraceEnabled()){
				LOGGER.trace("Returning resource from cache:\n{}", cachedResource.debugDump());
			}
			return cachedResource;
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Fetching resource {}, version {}, storing to cache (previously cached version {})", 
					new Object[]{oid, version, resourceCache.getVersion(oid)});
		}
		
		Collection<SelectorOptions<GetOperationOptions>> repoOptions = null;
		if (GetOperationOptions.isReadOnly(options)) {
			repoOptions = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
		}
		PrismObject<ResourceType> repositoryObject = repositoryService.getObject(ResourceType.class, oid, repoOptions, parentResult);
		
		return loadAndCacheResource(repositoryObject, options, parentResult);
	}

	
	private PrismObject<ResourceType> loadAndCacheResource(PrismObject<ResourceType> repositoryObject, 
			GetOperationOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		
		PrismObject<ResourceType> completedResource = completeResource(repositoryObject, null, false, options, parentResult);
		
		if (!isComplete(completedResource)) {
			// No not cache non-complete resources (e.g. those retrieved with noFetch)
			return completedResource; 
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Putting resource in cache:\n{}", completedResource.debugDump());
			Element xsdSchemaElement = ResourceTypeUtil.getResourceXsdSchema(completedResource);
			if (xsdSchemaElement == null) {
				LOGGER.trace("Schema: null");
			} else {
				LOGGER.trace("Schema:\n{}",
						DOMUtil.serializeDOMToString(ResourceTypeUtil.getResourceXsdSchema(completedResource)));
			}
		}
		OperationResult completeResourceResult = parentResult.findSubresult(OPERATION_COMPLETE_RESOURCE);
		if (completeResourceResult.isSuccess()) {
			// Cache only resources that are completely OK
			resourceCache.put(completedResource);
		}
		
		InternalMonitor.getResourceCacheStats().recordMiss();
		return completedResource;
	}
	
	public void deleteResource(String oid, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException {
		resourceCache.remove(oid);
		repositoryService.deleteObject(ResourceType.class, oid, parentResult);
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
	 * @param repoResource
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
	private PrismObject<ResourceType> completeResource(PrismObject<ResourceType> repoResource, ResourceSchema resourceSchema,
			boolean fetchedSchema, GetOperationOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException,
			CommunicationException, ConfigurationException {

		// do not add as a subresult..it will be added later, if the completing
		// of resource will be successfull.if not, it will be only set as a
		// fetch result in the resource..
		OperationResult result = parentResult.createMinorSubresult(OPERATION_COMPLETE_RESOURCE);
		
		try {
			
			applyConnectorSchemaToResource(repoResource, result);
						
		} catch (SchemaException e) {
			String message = "Schema error while applying connector schema to connectorConfiguration section of "+repoResource+": "+e.getMessage();
			result.recordPartialError(message, e);
			LOGGER.warn(message, e);
			return repoResource;
		} catch (ObjectNotFoundException e) {
			String message = "Object not found error while processing connector configuration of "+repoResource+": "+e.getMessage();
			result.recordPartialError(message, e);
			LOGGER.warn(message, e);
			return repoResource;
		} catch (RuntimeException e) {
			String message = "Unexpected error while processing connector configuration of "+repoResource+": "+e.getMessage();
			result.recordPartialError(message, e);
			LOGGER.warn(message, e);
			return repoResource;
		}

		PrismObject<ResourceType> newResource;
		
		if (isComplete(repoResource)) {
			// The resource is complete.
			newResource = repoResource;
			
		} else {
			// The resource is NOT complete. Try to fetch schema and capabilities
			
			if (GetOperationOptions.isNoFetch(options)) {
				// We need to fetch schema, but the noFetch option is specified. Therefore return whatever we have.
				result.recordSuccessIfUnknown();
				return repoResource;
			}
		
			ConnectorInstance connector = null;
			try {
				connector = getConnectorInstance(repoResource, false, result);
			} catch (ObjectNotFoundException e) {
				String message = "Error resolving connector reference in " + repoResource
									+ ": Error creating connector instace: " + e.getMessage();
				// Catch the exceptions. There are not critical. We need to catch them all because the connector may
				// throw even undocumented runtime exceptions.
				// Even non-complete resource may still be usable. The fetchResult indicates that there was an error
				result.recordPartialError(message, e);
				return repoResource;
			} catch (CommunicationException e) {
				// The resource won't work, the connector was not initialized (most likely it cannot reach the schema)
				// But we still want to return the resource object that is (partailly) working. At least for reconfiguration.
				String message = "Error communicating with the " + repoResource
						+ ": " + e.getMessage();
				result.recordPartialError(message, e);
				return repoResource;
			} catch (ConfigurationException e) {
				String message = "Connector configuration error for the " + repoResource
						+ ": " + e.getMessage();
				result.recordPartialError(message, e);
				return repoResource;
			} catch (SchemaException e) {
				String message = "Schema error for the " + repoResource
						+ ": " + e.getMessage();
				result.recordPartialError(message, e);
				return repoResource;
			} catch (RuntimeException e) {
				String message = "Generic connector error for the " + repoResource
						+ ": " + e.getMessage();
				result.recordPartialError(message, e);
				return repoResource;
			}
	
			try {
				
				completeSchemaAndCapabilities(repoResource, resourceSchema, fetchedSchema, connector, result);
				
			} catch (Exception ex) {
				// Catch the exceptions. There are not critical. We need to catch them all because the connector may
				// throw even undocumented runtime exceptions.
				// Even non-complete resource may still be usable. The fetchResult indicates that there was an error
				result.recordPartialError("Cannot complete resource schema and capabilities: "+ex.getMessage(), ex);
				return repoResource;
			}
			
			
			try {
				// Now we need to re-read the resource from the repository and re-aply the schemas. This ensures that we will
				// cache the correct version and that we avoid race conditions, etc.
				
				newResource = repositoryService.getObject(ResourceType.class, repoResource.getOid(), null, result);
				applyConnectorSchemaToResource(newResource, result);
				
			} catch (SchemaException e) {
				result.recordFatalError(e);
				throw e;
			} catch (ObjectNotFoundException e) {
				result.recordFatalError(e);
				throw e;
			} catch (RuntimeException e) {
				result.recordFatalError(e);
				throw e;
			}
			
		}
		
		try {
			// make sure it has parsed resource and refined schema. We are going to cache
			// it, so we want to cache it with the parsed schemas
			RefinedResourceSchemaImpl.getResourceSchema(newResource, prismContext);
			RefinedResourceSchemaImpl.getRefinedSchema(newResource);
			
		} catch (SchemaException e) {
			String message = "Schema error while processing schemaHandling section of "+newResource+": "+e.getMessage();
			result.recordPartialError(message, e);
			LOGGER.warn(message, e);
			return newResource;
		} catch (RuntimeException e) {
			String message = "Unexpected error while processing schemaHandling section of "+newResource+": "+e.getMessage();
			result.recordPartialError(message, e);
			LOGGER.warn(message, e);
			return newResource;
		}
		
		result.recordSuccessIfUnknown();

		return newResource;
	}

	private boolean isComplete(PrismObject<ResourceType> resource) {
		ResourceType resourceType = resource.asObjectable();
		Element xsdSchema = ResourceTypeUtil.getResourceXsdSchema(resource);
		if (xsdSchema == null) {
			return false;
		}
		CapabilitiesType capabilitiesType = resourceType.getCapabilities();
		if (capabilitiesType == null) {
			return false;
		}
		CachingMetadataType capCachingMetadata = capabilitiesType.getCachingMetadata();
		if (capCachingMetadata == null) {
			return false;
		}
		return true;
	}


	private void completeSchemaAndCapabilities(PrismObject<ResourceType> resource, ResourceSchema resourceSchema, boolean fetchedSchema,
			ConnectorInstance connector, OperationResult result) throws SchemaException, CommunicationException, ObjectNotFoundException, GenericFrameworkException, ConfigurationException {
		ResourceType resourceType = resource.asObjectable();

		if (resourceSchema == null) { 
			// Try to get existing schema from resource. We do not want to override this if it exists
			// (but we still want to refresh the capabilities, that happens below)
			resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
		}
			
		if (resourceSchema == null || resourceSchema.isEmpty()) {
		
			LOGGER.trace("Fetching resource schema for {}", resource);
			
			// Fetch schema from connector, UCF will convert it to
			// Schema Processor format and add all necessary annotations
			InternalMonitor.recordResourceSchemaFetch();
			List<QName> generateObjectClasses = ResourceTypeUtil.getSchemaGenerationConstraints(resource);
//			if (resourceType.getSchema() != null && resourceType.getSchema().getGenerationConstraints() != null){
//				generateObjectClasses = resourceType.getSchema().getGenerationConstraints().getGenerateObjectClass();
//			}
			resourceSchema = connector.fetchResourceSchema(generateObjectClasses, result);

			if (resourceSchema == null) {
				LOGGER.warn("No resource schema fetched from {}", resource);
			} else if (resourceSchema.isEmpty()) {
				LOGGER.warn("Empty resource schema fetched from {}", resource);
			} else {
				fetchedSchema = true;
				LOGGER.debug("Fetched resource schema for {}: {} definitions", resource, resourceSchema.getDefinitions().size());
			}
		}

		if (resourceSchema == null) {
			return;
		}
		
		Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
		
		// Schema
		if (fetchedSchema) {
			adjustSchemaForSimulatedCapabilities(resource, resourceSchema);
			ContainerDelta<XmlSchemaType> schemaContainerDelta = createSchemaUpdateDelta(resource, resourceSchema);
			modifications.add(schemaContainerDelta);
		}
		
		
		// Capabilities
		
		Collection<Object> capabilities;
		try {

			InternalMonitor.recordConnectorCapabilitiesFetchCount();
			
			capabilities = connector.fetchCapabilities(result);

		} catch (GenericFrameworkException ex) {
			throw new GenericConnectorException("Generic error in connector " + connector + ": "
					+ ex.getMessage(), ex);
		}
		
		CapabilitiesType capType = resourceType.getCapabilities();
		if (capType == null) {
			capType = new CapabilitiesType();
			resourceType.setCapabilities(capType);
		}
		
		CapabilityCollectionType nativeCapType = new CapabilityCollectionType();
		capType.setNative(nativeCapType);
		nativeCapType.getAny().addAll(capabilities);

		CachingMetadataType cachingMetadata = MiscSchemaUtil.generateCachingMetadata();
		capType.setCachingMetadata(cachingMetadata);
		
		ObjectDelta<ResourceType> capabilitiesReplaceDelta = ObjectDelta.createModificationReplaceContainer(ResourceType.class, resource.getOid(), 
				ResourceType.F_CAPABILITIES, prismContext, capType.asPrismContainerValue().clone());
		
		modifications.addAll(capabilitiesReplaceDelta.getModifications());
		
		if (fetchedSchema) {
			// We have successfully fetched the resource schema. Therefore the resource must be up.
			modifications.add(createResourceAvailabilityStatusDelta(resource, AvailabilityStatusType.UP));
		}

        try {
			repositoryService.modifyObject(ResourceType.class, resource.getOid(), modifications, result);
        } catch (ObjectAlreadyExistsException ex) {
        	// This should not happen
            throw new SystemException(ex);
        }

	}
	
	private ContainerDelta<XmlSchemaType> createSchemaUpdateDelta(PrismObject<ResourceType> resource, ResourceSchema resourceSchema) throws SchemaException {
		Document xsdDoc = null;
		try {
			// Convert to XSD
			LOGGER.trace("Serializing XSD resource schema for {} to DOM", resource);

			xsdDoc = resourceSchema.serializeToXsd();

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Serialized XSD resource schema for {}:\n{}",
						resource, DOMUtil.serializeDOMToString(xsdDoc));
			}

		} catch (SchemaException e) {
			throw new SchemaException("Error processing resource schema for "
					+ resource + ": " + e.getMessage(), e);
		}

		Element xsdElement = DOMUtil.getFirstChildElement(xsdDoc);
		if (xsdElement == null) {
			throw new SchemaException("No schema was generated for " + resource);
		}
		CachingMetadataType cachingMetadata = MiscSchemaUtil.generateCachingMetadata();

		// Store generated schema into repository (modify the original
		// Resource)
		LOGGER.info("Storing generated schema in resource {}", resource);

		ContainerDelta<XmlSchemaType> schemaContainerDelta = ContainerDelta.createDelta(
				ResourceType.F_SCHEMA, ResourceType.class, prismContext);
		PrismContainerValue<XmlSchemaType> cval = new PrismContainerValue<XmlSchemaType>(prismContext);
		schemaContainerDelta.setValueToReplace(cval);
		PrismProperty<CachingMetadataType> cachingMetadataProperty = cval
				.createProperty(XmlSchemaType.F_CACHING_METADATA);
		cachingMetadataProperty.setRealValue(cachingMetadata);
		List<QName> objectClasses = ResourceTypeUtil.getSchemaGenerationConstraints(resource);
		if (objectClasses != null) {
			PrismProperty<SchemaGenerationConstraintsType> generationConstraints = cval
					.createProperty(XmlSchemaType.F_GENERATION_CONSTRAINTS);
			SchemaGenerationConstraintsType constraints = new SchemaGenerationConstraintsType();
			constraints.getGenerateObjectClass().addAll(objectClasses);
			generationConstraints.setRealValue(constraints);
		}
		PrismProperty<SchemaDefinitionType> definitionProperty = cval.createProperty(XmlSchemaType.F_DEFINITION);
		ObjectTypeUtil.setXsdSchemaDefinition(definitionProperty, xsdElement);
		
		return schemaContainerDelta;
	}

	/**
	 * Apply proper definition (connector schema) to the resource.
	 */
	private void applyConnectorSchemaToResource(PrismObject<ResourceType> resource, OperationResult result)
			throws SchemaException, ObjectNotFoundException {

		synchronized (resource) {
			boolean immutable = resource.isImmutable();
			if (immutable) {
				resource.setImmutable(false);
			}
			try {
				ConnectorType connectorType = connectorTypeManager.getConnectorTypeReadOnly(resource.asObjectable(), result);
				PrismSchema connectorSchema = connectorTypeManager.getConnectorSchema(connectorType);
				if (connectorSchema == null) {
					throw new SchemaException("No connector schema in " + connectorType);
				}
				PrismContainerDefinition<ConnectorConfigurationType> configurationContainerDefinition = ConnectorTypeUtil
						.findConfigurationContainerDefinition(connectorType, connectorSchema);
				if (configurationContainerDefinition == null) {
					throw new SchemaException("No configuration container definition in schema of " + connectorType);
				}

				configurationContainerDefinition = configurationContainerDefinition.clone();
				PrismContainer<ConnectorConfigurationType> configurationContainer = ResourceTypeUtil
						.getConfigurationContainer(resource);
				// We want element name, minOccurs/maxOccurs and similar definition to be taken from the original, not the schema
				// the element is global in the connector schema. therefore it does not have correct maxOccurs
				if (configurationContainer != null) {
					configurationContainerDefinition.adoptElementDefinitionFrom(configurationContainer.getDefinition());
					configurationContainer.applyDefinition(configurationContainerDefinition, true);
				} else {
					configurationContainerDefinition.adoptElementDefinitionFrom(
							resource.getDefinition().findContainerDefinition(ResourceType.F_CONNECTOR_CONFIGURATION));
				}

				PrismObjectDefinition<ResourceType> objectDefinition = resource.getDefinition();
				PrismObjectDefinition<ResourceType> clonedObjectDefinition = objectDefinition
						.cloneWithReplacedDefinition(ResourceType.F_CONNECTOR_CONFIGURATION, configurationContainerDefinition);
				resource.setDefinition(clonedObjectDefinition);
			} finally {
				if (immutable) {
					resource.setImmutable(true);
				}
			}
		}
	}

	public void testConnection(PrismObject<ResourceType> resource, OperationResult parentResult) {

		// === test INITIALIZATION ===

		OperationResult initResult = parentResult
				.createSubresult(ConnectorTestOperation.CONNECTOR_INITIALIZATION.getOperation());
		ConnectorInstance connector;
		try {

			connector = getConnectorInstance(resource, true, initResult);
			initResult.recordSuccess();
		} catch (ObjectNotFoundException e) {
			// The connector was not found. The resource definition is either
			// wrong or the connector is not
			// installed.
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			initResult.recordFatalError("The connector was not found: "+e.getMessage(), e);
			return;
		} catch (SchemaException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			initResult.recordFatalError("Schema error while dealing with the connector definition: "+e.getMessage(), e);
			return;
		} catch (RuntimeException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			initResult.recordFatalError("Unexpected runtime error: "+e.getMessage(), e);
			return;
		} catch (CommunicationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			initResult.recordFatalError("Communication error: "+e.getMessage(), e);
			return;
		} catch (ConfigurationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			initResult.recordFatalError("Configuration error: "+e.getMessage(), e);
			return;
		}
		
			
		
		LOGGER.debug("Testing connection to the resource with oid {}", resource.getOid());

		// === test CONFIGURATION ===

		OperationResult configResult = parentResult
				.createSubresult(ConnectorTestOperation.CONFIGURATION_VALIDATION.getOperation());

		try {
			connector.configure(resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION)
					.getValue(), configResult);
			configResult.recordSuccess();
		} catch (CommunicationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			configResult.recordFatalError("Communication error", e);
			return;
		} catch (GenericFrameworkException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			configResult.recordFatalError("Generic error", e);
			return;
		} catch (SchemaException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			configResult.recordFatalError("Schema error", e);
			return;
		} catch (ConfigurationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			configResult.recordFatalError("Configuration error", e);
			return;
		} catch (RuntimeException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			configResult.recordFatalError("Unexpected runtime error", e);
			return;
		}

		// === test CONNECTION ===

		// delegate the main part of the test to the connector
		connector.test(parentResult);

		parentResult.computeStatus();
		if (!parentResult.isAcceptable()) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.DOWN, parentResult);
			// No point in going on. Following tests will fail anyway, they will
			// just produce misleading
			// messages.
			return;
		} else {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.UP, parentResult);
		}

		// === test SCHEMA ===

		OperationResult schemaResult = parentResult.createSubresult(ConnectorTestOperation.CONNECTOR_SCHEMA
				.getOperation());

		ResourceSchema schema = null;
		try {
			// Try to fetch schema from the connector. The UCF will convert it
			// to Schema Processor
			// format, so it is already structured
			InternalMonitor.recordResourceSchemaFetch();
			List<QName> generateObjectClasses = ResourceTypeUtil.getSchemaGenerationConstraints(resource);
//			ResourceType resourceType = resource.asObjectable();
//			if (resourceType.getSchema() != null && resourceType.getSchema().getGenerationConstraints() != null){
//				generateObjectClasses = resourceType.getSchema().getGenerationConstraints().getGenerateObjectClass();
//			}
			schema = connector.fetchResourceSchema(generateObjectClasses, schemaResult);
		} catch (CommunicationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			schemaResult.recordFatalError("Communication error: " + e.getMessage(), e);
			return;
		} catch (GenericFrameworkException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			schemaResult.recordFatalError("Generic error: " + e.getMessage(), e);
			return;
		} catch (ConfigurationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			schemaResult.recordFatalError("Configuration error: " + e.getMessage(), e);
			return;
		}

		if (schema == null || schema.isEmpty()) {
			// Resource does not support schema
			// If there is a static schema in resource definition this may still be OK
			try {
				schema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
			} catch (SchemaException e) {
				modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
				schemaResult.recordFatalError(e);
				return;
			}
			
			if (schema == null || schema.isEmpty()) {
				modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
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
			resource = completeResource(resource, schema, true, null, schemaResult);
		} catch (ObjectNotFoundException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			schemaResult.recordFatalError(
					"Object not found (unexpected error, probably a bug): " + e.getMessage(), e);
			return;
		} catch (SchemaException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			schemaResult.recordFatalError(
					"Schema processing error (probably connector bug): " + e.getMessage(), e);
			return;
		} catch (CommunicationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			schemaResult.recordFatalError("Communication error: " + e.getMessage(), e);
			return;
		} catch (ConfigurationException e) {
			modifyResourceAvailabilityStatus(resource, AvailabilityStatusType.BROKEN, parentResult);
			schemaResult.recordFatalError("Configuration error: " + e.getMessage(), e);
			return;
		}

		schemaResult.recordSuccess();

		// TODO: connector sanity (e.g. at least one account type, identifiers
		// in schema, etc.)

	}
	
	public void modifyResourceAvailabilityStatus(PrismObject<ResourceType> resource, AvailabilityStatusType status, OperationResult result){
			ResourceType resourceType = resource.asObjectable();
			
			synchronized (resource) {
				if (resourceType.getOperationalState() == null || resourceType.getOperationalState().getLastAvailabilityStatus() == null || resourceType.getOperationalState().getLastAvailabilityStatus() != status) {
					List<PropertyDelta<?>> modifications = new ArrayList<PropertyDelta<?>>();
					PropertyDelta<?> statusDelta = createResourceAvailabilityStatusDelta(resource, status);
					modifications.add(statusDelta);
					
					try{
						repositoryService.modifyObject(ResourceType.class, resourceType.getOid(), modifications, result);
					} catch(SchemaException ex){
						throw new SystemException(ex);
					} catch(ObjectAlreadyExistsException ex){
						throw new SystemException(ex);
					} catch(ObjectNotFoundException ex){
						throw new SystemException(ex);
					}
				}
				// ugly hack: change object even if it's immutable
				boolean immutable = resource.isImmutable();
				if (immutable) {
					resource.setImmutable(false);
				}
				if (resourceType.getOperationalState() == null) {
					OperationalStateType operationalState = new OperationalStateType();
					operationalState.setLastAvailabilityStatus(status);
					resourceType.setOperationalState(operationalState);
				} else {
					resourceType.getOperationalState().setLastAvailabilityStatus(status);
				}
				if (immutable) {
					resource.setImmutable(true);
				}
			}
		}
	
	private PropertyDelta<?> createResourceAvailabilityStatusDelta(PrismObject<ResourceType> resource, AvailabilityStatusType status) {
		PropertyDelta<?> statusDelta = PropertyDelta.createModificationReplaceProperty(OperationalStateType.F_LAST_AVAILABILITY_STATUS, resource.getDefinition(), status);
		statusDelta.setParentPath(new ItemPath(ResourceType.F_OPERATIONAL_STATE));
		return statusDelta;
	}

	/**
	 * Adjust scheme with respect to capabilities. E.g. disable attributes that
	 * are used for special purpose (such as account activation simulation).
	 *
	 * TODO treat also objectclass-specific capabilities here
	 */
	private void adjustSchemaForSimulatedCapabilities(PrismObject<ResourceType> resource, ResourceSchema resourceSchema) {
		ResourceType resourceType = resource.asObjectable();
		if (resourceType.getCapabilities() == null || resourceType.getCapabilities().getConfigured() == null) {
			return;
		}
		ActivationCapabilityType activationCapability = CapabilityUtil.getCapability(resourceType
				.getCapabilities().getConfigured().getAny(), ActivationCapabilityType.class);
		if (CapabilityUtil.getEffectiveActivationStatus(activationCapability) != null) {
			QName attributeName = activationCapability.getStatus().getAttribute();
			Boolean ignore = activationCapability.getStatus().isIgnoreAttribute();
			if (attributeName != null) {
				// The attribute used for enable/disable simulation should be ignored in the schema
				// otherwise strange things may happen, such as changing the same attribute both from
				// activation/enable and from the attribute using its native name.
				for (ObjectClassComplexTypeDefinition objectClassDefinition : resourceSchema
						.getDefinitions(ObjectClassComplexTypeDefinition.class)) {
					ResourceAttributeDefinition attributeDefinition = objectClassDefinition
							.findAttributeDefinition(attributeName);
					if (attributeDefinition != null) {
						if (ignore != null && !ignore.booleanValue()) {
							((ResourceAttributeDefinitionImpl) attributeDefinition).setIgnored(false);
						} else {
							((ResourceAttributeDefinitionImpl) attributeDefinition).setIgnored(true);
						}
					} else {
						// simulated activation attribute points to something that is not in the schema
						// technically, this is an error. But it looks to be quite common in connectors.
						// The enable/disable is using operational attributes that are not exposed in the
						// schema, but they work if passed to the connector.
						// Therefore we don't want to break anything. We could log an warning here, but the
						// warning would be quite frequent. Maybe a better place to warn user would be import
						// of the object.
						LOGGER.debug("Simulated activation attribute "
								+ attributeName
								+ " for objectclass "
								+ objectClassDefinition.getTypeName()
								+ " in "
								+ resource
								+ " does not exist in the resource schema. This may work well, but it is not clean. Connector exposing such schema should be fixed.");
					}
				}
			}
		}
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

	private ConnectorInstance getConnectorInstance(PrismObject<ResourceType> resource, boolean forceFresh, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		return connectorTypeManager.getConfiguredConnectorInstance(resource, forceFresh, parentResult);
	}

	public void applyDefinition(ObjectDelta<ResourceType> delta, ResourceType resourceWhenNoOid, GetOperationOptions options, OperationResult objectResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		
		if (delta.isAdd()) {
			PrismObject<ResourceType> resource = delta.getObjectToAdd();
			applyConnectorSchemaToResource(resource, objectResult);
			return;
			
		} else if (delta.isModify()) {
			// Go on
		} else {
			return;
		}

		if (delta.hasCompleteDefinition()){
			//nothing to do, all modifications has definitions..just aplly this deltas..
			return;
		}
		
	
        PrismObject<ResourceType> resource;
		String resourceOid = delta.getOid();
        if (resourceOid == null) {
            Validate.notNull(resourceWhenNoOid, "Resource oid not specified in the object delta, and resource is not specified as well. Could not apply definition.");
            resource = resourceWhenNoOid.asPrismObject();
        } else {
		    resource = getResource(resourceOid, options, objectResult);
        }

        ResourceType resourceType = resource.asObjectable();
//		ResourceType resourceType = completeResource(resource.asObjectable(), null, objectResult);
		//TODO TODO TODO FIXME FIXME FIXME copied from ObjectImprted..union this two cases
		PrismContainer<ConnectorConfigurationType> configurationContainer = ResourceTypeUtil.getConfigurationContainer(resourceType);
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
        
        //ItemDelta.findItemDelta(delta.getModifications(), ResourceType.F_SCHEMA, ContainerDelta.class) == null || 
       
        ReferenceDelta connectorRefDelta = ReferenceDelta.findReferenceModification(delta.getModifications(), ResourceType.F_CONNECTOR_REF);
        if (connectorRefDelta != null){
        	Item<PrismReferenceValue,PrismReferenceDefinition> connectorRefNew = connectorRefDelta.getItemNewMatchingPath(null);
        	if (connectorRefNew.getValues().size() == 1){
        		PrismReferenceValue connectorRefValue = connectorRefNew.getValues().iterator().next();
        		if (connectorRefValue.getOid() != null && !connectorOid.equals(connectorRefValue.getOid())){
        			connectorOid = connectorRefValue.getOid();
        		}
        	}
        }

        PrismObject<ConnectorType> connector = null;
        ConnectorType connectorType = null;
        try {
            connector = repositoryService.getObject(ConnectorType.class, connectorOid, null, objectResult);
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
			connectorSchema = PrismSchemaImpl.parse(connectorSchemaElement, true, "schema for " + connector, prismContext);
		} catch (SchemaException e) {
			objectResult.recordFatalError("Error parsing connector schema for " + connector + ": "+e.getMessage(), e);
			return;
		}
        QName configContainerQName = new QName(connectorType.getNamespace(), ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
		PrismContainerDefinition<ConnectorConfigurationType> configContainerDef =
				connectorSchema.findContainerDefinitionByElementName(configContainerQName);
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

		PrismContainer configContainer = resourceType.asPrismObject().findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
		configContainer.applyDefinition(configContainerDef);
 
        for (ItemDelta<?,?> itemDelta : delta.getModifications()){
        	applyItemDefinition(itemDelta, configContainerDef, objectResult);
        }
	}
	
	private <V extends PrismValue, D extends ItemDefinition> void applyItemDefinition(ItemDelta<V,D> itemDelta, 
			PrismContainerDefinition<ConnectorConfigurationType> configContainerDef, OperationResult objectResult) throws SchemaException {
		if (itemDelta.getParentPath() == null){
    		LOGGER.trace("No parent path defined for item delta {}", itemDelta);
    		return;
    	}
    	
    	QName first = ItemPath.getName(itemDelta.getParentPath().first());
    	
    	if (first == null){
    		return;
    	}
    	
    	if (itemDelta.getDefinition() == null && (ResourceType.F_CONNECTOR_CONFIGURATION.equals(first) || ResourceType.F_SCHEMA.equals(first))){
    		ItemPath path = itemDelta.getPath().rest();
    		D itemDef = configContainerDef.findItemDefinition(path);
    		if (itemDef == null){
    			LOGGER.warn("No definition found for item {}. Check your namespaces?", path);
    			objectResult.recordWarning("No definition found for item delta: " + itemDelta +". Check your namespaces?" );
//    			throw new SchemaException("No definition found for item " + path+ ". Check your namespaces?" );
    			return;
    		}
			itemDelta.applyDefinition(itemDef);
    		
    	}
	}
	
	public void applyDefinition(PrismObject<ResourceType> resource, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		applyConnectorSchemaToResource(resource, parentResult);
	}

	public void applyDefinition(ObjectQuery query, OperationResult result) {
		// TODO: not implemented yet
	}

	public Object executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		PrismObject<ResourceType> resource = getResource(resourceOid, null, result);
		ConnectorInstance connectorInstance = connectorTypeManager.getConfiguredConnectorInstance(resource, false, result);
		ExecuteProvisioningScriptOperation scriptOperation = ProvisioningUtil.convertToScriptOperation(script, "script on "+resource, prismContext);
		try {
			StateReporter reporter = new StateReporter(resourceOid, task);
			return connectorInstance.executeScript(scriptOperation, reporter, result);
		} catch (GenericFrameworkException e) {
			// Not expected. Transform to system exception
			result.recordFatalError("Generic provisioning framework error", e);
			throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
		}
	}

}
