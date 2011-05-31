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
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.Schema;
import java.util.List;
import java.util.Set;

/**
 * Connector instance configured for a specific resource.
 * 
 * This interface provides an unified facade to a connector capabilities
 * in the Unified Connector Framework interface. The connector is configured
 * to a specific resource instance and therefore can execute operations on 
 * resource.
 * 
 * @see ConnectorManager
 * 
 *  TODO: doc
 *  
 *  TODO: rich operation result
 * 
 * @author Radovan Semancik
 * 
 */
public interface ConfiguredConnector {
	
    /**
	 * Retrieves the schema from the resource.
	 * 
	 * @return Up-to-date resource schema.
	 */
	public Schema fetchResourceSchema() throws CommunicationException;
	
	public ResourceObject fetchObject(Set<ResourceObjectAttribute> identifiers) throws CommunicationException;

	/**
	 * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
	 * and what were not determined.
	 * 
	 * The exception should be thrown only if the connector is sure that nothing was done on the resource.
	 * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
	 * exception as something might have been done already.
	 * 
	 * The connector may return some (or all) of the attributes of created object. The connector should do
	 * this only such operation is efficient, e.g. in case that the created object is normal return value from
	 * the create operation. The connector must not execute additional operation to fetch the state of
	 * created resource. In case that the new state is not such a normal result, the connector must
	 * return null. Returning empty set means that the connector supports returning of new state, but nothing
	 * was returned (e.g. due to a limiting configuration). Returning null means that connector does not support
	 * returning of new object state and the caller should explicitly invoke fetchObject() in case that the
	 * information is needed.
	 * 
	 * @param object
	 * @param additionalOperations
	 * @throws CommunicationException
	 * @return created object attributes. May be null.
	 */
	public Set<ResourceObjectAttribute> addObject(ResourceObject object, Set<Operation> additionalOperations) throws CommunicationException;
	
	/**
	 * TODO: This should return indication how the operation went, e.g. what changes were applied, what were not
	 * and what results are we not sure about.
	 * 
	 * The exception should be thrown only if the connector is sure that nothing was done on the resource.
	 * E.g. in case of connect timeout or connection refused. Timeout during operation should not cause the
	 * exception as something might have been done already. 
	 * 
	 * @param identifiers
	 * @param changes
	 * @throws CommunicationException
	 */
	public void modifyObject(Set<ResourceObjectAttribute> identifiers, Set<Operation> changes) throws CommunicationException;
	
	public void deleteObject(Set<ResourceObjectAttribute> identifiers) throws CommunicationException;
	
	/**
	 * Creates a live Java object from a token previously serialized to string.
	 * 
	 * Serialized token is not portable to other connectors or other resources.
	 * However, newer versions of the connector should understand tokens generated
	 * by previous connector version.
	 * 
	 * @param serializedToken
	 * @return
	 */
	public Token deserializeToken(String serializedToken);
	
	/**
	 * Returns the latest token. In other words, returns a token that
	 * corresponds to a current state of the resource. If fetchChanges
	 * is immediately called with this token, nothing should be returned
	 * (Figuratively speaking, neglecting concurrent resource modifications).
	 * 
	 * @return
	 * @throws CommunicationException
	 */
	public Token fetchCurrentToken() throws CommunicationException;
	
	/**
	 * Token may be null. That means "from the beginning of history".
	 * 
	 * @param lastToken
	 * @return
	 */
	public List<Change> fetchChanges(Token lastToken) throws CommunicationException;
	
	//public ValidationResult validateConfiguration(ResourceConfiguration newConfiguration);
	
	//public void applyConfiguration(ResourceConfiguration newConfiguration) throws MisconfigurationException; 
	
}
