/*
 * Copyright (c) 2010-2017 Evolveum
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorInstanceSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;

/**
 * Class that manages the ConnectorType objects in repository.
 * 
 * It creates new ConnectorType objects when a new local connector is
 * discovered, takes care of remote connector discovery, etc.
 * 
 * @author Radovan Semancik
 * 
 */
@Component
public class ConnectorManager {
	
	private static final String USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA = ConnectorManager.class.getName()+".parsedSchema";
	
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
    ApplicationContext springContext;
	
	@Autowired(required = true)
	private PrismContext prismContext;

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorManager.class);

	private Collection<ConnectorFactory> connectorFactories;
	private Map<ConfiguredConnectorCacheKey, ConfiguredConnectorInstanceEntry> connectorInstanceCache = new ConcurrentHashMap<>();
	private Map<String, ConnectorType> connectorTypeCache = new ConcurrentHashMap<>();

	public Collection<ConnectorFactory> getConnectorFactories() {
		if (connectorFactories == null) {
			String[] connectorFactoryBeanNames = springContext.getBeanNamesForType(ConnectorFactory.class);
			LOGGER.debug("Connector factories bean names: {}", Arrays.toString(connectorFactoryBeanNames));
			if (connectorFactoryBeanNames == null) {
				return null;
			}
			connectorFactories = new ArrayList<>(connectorFactoryBeanNames.length);
			for (String connectorFactoryBeanName: connectorFactoryBeanNames) {
				Object bean = springContext.getBean(connectorFactoryBeanName);
				if (bean instanceof ConnectorFactory) {
					connectorFactories.add((ConnectorFactory)bean);
				} else {
					LOGGER.error("Bean {} is not instance of ConnectorFactory, it is {}, skipping", connectorFactoryBeanName, bean.getClass());
				}
			}
		}
		return connectorFactories;
	}
	
	private ConnectorFactory determineConnectorFactory(ConnectorType connectorType) {
		if (connectorType == null) {
			return null;
		}
		return determineConnectorFactory(connectorType.getFramework());
	}
	
	private ConnectorFactory determineConnectorFactory(String frameworkIdentifier) {
		for (ConnectorFactory connectorFactory: getConnectorFactories()) {
			if (connectorFactory.supportsFramework(frameworkIdentifier)) {
				return connectorFactory;
			}
		}
		return null;
	}
	
	public ConnectorInstance getConfiguredConnectorInstance(ConnectorSpec connectorSpec, boolean forceFresh, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ConfiguredConnectorCacheKey cacheKey = connectorSpec.getCacheKey();
		if (connectorInstanceCache.containsKey(cacheKey)) {
			// Check if the instance can be reused
			ConfiguredConnectorInstanceEntry configuredConnectorInstanceEntry = connectorInstanceCache.get(cacheKey);

			if (!forceFresh && configuredConnectorInstanceEntry.connectorOid.equals(connectorSpec.getConnectorOid())
					&& configuredConnectorInstanceEntry.configuration.equivalent(connectorSpec.getConnectorConfiguration())) {

				// We found entry that matches
				LOGGER.trace(
						"HIT in connector cache: returning configured connector {} from cache", connectorSpec);
				return configuredConnectorInstanceEntry.connectorInstance;

			} else {
				// There is an entry but it does not match. We assume that the
				// resource configuration has changed
				// and the old entry is useless now. So remove it.
				connectorInstanceCache.remove(cacheKey);
			}

		}
		if (forceFresh) {
			LOGGER.debug("FORCE in connector cache: creating configured connector {}", connectorSpec);
		} else {
			LOGGER.debug("MISS in connector cache: creating configured connector {}", connectorSpec);
		}

		// No usable connector in cache. Let's create it.
		ConnectorInstance configuredConnectorInstance = createConfiguredConnectorInstance(connectorSpec, result);

		// .. and cache it
		ConfiguredConnectorInstanceEntry cacheEntry = new ConfiguredConnectorInstanceEntry();
		cacheEntry.connectorOid = connectorSpec.getConnectorOid();
		cacheEntry.configuration = connectorSpec.getConnectorConfiguration();
		cacheEntry.connectorInstance = configuredConnectorInstance;
		connectorInstanceCache.put(cacheKey, cacheEntry);

		return configuredConnectorInstance;
	}

	private ConnectorInstance createConfiguredConnectorInstance(ConnectorSpec connectorSpec, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		
		ConnectorType connectorType = getConnectorTypeReadOnly(connectorSpec, result);
		
		ConnectorFactory connectorFactory = determineConnectorFactory(connectorType);
		
		ConnectorInstance connector = null;
		try {

			connector = connectorFactory.createConnectorInstance(connectorType,
					ResourceTypeUtil.getResourceNamespace(connectorSpec.getResource()), connectorSpec.toString());

		} catch (ObjectNotFoundException e) {
			result.recordFatalError(e.getMessage(), e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		}
		PrismContainerValue<ConnectorConfigurationType> connectorConfigurationVal = connectorSpec.getConnectorConfiguration().getValue();
		if (connectorConfigurationVal == null) {
			SchemaException e = new SchemaException("No connector configuration in "+connectorSpec);
			result.recordFatalError(e);
			throw e;
		}
		try {
			connector.configure(connectorConfigurationVal, result);
			
			ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(connectorSpec.getResource(), prismContext);
			Collection<Object> capabilities = ResourceTypeUtil.getNativeCapabilitiesCollection(connectorSpec.getResource().asObjectable());
			
			connector.initialize(resourceSchema, capabilities, ResourceTypeUtil.isCaseIgnoreAttributeNames(connectorSpec.getResource().asObjectable()), result);
			
			InternalMonitor.recordCount(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
			
		} catch (GenericFrameworkException e) {
			// Not expected. Transform to system exception
			result.recordFatalError("Generic provisioning framework error", e);
			throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
		} catch (CommunicationException e) {
			result.recordFatalError(e);
			throw e;
		} catch (ConfigurationException e) {
			result.recordFatalError(e);
			throw e;
		}
		
		// This log message should be INFO level. It happens only occasionally.
		// If it happens often, it may be an
		// indication of a problem. Therefore it is good for admin to see it.
		LOGGER.info("Created new connector instance for {}: {} v{}",
				connectorSpec, connectorType.getConnectorType(), connectorType.getConnectorVersion());

		return connector;
	}

	public ConnectorType getConnectorTypeReadOnly(ConnectorSpec connectorSpec, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		if (connectorSpec.getConnectorOid() == null) {
			result.recordFatalError("Connector OID missing in " + connectorSpec);
			throw new ObjectNotFoundException("Connector OID missing in " + connectorSpec);
		}
		String connOid = connectorSpec.getConnectorOid();
		ConnectorType connectorType = connectorTypeCache.get(connOid);
		if (connectorType == null) {
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
			PrismObject<ConnectorType> repoConnector = repositoryService.getObject(ConnectorType.class, connOid, 
					options, result);
			connectorType = repoConnector.asObjectable();
			connectorTypeCache.put(connOid, connectorType);
		} else {
			String currentConnectorVersion = repositoryService.getVersion(ConnectorType.class, connOid, result);
			if (!currentConnectorVersion.equals(connectorType.getVersion())) {
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
				PrismObject<ConnectorType> repoConnector = repositoryService.getObject(ConnectorType.class, connOid, options, result);
				connectorType = repoConnector.asObjectable();
				connectorTypeCache.put(connOid, connectorType);
			}
		}
		if (connectorType.getConnectorHost() == null && connectorType.getConnectorHostRef() != null) {
			// We need to resolve the connector host
			String connectorHostOid = connectorType.getConnectorHostRef().getOid();
			PrismObject<ConnectorHostType> connectorHost = repositoryService.getObject(ConnectorHostType.class, connectorHostOid, null, result);
			connectorType.setConnectorHost(connectorHost.asObjectable());
		}
		PrismObject<ConnectorType> connector = connectorType.asPrismObject();
		Object userDataEntry = connector.getUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA);
		if (userDataEntry == null) {
			InternalMonitor.recordCount(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
			PrismSchema connectorSchema = ConnectorTypeUtil.parseConnectorSchema(connectorType, prismContext);
			if (connectorSchema == null) {
				throw new SchemaException("No connector schema in "+connectorType);
			}
			connector.setUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA, connectorSchema);			
		}
		return connectorType;
	}
	
	public PrismSchema getConnectorSchema(ConnectorType connectorType) throws SchemaException {
		PrismObject<ConnectorType> connector = connectorType.asPrismObject();
		PrismSchema connectorSchema;
		Object userDataEntry = connector.getUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA);
		if (userDataEntry == null) {
			InternalMonitor.recordCount(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
			connectorSchema = ConnectorTypeUtil.parseConnectorSchema(connectorType, prismContext);
			if (connectorSchema == null) {
				throw new SchemaException("No connector schema in "+connectorType);
			}
			connector.setUserData(USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA, connectorSchema);			
		} else {
			if (userDataEntry instanceof PrismSchema) {
				connectorSchema = (PrismSchema)userDataEntry;
			} else {
				throw new IllegalStateException("Expected PrismSchema under user data key "+
						USER_DATA_KEY_PARSED_CONNECTOR_SCHEMA+ "in "+connectorType+", but got "+userDataEntry.getClass());
			}
		}
		return connectorSchema;
	}

	public Set<ConnectorType> discoverLocalConnectors(OperationResult parentResult) {
		try {
			return discoverConnectors(null, parentResult);
		} catch (CommunicationException e) {
			// This should never happen as no remote operation is executed
			// convert to runtime exception and record in result.
			parentResult.recordFatalError("Unexpected error: " + e.getMessage(), e);
			throw new SystemException("Unexpected error: " + e.getMessage(), e);
		}
	}

	/**
	 * Lists local connectors and makes sure that appropriate ConnectorType
	 * objects for them exist in repository.
	 * 
	 * It will never delete any repository object, even if the corresponding
	 * connector cannot be found. The connector may temporarily removed, may be
	 * present on a different node, manual upgrade may be needed etc.
	 * 
	 * @return set of discovered connectors (new connectors found)
	 * @throws CommunicationException
	 */
//	@SuppressWarnings("unchecked")
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult)
			throws CommunicationException {

		OperationResult result = parentResult.createSubresult(ConnectorManager.class.getName()
				+ ".discoverConnectors");
		result.addParam("host", hostType);

		// Make sure that the provided host has an OID.
		// We need the host to have OID, so we can properly link connectors to
		// it
		if (hostType != null && hostType.getOid() == null) {
			throw new SystemException("Discovery attempt with non-persistent " + hostType);
		}

		Set<ConnectorType> discoveredConnectors = new HashSet<ConnectorType>();
		
		for (ConnectorFactory connectorFactory: getConnectorFactories()) {
	
			Set<ConnectorType> foundConnectors;
			try {
				
				foundConnectors = connectorFactory.listConnectors(hostType, result);
				
			} catch (CommunicationException ex) {
				result.recordFatalError("Discovery failed: " + ex.getMessage(), ex);
				throw new CommunicationException("Discovery failed: " + ex.getMessage(), ex);
			}
			
			if (foundConnectors == null) {
				LOGGER.trace("Connector factory {} discovered null connectors, skipping", connectorFactory);
				continue;
			}
			
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Got {} connectors from {}: {}", new Object[] { foundConnectors.size(), hostType, foundConnectors });
			}
	
			for (ConnectorType foundConnector : foundConnectors) {
	
				LOGGER.trace("Found connector {}", foundConnector);
	
				boolean inRepo = true;
				try {
					inRepo = isInRepo(foundConnector, result);
				} catch (SchemaException e1) {
					LOGGER.error(
							"Unexpected schema problem while checking existence of "
									+ ObjectTypeUtil.toShortString(foundConnector), e1);
					result.recordPartialError(
							"Unexpected schema problem while checking existence of "
									+ ObjectTypeUtil.toShortString(foundConnector), e1);
					// But continue otherwise ...
				}
				if (!inRepo) {
	
					LOGGER.trace("Connector {} not in the repository, \"dicovering\" it", foundConnector);
	
					// First of all we need to "embed" connectorHost to the
					// connectorType. The UCF does not
					// have access to repository, therefore it cannot resolve it for
					// itself
					if (hostType != null && foundConnector.getConnectorHost() == null) {
						foundConnector.setConnectorHost(hostType);
					}
	
					if (foundConnector.getSchema() == null) {
						LOGGER.warn("Connector {} haven't provided configuration schema", foundConnector);
					}
					
					// Sanitize framework-supplied OID
					if (StringUtils.isNotEmpty(foundConnector.getOid())) {
						LOGGER.warn("Provisioning framework " + foundConnector.getFramework()
								+ " supplied OID for connector " + ObjectTypeUtil.toShortString(foundConnector));
						foundConnector.setOid(null);
					}
	
					// Store the connector object
					String oid;
					try {
						prismContext.adopt(foundConnector);
						oid = repositoryService.addObject(foundConnector.asPrismObject(), null, result);
					} catch (ObjectAlreadyExistsException e) {
						// We don't specify the OID, therefore this should never
						// happen
						// Convert to runtime exception
						LOGGER.error("Got ObjectAlreadyExistsException while not expecting it: " + e.getMessage(), e);
						result.recordFatalError(
								"Got ObjectAlreadyExistsException while not expecting it: " + e.getMessage(), e);
						throw new SystemException("Got ObjectAlreadyExistsException while not expecting it: "
								+ e.getMessage(), e);
					} catch (SchemaException e) {
						// If there is a schema error it must be a bug. Convert to
						// runtime exception
						LOGGER.error("Got SchemaException while not expecting it: " + e.getMessage(), e);
						result.recordFatalError("Got SchemaException while not expecting it: " + e.getMessage(), e);
						throw new SystemException("Got SchemaException while not expecting it: " + e.getMessage(), e);
					}
					foundConnector.setOid(oid);
					discoveredConnectors.add(foundConnector);
					LOGGER.info("Discovered new connector " + foundConnector);
				}
			}
		}

		result.recordSuccess();
		return discoveredConnectors;
	}

	private boolean isInRepo(ConnectorType connectorType, OperationResult result) throws SchemaException {
		ObjectQuery query = QueryBuilder.queryFor(ConnectorType.class, prismContext)
				.item(SchemaConstants.C_CONNECTOR_FRAMEWORK).eq(connectorType.getFramework())
				.and().item(SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE).eq(connectorType.getConnectorType())
				.build();

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Looking for connector in repository:\n{}", query.debugDump());
		}
		
		List<PrismObject<ConnectorType>> foundConnectors;
		try {
			foundConnectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
		} catch (SchemaException e) {
			// If there is a schema error it must be a bug. Convert to runtime exception
			LOGGER.error("Got SchemaException while not expecting it: " + e.getMessage(), e);
			result.recordFatalError("Got SchemaException while not expecting it: " + e.getMessage(), e);
			throw new SystemException("Got SchemaException while not expecting it: " + e.getMessage(), e);
		}

		if (foundConnectors.size() == 0) {
			// Nothing found, the connector is not in the repo
			return false;
		}

		String foundOid = null;
		for (PrismObject<ConnectorType> foundConnector : foundConnectors) {
			if (compareConnectors(connectorType.asPrismObject(), foundConnector)) {
				if (foundOid != null) {
					// More than one connector matches. Inconsistent repo state. Log error.
					result.recordPartialError("Found more than one connector that matches " + connectorType.getFramework()
							+ " : " + connectorType.getConnectorType() + " : " + connectorType.getVersion() + ". OIDs "
							+ foundConnector.getOid() + " and " + foundOid + ". Inconsistent database state.");
					LOGGER.error("Found more than one connector that matches " + connectorType.getFramework() + " : "
							+ connectorType.getConnectorType() + " : " + connectorType.getVersion() + ". OIDs "
							+ foundConnector.getOid() + " and " + foundOid + ". Inconsistent database state.");
					// But continue working otherwise. This is probably not critical.
					return true;
				}
				foundOid = foundConnector.getOid();
			}
		}

		return (foundOid != null);
	}

	private boolean compareConnectors(PrismObject<ConnectorType> prismA, PrismObject<ConnectorType> prismB) {
		ConnectorType a = prismA.asObjectable();
		ConnectorType b = prismB.asObjectable();
		if (!a.getFramework().equals(b.getFramework())) {
			return false;
		}
		if (!a.getConnectorType().equals(b.getConnectorType())) {
			return false;
		}
		if (a.getConnectorHostRef() != null) {
			if (!a.getConnectorHostRef().equals(b.getConnectorHostRef())) {
				return false;
			}
		} else {
			if (b.getConnectorHostRef() != null) {
				return false;
			}
		}
		if (a.getConnectorVersion() == null && b.getConnectorVersion() == null) {
			// Both connectors without version. This is OK.
			return true;
		}
		if (a.getConnectorVersion() != null && b.getConnectorVersion() != null) {
			// Both connectors with version. This is OK.
			return a.getConnectorVersion().equals(b.getConnectorVersion());
		}
		// One connector has version and other does not. This is inconsistency
		LOGGER.error("Inconsistent representation of ConnectorType, one has connectorVersion and other does not. OIDs: "
				+ a.getOid() + " and " + b.getOid());
		// Obviously they don't match
		return false;
	}

    public String getFrameworkVersion() {
    	ConnectorFactory connectorFactory = determineConnectorFactory(SchemaConstants.ICF_FRAMEWORK_URI);
        return connectorFactory.getFrameworkVersion();
    }

    private class ConfiguredConnectorInstanceEntry {
		public String connectorOid;
		public PrismContainer<ConnectorConfigurationType> configuration;
		public ConnectorInstance connectorInstance;
	}

	public void connectorFrameworkSelfTest(OperationResult parentTestResult, Task task) {
		for (ConnectorFactory connectorFactory: getConnectorFactories()) {
				connectorFactory.selfTest(parentTestResult);
		}
	}

	public void shutdown() {
		for (Entry<ConfiguredConnectorCacheKey, ConfiguredConnectorInstanceEntry> connectorInstanceCacheEntry: connectorInstanceCache.entrySet()) {
			connectorInstanceCacheEntry.getValue().connectorInstance.dispose();
		}
		for (ConnectorFactory connectorFactory: getConnectorFactories()) {
			connectorFactory.shutdown();
		}
	}
	
	private interface ConnectorFactoryConsumer {
		void process(ConnectorFactory connectorFactory) throws CommunicationException;
	}

}
