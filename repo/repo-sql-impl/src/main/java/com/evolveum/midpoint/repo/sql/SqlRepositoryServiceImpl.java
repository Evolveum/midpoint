/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.sql.helpers.*;
import com.evolveum.midpoint.repo.sqlbase.ConflictWatcherImpl;
import com.evolveum.midpoint.repo.sqlbase.OperationLogger;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Main implementation of the {@link RepositoryService}, based on SQL database using ORM/Hibernate.
 * Don't autowire by this class, use {@link RepositoryService} class instead.
 */
public class SqlRepositoryServiceImpl extends SqlBaseService implements RepositoryService {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);

    public static final String OP_IS_ANY_SUBORDINATE = "isAnySubordinate"; // not part of API anymore

    public static final String PERFORMANCE_LOG_NAME = SqlRepositoryServiceImpl.class.getName() + ".performance";
    public static final String CONTENTION_LOG_NAME = SqlRepositoryServiceImpl.class.getName() + ".contention";

    public static final int CONTENTION_LOG_DEBUG_THRESHOLD = 3;
    public static final int MAIN_LOG_WARN_THRESHOLD = 8;

    private static final int RESTART_LIMIT = 1000;

    // just a safeguard (watchers per thread should be at most 1-2)
    private static final int MAX_CONFLICT_WATCHERS = 10;
    public static final int MAX_CONSTRAINT_NAME_LENGTH = 40;

    private final MatchingRuleRegistry matchingRuleRegistry;
    private final PrismContext prismContext;
    private final RelationRegistry relationRegistry;

    // autowired because sadly these involve dependency cycles
    @Autowired private SequenceHelper sequenceHelper;
    @Autowired private ObjectRetriever objectRetriever;
    @Autowired private ObjectUpdater objectUpdater;
    @Autowired private OrgClosureManager closureManager;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    private final ThreadLocal<List<ConflictWatcherImpl>> conflictWatchersThreadLocal =
            ThreadLocal.withInitial(ArrayList::new);

    private FullTextSearchConfigurationType fullTextSearchConfiguration;

    public SqlRepositoryServiceImpl(
            BaseHelper baseHelper,
            MatchingRuleRegistry matchingRuleRegistry,
            PrismContext prismContext,
            RelationRegistry relationRegistry) {

        super(baseHelper);

        this.matchingRuleRegistry = matchingRuleRegistry;
        this.prismContext = prismContext;
        this.relationRegistry = relationRegistry;
    }

    @PostConstruct
    public void init() throws RepositoryServiceFactoryException {
        LOGGER.info("Repository initialization finished.");
    }

    @Override
    public SqlRepositoryConfiguration sqlConfiguration() {
        return baseHelper.getConfiguration();
    }

    @FunctionalInterface
    public interface ResultSupplier<RV> {
        RV get() throws ObjectNotFoundException, SchemaException;
    }

    @FunctionalInterface
    public interface ResultQueryBasedSupplier<RV> {
        RV get(ObjectQuery query) throws SchemaException;
    }

    @NotNull
    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Getting object '{}' with oid '{}': {}", type.getSimpleName(), oid, result.getOperation());
        InternalMonitor.recordRepositoryRead(type, oid);

        OperationResult subResult = result.subresult(GET_OBJECT)
                .addQualifier(type.getSimpleName())
                .setMinor()
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();

        PrismObject<T> object = null;
        try {
            // "objectLocal" is here just to provide effectively final variable for the lambda below
            PrismObject<T> objectLocal = executeAttempts(oid, OP_GET_OBJECT, type, "getting",
                    subResult, () -> objectRetriever.getObjectAttempt(type, oid, options, subResult));
            object = objectLocal;
            invokeConflictWatchers((w) -> w.afterGetObject(objectLocal));
        } finally {
            OperationLogger.logGetObject(type, oid, options, object, subResult);
        }

        return object;
    }

    private <RV> RV executeAttempts(String oid, String operationName, Class<?> type, String operationVerb, OperationResult subResult,
            ResultSupplier<RV> supplier) throws ObjectNotFoundException, SchemaException {
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operationName, type);
        int attempt = 1;
        try {
            while (true) {
                try {
                    return supplier.get();
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, operationVerb, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <RV> RV executeAttemptsNoSchemaException(
            String oid, String operationName, Class<?> type, String operationVerb,
            OperationResult subResult, ResultSupplier<RV> supplier) throws ObjectNotFoundException {
        try {
            return executeAttempts(oid, operationName, type, operationVerb, subResult, supplier);
        } catch (SchemaException e) {
            throw new AssertionError("Should not occur", e);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private <RV> RV executeQueryAttemptsNoSchemaException(ObjectQuery query,
            String operationName, Class<?> type, String operationVerb, OperationResult subResult,
            Supplier<RV> emptyQueryResultSupplier, ResultQueryBasedSupplier<RV> supplier) {
        try {
            return executeQueryAttempts(query, operationName, type, operationVerb, subResult, emptyQueryResultSupplier, supplier);
        } catch (SchemaException e) {
            throw new AssertionError("Should not occur", e);
        }
    }

    private <RV> RV executeQueryAttempts(ObjectQuery query, String operationName, Class<?> type, String operationVerb, OperationResult subResult,
            Supplier<RV> emptyQueryResultSupplier, ResultQueryBasedSupplier<RV> supplier) throws SchemaException {

        if (query != null) {
            query = simplify(query, subResult);
            if (query == null) {
                return emptyQueryResultSupplier.get();
            }
        }

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(operationName, type);
        int attempt = 1;
        try {
            while (true) {
                try {
                    return supplier.get(query);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operationVerb, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @NotNull
    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            @NotNull Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult result) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, false, null);

        OperationResult subResult = result.subresult(SEARCH_OBJECTS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        return executeQueryAttempts(query, OP_SEARCH_OBJECTS, type, "searching", subResult,
                () -> new SearchResultList<>(new ArrayList<>(0)),
                (q) -> objectRetriever.searchObjectsAttempt(type, q, options, subResult));
    }

    // utility method that simplifies a query, and checks for trivial cases (minimizing client code for such situation)
    // returns null if the query is equivalent to NONE (TODO this is counter-intuitive, fix that)
    private ObjectQuery simplify(ObjectQuery query, OperationResult subResult) {
        ObjectFilter filter = query.getFilter();
        filter = ObjectQueryUtil.simplify(filter);
        if (filter instanceof NoneFilter) {
            subResult.recordSuccess();
            return null;
        } else {
            query = replaceSimplifiedFilter(query, filter);
        }
        return query;
    }

    @NotNull
    private ObjectQuery replaceSimplifiedFilter(ObjectQuery query, ObjectFilter filter) {
        query = query.cloneWithoutFilter();
        query.setFilter(filter instanceof AllFilter ? null : filter);
        return query;
    }

    @Override
    public @NotNull <T extends Containerable> SearchResultList<T> searchContainers(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
            throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        logSearchInputParameters(type, query, false, null);

        OperationResult result = parentResult.subresult(SEARCH_CONTAINERS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        return executeQueryAttempts(query, "searchContainers", type, "searching", result,
                () -> new SearchResultList<>(new ArrayList<>(0)),
                (q) -> objectRetriever.searchContainersAttempt(type, q, options, result));
    }

    @Override
    public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Counting containers of type '{}', query (on trace level).", type.getSimpleName());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", query == null ? "undefined" : query.debugDump());
        }

        OperationResult result = parentResult.subresult(COUNT_CONTAINERS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        return executeQueryAttemptsNoSchemaException(query, "countContainers", type, "counting", result,
                () -> 0,
                (q) -> objectRetriever.countContainersAttempt(type, q, options, result));
    }

    @Override
    public int countReferences(@Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult) {
        throw new UnsupportedOperationException("Reference search/count is not supported in Generic repository");
    }

    @Override
    public @NotNull SearchResultList<ObjectReferenceType> searchReferences(@NotNull ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult) {
        throw new UnsupportedOperationException("Reference search/count is not supported in Generic repository");
    }

    private <T> void logSearchInputParameters(Class<T> type, ObjectQuery query, boolean iterative, Boolean strictlySequential) {
        ObjectPaging paging = query != null ? query.getPaging() : null;
        LOGGER.debug("Searching objects of type '{}', query (on trace level), offset {}, count {}, iterative {}, strictlySequential {}.",
                type.getSimpleName(), (paging != null ? paging.getOffset() : "undefined"),
                (paging != null ? paging.getMaxSize() : "undefined"), iterative, strictlySequential);

        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        LOGGER.trace("Full query\n{}", query == null ? "undefined" : query.debugDump());

        if (iterative) {
            LOGGER.trace("Iterative search by paging defined by the configuration: {}, batch size {}",
                    sqlConfiguration().isIterativeSearchByPaging(),
                    sqlConfiguration().getIterativeSearchByPagingBatchSize());
        }
    }

    @Override
    @NotNull
    public <T extends ObjectType> String addObject(
            @NotNull PrismObject<T> object, RepoAddOptions options, @NotNull OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        Validate.notNull(object, "Object must not be null.");
        validateName(object);
        Validate.notNull(result, "Operation result must not be null.");

        if (options == null) {
            options = new RepoAddOptions();
        }

        LOGGER.debug("Adding object type '{}', overwrite={}, allowUnencryptedValues={}",
                object.getCompileTimeClass().getSimpleName(), options.isOverwrite(),
                options.isAllowUnencryptedValues());

        if (InternalsConfig.encryptionChecks && !RepoAddOptions.isAllowUnencryptedValues(options)) {
            CryptoUtil.checkEncrypted(object);
        }

        if (InternalsConfig.consistencyChecks) {
            object.checkConsistence(ConsistencyCheckScope.THOROUGH);
        } else {
            object.checkConsistence(ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
        }

        if (LOGGER.isTraceEnabled()) {
            // Explicitly log name
            PolyStringType namePolyType = object.asObjectable().getName();
            LOGGER.trace("NAME: {} - {}", namePolyType.getOrig(), namePolyType.getNorm());
        }

        OperationResult subResult = result.subresult(ADD_OBJECT)
                .addQualifier(object.asObjectable().getClass().getSimpleName())
                .addParam("object", object)
                .addParam("options", options.toString())
                .build();

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(
                options.isOverwrite() ? OP_ADD_OBJECT_OVERWRITE : OP_ADD_OBJECT,
                object.getCompileTimeClass());
        int attempt = 1;
        int restarts = 0;
        try {
            final String operation = "adding";

            String proposedOid = object.getOid();
            while (true) {
                try {
                    String createdOid = objectUpdater.addObjectAttempt(object, options, subResult);
                    invokeConflictWatchers((w) -> w.afterAddObject(createdOid, object));
                    return createdOid;
                } catch (RestartOperationRequestedException ex) {
                    // special case: we want to restart but we do not want to count these
                    LOGGER.trace("Restarting because of {}", ex.getMessage());
                    restarts++;
                    if (restarts > RESTART_LIMIT) {
                        throw new IllegalStateException("Too many operation restarts");
                    }
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(proposedOid, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
            OperationLogger.logAdd(object, options, subResult);
            subResult.computeStatusIfUnknown();
        }
    }

    public void invokeConflictWatchers(Consumer<ConflictWatcherImpl> consumer) {
        emptyIfNull(conflictWatchersThreadLocal.get()).forEach(consumer);
    }

    private void validateName(PrismObject<?> object) throws SchemaException {
        PrismProperty<?> name = object.findProperty(ObjectType.F_NAME);
        //noinspection ConstantConditions
        if (name == null || ((PolyString) name.getRealValue()).isEmpty()) {
            throw new SchemaException("Attempt to add object without name.");
        }
    }

    @NotNull
    @Override
    public <T extends ObjectType> DeleteObjectResult deleteObject(
            Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Deleting object type '{}' with oid '{}'", type.getSimpleName(), oid);

        OperationResult subResult = result.subresult(DELETE_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();
        try {

            DeleteObjectResult rv = executeAttemptsNoSchemaException(oid, OP_DELETE_OBJECT, type, "deleting",
                    subResult, () -> objectUpdater.deleteObjectAttempt(type, oid, subResult)
            );
            invokeConflictWatchers((w) -> w.afterDeleteObject(oid));
            return rv;

        } finally {
            OperationLogger.logDelete(type, oid, subResult);
        }
    }

    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Counting objects of type '{}', query (on trace level).", type.getSimpleName());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", query == null ? "undefined" : query.debugDump());
        }

        OperationResult subResult = result.subresult(COUNT_OBJECTS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        return executeQueryAttemptsNoSchemaException(query, OP_COUNT_OBJECTS, type, "counting", subResult,
                () -> 0,
                (q) -> objectRetriever.countObjectsAttempt(type, q, options, subResult));
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return modifyObject(type, oid, modifications, null, result);
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            RepoModifyOptions options,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            return modifyObject(type, oid, modifications, null, options, result);
        } catch (PreconditionViolationException e) {
            throw new AssertionError(e); // with null precondition we couldn't get this exception
        }
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            ModificationPrecondition<T> precondition,
            RepoModifyOptions options,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {

        Validate.notNull(modifications, "Modifications must not be null.");
        Validate.notNull(type, "Object class in delta must not be null.");
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.subresult(MODIFY_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .addArbitraryObjectCollectionAsParam("modifications", modifications)
                .build();

        if (modifications.isEmpty() && !RepoModifyOptions.isForceReindex(options)) {
            LOGGER.debug("Modification list is empty, nothing was modified.");
            subResult.recordStatus(OperationResultStatus.SUCCESS, "Modification list is empty, nothing was modified.");
            return new ModifyObjectResult<>(modifications);
        }

        checkModifications(modifications);
        logNameChange(modifications);

        // TODO executeAttempts?
        final String operation = "modifying";
        int attempt = 1;
        int restarts = 0;

        boolean noFetchExtensionValueInsertionForbidden = false;

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_MODIFY_OBJECT, type);

        try {
            while (true) {
                try {
                    ModifyObjectResult<T> rv = objectUpdater.modifyObjectAttempt(type, oid, modifications, precondition, options,
                            attempt, subResult, this, noFetchExtensionValueInsertionForbidden, null);
                    invokeConflictWatchers((w) -> w.afterModifyObject(oid));
                    rv.setPerformanceRecord(
                            pm.registerOperationFinish(opHandle, attempt));
                    return rv;
                } catch (RestartOperationRequestedException ex) {
                    // special case: we want to restart but we do not want to count these
                    LOGGER.trace("Restarting because of {}", ex.getMessage());
                    restarts++;
                    if (restarts > RESTART_LIMIT) {
                        throw new IllegalStateException("Too many operation restarts");
                    } else if (ex.isForbidNoFetchExtensionValueAddition()) {
                        noFetchExtensionValueInsertionForbidden = true;
                    }
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Got exception while processing modifications on {}:{}:\n{}", type.getSimpleName(), oid,
                    DebugUtil.debugDump(modifications), t);
            pm.registerOperationFinish(opHandle, attempt);
            throw t;
        } finally {
            OperationLogger.logModify(type, oid, modifications, precondition, options, subResult);
        }
    }

    private void checkModifications(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) {

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(modifications);
        }

        if (InternalsConfig.consistencyChecks) {
            ItemDeltaCollectionsUtil.checkConsistence(modifications, ConsistencyCheckScope.THOROUGH);
        } else {
            ItemDeltaCollectionsUtil.checkConsistence(modifications, ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
        }
    }

    private void logNameChange(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        if (LOGGER.isTraceEnabled()) {
            for (ItemDelta<?, ?> modification : modifications) {
                if (modification instanceof PropertyDelta<?>) {
                    PropertyDelta<?> propDelta = (PropertyDelta<?>) modification;
                    if (propDelta.getPath().equivalent(ObjectType.F_NAME)) {
                        Collection<PrismPropertyValue<PolyString>> values = propDelta.getValues(PolyString.class);
                        for (PrismPropertyValue<PolyString> pval : values) {
                            PolyString value = pval.getValue();
                            LOGGER.trace("NAME delta: {} - {}", value.getOrig(), value.getNorm());
                        }
                    }
                }
            }
        }
    }

    @NotNull
    @Override
    @Experimental
    public <T extends ObjectType> ModifyObjectResult<T> modifyObjectDynamically(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> getOptions,
            @NotNull ModificationsSupplier<T> modificationsSupplier,
            RepoModifyOptions modifyOptions,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        Validate.notNull(type, "Object class in delta must not be null.");
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(modificationsSupplier, "Modifications supplier must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        // TODO executeAttempts?
        final String operation = "modifying";
        int attempt = 1;
        int restarts = 0;

        boolean noFetchExtensionValueInsertionForbidden = false;

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_MODIFY_OBJECT_DYNAMICALLY, type);

        OperationResult result = parentResult.subresult(MODIFY_OBJECT_DYNAMICALLY)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();

        ModifyObjectResult<T> rv = null;
        try {
            while (true) {
                try {
                    ModificationsSupplier<T> innerModificationsSupplier = object -> {
                        Collection<? extends ItemDelta<?, ?>> modifications = modificationsSupplier.get(object);
                        checkModifications(modifications);
                        logNameChange(modifications);
                        return modifications;
                    };
                    rv = objectUpdater.modifyObjectDynamicallyAttempt(type, oid, getOptions, innerModificationsSupplier,
                            modifyOptions, attempt, result, this, noFetchExtensionValueInsertionForbidden);
                    invokeConflictWatchers((w) -> w.afterModifyObject(oid));
                    rv.setPerformanceRecord(
                            pm.registerOperationFinish(opHandle, attempt));
                    return rv;
                } catch (RestartOperationRequestedException ex) {
                    // special case: we want to restart but we do not want to count these
                    LOGGER.trace("Restarting because of {}", ex.getMessage());
                    restarts++;
                    if (restarts > RESTART_LIMIT) {
                        throw new IllegalStateException("Too many operation restarts");
                    } else if (ex.isForbidNoFetchExtensionValueAddition()) {
                        noFetchExtensionValueInsertionForbidden = true;
                    }
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, operation, attempt, ex, result);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Got exception while processing dynamic modifications on {}:{}", type.getSimpleName(), oid, t);
            pm.registerOperationFinish(opHandle, attempt);
            throw t;
        } finally {
            OperationLogger.logModifyDynamically(type, oid, rv, modifyOptions, result);
        }
    }

    @Override
    public @NotNull String getRepositoryType() {
        return IMPLEMENTATION_SHORT_NAME;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#repositorySelfTest(com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public void repositorySelfTest(OperationResult parentResult) {
        // No self-tests for now
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {
        closureManager.checkAndOrRebuild(true, repairIfNecessary, false, false, testResult);
    }

    @Override
    public <T extends ObjectType> String getVersion(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(oid, "Object oid must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting version for {} with oid '{}'.", type.getSimpleName(), oid);

        OperationResult subResult = parentResult.subresult(GET_VERSION)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_GET_VERSION, type);

        final String operation = "getting version";
        int attempt = 1;
        try {
            while (true) {
                try {
                    String rv = objectRetriever.getVersionAttempt(type, oid, subResult);
                    invokeConflictWatchers((w) -> w.afterGetVersion(oid, rv));
                    return rv;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
            ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
            OperationResult result) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, true, strictlySequential);

        OperationResult subResult = result.subresult(SEARCH_OBJECTS_ITERATIVE)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        if (query != null) {
            ObjectFilter filter = query.getFilter();
            filter = ObjectQueryUtil.simplify(filter);
            if (filter instanceof NoneFilter) {
                subResult.recordSuccess();
                return null;
            } else {
                query = replaceSimplifiedFilter(query, filter);
            }
        }

        // We don't try to be smarter than our client: if he explicitly requests e.g. single transaction
        // against DB that does not support it, or if he requests simple paging where strictly sequential one is
        // indicated, we will obey (with a warning in some cases).
        IterationMethodType iterationMethod;
        IterationMethodType explicitIterationMethod = GetOperationOptions.getIterationMethod(SelectorOptions.findRootOptions(options));
        if (explicitIterationMethod == null || explicitIterationMethod == IterationMethodType.DEFAULT) {
            if (sqlConfiguration().isIterativeSearchByPaging()) {
                if (strictlySequential) {
                    if (isCustomPagingOkWithPagedSeqIteration(query)) {
                        iterationMethod = IterationMethodType.STRICTLY_SEQUENTIAL_PAGING;
                    } else if (isCustomPagingOkWithFetchAllIteration(query)) {
                        LOGGER.debug("Iterative search by paging was defined in the repository configuration, and strict sequentiality "
                                + "was requested. However, a custom paging precludes its application. Therefore switching to "
                                + "'fetch all' iteration method. Paging requested: " + query.getPaging());
                        iterationMethod = IterationMethodType.FETCH_ALL;
                    } else {
                        LOGGER.warn("Iterative search by paging was defined in the repository configuration, and strict sequentiality "
                                + "was requested. However, a custom paging precludes its application and maxSize is either "
                                + "undefined or too large (over " + sqlConfiguration().getMaxObjectsForImplicitFetchAllIterationMethod()
                                + "). Therefore switching to simple paging iteration method. Paging requested: " + query.getPaging());
                        iterationMethod = IterationMethodType.SIMPLE_PAGING;
                    }
                } else {
                    iterationMethod = IterationMethodType.SIMPLE_PAGING;
                }
            } else {
                iterationMethod = IterationMethodType.SINGLE_TRANSACTION;
            }
        } else {
            iterationMethod = explicitIterationMethod;
        }

        if (strictlySequential && iterationMethod == IterationMethodType.SIMPLE_PAGING) {
            LOGGER.warn("Using simple paging where strictly sequential one is indicated: type={}, query={}", type, query);
        } else if (sqlConfiguration().isIterativeSearchByPaging() && explicitIterationMethod == IterationMethodType.SINGLE_TRANSACTION) {
            // we should introduce some 'native iteration supported' flag for the DB configuration to avoid false warnings here
            // based on 'iterativeSearchByPaging' setting for databases that support native iteration
            LOGGER.warn("Using single transaction iteration where DB indicates paging should be used: type={}, query={}", type, query);
        }

        LOGGER.trace("Using iteration method {} for type={}, query={}", iterationMethod, type, query);

        SearchResultMetadata rv = null; // todo what about returning values from other search methods?
        switch (iterationMethod) {
            case SINGLE_TRANSACTION:
                rv = searchObjectsIterativeBySingleTransaction(type, query, handler, options, subResult);
                break;
            case SIMPLE_PAGING:
                objectRetriever.searchObjectsIterativeByPaging(type, query, handler, options, subResult);
                break;
            case STRICTLY_SEQUENTIAL_PAGING:
                objectRetriever.searchObjectsIterativeByPagingStrictlySequential(type, query, handler, options, subResult);
                break;
            case FETCH_ALL:
                objectRetriever.searchObjectsIterativeByFetchAll(type, query, handler, options, subResult);
                break;
            default:
                throw new AssertionError("iterationMethod: " + iterationMethod);
        }
        return rv;
    }

    @Override
    public SearchResultMetadata searchReferencesIterative(
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<ObjectReferenceType> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        throw new UnsupportedOperationException("Reference search/count is not supported in Generic repository");
    }

    private boolean isCustomPagingOkWithFetchAllIteration(ObjectQuery query) {
        return query != null
                && query.getPaging() != null
                && query.getPaging().getMaxSize() != null
                && query.getPaging().getMaxSize() <= sqlConfiguration().getMaxObjectsForImplicitFetchAllIterationMethod();
    }

    public static boolean isCustomPagingOkWithPagedSeqIteration(ObjectQuery query) {
        if (query == null || query.getPaging() == null) {
            return true;
        }
        ObjectPaging paging = query.getPaging();
        return !paging.hasOrdering() && paging.getOffset() == null;
    }

    @Nullable
    private <T extends ObjectType> SearchResultMetadata searchObjectsIterativeBySingleTransaction(
            Class<T> type,
            ObjectQuery query,
            ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult subResult) {
        /*
         * Here we store OIDs that were already sent to the client during previous attempts.
         */
        Set<String> retrievedOids = new HashSet<>();

        final String operation = "searching iterative";
        int attempt = 1;
        try {
            while (true) {
                try {
                    objectRetriever.searchObjectsIterativeAttempt(type, query, handler, options, subResult, retrievedOids);
                    return null;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, subResult);
                }
            }
        } finally {
            // temporary workaround, just to know the number of calls
            SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
            long opHandle = pm.registerOperationStart(OP_SEARCH_OBJECTS_ITERATIVE, type);
            pm.registerOperationFinish(opHandle, attempt);
        }
        // TODO conflict checking (if needed)
    }

    protected boolean isAnySubordinate(String ancestorOrgOid, Collection<String> descendantOrgOids) {
        Validate.notNull(ancestorOrgOid, "upperOrgOid must not be null.");
        Validate.notNull(descendantOrgOids, "lowerObjectOids must not be null.");

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Querying for subordination upper {}, lower {}", ancestorOrgOid, descendantOrgOids);
        }

        if (descendantOrgOids.isEmpty()) {
            // trivial case
            return false;
        }

        // TODO executeAttempts
        int attempt = 1;

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_IS_ANY_SUBORDINATE, OrgType.class);
        try {
            while (true) {
                try {
                    return objectRetriever.isAnySubordinateAttempt(ancestorOrgOid, descendantOrgOids);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(ancestorOrgOid, OP_IS_ANY_SUBORDINATE, attempt, ex, null);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {

        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.subresult(ADVANCE_SEQUENCE)
                .addParam("oid", oid)
                .build();

        if (LOGGER.isTraceEnabled()) {LOGGER.trace("Advancing sequence {}", oid);}

        // TODO executeAttempts
        int attempt = 1;

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_ADVANCE_SEQUENCE, SequenceType.class);
        try {
            while (true) {
                try {
                    return sequenceHelper.advanceSequenceAttempt(oid, result);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, "advanceSequence", attempt, ex, null);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.subresult(RETURN_UNUSED_VALUES_TO_SEQUENCE)
                .addParam("oid", oid)
                .build();

        LOGGER.trace("Returning unused values of {} to sequence {}", unusedValues, oid);
        if (unusedValues == null || unusedValues.isEmpty()) {
            result.recordSuccess();
            return;
        }

        // TODO executeAttempts
        int attempt = 1;

        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_RETURN_UNUSED_VALUES_TO_SEQUENCE, SequenceType.class);
        try {
            while (true) {
                try {
                    sequenceHelper.returnUnusedValuesToSequenceAttempt(oid, unusedValues, result);
                    return;
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(oid, "returnUnusedValuesToSequence", attempt, ex, null);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public @NotNull <T extends ObjectType> Collection<Long> allocateContainerIdentifiers(
            @NotNull Class<T> type, @NotNull String oid, int howMany, @NotNull OperationResult result) {
        throw new UnsupportedOperationException("Not supported by the generic repository");
    }

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(
            RepositoryQueryDiagRequest request, OperationResult result) {

        Validate.notNull(request, "Request must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Executing arbitrary query '{}'.", request);

        final String operation = "querying";
        int attempt = 1;

        OperationResult subResult = result.subresult(EXECUTE_QUERY_DIAGNOSTICS)
                .setMinor()
                .addParam("request", request.toString())
                .build();

        // TODO executeAttempts
        SqlPerformanceMonitorImpl pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(OP_EXECUTE_QUERY_DIAGNOSTICS, null);

        try {
            while (true) {
                try {
                    return objectRetriever.executeQueryDiagnosticsRequest(request, subResult);
                } catch (RuntimeException ex) {
                    attempt = baseHelper.logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewAttempt(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public <O extends ObjectType> boolean isDescendant(
            PrismObject<O> object, String ancestorOrgOid) {
        List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
        List<String> objParentOrgOids = new ArrayList<>(objParentOrgRefs.size());
        for (ObjectReferenceType objParentOrgRef : objParentOrgRefs) {
            objParentOrgOids.add(objParentOrgRef.getOid());
        }
        return isAnySubordinate(ancestorOrgOid, objParentOrgOids);
    }

    @Override
    public <O extends ObjectType> boolean isAncestor(
            PrismObject<O> object, String descendantOrgOid) {
        // object is not considered ancestor of itself
        if (object.getOid() == null || object.getOid().equals(descendantOrgOid)) {
            return false;
        }

        return isAnySubordinate(object.getOid(), List.of(descendantOrgOid));
    }

    @Override
    public synchronized void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
        if (PrismUtil.realValueEquals(fullTextSearchConfiguration, fullTextSearch)) {
            LOGGER.trace("Ignoring full text search configuration update => the real value has not changed");
            return;
        }
        LOGGER.info("Applying full text search configuration ({} entries)",
                fullTextSearch != null ? fullTextSearch.getIndexed().size() : 0);
        fullTextSearchConfiguration = fullTextSearch;
    }

    @Override
    public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
        return fullTextSearchConfiguration;
    }

    @Override
    public void postInit(OperationResult result) throws SchemaException {
        LOGGER.debug("Executing repository postInit method");
        systemConfigurationChangeDispatcher.dispatch(true, true, result);
    }

    @Override
    public ConflictWatcher createAndRegisterConflictWatcher(@NotNull String oid) {
        List<ConflictWatcherImpl> watchers = conflictWatchersThreadLocal.get();
        if (watchers.size() >= MAX_CONFLICT_WATCHERS) {
            throw new IllegalStateException("Conflicts watchers leaking: reached limit of "
                    + MAX_CONFLICT_WATCHERS + ": " + watchers);
        }
        ConflictWatcherImpl watcher = new ConflictWatcherImpl(oid);
        watchers.add(watcher);
        return watcher;
    }

    @Override
    public void unregisterConflictWatcher(ConflictWatcher watcher) {
        ConflictWatcherImpl watcherImpl = (ConflictWatcherImpl) watcher;
        List<ConflictWatcherImpl> watchers = conflictWatchersThreadLocal.get();
        // change these exceptions to logged errors, eventually
        if (watchers == null) {
            throw new IllegalStateException(
                    "No conflict watchers registered for current thread; tried to unregister " + watcher);
        } else if (!watchers.remove(watcherImpl)) { // expecting there's only one
            throw new IllegalStateException(
                    "Tried to unregister conflict watcher " + watcher + " that was not registered");
        }
    }

    @Override
    public boolean hasConflict(ConflictWatcher watcher, OperationResult parentResult) {
        OperationResult result = parentResult.subresult(HAS_CONFLICT)
                .setMinor()
                .addParam("oid", watcher.getOid())
                .addParam("watcherClass", watcher.getClass().getName())
                .build();
        try {
            boolean rv;
            if (watcher.hasConflict()) {
                rv = true;
            } else {
                try {
                    getVersion(ObjectType.class, watcher.getOid(), result);
                } catch (ObjectNotFoundException e) {
                    // just ignore this
                }
                rv = watcher.hasConflict();
            }
            result.addReturn("hasConflict", rv);
            return rv;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * This is an approximate implementation, not taking care of two clients appending the diag information concurrently.
     * So there could be situations when obsolete information is not removed because of this.
     */
    @Override
    public <T extends ObjectType> void addDiagnosticInformation(
            Class<T> type, String oid,
            DiagnosticInformationType information, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.subresult(ADD_DIAGNOSTIC_INFORMATION)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("oid", oid)
                .build();
        try {
            PrismObject<T> object = getObject(type, oid, null, result);
            boolean canStoreInfo = pruneDiagnosticInformation(type, oid, information,
                    object.asObjectable().getDiagnosticInformation(), result);
            if (canStoreInfo) {
                List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(type)
                        .item(ObjectType.F_DIAGNOSTIC_INFORMATION).add(information)
                        .asItemDeltas();
                modifyObject(type, oid, modifications, result);
            }
            result.computeStatus();
        } catch (Throwable t) {
            result.recordFatalError("Couldn't add diagnostic information: " + t.getMessage(), t);
            throw t;
        }
    }

    // TODO replace by something in system configuration (postponing until this feature is used more)

    private static final Map<String, Integer> DIAG_INFO_CLEANUP_POLICY = new HashMap<>();

    static {
        DIAG_INFO_CLEANUP_POLICY.put(SchemaConstants.TASK_THREAD_DUMP_URI, 5);
        DIAG_INFO_CLEANUP_POLICY.put(null, 2);
    }

    // returns true if the new information can be stored
    private <T extends ObjectType> boolean pruneDiagnosticInformation(
            Class<T> type, String oid, DiagnosticInformationType newInformation,
            List<DiagnosticInformationType> oldInformationList, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        String infoType = newInformation.getType();
        if (infoType == null) {
            throw new IllegalArgumentException("Diagnostic information type is not specified");
        }
        Integer limit;
        if (DIAG_INFO_CLEANUP_POLICY.containsKey(infoType)) {
            limit = DIAG_INFO_CLEANUP_POLICY.get(infoType);
        } else {
            limit = DIAG_INFO_CLEANUP_POLICY.get(null);
        }
        LOGGER.trace("Limit for diagnostic information of type '{}': {}", infoType, limit);
        if (limit != null) {
            List<DiagnosticInformationType> oldToPrune = oldInformationList.stream()
                    .filter(i -> infoType.equals(i.getType()))
                    .collect(Collectors.toList());
            int pruneToSize = limit > 0 ? limit - 1 : 0;
            if (oldToPrune.size() > pruneToSize) {
                oldToPrune.sort(Comparator.nullsFirst(
                        Comparator.comparing(i -> XmlTypeConverter.toDate(i.getTimestamp()))));
                List<DiagnosticInformationType> toDelete =
                        oldToPrune.subList(0, oldToPrune.size() - pruneToSize);
                LOGGER.trace("Going to delete {} diagnostic information values", toDelete.size());
                List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(type)
                        .item(ObjectType.F_DIAGNOSTIC_INFORMATION).deleteRealValues(toDelete)
                        .asItemDeltas();
                modifyObject(type, oid, modifications, result);
            }
            return limit > 0;
        } else {
            return true;
        }
    }

    @PreDestroy
    public void destroy() {
        super.destroy();
        LOGGER.info("Shutdown complete.");
    }

    @Override
    public boolean supports(@NotNull Class<? extends ObjectType> type) {
        return RObjectType.getByJaxbTypeIfExists(type) != null;

    }

    @Override
    public <T extends Containerable> SearchResultMetadata searchContainersIterative(Class<T> type, ObjectQuery query, ObjectHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
        throw new UnsupportedOperationException("Not supported");
    }
}
