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
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;

/**
 * WORK IN PROGRESS
 * 
 * There be dragons.
 * Beware the dog.
 * Do not trespass.
 * 
 * This is supposed to replace provisioning-1.wsdl
 * 
 * @author Radovan Semancik
 */
public interface ProvisioningService {
	
	/**
	 * Returns object for provided OID.
     * 
     * Must fail if object with the OID does not exists.
     * 
     * Resource Object Shadows:
     * The resource object shadow attributes may be retrieved from
     * the local database, directly form the resource or a
     * combination of both. The retrieval may fail due to resource
     * failure, network failure or similar external cases. The
     * retrieval may also take relatively long time (e.g. until it
     * times out).
     * 
	 * @param oid OID of the object to get
	 * @param resolve list of properties to resolve in the fetched object
	 * @param result parent OperationResult
	 * @return Object fetched from repository and/or resource
	 * 
	 * @throws ObjectNotFoundException requested object does not exist
	 * @throws CommunicationException error communicating with the resource
	 * @throws SchemaException error dealing with resource schema
	 * @throws IllegalArgumentException wrong OID format, etc.
	 * @throws GenericConnectorException unknown connector framework error
	 */
	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException;

	/**
	 * Add new object.
	 *
     * The OID provided in the input message may be empty. In that case
     * the OID will be assigned by the implementation of this method
     * and it will be provided as return value.
     *
     * This operation should fail if such object already exists (if
     * object with the provided OID already exists).
     *
     * The operation may fail if provided OID is in an unusable format
     * for the storage. Generating own OIDs and providing them to this
     * method is not recommended for normal operation.
     *
     * Should be atomic. Should not allow creation of two objects with
     * the same OID (even if created in parallel).
	 *
     * The operation may fail if the object to be created does not
     * conform to the underlying schema of the storage system or the
     * schema enforced by the implementation.
     *          
	 * @param object object to create
	 * @param scripts scripts to execute before/after the operation
	 * @param parentResult parent OperationResult
	 * @return OID assigned to the created object
	 * 
	 * @throws ObjectAlreadyExistsException object with specified identifiers already exists, cannot add
	 * @throws SchemaException error dealing with resource schema, e.g. schema violation
	 * @throws CommunicationException error communicating with the resource
	 * @throws IllegalArgumentException wrong OID format, etc.
	 * @throws GenericConnectorException unknown connector framework error
	 */
	public String addObject(ObjectType object, ScriptsType scripts, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException, CommunicationException;
	
	// TODO: Other operations from provisioning-1.wsdl
}
