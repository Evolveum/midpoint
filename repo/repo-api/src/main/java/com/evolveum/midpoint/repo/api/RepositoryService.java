/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.repo.api.util.AccessCertificationSupportMixin;
import com.evolveum.midpoint.repo.api.util.CaseSupportMixin;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.eval.OrgTreeEvaluator;

import com.evolveum.midpoint.schema.selector.spec.ValueSelector;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.schema.selector.eval.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * <p>Identity Repository Interface.</p>
 * <ul>
 *   <li>Status: public</li>
 *   <li>Stability: stable</li>
 * </ul>
 * <p>
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
 *
 * @author Radovan Semancik
 * @version 3.1.1
 */
public interface RepositoryService extends OrgTreeEvaluator, CaseSupportMixin, AccessCertificationSupportMixin {

    String CLASS_NAME_WITH_DOT = RepositoryService.class.getName() + ".";

    String OP_ADD_OBJECT = "addObject";
    String OP_ADD_OBJECT_OVERWRITE = "addObjectOverwrite"; // addObject with overwrite option
    String OP_DELETE_OBJECT = "deleteObject";
    String OP_COUNT_OBJECTS = "countObjects";
    String OP_MODIFY_OBJECT = "modifyObject";
    String OP_MODIFY_OBJECT_DYNAMICALLY = "modifyObjectDynamically";
    String OP_GET_VERSION = "getVersion";
    String OP_IS_DESCENDANT = "isDescendant";
    String OP_IS_ANCESTOR = "isAncestor";
    String OP_ADVANCE_SEQUENCE = "advanceSequence";
    String OP_RETURN_UNUSED_VALUES_TO_SEQUENCE = "returnUnusedValuesToSequence";
    String OP_ALLOCATE_CONTAINER_IDENTIFIERS = "allocateContainerIdentifiers";
    String OP_EXECUTE_QUERY_DIAGNOSTICS = "executeQueryDiagnostics";
    String OP_GET_OBJECT = "getObject";
    String OP_SEARCH_OBJECTS = "searchObjects";
    String OP_SEARCH_OBJECTS_ITERATIVE = "searchObjectsIterative";
    String OP_SEARCH_OBJECTS_ITERATIVE_PAGE = "searchObjectsIterativePage";
    String OP_SEARCH_CONTAINERS = "searchContainers";

    String OP_SEARCH_CONTAINERS_ITERATIVE = "searchContainersIterative";
    String OP_SEARCH_CONTAINERS_ITERATIVE_PAGE = "searchContainersIterativePage";
    String OP_COUNT_CONTAINERS = "countContainers";
    String OP_SEARCH_REFERENCES = "searchReferences";


    String OP_SEARCH_REFERENCES_ITERATIVE = "searchReferencesIterative";
    String OP_SEARCH_REFERENCES_ITERATIVE_PAGE = "searchReferencesIterativePage";
    String OP_COUNT_REFERENCES = "countReferences";

    String OP_SEARCH_AGGREGATE = "searchAggregate";
    String OP_COUNT_AGGREGATE = "countAggregate";


    String OP_FETCH_EXT_ITEMS = "fetchExtItems";
    String OP_ADD_DIAGNOSTIC_INFORMATION = "addDiagnosticInformation";
    String OP_HAS_CONFLICT = "hasConflict";
    String OP_REPOSITORY_SELF_TEST = "repositorySelfTest";
    String OP_TEST_ORG_CLOSURE_CONSISTENCY = "testOrgClosureConsistency";

    // Not used by new repo, instead class specific prefix + OP_* constants are used
    String GET_OBJECT = CLASS_NAME_WITH_DOT + OP_GET_OBJECT;
    String ADD_OBJECT = CLASS_NAME_WITH_DOT + OP_ADD_OBJECT;
    String DELETE_OBJECT = CLASS_NAME_WITH_DOT + OP_DELETE_OBJECT;
    String SEARCH_OBJECTS = CLASS_NAME_WITH_DOT + OP_SEARCH_OBJECTS;
    String SEARCH_CONTAINERS = CLASS_NAME_WITH_DOT + OP_SEARCH_CONTAINERS;

    String SEARCH_AGGREGATE = CLASS_NAME_WITH_DOT + OP_SEARCH_AGGREGATE;
    String COUNT_AGGREGATE = CLASS_NAME_WITH_DOT + OP_COUNT_AGGREGATE;
    String COUNT_CONTAINERS = CLASS_NAME_WITH_DOT + OP_COUNT_CONTAINERS;
    String MODIFY_OBJECT = CLASS_NAME_WITH_DOT + OP_MODIFY_OBJECT;
    String COUNT_OBJECTS = CLASS_NAME_WITH_DOT + OP_COUNT_OBJECTS;
    String MODIFY_OBJECT_DYNAMICALLY = CLASS_NAME_WITH_DOT + OP_MODIFY_OBJECT_DYNAMICALLY;
    String GET_VERSION = CLASS_NAME_WITH_DOT + OP_GET_VERSION;
    String SEARCH_OBJECTS_ITERATIVE = CLASS_NAME_WITH_DOT + OP_SEARCH_OBJECTS_ITERATIVE;
    String ADVANCE_SEQUENCE = CLASS_NAME_WITH_DOT + OP_ADVANCE_SEQUENCE;
    String RETURN_UNUSED_VALUES_TO_SEQUENCE = CLASS_NAME_WITH_DOT + OP_RETURN_UNUSED_VALUES_TO_SEQUENCE;
    String EXECUTE_QUERY_DIAGNOSTICS = CLASS_NAME_WITH_DOT + OP_EXECUTE_QUERY_DIAGNOSTICS;
    String ADD_DIAGNOSTIC_INFORMATION = CLASS_NAME_WITH_DOT + OP_ADD_DIAGNOSTIC_INFORMATION;
    String HAS_CONFLICT = CLASS_NAME_WITH_DOT + OP_HAS_CONFLICT;

    String KEY_DIAG_DATA = "repositoryDiagData"; // see GetOperationOptions.attachDiagData
    String KEY_ORIGINAL_OBJECT = "repositoryOriginalObject";

    Trace LOGGER = TraceManager.getTrace(RepositoryService.class);

    /**
     * Returns object for provided OID.
     *
     * Must fail if object with the OID does not exist.
     *
     * @param oid OID of the object to get
     * @param parentResult parent OperationResult (in/out)
     * @return Object fetched from repository
     * @throws ObjectNotFoundException requested object does not exist
     * @throws SchemaException error dealing with storage schema
     * @throws IllegalArgumentException wrong OID format, etc.
     */
    @NotNull <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException;

    /**
     * Returns object version for provided OID.
     * <p>
     * Must fail if object with the OID does not exist.
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

    // Add/modify/delete

    /**
     * Add new object.
     *
     * The OID provided in the input message may be empty.
     * In that case the OID will be assigned by the implementation of this method
     * and it will be provided as return value.
     *
     * This operation should fail if such object already exists (if object with
     * the provided OID already exists).
     * Overwrite is possible if {@link RepoAddOptions#isOverwrite()} is true, but only
     * for the object of the same type.
     *
     * The operation may fail if provided OID is in an unusable format for the storage.
     * Generating own OIDs and providing them to this method is not
     * recommended for normal operation.
     *
     * Should be atomic.
     * Should not allow creation of two objects with the same OID (even if created in parallel).
     *
     * The operation may fail if the object to be created does not conform to the underlying
     * schema of the storage system or the schema enforced by the implementation.
     *
     * Note: no need for explicit type parameter here.
     * The object parameter contains the information.
     *
     * @param object object to create
     * @param parentResult parent OperationResult (in/out)
     * @return OID assigned to the created object
     * @throws ObjectAlreadyExistsException object with specified identifiers already exists, cannot add
     * @throws SchemaException error dealing with storage schema, e.g. schema violation
     * @throws IllegalArgumentException wrong OID format, etc.
     */
    @NotNull <T extends ObjectType> String addObject(
            @NotNull PrismObject<T> object,
            RepoAddOptions options,
            @NotNull OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException;

    /**
     * <p>Modifies object using relative change description.</p>
     * Must fail if user with provided OID does not exist.
     * Must fail if any of the described changes cannot be applied.
     * Should be atomic.
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
    @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable RepoModifyOptions options,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException;

    @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable ModificationPrecondition<T> precondition,
            @Nullable RepoModifyOptions options,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException;

    /**
     * Modifies an object dynamically. This means that the deltas are not provided by the caller, but computed by specified
     * supplier, based on the current object state.
     *
     * This is to allow more complex atomic modifications with low overhead: Instead of calling getObject + compute deltas +
     * modifyObject (with precondition that the object has not changed in the meanwhile) + repeating if the precondition fails,
     * we now simply use modifyObjectDynamically that does all of this within a single DB transaction.
     *
     * BEWARE: Do not use unless really needed. Use modifyObject method instead.
     *
     * @param type Type of the object to modify
     * @param oid OID of the object to modify
     * @param getOptions Options to use when getting the original object state
     * @param modificationsSupplier Supplier of the modifications (item deltas) to be applied on the object
     * @param modifyOptions Options to be used when modifying the object
     * @param parentResult Operation result into which we put our result
     */
    @Experimental
    @NotNull
    default <T extends ObjectType> ModifyObjectResult<T> modifyObjectDynamically(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> getOptions,
            @NotNull ModificationsSupplier<T> modificationsSupplier,
            @Nullable RepoModifyOptions modifyOptions,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    @FunctionalInterface
    interface ModificationsSupplier<T extends ObjectType> {
        @NotNull Collection<? extends ItemDelta<?, ?>> get(T object) throws SchemaException;
    }

    /**
     * <p>Deletes object with specified OID.</p>
     * <p>
     * Must fail if object with specified OID does not exist. Should be atomic.
     * </p>
     *
     * @param oid OID of object to delete
     * @param result parent OperationResult (in/out)
     * @throws ObjectNotFoundException specified object does not exist
     * @throws IllegalArgumentException wrong OID format, described change is not applicable
     */
    @NotNull <T extends ObjectType> DeleteObjectResult deleteObject(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException;

    @Experimental
    default ModifyObjectResult<SimulationResultType> deleteSimulatedProcessedObjects(
            String oid, @Nullable String transactionId, OperationResult result) throws SchemaException, ObjectNotFoundException {
        throw new UnsupportedOperationException("Not supported yet");
    }

    // Counting/searching

    <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult);

    /**
     * Search for "sub-object" structures, i.e. containers.
     */
    @NotNull <T extends Containerable> SearchResultList<T> searchContainers(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException;

    /**
     * Reference count - currently supporting roleMembershipRef and linkRef search.
     * See {@link #searchReferences(ObjectQuery, Collection, OperationResult)} for more details.
     *
     * @param query mandatory query
     */
    int countReferences(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult);

    /**
     * Reference search - currently supporting roleMembershipRef and linkRef search.
     * This returns reference objects extracted from the actual object(s) that own them,
     * but selection of which (and cardinality of the result list) is based on a repository search.
     *
     * Query must not be null and its filter must be:
     *
     * * either a OWNER-BY filter,
     * * or AND filter containing exactly one OWNER-BY filter and optionally one or more REF filters with empty path (self).
     *
     * @param query mandatory query with exactly one root OWNER-BY and additional REF filters
     */
    @NotNull SearchResultList<ObjectReferenceType> searchReferences(
            @NotNull ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException;

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
    @NotNull <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
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

    /**
     * Search for objects in the repository in an iterative fashion.
     *
     * Searches through all object types. Calls a specified handler for each object found.
     * If no search criteria specified, list of all objects of specified type is returned.
     *
     * Searches through all object types.
     * Returns a list of objects that match search criteria.
     *
     * Returns empty list if object type is correct but there are no objects of
     * that type. The ordering of the results is not significant and may be arbitrary
     * unless sorting in the paging is used.
     *
     * Should fail if object type is wrong. Should fail if unknown property is
     * specified in the query.
     *
     * [NOTE]
     * ====
     * New repository uses single reliable iteration method similar to strictly sequential paging
     * and supports custom ordering (currently only one).
     * New repository ignores strictlySequential parameter and related get options completely.
     *
     * In old repository there are three iteration methods (see IterationMethodType):
     *
     * - SINGLE_TRANSACTION: Fetches objects in single DB transaction. Not supported for all DBMSs.
     * - SIMPLE_PAGING: Uses the "simple paging" method: takes objects (e.g.) numbered 0 to 49, then 50 to 99,
     * then 100 to 149, and so on. The disadvantage is that if the order of objects is changed
     * during operation (e.g. by inserting/deleting some of them) then some objects can be
     * processed multiple times, where others can be skipped.
     * - STRICTLY_SEQUENTIAL_PAGING: Uses the "strictly sequential paging" method: sorting returned objects by OID. This
     * is (almost) reliable in such a way that no object would be skipped. However, custom
     * paging cannot be used in this mode.
     *
     * If GetOperationOptions.iterationMethod is specified, it is used without any further considerations.
     * Otherwise, the repository configuration determines whether to use SINGLE_TRANSACTION or a paging. In the latter case,
     * strictlySequential flag determines between SIMPLE_PAGING (if false) and STRICTLY_SEQUENTIAL_PAGING (if true).
     *
     * If explicit GetOperationOptions.iterationMethod is not provided, and paging is prescribed, and strictlySequential flag
     * is true and client-provided paging conflicts with the paging used by the iteration method, a warning is issued, and
     * iteration method is switched to SIMPLE_PAGING.
     * ====
     *
     * Sources of conflicts:
     * - ordering is specified
     * - offset is specified
     * (limit is not a problem)
     *
     * @param query search query
     * @param handler result handler
     * @param strictlySequential takes care not to skip any object nor to process objects more than once
     * @param parentResult parent OperationResult (in/out)
     * @return summary information about the search result
     * @throws IllegalArgumentException wrong object type
     * @throws SchemaException unknown property used in search query
     */
    <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
            OperationResult parentResult)
            throws SchemaException;

    /**
     * Executes iterative container search using the provided `handler` to process each container.
     *
     * @param query search query
     * @param handler result handler
     * @param options get options to use for the search
     * @param parentResult parent OperationResult (in/out)
     * @return summary information about the search result
     */
    @Experimental
    <T extends Containerable> SearchResultMetadata searchContainersIterative(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<T> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException;

    /**
     * Executes iterative reference search using the provided `handler` to process each references.
     *
     * @param query search query
     * @param handler result handler
     * @param options get options to use for the search
     * @param parentResult parent OperationResult (in/out)
     * @return summary information about the search result
     */
    @Experimental
    SearchResultMetadata searchReferencesIterative(
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<ObjectReferenceType> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException;

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
     * method should be able to find objects with such linkRefs otherwise we
     * will not be able to do proper cleanup.
     * </p>
     *
     * @param shadowOid OID of shadow
     * @param parentResult parentResult parent OperationResult (in/out)
     * @return Object representing owner of specified account (subclass of FocusType)
     * @throws IllegalArgumentException wrong OID format
     * @deprecated TODO: we want to remove this in midScale
     */
    @Deprecated
    default <F extends FocusType> PrismObject<F> searchShadowOwner(
            String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        ObjectQuery query = PrismContext.get()
                .queryFor(FocusType.class)
                .item(FocusType.F_LINK_REF).ref(shadowOid, null, PrismConstants.Q_ANY)
                .build();
        SearchResultList<PrismObject<FocusType>> searchResult;
        try {
            searchResult = searchObjects(FocusType.class, query, options, parentResult);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when searching for shadow owner");
        }

        if (searchResult.isEmpty()) {
            return null; // account shadow owner was not found
        } else if (searchResult.size() > 1) {
            LOGGER.warn("Found {} owners for shadow oid {}, returning first owner.", searchResult.size(), shadowOid);
        }
        //noinspection unchecked
        return (PrismObject<F>) searchResult.get(0);
    }

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
     * Allocates required number of container identifiers, presumably to be explicitly used for new container values during
     * ADD or MODIFY operations. See e.g. MID-8659.
     *
     * Returns a collection of identifiers that is guaranteed to be safely used by the client.
     *
     * Limitations:
     *
     * . To be used only for the new (native) repository.
     * . Currently, there is no "return unused identifiers" method. We assume the space of CIDs is huge.
     * We assume that the allocated identifiers will be used in majority of the cases.
     *
     * @throws ObjectNotFoundException If object is not found, exception is recorded as handled error.
     */
    <T extends ObjectType> @NotNull Collection<Long> allocateContainerIdentifiers(
            @NotNull Class<T> type,
            @NotNull String oid,
            int howMany,
            @NotNull OperationResult result) throws ObjectNotFoundException;

    @Experimental
    @ApiStatus.Internal
    @NotNull
    default SearchResultList<PrismContainerValue<?>> searchAggregate(AggregateQuery<?> query, OperationResult parentResult) throws SchemaException {
        throw new UnsupportedOperationException("Not Supported");
    }

    @Experimental
    @ApiStatus.Internal
    default int countAggregate(AggregateQuery<?> query, OperationResult parentResult) throws SchemaException {
        throw new UnsupportedOperationException("Not Supported");
    }

    /**
     * Provide repository run-time configuration and diagnostic information.
     * May execute diagnostic query on the database.
     */
    @NotNull RepositoryDiag getRepositoryDiag();

    /**
     * Returns short type identifier of the repository implementation.
     * This should be the same as {@link RepositoryDiag#getImplementationShortName()}.
     */
    @NotNull String getRepositoryType();

    // Temporary, as always...
    default boolean isNative() {
        return getRepositoryType().equals("Native");
    }

    /** Is this a generic repository implementation running over anything other than H2? */
    boolean isGenericNonH2();

    /** Returns `true` if the given object type is supported. */
    boolean supports(@NotNull Class<? extends ObjectType> type);

    default boolean supportsMarks() {
        return supports(MarkType.class);
    }

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

    /**
     * Use `SelectorMatcher` in `repo-common` module instead.
     * Or, call directly the {@link ValueSelector#matches(PrismValue, MatchingContext)} method.
     */
    @Deprecated
    default <O extends ObjectType> boolean selectorMatches(
            @Nullable ObjectSelectorType objectSelector,
            @Nullable PrismObject<O> object,
            @Nullable ObjectFilterExpressionEvaluator filterEvaluator,
            @NotNull Trace logger,
            @NotNull String logMessagePrefix)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        var value = object != null ? object.getValue() : null;
        return ObjectSelectorMatcher.selectorMatches(
                objectSelector, value, filterEvaluator, logger, logMessagePrefix, this);
    }

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

    @Override
    @NotNull
    default RepositoryService repositoryService() {
        return this;
    }
}
