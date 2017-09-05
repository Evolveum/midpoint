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
package com.evolveum.midpoint.repo.api;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * <p>Identity Repository Interface.</p>
 * <p>
 * <ul>
 *   <li>Status: public</li>
 *   <li>Stability: stable</li>
 * </ul>
 * @version 3.1.1
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

    String CLASS_NAME_WITH_DOT = RepositoryService.class.getName() + ".";
    String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";
    @Deprecated
    String LIST_ACCOUNT_SHADOW = CLASS_NAME_WITH_DOT + "listAccountShadowOwner";
    String ADD_OBJECT = CLASS_NAME_WITH_DOT + "addObject";
    String DELETE_OBJECT = CLASS_NAME_WITH_DOT + "deleteObject";
    @Deprecated
    String CLAIM_TASK = CLASS_NAME_WITH_DOT + "claimTask";
    @Deprecated
    String RELEASE_TASK = CLASS_NAME_WITH_DOT + "releaseTask";
    String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + "searchObjects";
	String SEARCH_CONTAINERS = CLASS_NAME_WITH_DOT + "searchContainers";
	String COUNT_CONTAINERS = CLASS_NAME_WITH_DOT + "countContainers";
    String LIST_RESOURCE_OBJECT_SHADOWS = CLASS_NAME_WITH_DOT + "listResourceObjectShadows";
    String MODIFY_OBJECT = CLASS_NAME_WITH_DOT + "modifyObject";
    String COUNT_OBJECTS = CLASS_NAME_WITH_DOT + "countObjects";
    String GET_VERSION = CLASS_NAME_WITH_DOT + "getVersion";
    String SEARCH_OBJECTS_ITERATIVE = CLASS_NAME_WITH_DOT + "searchObjectsIterative";
    String SEARCH_SHADOW_OWNER = CLASS_NAME_WITH_DOT + "searchShadowOwner";
	String ADVANCE_SEQUENCE = CLASS_NAME_WITH_DOT + "advanceSequence";
	String RETURN_UNUSED_VALUES_TO_SEQUENCE = CLASS_NAME_WITH_DOT + "returnUnusedValuesToSequence";
	String EXECUTE_QUERY_DIAGNOSTICS = CLASS_NAME_WITH_DOT + "executeQueryDiagnostics";

	String KEY_DIAG_DATA = "repositoryDiagData";			// see GetOperationOptions.attachDiagData

	/**
	 * Returns object for provided OID.
	 *
	 * Must fail if object with the OID does not exists.
	 *
	 * @param oid
	 *            OID of the object to get
	 * @param parentResult
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
	<T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;

//	<T extends ObjectType> PrismObject<T> getContainerValue(Class<T> type, String oid, long id,
//															Collection<SelectorOptions<GetOperationOptions>> options,
//															OperationResult parentResult)
//			throws ObjectNotFoundException, SchemaException;

	/**
	 * Returns object version for provided OID.
	 *
	 * Must fail if object with the OID does not exists.
	 *
	 * This is a supposed to be a very lightweight and cheap operation. It is used to support
	 * efficient caching of expensive objects.
	 *
	 * @param oid
	 *            OID of the object to get
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return Object version
	 *
	 * @throws ObjectNotFoundException
	 *             requested object does not exist
	 * @throws SchemaException
	 *             error dealing with storage schema
	 * @throws IllegalArgumentException
	 *             wrong OID format, etc.
	 */
	<T extends ObjectType> String getVersion(Class<T> type,String oid, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException;

	<T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult);

	/**
	 * <p>Add new object.</p>
	 * <p>
	 * The OID provided in the input message may be empty. In that case the OID
	 * will be assigned by the implementation of this method and it will be
	 * provided as return value.
	 * </p><p>
	 * This operation should fail if such object already exists (if object with
	 * the provided OID already exists).
	 * </p><p>
	 * The operation may fail if provided OID is in an unusable format for the
	 * storage. Generating own OIDs and providing them to this method is not
	 * recommended for normal operation.
	 * </p><p>
	 * Should be atomic. Should not allow creation of two objects with the same
	 * OID (even if created in parallel).
	 * </p><p>
	 * The operation may fail if the object to be created does not conform to
	 * the underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * </p><p>
	 * Note: no need for explicit type parameter here. The object parameter contains the information.
	 * </p>
	 *
	 * @param object
	 *            object to create
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
	<T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException;


	/**
	 * <p>Search for objects in the repository.</p>
	 * <p>If no search criteria specified, list of all objects of specified type is returned.</p>
	 * <p>
	 * Searches through all object types.
	 * Returns a list of objects that match search criteria.
	 * </p><p>
	 * Returns empty list if object type is correct but there are no objects of
	 * that type. The ordering of the results is not significant and may be arbitrary
	 * unless sorting in the paging is used.
	 * </p><p>
	 * Should fail if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 *
	 * @param query
	 *            search query
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

	@NotNull
	<T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
			throws SchemaException;

	/**
	 * Search for "sub-object" structures, i.e. containers.
	 * Currently, only one type of search is available: certification case search.
	 */
	<T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException;

	/**
	 * <p>Search for objects in the repository in an iterative fashion.</p>
	 * <p>Searches through all object types. Calls a specified handler for each object found.
	 * If no search criteria specified, list of all objects of specified type is returned.</p>
	 * <p>
	 * Searches through all object types.
	 * Returns a list of objects that match search criteria.
	 * </p><p>
	 * Returns empty list if object type is correct but there are no objects of
	 * that type. The ordering of the results is not significant and may be arbitrary
	 * unless sorting in the paging is used.
	 * </p><p>
	 * Should fail if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 *
	 * @param query
	 *            search query
	 * @param handler
	 *            result handler
	 * @param strictlySequential
	 * 			  takes care not to skip any object nor to process objects more than once;
	 * 			  currently requires paging NOT to be used - uses its own paging
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

	<T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
			ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
			OperationResult parentResult)
			throws SchemaException;

	/**
	 * <p>Returns the number of objects that match specified criteria.</p>
	 * <p>If no search criteria specified, count of all objects of specified type is returned.</p>
     * <p>
	 * Should fail if object type is wrong. Should fail if unknown property is
	 * specified in the query.
	 * </p>
	 *
	 * @param query
	 *            search query
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 * @return count of objects of specified type that match search criteria (subject
	 *         to paging)
	 *
	 * @throws IllegalArgumentException
	 *             wrong object type
	 * @throws SchemaException
	 *             unknown property used in search query
	 */
	<T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult)
			throws SchemaException;

	<T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options,
			OperationResult parentResult) throws SchemaException;

	boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids) throws SchemaException;

	<O extends ObjectType> boolean isDescendant(PrismObject<O> object, String orgOid) throws SchemaException;

	<O extends ObjectType> boolean isAncestor(PrismObject<O> object, String oid) throws SchemaException;

	/**
	 * <p>Modifies object using relative change description.</p>
	 * Must fail if user with
	 * provided OID does not exists. Must fail if any of the described changes
	 * cannot be applied. Should be atomic.
	 * </p><p>
	 * If two or more modify operations are executed in parallel, the operations
	 * should be merged. In case that the operations are in conflict (e.g. one
	 * operation adding a value and the other removing the same value), the
	 * result is not deterministic.
	 * </p><p>
	 * The operation may fail if the modified object does not conform to the
	 * underlying schema of the storage system or the schema enforced by the
	 * implementation.
	 * </p>
	 *
	 * TODO: optimistic locking
	 *
	 * Note: the precondition is checked only if actual modification is going to take place
	 * (not e.g. if the list of modifications is empty).
	 *
	 * @param parentResult
	 *            parent OperationResult (in/out)
	 *
	 * @throws ObjectNotFoundException
	 *             specified object does not exist
	 * @throws SchemaException
	 *             resulting object would violate the schema
     * @throws ObjectAlreadyExistsException
     *             if resulting object would have name which already exists in another object of the same type
	 * @throws IllegalArgumentException
	 *             wrong OID format, described change is not applicable
	 */
	<T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

	<T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, RepoModifyOptions options, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

	<T extends ObjectType> void modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
			ModificationPrecondition<T> precondition, RepoModifyOptions options, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException;

	/**
	 * <p>Deletes object with specified OID.</p>
	 * <p>
	 * Must fail if object with specified OID does not exists. Should be atomic.
	 * </p>
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
	<T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult) throws ObjectNotFoundException;

	/**
	 * <p>Returns the User object representing owner of specified account (account
	 * shadow).</p>
	 * <p>
	 * May return null if there is no owner specified for the account.
	 * </p><p>
	 * May only be called with OID of AccountShadow object.
	 * </p><p>
	 * Implements the backward "owns" association between account shadow and
	 * user. Forward association is implemented by property "account" of user
	 * object.
	 * </p><p>
	 * This is a "list" operation even though it may return at most one owner.
	 * However the operation implies searching the repository for an owner,
	 * which may be less efficient that following a direct association. Hence it
	 * is called "list" to indicate that there may be non-negligible overhead.
	 * </p>
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
	@Deprecated
	PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException;

	/**
	 * <p>Returns the object representing owner of specified shadow.</p>
	 * <p>
	 * Implements the backward "owns" association between account shadow and
	 * user. Forward association is implemented by linkRef reference in subclasses
	 * of FocusType.
	 * </p
	 * <p>
	 * Returns null if there is no owner for the shadow.
	 * </p>
	 * <p>
	 * This is a "search" operation even though it may return at most one owner.
	 * However the operation implies searching the repository for an owner,
	 * which may be less efficient that following a direct association. Hence it
	 * is called "search" to indicate that there may be non-negligible overhead.
	 * </p>
	 * <p>
	 * This method should not die even if the specified shadow does not exist.
	 * Even if the shadow is gone, it still may be used in some linkRefs. This
	 * method should be able to find objects with such linkeRefs otherwise we
	 * will not be able to do proper cleanup.
	 * </p>
	 *
	 * @param shadowOid
	 *            OID of shadow
	 * @param parentResult
	 *            parentResult parent OperationResult (in/out)
	 * @return Object representing owner of specified account (subclass of FocusType)
	 *
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 */
	<F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult);

	/**
	 * <p>Search for resource object shadows of a specified type that belong to the
	 * specified resource.</p>
	 * <p>
	 * Returns a list of such object shadows or empty list
	 * if nothing was found.
	 * </p><p>
	 * Implements the backward "has" association between resource and resource
	 * object shadows. Forward association is implemented by property "resource"
	 * of resource object shadow.
	 * </p><p>
	 * May only be called with OID of Resource object.
	 * </p>
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
     * @throws SchemaException
     *             found object is not type of {@link ShadowType}
	 * @throws IllegalArgumentException
	 *             wrong OID format
	 */
	<T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException;

	/**
	 *
	 * This operation is guaranteed to be atomic. If two threads or even two nodes request a value from
	 * the same sequence at the same time then different values will be returned.
	 *
	 * @param oid sequence OID
	 * @param parentResult Operation result
	 * @return next unallocated counter value
	 * @throws ObjectNotFoundException the sequence does not exist
	 * @throws SchemaException the sequence cannot produce a value (e.g. maximum counter reached)
	 */
	long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

	/**
	 *
	 * The sequence may ignore the values, e.g. if value re-use is disabled or when the list of
	 * unused values is full. In such a case the values will be ignored silently and no error is indicated.
	 *
	 * @param oid sequence OID
	 * @param unusedValues values to return
	 * @param parentResult Operation result
	 */
	void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult) throws ObjectNotFoundException, SchemaException;

    /**
	 * Provide repository run-time configuration and diagnostic information.
	 */
    RepositoryDiag getRepositoryDiag();

    /**
	 * Runs a short, non-descructive repository self test.
	 * This methods should never throw a (checked) exception. All the results
	 * should be recorded under the provided result structure (including fatal errors).
	 *
	 * This should implement ONLY self-tests that are IMPLEMENTATION-SPECIFIC. It must not
	 * implement self-tests that are generic and applies to all repository implementations.
	 * Such self-tests must be implemented in higher layers.
	 *
	 * If the repository has no self-tests then the method should return immediately
	 * without changing the result structure. It must not throw an exception in this case.
	 */
    void repositorySelfTest(OperationResult parentResult);

    /**
     * Checks a closure for consistency, repairing any problems found.
     * This methods should never throw a (checked) exception. All the results
     * should be in the returned result structure (including fatal errors).
     *
     * The current implementation expects closure to be of reasonable size - so
     * it could be fetched into main memory as well as recomputed online
     * (perhaps up to ~250K entries). In future, this method will be reimplemented.
     *
     * BEWARE, this method locks out the M_ORG_CLOSURE table, so org-related operations
     * would wait until it completes.
     *
     * TODO this method is SQL service specific; it should be generalized/fixed somehow.
     */
    void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult);

	/**
	 * A bit of hack - execute arbitrary query, e.g. hibernate query in case of SQL repository.
	 * Use with all the care!
	 *
	 * @param request Diagnostics request
	 * @param result Operation result
	 * @return diagnostics response
	 */
	RepositoryQueryDiagResponse executeQueryDiagnostics(RepositoryQueryDiagRequest request, OperationResult result);

	<O extends ObjectType> boolean selectorMatches(ObjectSelectorType objectSelector, PrismObject<O> object,
			Trace logger, String logMessagePrefix) throws SchemaException;

	/**
	 * Returns matching rule supported by the repository for a given data type (String, PolyString, ...), for
	 * originally intended matching rule.
	 *
	 * New matching rule must NOT be less selective than the original one. I.e. if values V1, V2 would not match
	 * under the original one, they must not also match under the replacement. Therefore it is safe to replace
	 * distinguishedName with stringIgnoreCase (but not e.g. the other way around; nor exchangeEmailAddresses
	 * can be replaced by stringIgnoreCase, because the prefix part is case sensitive).
	 *
	 * The assumption is that for unsupported matching rules the repository will store normalized values. And it
	 * will normalize any values that are used in queries. This is the obligation of the client. So, theoretically,
	 * it is safe to replace any such matching rule with default (exact) matching rule. But if we replace it with
	 * something that does not return false positives (i.e. something that is not less sensitive), we get some
	 * resiliency w.r.t. non-normalized values in repository. TODO TODO TODO think again
	 *
	 * If the original matching rule is not supported by the given data type (e.g. trying to use exchangeEmailAddress
	 * on PolyString), the result may be arbitrary. TODO think again also about this
	 */
	QName getApproximateSupportedMatchingRule(Class<?> dataType, QName originalMatchingRule);

	void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch);

	FullTextSearchConfigurationType getFullTextSearchConfiguration();

	void postInit(OperationResult result) throws SchemaException;

	ConflictWatcher createAndRegisterConflictWatcher(String oid);

	void unregisterConflictWatcher(ConflictWatcher watcher);

	boolean hasConflict(ConflictWatcher watcher, OperationResult result);
}
