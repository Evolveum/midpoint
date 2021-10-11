/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * <p>Identity Repository Interface.</p>
 * <p>
 * <ul>
 *   <li>Status: public</li>
 *   <li>Stability: stable</li>
 * </ul>
 *
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
 * @version 3.1.1
 */
public interface RepositoryService {

    String CLASS_NAME_WITH_DOT = RepositoryService.class.getName() + ".";
    String GET_OBJECT = CLASS_NAME_WITH_DOT + "getObject";
    String ADD_OBJECT = CLASS_NAME_WITH_DOT + "addObject";
    String DELETE_OBJECT = CLASS_NAME_WITH_DOT + "deleteObject";
    String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + "searchObjects";
    String SEARCH_CONTAINERS = CLASS_NAME_WITH_DOT + "searchContainers";
    String COUNT_CONTAINERS = CLASS_NAME_WITH_DOT + "countContainers";
    String MODIFY_OBJECT = CLASS_NAME_WITH_DOT + "modifyObject";
    String COUNT_OBJECTS = CLASS_NAME_WITH_DOT + "countObjects";
    String GET_VERSION = CLASS_NAME_WITH_DOT + "getVersion";
    String SEARCH_OBJECTS_ITERATIVE = CLASS_NAME_WITH_DOT + "searchObjectsIterative";
    String SEARCH_SHADOW_OWNER = CLASS_NAME_WITH_DOT + "searchShadowOwner";
    String ADVANCE_SEQUENCE = CLASS_NAME_WITH_DOT + "advanceSequence";
    String RETURN_UNUSED_VALUES_TO_SEQUENCE = CLASS_NAME_WITH_DOT + "returnUnusedValuesToSequence";
    String EXECUTE_QUERY_DIAGNOSTICS = CLASS_NAME_WITH_DOT + "executeQueryDiagnostics";
    String ADD_DIAGNOSTIC_INFORMATION = CLASS_NAME_WITH_DOT + "addDiagnosticInformation";
    String HAS_CONFLICT = CLASS_NAME_WITH_DOT + "hasConflict";

    String KEY_DIAG_DATA = "repositoryDiagData";            // see GetOperationOptions.attachDiagData
    String KEY_ORIGINAL_OBJECT = "repositoryOriginalObject";

    String OP_ADD_OBJECT = "addObject";
    String OP_DELETE_OBJECT = "deleteObject";
    String OP_COUNT_OBJECTS = "countObjects";
    String OP_MODIFY_OBJECT = "modifyObject";
    String OP_GET_VERSION = "getVersion";
    String OP_IS_ANY_SUBORDINATE = "isAnySubordinate";
    String OP_ADVANCE_SEQUENCE = "advanceSequence";
    String OP_RETURN_UNUSED_VALUES_TO_SEQUENCE = "returnUnusedValuesToSequence";
    String OP_EXECUTE_QUERY_DIAGNOSTICS = "executeQueryDiagnostics";
    String OP_GET_OBJECT = "getObject";
    String OP_SEARCH_SHADOW_OWNER = "searchShadowOwner";
    String OP_SEARCH_OBJECTS = "searchObjects";
    String OP_SEARCH_OBJECTS_ITERATIVE = "searchObjectsIterative";
    String OP_FETCH_EXT_ITEMS = "fetchExtItems";

    /**
     * Returns object for provided OID.
     * <p>
     * Must fail if object with the OID does not exists.
     *
     * @param oid OID of the object to get
     * @param parentResult parent OperationResult (in/out)
     * @return Object fetched from repository
     * @throws ObjectNotFoundException requested object does not exist
     * @throws SchemaException error dealing with storage schema
     * @throws IllegalArgumentException wrong OID format, etc.
     */
    @NotNull <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

//    <T extends ObjectType> PrismObject<T> getContainerValue(Class<T> type, String oid, long id,
//                                                            Collection<SelectorOptions<GetOperationOptions>> options,
//                                                            OperationResult parentResult)
//            throws ObjectNotFoundException, SchemaException;

    /**
     * Returns object version for provided OID.
     * <p>
     * Must fail if object with the OID does not exists.
     * <p>
     * This is a supposed to be a very lightweight and cheap operation. It is used to support
     * efficient caching of expensive objects.
     *
     * @param oid OID of the object to get
     * @param parentResult parent OperationResult (in/out)
     * @return Object version
     * @throws ObjectNotFoundException requested object does not exist
     * @throws SchemaException error dealing with storage schema
     * @throws IllegalArgumentException wrong OID format, etc.
     */
    <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
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
     * @param object object to create
     * @param parentResult parent OperationResult (in/out)
     * @return OID assigned to the created object
     * @throws ObjectAlreadyExistsException object with specified identifiers already exists, cannot add
     * @throws SchemaException error dealing with storage schema, e.g. schema violation
     * @throws IllegalArgumentException wrong OID format, etc.
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
     * @param query search query
     * @param parentResult parent OperationResult (in/out)
     * @return all objects of specified type that match search criteria (subject
     * to paging)
     * @throws IllegalArgumentException wrong object type
     * @throws SchemaException unknown property used in search query
     */

    @NotNull <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
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
     * @param query search query
     * @param handler result handler
     * @param strictlySequential takes care not to skip any object nor to process objects more than once; see below
     * @param parentResult parent OperationResult (in/out)
     * @return all objects of specified type that match search criteria (subject to paging)
     * @throws IllegalArgumentException wrong object type
     * @throws SchemaException unknown property used in search query
     * <p>
     * A note related to iteration method:
     * <p>
     * There are three iteration methods (see IterationMethodType):
     * - SINGLE_TRANSACTION: Fetches objects in single DB transaction. Not supported for all DBMSs.
     * - SIMPLE_PAGING: Uses the "simple paging" method: takes objects (e.g.) numbered 0 to 49, then 50 to 99,
     * then 100 to 149, and so on. The disadvantage is that if the order of objects is changed
     * during operation (e.g. by inserting/deleting some of them) then some objects can be
     * processed multiple times, where others can be skipped.
     * - STRICTLY_SEQUENTIAL_PAGING: Uses the "strictly sequential paging" method: sorting returned objects by OID. This
     * is (almost) reliable in such a way that no object would be skipped. However, custom
     * paging cannot be used in this mode.
     * <p>
     * If GetOperationOptions.iterationMethod is specified, it is used without any further considerations.
     * Otherwise, the repository configuration determines whether to use SINGLE_TRANSACTION or a paging. In the latter case,
     * strictlySequential flag determines between SIMPLE_PAGING (if false) and STRICTLY_SEQUENTIAL_PAGING (if true).
     * <p>
     * If explicit GetOperationOptions.iterationMethod is not provided, and paging is prescribed, and strictlySequential flag
     * is true and client-provided paging conflicts with the paging used by the iteration method, a warning is issued, and
     * iteration method is switched to SIMPLE_PAGING.
     * <p>
     * Sources of conflicts:
     * - ordering is specified
     * - offset is specified
     * (limit is not a problem)
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
     * @param query search query
     * @param parentResult parent OperationResult (in/out)
     * @return count of objects of specified type that match search criteria (subject
     * to paging)
     * @throws IllegalArgumentException wrong object type
     * @throws SchemaException unknown property used in search query
     */
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
     * <p>
     * TODO: optimistic locking
     * <p>
     * Note: the precondition is checked only if actual modification is going to take place
     * (not e.g. if the list of modifications is empty).
     *
     * @param parentResult parent OperationResult (in/out)
     * @throws ObjectNotFoundException specified object does not exist
     * @throws SchemaException resulting object would violate the schema
     * @throws ObjectAlreadyExistsException if resulting object would have name which already exists in another object of the same type
     * @throws IllegalArgumentException wrong OID format, described change is not applicable
     */
    @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, RepoModifyOptions options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications,
            ModificationPrecondition<T> precondition, RepoModifyOptions options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException;

    /**
     * <p>Deletes object with specified OID.</p>
     * <p>
     * Must fail if object with specified OID does not exists. Should be atomic.
     * </p>
     *
     * @param oid OID of object to delete
     * @param parentResult parent OperationResult (in/out)
     * @throws ObjectNotFoundException specified object does not exist
     * @throws IllegalArgumentException wrong OID format, described change is not applicable
     */
    @NotNull <T extends ObjectType> DeleteObjectResult deleteObject(Class<T> type, String oid, OperationResult parentResult) throws ObjectNotFoundException;

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
     * @param shadowOid OID of shadow
     * @param parentResult parentResult parent OperationResult (in/out)
     * @return Object representing owner of specified account (subclass of FocusType)
     * @throws IllegalArgumentException wrong OID format
     */
    <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult);

    /**
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
     * Runs a short, non-destructive repository self test.
     * This methods should never throw a (checked) exception. All the results
     * should be recorded under the provided result structure (including fatal errors).
     * <p>
     * This should implement ONLY self-tests that are IMPLEMENTATION-SPECIFIC. It must not
     * implement self-tests that are generic and applies to all repository implementations.
     * Such self-tests must be implemented in higher layers.
     * <p>
     * If the repository has no self-tests then the method should return immediately
     * without changing the result structure. It must not throw an exception in this case.
     */
    void repositorySelfTest(OperationResult parentResult);

    /**
     * Checks a closure for consistency, repairing any problems found.
     * This methods should never throw a (checked) exception. All the results
     * should be in the returned result structure (including fatal errors).
     * <p>
     * The current implementation expects closure to be of reasonable size - so
     * it could be fetched into main memory as well as recomputed online
     * (perhaps up to ~250K entries). In future, this method will be reimplemented.
     * <p>
     * BEWARE, this method locks out the M_ORG_CLOSURE table, so org-related operations
     * would wait until it completes.
     * <p>
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
            ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch);

    FullTextSearchConfigurationType getFullTextSearchConfiguration();

    void postInit(OperationResult result) throws SchemaException;

    ConflictWatcher createAndRegisterConflictWatcher(@NotNull String oid);

    void unregisterConflictWatcher(ConflictWatcher watcher);

    boolean hasConflict(ConflictWatcher watcher, OperationResult result);

    /**
     * Adds a diagnostic information, honoring cleanup rules (deleting obsolete records).
     */
    <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid, DiagnosticInformationType information, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    PerformanceMonitor getPerformanceMonitor();
}
