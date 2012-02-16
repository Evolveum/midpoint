/**
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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;

/**
 * Class that manages the ConnectorType objects in repository.
 * 
 * It creates new ConnectorType objects when a new local connector is discovered,
 * takes care of remote connector discovery, etc.
 * 
 * @author Radovan Semancik
 *
 */
@Component
public class ConnectorTypeManager {
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	@Autowired
	private ConnectorFactory connectorFactory;

	private static final Trace LOGGER = TraceManager.getTrace(ConnectorTypeManager.class);

	private Map<String, ConfiguredConnectorInstanceEntry> connectorInstanceCache = new HashMap<String, ConnectorTypeManager.ConfiguredConnectorInstanceEntry>();

	public ConnectorInstance getConfiguredConnectorInstance(ResourceType resource, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		String resourceOid = resource.getOid();
		String connectorOid = ResourceTypeUtil.getConnectorOid(resource);
		if (connectorInstanceCache.containsKey(resourceOid)) {
			// Check if the instance can be reused
			ConfiguredConnectorInstanceEntry configuredConnectorInstanceEntry = connectorInstanceCache.get(resourceOid);

			if (configuredConnectorInstanceEntry.connectorOid.equals(connectorOid)
					&& ResourceTypeUtil.compareConfiguration(configuredConnectorInstanceEntry.configuration,
							resource.getConfiguration())) {

				// We found entry that matches
				return configuredConnectorInstanceEntry.connectorInstance;

			} else {
				// There is an entry but it does not match. We assume that the resource configuration has changed
				// and the old entry is useless now. So remove it.
				connectorInstanceCache.remove(resourceOid);
			}

		}

		// No usable connector in cache. Let's create it.
		ConnectorInstance configuredConnectorInstance = createConfiguredConnectorInstance(resource, result);

		// .. and cache it
		ConfiguredConnectorInstanceEntry cacheEntry = new ConfiguredConnectorInstanceEntry();
		cacheEntry.connectorOid = connectorOid;
		cacheEntry.configuration = resource.getConfiguration();
		cacheEntry.connectorInstance = configuredConnectorInstance;
		connectorInstanceCache.put(resourceOid, cacheEntry);

		return configuredConnectorInstance;
	}

	private ConnectorInstance createConfiguredConnectorInstance(ResourceType resource, OperationResult result)
			throws ObjectNotFoundException, SchemaException, CommunicationException {
		// This log message should be INFO level. It happens only occasionally. If it happens often, it may be an
		// indication of a problem. Therefore it is good for admin to see it. 
		LOGGER.info("Creating new connector instance for {}", ObjectTypeUtil.toShortString(resource));
		ConnectorType connectorType = getConnectorType(resource, result);
		ConnectorInstance connector = null;
		try {

			connector = connectorFactory.createConnectorInstance(connectorType, resource.getNamespace());

		} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException e) {
			result.recordFatalError(e.getMessage(), e);
			throw new ObjectNotFoundException(e.getMessage(), e);
		}
		try {
			connector.configure(resource.getConfiguration(), result);
		} catch (GenericFrameworkException e) {
			// Not expected. Transform to system exception
			result.recordFatalError("Generic provisioning framework error", e);
			throw new SystemException("Generic provisioning framework error: " + e.getMessage(), e);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException e) {
			throw new CommunicationException(e.getMessage(), e);
		}

		return connector;
	}

	public ConnectorType getConnectorType(ResourceType resource, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		if (resource.getConnector() != null) {
			return resource.getConnector();
		}
		if (resource.getConnectorRef() == null || resource.getConnectorRef().getOid() == null) {
			result.recordFatalError("Connector reference missing in the resource "
					+ ObjectTypeUtil.toShortString(resource));
			throw new ObjectNotFoundException("Connector reference missing in the resource "
					+ ObjectTypeUtil.toShortString(resource));
		}
		String connOid = resource.getConnectorRef().getOid();
		ConnectorType connectorType = repositoryService.getObject(ConnectorType.class, connOid, null, result);
		return connectorType;
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
	 * Lists local connectors and makes sure that appropriate ConnectorType objects for them exist in repository.
	 * 
	 * It will never delete any repository object, even if the corresponding connector cannot be found. The connector may
	 * temporarily removed, may be present on a different node, manual upgrade may be needed etc.
	 *  
	 * @return set of discovered connectors (new connectors found)
	 * @throws CommunicationException 
	 */
	public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult)
			throws CommunicationException {

		OperationResult result = parentResult.createSubresult(ConnectorTypeManager.class.getName()
				+ ".discoverConnectors");
		result.addParam("host", hostType);

		// Make sure that the provided host has an OID.
		// We need the host to have OID, so we can properly link connectors to it
		if (hostType != null && hostType.getOid() == null) {
			throw new SystemException("Discovery attempt with non-persistent " + ObjectTypeUtil.toShortString(hostType));
		}

		Set<ConnectorType> discoveredConnectors = new HashSet<ConnectorType>();
		Set<ConnectorType> foundConnectors;
		try {
			foundConnectors = connectorFactory.listConnectors(hostType, result);
		} catch (com.evolveum.midpoint.provisioning.ucf.api.CommunicationException ex) {
			result.recordFatalError("Discovery failed: " + ex.getMessage(), ex);
			throw new CommunicationException("Discovery failed: " + ex.getMessage(), ex);
		}

		for (ConnectorType foundConnector : foundConnectors) {

			LOGGER.trace("Found connector " + ObjectTypeUtil.toShortString(foundConnector));

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

				LOGGER.trace("Connector " + ObjectTypeUtil.toShortString(foundConnector)
						+ " not in the repository, \"dicovering\" it");

				// First of all we need to "embed" connectorHost to the connectorType. The UCF does not
				// have access to repository, therefore it cannot resolve it for itself
				if (hostType != null && foundConnector.getConnectorHost() == null) {
					foundConnector.setConnectorHost(hostType);
				}

				// Connector schema is normally not generated.
				// Let's instantiate the connector and generate the schema
				ConnectorInstance connectorInstance = null;
				try {
					connectorInstance = connectorFactory.createConnectorInstance(foundConnector, null);
					PrismSchema connectorSchema = connectorInstance.generateConnectorSchema();
					if (connectorSchema == null) {
						LOGGER.warn("Connector {} haven't provided configuration schema",
								ObjectTypeUtil.toShortString(foundConnector));
					} else {
						LOGGER.trace("Generated connector schema for {}: {} definitions",
								ObjectTypeUtil.toShortString(foundConnector), connectorSchema.getDefinitions().size());
						Document xsdDoc = null;
						// Convert to XSD
						xsdDoc = connectorSchema.serializeToXsd();
						Element xsdElement = DOMUtil.getFirstChildElement(xsdDoc);
						LOGGER.trace("Generated XSD connector schema: {}", DOMUtil.serializeDOMToString(xsdElement));
						if (foundConnector.getSchema() == null) {
							foundConnector.setSchema(new XmlSchemaType());
						}
						foundConnector.getSchema().getAny().add(xsdElement);
					}
				} catch (com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException ex) {
					LOGGER.error(
							"Cannot instantiate discovered connector " + ObjectTypeUtil.toShortString(foundConnector),
							ex);
					result.recordPartialError(
							"Cannot instantiate discovered connector " + ObjectTypeUtil.toShortString(foundConnector),
							ex);
					// Skipping schema generation, but otherwise going on
				} catch (SchemaException e) {
					LOGGER.error(
							"Error processing connector schema for " + ObjectTypeUtil.toShortString(foundConnector)
									+ ": " + e.getMessage(), e);
					result.recordPartialError(
							"Error processing connector schema for " + ObjectTypeUtil.toShortString(foundConnector)
									+ ": " + e.getMessage(), e);
					// Skipping schema generation, but otherwise going on
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
					oid = repositoryService.addObject(foundConnector, result);
				} catch (ObjectAlreadyExistsException e) {
					// We don't specify the OID, therefore this should never happen
					// Convert to runtime exception
					LOGGER.error("Got ObjectAlreadyExistsException while not expecting it: " + e.getMessage(), e);
					result.recordFatalError(
							"Got ObjectAlreadyExistsException while not expecting it: " + e.getMessage(), e);
					throw new SystemException("Got ObjectAlreadyExistsException while not expecting it: "
							+ e.getMessage(), e);
				} catch (SchemaException e) {
					// If there is a schema error it must be a bug. Convert to runtime exception
					LOGGER.error("Got SchemaException while not expecting it: " + e.getMessage(), e);
					result.recordFatalError("Got SchemaException while not expecting it: " + e.getMessage(), e);
					throw new SystemException("Got SchemaException while not expecting it: " + e.getMessage(), e);
				}
				foundConnector.setOid(oid);
				discoveredConnectors.add(foundConnector);
			}

		}

		result.recordSuccess();
		return discoveredConnectors;
	}

	/**
	 * @param localConnector
	 * @return
	 * @throws SchemaException 
	 */
	private boolean isInRepo(ConnectorType connector, OperationResult result) throws SchemaException {
		Document doc = DOMUtil.getDocument();
		Element filter = QueryUtil
				.createAndFilter(
						doc,
						QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_FRAMEWORK,
								connector.getFramework()),
						QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE,
								connector.getConnectorType()));

		QueryType query = new QueryType();
		query.setFilter(filter);

		List<ConnectorType> foundConnectors;
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
		for (ConnectorType foundConnector : foundConnectors) {
			if (compareConnectors(connector, foundConnector)) {
				if (foundOid != null) {
					// More than one connector matches. Inconsistent repo state. Log error.
					result.recordPartialError("Found more than one connector that matches " + connector.getFramework()
							+ " : " + connector.getConnectorType() + " : " + connector.getVersion() + ". OIDs "
							+ foundConnector.getOid() + " and " + foundOid + ". Inconsistent database state.");
					LOGGER.error("Found more than one connector that matches " + connector.getFramework() + " : "
							+ connector.getConnectorType() + " : " + connector.getVersion() + ". OIDs "
							+ foundConnector.getOid() + " and " + foundOid + ". Inconsistent database state.");
					// But continue working otherwise. This is probably not critical.
					return true;
				}
				foundOid = foundConnector.getOid();
			}
		}

		return (foundOid != null);
	}

	/**
	 * @param icfConnectorFacade
	 * @param foundConnector
	 * @return
	 */
	private boolean compareConnectors(ConnectorType a, ConnectorType b) {
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
		if (a.getVersion() == null && b.getVersion() == null) {
			// Both connectors without version. This is OK.
			return true;
		}
		if (a.getVersion() != null && b.getVersion() != null) {
			// Both connectors with version. This is OK.
			return a.getVersion().equals(b.getVersion());
		}
		// One connector has version and other does not. This is inconsistency
		LOGGER.error("Inconsistent representation of ConnectorType, one has version and other does not. OIDs: "
				+ a.getOid() + " and " + b.getOid());
		// Obviously they don't match
		return false;
	}

	private class ConfiguredConnectorInstanceEntry {
		public String connectorOid;
		public ResourceConfigurationType configuration;
		public ConnectorInstance connectorInstance;
	}

}
