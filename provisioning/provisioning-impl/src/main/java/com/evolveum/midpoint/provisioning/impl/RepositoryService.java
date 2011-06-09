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

import java.util.List;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * The new repository service definition.
 * 
 * WORK IN PROGRESS
 * 
 * @author Radovan Semancik
 */
public interface RepositoryService {

	/**
	 * Returns object for provided OID.
     * 
     * Must fail if object with the OID does not exists.
     * 
	 * @param oid OID of the object to get
	 * @param resolve list of properties to resolve in the fetched object
	 * @param result parent OperationResult (in/out)
	 * @return Object fetched from repository
	 * 
	 * @throws ObjectNotFoundException requested object does not exist
	 * @throws SchemaException error dealing with resource schema
	 * @throws IllegalArgumentException wrong OID format, etc.
	 */
	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult parentResult)
		throws ObjectNotFoundException, SchemaException;

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
	 * @param parentResult parent OperationResult (in/out)
	 * @return OID assigned to the created object
	 * 
	 * @throws ObjectAlreadyExistsException object with specified identifiers already exists, cannot add
	 * @throws SchemaException error dealing with resource schema, e.g. schema violation
	 * @throws IllegalArgumentException wrong OID format, etc.
	 */
	public String addObject(ObjectType object, OperationResult parentResult) 
			throws ObjectAlreadyExistsException, SchemaException;
	
	/**
	 * Returns all objects of specified type in the repository.
     * 
     * This can be considered as a simplified search operation.
     * 
     * Returns empty list if object type is correct but there are
     * no objects of that type.
     * 
     * Should fail if object type is wrong.
     * 
	 * @param objectType
	 * @param paging paging specification to limit operation result (optional)
	 * @param parentResult parent OperationResult (in/out)
	 * @return all objects of specified type (subject to paging)
	 * 
	 * @throws IllegalArgumentException wrong object type
	 */
	public ObjectListType listObjects(Class objectType, PagingType paging, OperationResult parentResult);
	
	/**
	 * Search for objects in the repository. Searches through all
     * object types. Returns a list of objects that match search
     * criteria.
     * 
     * Returns empty list if object type is correct but there are
     * no objects of that type.
     * 
     * Should fail if object type is wrong.
     * Should fail if unknown property is specified in the query.
     * 
	 * @param query search query
	 * @param paging paging specification to limit operation result (optional)
	 * @param parentResult parent OperationResult (in/out)
	 * @return all objects of specified type that match search criteria (subject to paging)
	 * 
	 * @throws IllegalArgumentException wrong object type
	 * @throws SchemaException unknown property used in search query
	 */
	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException;

	/**
	 * Modifies object using relative change description.
     * Must fail if user with provided OID does not exists.
     * Must fail if any of the described changes cannot be applied.
     * Should be atomic.
     * 
     * If two or more modify operations are executed in parallel, the
     * operations should be merged. In case that the operations are in
     * conflict (e.g. one operation adding a value and the other
     * removing the same value), the result is not deterministic.
     * 
     * The operation may fail if the modified object does not
     * conform to the underlying schema of the storage system or the
     * schema enforced by the implementation.
     * 
     * TODO: optimistic locking
     * 
	 * @param objectChange specification of object changes
	 * @param scripts scripts that should be executed before of after operation
	 * @param parentResult parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException specified object does not exist
	 * @throws SchemaException resulting object would violate the schema
	 * @throws IllegalArgumentException wrong OID format, described change is not applicable
	 */
	public void modifyObject(ObjectModificationType objectChange, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;
	
	/**
	 * Deleted object with provided OID.
     * Must fail if object with specified OID does not exists.
     * Should be atomic.
	 * 
	 * @param oid OID of object to delete
	 * @param parentResult parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException specified object does not exist
	 * @throws IllegalArgumentException wrong OID format, described change is not applicable
	 */
	public void deleteObject(String oid, OperationResult parentResult)
			throws ObjectNotFoundException;
	
	/**
	 * Returns list of available values for specified properties.
     * 
     * The returned values can be used as valid values for properties
     * of the specific object. The provided values can be used e.g.
     * for listing them in GUI list boxes, for early validation
     * (pre-validation), displaying help messages, auto-complete, etc.
     * 
     * In case the list of available values is too big or it is not
     * available, the empty list should be returned, setting the
     * "closed" flag to false.
	 * 
	 * @param oid OID of the object for which to determine values
	 * @param properties list of properties for which to determine values
	 * @param parentResult parentResult parent OperationResult (in/out)
	 * @return list of available values
	 * 
	 * @throws ObjectNotFoundException specified object does not exist
	 * @throws IllegalArgumentException wrong OID format
	 */
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid, PropertyReferenceListType properties, OperationResult parentResult)
			throws ObjectNotFoundException;
	
	/**
	 * Returns the User object representing owner of specified account
     * (account shadow).
     * 
     * May return null if there is no owner
     * specified for the account.
     * 
     * May only be called with OID of AccountShadow object.
     * 
     * Implements the backward "owns" association between account
     * shadow and user. Forward association is implemented by property
     * "account" of user object.
     * 
     * This is a "list" operation even though it may return at most
     * one owner. However the operation implies searching the repository
     * for an owner, which may be less efficient that following a direct
     * association. Hence it is called "list" to indicate that there
     * may be non-negligible overhead.
	 * 
	 * @param accountOid OID of account shadow
	 * @return User object representing owner of specified account
	 * 
 	 * @throws ObjectNotFoundException specified object does not exist
	 * @throws IllegalArgumentException wrong OID format
	 */
	public UserType listAccountShadowOwner(String accountOid) throws ObjectNotFoundException;
	
	/**
	 * Search for resource object shadows of a specified type that
     * belong to the specified resource. Returns a list of such object
     * shadows or empty list if nothing was found.
     * 
     * Implements the backward "has" association between resource and
     * resource object shadows. Forward association is implemented by
     * property "resource" of resource object shadow.
     * 
     * May only be called with OID of Resource object.
	 * 
	 * @param resourceOid OID of resource definition (ResourceType)
	 * @return resource object shadows of a specified type from specified resource
	 * 
	 * @throws ObjectNotFoundException specified object does not exist
	 * @throws IllegalArgumentException wrong OID format
	 */
	public List<ResourceObjectShadowType> listResourceObjectShadows(String resourceOid, Class resourceObjectShadowType) throws ObjectNotFoundException;
}
