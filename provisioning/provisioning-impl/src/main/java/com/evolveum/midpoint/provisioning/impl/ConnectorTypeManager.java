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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

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
	private RepositoryService repositoryService;
	@Autowired
	private ConnectorFactory connectorFactory;
	
	private static final Trace LOGGER = TraceManager.getTrace(ProvisioningServiceImpl.class);

	
	/**
	 * Lists local connectors and makes sure that appropriate ConnectorType objects for them exist in repository.
	 * 
	 * It will never delete any repository object, even if the corresponding connector cannot be found. The connector may
	 * temporarily removed, may be present on a different node, manual upgrade may be needed etc.
	 *  
	 * @return set of discovered connectors (new connectors found)
	 */
	public Set<ConnectorType> discoverLocalConnectors(OperationResult parentResult) {
		
		OperationResult result = parentResult.createSubresult(ConnectorTypeManager.class.getName()+".discoverLocalConnectors");
		
		Set<ConnectorType> discoveredConnectors = new HashSet<ConnectorType>();
		Set<ConnectorType> localConnectors = connectorFactory.listConnectors();
		
		for (ConnectorType localConnector : localConnectors) {
		
			if (!isInRepo(localConnector,result)) {
				// Sanitize framework-supplied OID
				if (localConnector.getOid()!=null) {
					LOGGER.warn("Provisioning framework "+localConnector.getFramework()+" supplied OID for connector "+ObjectTypeUtil.toShortString(localConnector));
					localConnector.setOid(null);
				}
				
				String oid;
				try {
					oid = repositoryService.addObject(localConnector, result);
				} catch (ObjectAlreadyExistsException e) {
					// We don't specify the OID, therefore this should never happen
					// Convert to runtime exception
					LOGGER.error("Got ObjectAlreadyExistsException while not expecting it: "+e.getMessage(),e);
					result.recordFatalError("Got ObjectAlreadyExistsException while not expecting it: "+e.getMessage(),e);
					throw new SystemException("Got ObjectAlreadyExistsException while not expecting it: "+e.getMessage(),e);
				} catch (SchemaException e) {
					// If there is a schema error it must be a bug. Convert to runtime exception
					LOGGER.error("Got SchemaException while not expecting it: "+e.getMessage(),e);
					result.recordFatalError("Got SchemaException while not expecting it: "+e.getMessage(),e);
					throw new SystemException("Got SchemaException while not expecting it: "+e.getMessage(),e);
				}
				localConnector.setOid(oid);
				discoveredConnectors.add(localConnector);
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
	private boolean isInRepo(ConnectorType connector, OperationResult result) {
		Document doc = DOMUtil.getDocument();
        Element filter =
                QueryUtil.createAndFilter(doc,
	                QueryUtil.createTypeFilter(doc, ObjectTypes.CONNECTOR.getObjectTypeUri()),
	                QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_FRAMEWORK,connector.getFramework()),
	                QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE,connector.getConnectorType())
                );
		
		QueryType query = new QueryType();
        query.setFilter(filter);
		
        ObjectListType foundConnectors;
		try {
			foundConnectors = repositoryService.searchObjects(query, null, result);
		} catch (SchemaException e) {
			// If there is a schema error it must be a bug. Convert to runtime exception
			LOGGER.error("Got SchemaException while not expecting it: "+e.getMessage(),e);
			result.recordFatalError("Got SchemaException while not expecting it: "+e.getMessage(),e);
			throw new SystemException("Got SchemaException while not expecting it: "+e.getMessage(),e);
		}
		
		if (foundConnectors.getObject().size()==0) {
			// Nothing found, the connector is not in the repo
			return false;
		}
		
		String foundOid = null;
		for (ObjectType o : foundConnectors.getObject()) {
			if (!(o instanceof ConnectorType)) {
				LOGGER.error("Repository malfunction, got "+o+" while expecting "+ConnectorType.class.getSimpleName());
				result.recordFatalError("Repository malfunction, got "+o+" while expecting "+ConnectorType.class.getSimpleName());
				throw new IllegalStateException("Repository malfunction, got "+o+" while expecting "+ConnectorType.class.getSimpleName());
			}
			ConnectorType foundConnector = (ConnectorType)o;
			if (compareConnectors(connector,foundConnector)) {
				if (foundOid!=null) {
					// More than one connector matches. Inconsistent repo state. Log error.
					result.recordPartialError("Found more than one connector that matches "+connector.getFramework()+" : "+connector.getConnectorType()+" : "+connector.getVersion()+". OIDs "+foundConnector.getOid()+" and "+foundOid+". Inconsistent database state.");
					LOGGER.error("Found more than one connector that matches "+connector.getFramework()+" : "+connector.getConnectorType()+" : "+connector.getVersion()+". OIDs "+foundConnector.getOid()+" and "+foundOid+". Inconsistent database state.");
					// But continue working otherwise. This is probably not critical.
					return true;
				}
				foundOid=foundConnector.getOid();
			}
		}
		
		return (foundOid!=null);
	}

	/**
	 * @param connector
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
		if (a.getVersion()==null && b.getVersion()==null) {
			// Both connectors without version. This is OK.
			return true;
		}
		if (a.getVersion()!=null && b.getVersion()!=null) {
			// Both connectors with version. This is OK.
			return a.getVersion().equals(b.getVersion());
		}
		// One connector has version and other does not. This is inconsistency
		LOGGER.error("Inconsistent representation of ConnectorType, one has version and other does not. OIDs: "+a.getOid()+" and "+b.getOid());
		// Obviously they don't match
		return false;
	}
	
}
