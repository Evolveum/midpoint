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
package com.evolveum.midpoint.repo.api;

import java.util.List;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
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
 * <p>Identity Repository Interface.</p>
 * <p>
 * Status: public
 * Stability: draft
 * @version 0.3
 * @author Radovan Semancik
 * </p><p>
 * This service provides repository for objects that are commonly found
 * in identity management deployments. It is used for storage and retrieval
 * of objects. It also supports modifications (relative changes), searching
 * and basic coordination.
 * </p><p>
 * Supported object types:
 * <ul>
 *         <li>All object types from Common Schema</li>
 *         <li>All object types from Identity Schema</li>
 *         <li>All object types from IDM Model Schema</li>
 * </ul>
 * </p><p>
 * Identity repository may add some kind of basic logic in addition to a
 * pure storage of data. E.g. it may check referential consistency,
 * validate schema, etc.
 * </p><p>
 * The implementation may store the objects and properties in any suitable
 * way and it is not required to check any schema beyond the basic common schema
 * structures. However, the implementation MAY be able to check additional
 * schema definitions, e.g. to check for mandatory and allowed properties
 * and property types. This may be either explicit (e.g. implementation checking
 * against provided XML schema) or implicit, conforming to the constraints of
 * the underlying storage (e.g. LDAP schema enforced by underlying directory server).
 * One way or another, the implementation may fail to store the objects that violate
 * the schema. The method how the schemas are "loaded" to the implementation is not
 * defined by this interface. This interface even cannot "reveal" the schema to its
 * users (at least not now). Therefore clients of this interface must be prepared to
 * handle schema violation errors.
 * </p><p>
 * The implementation is not required to index the data or provide any other
 * optimizations. This depends on the specific implementation, its configuration
 * and the underlying storage system. Qualitative constraints (such as performance)
 * are NOT defined by this interface definition.
 * </p>
 * <h1>Naming Conventions</h1>
 * <p>
 * operations should be named as &lt;operation&gt;&lt;objectType&gt; e.g. addUser,
 * modifyAccount, searchObjects. The operations that returns single object
 * instance or works on single object should be named in singular (e.g. addUser).
 * The operation that return multiple instances should be named in plural (e.g. listObjects).
 * Operations names should be unified as well:
 * <ul>
 *        <li>add, modify, delete - writing to repository, single object, need OID</li>
 *        <li>get - retrieving single object by OID</li>
 *        <li>list - returning all objects, no or fixed search criteria</li>
 *        <li>search - returning subset of objects with flexible search criteria</li>
 * </ul>
 * </p>
 * <h1>Notes</h1>
 * <p>
 * The definition of this interface is somehow "fuzzy" at places. E.g.
 * allowing schema-aware implementation but not mandating it, recommending
 * to remove duplicates, but tolerating them, etc. The reason for this is
 * to have better fit to the underlying storage mechanisms and therefore
 * more efficient and simpler implementation. It may complicate the clients
 * if the code needs to be generic and fit each and every implementation of
 * this interface. However, such code will be quite rare. Most of the custom code
 * will be developed to work on a specific storage (e.g. Oracle DB or LDAP)
 * and therefore can be made slightly implementation-specific. Changing the
 * storage in a running IDM system is extremely unlikely.
 * </p>
 * <h1>TODO</h1>
 * <p>
 * <ul>
 *  <li>TODO: Atomicity, consistency</li>
 *  <li>TODO: security constraints</li>
 *  <li>TODO: inherently thread-safe</li>
 *  <li>TODO: note about distributed storage systems and weak/eventual consistency</li>
 *  <li>TODO: task coordination</li>
 * </ul>
 * </p> 
 */
public interface RepositoryService {

	/**
	 * Returns object for provided OID.
	 * 
	 * Must fail if object with the OID does not exists.
	 * 
	 * @param oid
	 *            OID of the object to get
	 * @param resolve
	 *            list of properties to resolve in the fetched object
	 * @param result
	 *            parent OperationResult (in/out)
	 * @return Object fetched from repository
	 * 
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws SchemaException
	 *             error dealing with storage schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 */
	public <T> T getObject(Class<T> type,String oid, PropertyReferenceListType resolve, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;
	
	@Deprecated
	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult parentResult)
	throws ObjectNotFoundException, SchemaException;

	/**
	 * Add new object.
	 * 
	 * The OID provided in the input message may be empty. In that case the OID
	 * will be assigned by the implementation of this method and it will be
	 * provided as return value.
	 * 
	 * This operation should fail if such object already exists (if object with
	 * the provided OID already exists).
	 * 
	 * The operation may fail if provided OID is in an unusable format for the
	 * storage. Generating own OIDs and providing them to this method is not
	 * recommended for normal operation.
	 * 
	 * Should be atomic. Should not allow creation of two objects with the same
	 * OID (even if created in parallel).
	 * 
	 * The operation may fail if the object to be created does not conform to
	 * the underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * 
	 * @param object
	 *            object to create
	 * @param scripts
	 *            scripts to execute before/after the operation
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return OID assigned to the created object
	 * 
	 * @throws ObjectAlreadyExistsException
	 *             object with specified identifiers already exists, cannot add
	 * @throws SchemaException
	 *             error dealing with storage schema, e.g. schema violation
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 */
	public String addObject(ObjectType object, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException;

	/**
	 * Returns all objects of specified type in the repository.
	 * 
	 * This can be considered as a simplified search operation.
	 * 
	 * Returns empty list if object type is correct but there are no objects of
	 * that type. The ordering of the results is not significant and may be arbitrary
	 * unless sorting in the paging is used.
	 * 
	 * Should fail if object type is wrong.
	 * 
	 * @param objectType
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return all objects of specified type (subject to paging)
	 * 
	 * @throws IllegalArgumentException
	 *             wrong object type
	 */
	public ObjectListType listObjects(Class objectType, PagingType paging, OperationResult parentResult);

	/**
	 * Search for objects in the repository. Searches through all object types.
	 * Returns a list of objects that match search criteria.
	 * 
	 * Returns empty list if object type is correct but there are no objects of
	 * that type. The ordering of the results is not significant and may be arbitrary
	 * unless sorting in the paging is used.
	 * 
	 * Should fail if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * 
	 * @param query
	 *            search query
	 * @param paging
	 *            paging specification to limit operation result (optional)
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return all objects of specified type that match search criteria (subject
	 *         to paging)
	 * 
	 * @throws IllegalArgumentException
	 *             wrong object type
	 * @throws SchemaException
	 *             unknown property used in search query
	 */
	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException;

	/**
	 * Modifies object using relative change description. Must fail if user with
	 * provided OID does not exists. Must fail if any of the described changes
	 * cannot be applied. Should be atomic.
	 * 
	 * If two or more modify operations are executed in parallel, the operations
	 * should be merged. In case that the operations are in conflict (e.g. one
	 * operation adding a value and the other removing the same value), the
	 * result is not deterministic.
	 * 
	 * The operation may fail if the modified object does not conform to the
	 * underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * 
	 * TODO: optimistic locking
	 * 
	 * @param objectChange
	 *            specification of object changes
	 * @param scripts
	 *            scripts that should be executed before of after operation
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws SchemaException
	 *             resulting object would violate the schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 */
	public void modifyObject(ObjectModificationType objectChange, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;

	/**
	 * Deletes object with specified OID. Must fail if object with specified OID
	 * does not exists. Should be atomic.
	 * 
	 * @param oid
	 *            OID of object to delete
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 */
	public void deleteObject(String oid, OperationResult parentResult) throws ObjectNotFoundException;

	/**
	 * Returns list of available values for specified properties.
	 * 
	 * The returned values can be used as valid values for properties of the
	 * specific object. The provided values can be used e.g. for listing them in
	 * GUI list boxes, for early validation (pre-validation), displaying help
	 * messages, auto-complete, etc.
	 * 
	 * In case the list of available values is too big or it is not available,
	 * the empty list should be returned, setting the "closed" flag to false.
	 * 
	 * @param oid
	 *            OID of the object for which to determine values
	 * @param properties
	 *            list of properties for which to determine values
	 * @param parentResult
	 *            parentResult parent OperationResult (in/out)
	 * @return list of available values
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 */
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult parentResult)
			throws ObjectNotFoundException;

	/**
	 * Returns the User object representing owner of specified account (account
	 * shadow).
	 * 
	 * May return null if there is no owner specified for the account.
	 * 
	 * May only be called with OID of AccountShadow object.
	 * 
	 * Implements the backward "owns" association between account shadow and
	 * user. Forward association is implemented by property "account" of user
	 * object.
	 * 
	 * This is a "list" operation even though it may return at most one owner.
	 * However the operation implies searching the repository for an owner,
	 * which may be less efficient that following a direct association. Hence it
	 * is called "list" to indicate that there may be non-negligible overhead.
	 * 
	 * @param accountOid
	 *            OID of account shadow
	 * @param parentResult
	 *            parentResult parent OperationResult (in/out)
	 * @return User object representing owner of specified account
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 */
	public UserType listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException;

	/**
	 * Search for resource object shadows of a specified type that belong to the
	 * specified resource. Returns a list of such object shadows or empty list
	 * if nothing was found.
	 * 
	 * Implements the backward "has" association between resource and resource
	 * object shadows. Forward association is implemented by property "resource"
	 * of resource object shadow.
	 * 
	 * May only be called with OID of Resource object.
	 * 
	 * @param resourceOid
	 *            OID of resource definition (ResourceType)
	 * @param parentResult
	 *            parentResult parent OperationResult (in/out)
	 * @return resource object shadows of a specified type from specified
	 *         resource
	 * 
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 */
	public <T extends ResourceObjectShadowType> List<T> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException;
	
	/**
	 * Claim a task.
	 * 
	 * The task can be claimed only by a single node in the cluster. Attempt to claim an
	 * already claimed task results in an exception. The claim must be atomic. It is kind
	 * of a lock for the system.
	 * 
	 * TODO: better description
	 * 
	 * @param oid task OID
	 * @param parentResult parentResult parent OperationResult (in/out)
	 * @throws ObjectNotFoundException the task with specified OID was not found
	 * @throws ConcurrencyException attempt to claim already claimed task
	 * @throws SchemaException error dealing with storage schema
	 * @throws IllegalArgumentException
	 *             wrong OID format or a specified object is not a task
	 */
	public void claimTask(String oid, OperationResult parentResult) throws ObjectNotFoundException, ConcurrencyException, SchemaException;
	
	/**
	 * Release a claimed task.
	 * 
	 * TODO: better description
	 * 
	 * Note: Releasing a task that is not claimed is not an error. Warning should be logged, but this
	 * should not throw any exception.
	 *  
	 * @param oid task OID
	 * @param parentResult parentResult parent OperationResult (in/out)
	 * @throws ObjectNotFoundException the task with specified OID was not found
	 * @throws SchemaException error dealing with storage schema
	 * @throws IllegalArgumentException
	 *             wrong OID format or a specified object is not a task
	 */
	public void releaseTask(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;
}
