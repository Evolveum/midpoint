/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.xml.namespace.QName;

import com.google.common.base.Strings;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLQuery;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_ConditionEntry;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrg;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgClosure;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqale.update.AddObjectContext;
import com.evolveum.midpoint.repo.sqale.update.RootUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.*;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Repository implementation based on SQL, JDBC and Querydsl without any ORM.
 * WORK IN PROGRESS.
 *
 * Structure of main public methods (WIP):
 * - arg checks
 * - debug log
 * - create op-result, immediately followed by try/catch/finally (see addObject for example)
 * - more arg checks :-) (here or before op result depending on the needs)
 * - call to executeMethodName(...) where perf monitor is initialized followed by try/catch/finally
 * - finally in main method:
 *
 * TODO/document?:
 * - ignore useNoFetchExtensionValuesInsertion - related to Hibernate,
 */
public class SqaleRepositoryService implements RepositoryService {

    private static final Trace LOGGER = TraceManager.getTrace(SqaleRepositoryService.class);

    /**
     * Class name prefix for operation names, including the dot separator.
     * Use with various `RepositoryService.OP_*` constants, not with constants without `OP_`
     * prefix because they already contain class name of the service interface.
     */
    private static final String OP_NAME_PREFIX = SqaleRepositoryService.class.getSimpleName() + '.';

    private static final int MAX_CONFLICT_WATCHERS = 10;

    private final SqaleRepoContext repositoryContext;
    private final SqlQueryExecutor sqlQueryExecutor;
    private final SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection;

    private SqlPerformanceMonitorImpl performanceMonitor; // set to null in destroy

    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    private final ThreadLocal<List<ConflictWatcherImpl>> conflictWatchersThreadLocal =
            ThreadLocal.withInitial(ArrayList::new);

    private FullTextSearchConfigurationType fullTextSearchConfiguration;

    public SqaleRepositoryService(
            SqaleRepoContext repositoryContext,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        this.repositoryContext = repositoryContext;
        this.sqlQueryExecutor = new SqlQueryExecutor(repositoryContext);
        this.sqlPerformanceMonitorsCollection = sqlPerformanceMonitorsCollection;

        // monitor initialization and registration
        performanceMonitor = new SqlPerformanceMonitorImpl(
                repositoryConfiguration().getPerformanceStatisticsLevel(),
                repositoryConfiguration().getPerformanceStatisticsFile());
        sqlPerformanceMonitorsCollection.register(performanceMonitor);
    }

    @Override
    public @NotNull <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        Objects.requireNonNull(type, "Object type must not be null.");
        UUID oidUuid = checkOid(oid);
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting object '{}' with OID '{}': {}",
                type.getSimpleName(), oid, parentResult.getOperation());
        InternalMonitor.recordRepositoryRead(type, oid);

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_GET_OBJECT)
                .addQualifier(type.getSimpleName())
                .setMinor()
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();

        PrismObject<T> object = null;
        try {
            object = executeGetObject(type, oidUuid, options);
            return object;
        } catch (ObjectNotFoundException e) {
            recordException(e, operationResult,
                    !GetOperationOptions.isAllowNotFound(SelectorOptions.findRootOptions(options)));
            throw e;
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
            OperationLogger.logGetObject(type, oid, options, object, operationResult);
        }
    }

    private <T extends ObjectType> PrismObject<T> executeGetObject(
            Class<T> type,
            UUID oidUuid,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException {
        PrismObject<T> object;
        long opHandle = registerOperationStart(OP_GET_OBJECT, type);
        try (JdbcSession jdbcSession =
                repositoryContext.newJdbcSession().startReadOnlyTransaction()) {
            //noinspection unchecked
            object = (PrismObject<T>) readByOid(jdbcSession, type, oidUuid, options)
                    .asPrismObject();
            jdbcSession.commit();
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt (separate try from JDBC session)
        }

        // TODO attempts on conflict
        // "objectLocal" is here just to provide effectively final variable for the lambda below
//            PrismObject<T> objectLocal = executeAttempts(oid, OP_GET_OBJECT, type, "getting",
//                    subResult, () -> objectRetriever.getObjectAttempt(type, oid, options, operationResult));
//            object = objectLocal;
        invokeConflictWatchers((w) -> w.afterGetObject(object));

        // TODO both update and get need this? I believe not.
        //  ObjectTypeUtil.normalizeAllRelations(object, schemaService.relationRegistry());
        //  If this passes model-intest, just delete it.
        return object;
    }

    private UUID checkOid(String oid) {
        Objects.requireNonNull(oid, "OID must not be null");
        try {
            return UUID.fromString(oid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID string: " + oid, e);
        }
    }

    /** Read object using provided {@link JdbcSession} as a part of already running transaction. */
    private <S extends ObjectType> S readByOid(
            @NotNull JdbcSession jdbcSession,
            @NotNull Class<S> schemaType,
            @NotNull UUID oid,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException {

        SqaleTableMapping<S, QObject<MObject>, MObject> rootMapping =
                repositoryContext.getMappingBySchemaType(schemaType);
        QObject<MObject> root = rootMapping.defaultAlias();

        Tuple result = jdbcSession.newQuery()
                .from(root)
                .select(rootMapping.selectExpressions(root, options))
                .where(root.oid.eq(oid))
                .fetchOne();

        if (result == null || result.get(root.fullObject) == null) {
            throw new ObjectNotFoundException(schemaType, oid.toString());
        }

        return rootMapping.toSchemaObject(result, root, options, jdbcSession, false);
    }

    @Override
    public <T extends ObjectType> String getVersion(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        UUID uuid = checkOid(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting version for {} with oid '{}'.", type.getSimpleName(), oid);

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_GET_VERSION)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();

        try {
            return executeGetVersion(type, uuid);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private <T extends ObjectType> String executeGetVersion(Class<T> type, UUID oid)
            throws ObjectNotFoundException {
        long opHandle = registerOperationStart(OP_GET_VERSION, type);
        try (JdbcSession jdbcSession =
                repositoryContext.newJdbcSession().startReadOnlyTransaction()) {
            SqaleTableMapping<T, QObject<MObject>, MObject> rootMapping =
                    repositoryContext.getMappingBySchemaType(type);
            QObject<MObject> root = rootMapping.defaultAlias();

            Integer version = jdbcSession.newQuery()
                    .select(root.version)
                    .from(root)
                    .where(root.oid.eq(oid))
                    .fetchOne();
            if (version == null) {
                throw new ObjectNotFoundException(type, oid.toString());
            }

            String versionString = version.toString();
            invokeConflictWatchers((w) -> w.afterGetVersion(oid.toString(), versionString));
            return versionString;
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt (separate try from JDBC session)
        }
    }

    // Add/modify/delete

    @Override
    @NotNull
    public <T extends ObjectType> String addObject(
            @NotNull PrismObject<T> object,
            @Nullable RepoAddOptions options,
            @NotNull OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException {

        Objects.requireNonNull(object, "Object must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        if (options == null) {
            options = new RepoAddOptions();
        }

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_ADD_OBJECT)
                .addQualifier(object.asObjectable().getClass().getSimpleName())
                .addParam("object", object)
                .addParam("options", options.toString())
                .build();

        try {
            PolyString name = object.getName();
            if (name == null || Strings.isNullOrEmpty(name.getOrig())) {
                throw new SchemaException("Attempt to add object without name.");
            }

            //noinspection ConstantConditions
            LOGGER.debug(
                    "Adding object type '{}', overwrite={}, allowUnencryptedValues={}, name={} - {}",
                    object.getCompileTimeClass().getSimpleName(), options.isOverwrite(),
                    options.isAllowUnencryptedValues(), name.getOrig(), name.getNorm());

            if (InternalsConfig.encryptionChecks && !RepoAddOptions.isAllowUnencryptedValues(options)) {
                CryptoUtil.checkEncrypted(object);
            }

            if (InternalsConfig.consistencyChecks) {
                object.checkConsistence(ConsistencyCheckScope.THOROUGH);
            } else {
                object.checkConsistence(ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
            }

            if (object.getVersion() == null) {
                object.setVersion("1");
            }

            return object.getOid() == null || !options.isOverwrite()
                    ? executeAddObject(object)
                    : executeOverwriteObject(object);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
            OperationLogger.logAdd(object, options, operationResult);
        }
    }

    private <T extends ObjectType> String executeAddObject(
            @NotNull PrismObject<T> object)
            throws SchemaException, ObjectAlreadyExistsException {
        long opHandle = registerOperationStart(OP_ADD_OBJECT, object);
            /* old repo code missing in new repo:
            int attempt = 1;
            int restarts = 0;
            String proposedOid = object.getOid();
            while (true) {
            */
        // TODO use executeAttempts

        try {
            String oid = new AddObjectContext<>(repositoryContext, object)
                    .execute();
            invokeConflictWatchers((w) -> w.afterAddObject(oid, object));
            return oid;
        /*
            } catch (RestartOperationRequestedException ex) {
                // special case: we want to restart but we do not want to count these
                LOGGER.trace("Restarting because of {}", ex.getMessage());
                restarts++;
                if (restarts > RESTART_LIMIT) {
                    throw new IllegalStateException("Too many operation restarts");
                }
            } catch (RuntimeException ex) {
                attempt = baseHelper.logOperationAttempt(proposedOid, "adding", attempt, ex, subResult);
//                    pm.registerOperationNewAttempt(opHandle, attempt);
            }
        }
    */
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt
        }
    }

    /** Overwrite is more like update than add. */
    private <T extends ObjectType> String executeOverwriteObject(
            @NotNull PrismObject<T> newObject)
            throws SchemaException, RepositoryException, ObjectAlreadyExistsException {

        String oid = newObject.getOid();
        UUID oidUuid = checkOid(oid);

        long opHandle = registerOperationStart(OP_ADD_OBJECT_OVERWRITE, newObject);
        // TODO use executeAttempts
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            try {
                //noinspection ConstantConditions
                RootUpdateContext<T, QObject<MObject>, MObject> updateContext =
                        prepareUpdateContext(jdbcSession, newObject.getCompileTimeClass(), oidUuid);
                PrismObject<T> prismObject = updateContext.getPrismObject();
                // no precondition check for overwrite

                invokeConflictWatchers(w -> w.beforeModifyObject(prismObject));
                newObject.setUserData(RepositoryService.KEY_ORIGINAL_OBJECT, prismObject.clone());
                ObjectDelta<T> delta = prismObject.diff(newObject, EquivalenceStrategy.LITERAL);
                Collection<? extends ItemDelta<?, ?>> modifications = delta.getModifications();

                LOGGER.trace("overwriteAddObjectAttempt: originalOid={}, modifications={}",
                        oid, modifications);

                Collection<? extends ItemDelta<?, ?>> executedModifications =
                        updateContext.execute(modifications);

                if (!executedModifications.isEmpty()) {
                    invokeConflictWatchers((w) -> w.afterModifyObject(oid));
                }
                LOGGER.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());
            } catch (ObjectNotFoundException e) {
                // so it is just plain addObject after all
                new AddObjectContext<>(repositoryContext, newObject)
                        .execute();
                invokeConflictWatchers((w) -> w.afterAddObject(oid, newObject));
            }
            jdbcSession.commit();
            return oid;
        } catch (RuntimeException e) {
            SqaleUtils.handlePostgresException(e);
            throw e;
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt
        }
    }

    @Override
    @NotNull
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return modifyObject(type, oid, modifications, null, parentResult);
    }

    @Override
    @NotNull
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable RepoModifyOptions options,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        try {
            return modifyObject(type, oid, modifications, null, options, parentResult);
        } catch (PreconditionViolationException e) {
            throw new AssertionError(e); // with null precondition we couldn't get this exception
        }
    }

    @Override
    @NotNull
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable ModificationPrecondition<T> precondition,
            @Nullable RepoModifyOptions options,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {

        Objects.requireNonNull(modifications, "Modifications must not be null.");
        Objects.requireNonNull(type, "Object class in delta must not be null.");
        UUID oidUuid = checkOid(oid);
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        if (options == null) {
            options = new RepoModifyOptions();
        }

        LOGGER.debug("Modify object type '{}', oid={}, reindex={}",
                type.getSimpleName(), oid, options.isForceReindex());

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_MODIFY_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .addArbitraryObjectCollectionAsParam("modifications", modifications)
                .build();

        try {
            if (modifications.isEmpty() && !RepoModifyOptions.isForceReindex(options)) {
                LOGGER.debug("Modification list is empty, nothing was modified.");
                operationResult.recordStatus(OperationResultStatus.SUCCESS,
                        "Modification list is empty, nothing was modified.");
                return new ModifyObjectResult<>(modifications);
            }

            checkModifications(modifications);
            logTraceModifications(modifications);

            return executeModifyObject(type, oidUuid, modifications, precondition);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
            OperationLogger.logModify(type, oid, modifications, precondition, options, operationResult);
        }
    }

    @NotNull
    private <T extends ObjectType> ModifyObjectResult<T> executeModifyObject(
            @NotNull Class<T> type,
            @NotNull UUID oidUuid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable ModificationPrecondition<T> precondition)
            throws SchemaException, ObjectNotFoundException, PreconditionViolationException, RepositoryException {

        long opHandle = registerOperationStart(OP_MODIFY_OBJECT, type);
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            RootUpdateContext<T, QObject<MObject>, MObject> updateContext =
                    prepareUpdateContext(jdbcSession, type, oidUuid);
            PrismObject<T> prismObject = updateContext.getPrismObject();
            if (precondition != null && !precondition.holds(prismObject)) {
                jdbcSession.rollback();
                throw new PreconditionViolationException(
                        "Modification precondition does not hold for " + prismObject);
            }
            invokeConflictWatchers(w -> w.beforeModifyObject(prismObject));
            PrismObject<T> originalObject = prismObject.clone(); // for result later

            modifications = updateContext.execute(modifications);
            jdbcSession.commit();

            LOGGER.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());

            if (!modifications.isEmpty()) {
                invokeConflictWatchers((w) -> w.afterModifyObject(prismObject.getOid()));
            }
            return new ModifyObjectResult<>(originalObject, prismObject, modifications);
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt
        }
    }

    /** Read object for update and returns update context that contains it. */
    private <S extends ObjectType, Q extends QObject<R>, R extends MObject>
    RootUpdateContext<S, Q, R> prepareUpdateContext(
            @NotNull JdbcSession jdbcSession,
            @NotNull Class<S> schemaType,
            @NotNull UUID oid)
            throws SchemaException, ObjectNotFoundException {

        SqaleTableMapping<S, QObject<R>, R> rootMapping =
                repositoryContext.getMappingBySchemaType(schemaType);
        QObject<R> entityPath = rootMapping.defaultAlias();

        Tuple result = jdbcSession.newQuery()
                .select(entityPath.oid, entityPath.fullObject, entityPath.containerIdSeq)
                .from(entityPath)
                .where(entityPath.oid.eq(oid))
                .forUpdate()
                .fetchOne();

        if (result == null || result.get(entityPath.fullObject) == null) {
            throw new ObjectNotFoundException(schemaType, oid.toString());
        }

        S object = rootMapping.toSchemaObject(
                result, entityPath, Collections.emptyList(), jdbcSession, true);

        R rootRow = rootMapping.newRowObject();
        rootRow.oid = oid;
        rootRow.containerIdSeq = result.get(entityPath.containerIdSeq);
        // This column is generated, some sub-entities need it, but we can't push it to DB.
        rootRow.objectType = MObjectType.fromSchemaType(object.getClass());
        // we don't care about full object in row

        return new RootUpdateContext<>(repositoryContext, jdbcSession, object, rootRow);
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObjectDynamically(
            @NotNull Class<T> type,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> getOptions,
            @NotNull ModificationsSupplier<T> modificationsSupplier,
            @Nullable RepoModifyOptions modifyOptions,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        // TODO write proper implementation
        PrismObject<T> object = executeGetObject(type, UUID.fromString(oid), getOptions);
        Collection<? extends ItemDelta<?, ?>> modifications =
                modificationsSupplier.get(object.asObjectable());
        return modifyObject(type, oid, modifications, modifyOptions, parentResult);
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

    private void logTraceModifications(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
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

    @Override
    public @NotNull <T extends ObjectType> DeleteObjectResult deleteObject(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException {

        Validate.notNull(type, "Object type must not be null.");
        UUID oidUuid = checkOid(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Deleting object type '{}' with oid '{}'", type.getSimpleName(), oid);

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_DELETE_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("oid", oid)
                .build();
        try {
            return executeDeleteObject(type, oid, oidUuid);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @NotNull
    private <T extends ObjectType> DeleteObjectResult executeDeleteObject(
            Class<T> type, String oid, UUID oidUuid) throws ObjectNotFoundException {

        long opHandle = registerOperationStart(OP_DELETE_OBJECT, type);
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            DeleteObjectResult result = deleteObjectAttempt(type, oidUuid, jdbcSession);
            invokeConflictWatchers((w) -> w.afterDeleteObject(oid));

            jdbcSession.commit();
            return result;
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt
        }
    }

    private <T extends ObjectType, Q extends QObject<R>, R extends MObject>
    DeleteObjectResult deleteObjectAttempt(Class<T> type, UUID oid, JdbcSession jdbcSession)
            throws ObjectNotFoundException {

        QueryTableMapping<T, Q, R> mapping = repositoryContext.getMappingBySchemaType(type);
        Q entityPath = mapping.defaultAlias();
        byte[] fullObject = jdbcSession.newQuery()
                .select(entityPath.fullObject)
                .forUpdate()
                .from(entityPath)
                .where(entityPath.oid.eq(oid))
                .fetchOne();
        if (fullObject == null) {
            throw new ObjectNotFoundException(type, oid.toString());
        }

        // object delete cascades to all owned related rows
        jdbcSession.newDelete(entityPath)
                .where(entityPath.oid.eq(oid))
                .execute();

        return new DeleteObjectResult(new String(fullObject, StandardCharsets.UTF_8));
    }

    // region Counting/searching
    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        Objects.requireNonNull(type, "Object type must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_COUNT_OBJECTS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        try {
            logSearchInputParameters(type, query, "Count objects");

            query = simplifyQuery(query);
            if (isNoneQuery(query)) {
                return 0;
            }

            return executeCountObject(type, query, options);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private <T extends ObjectType> int executeCountObject(
            @NotNull Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws RepositoryException {

        long opHandle = registerOperationStart(OP_COUNT_OBJECTS, type);
        try {
            return sqlQueryExecutor.count(
                    SqaleQueryContext.from(type, repositoryContext),
                    query, options);
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt (separate try from JDBC session)
        }
    }

    @Override
    public @NotNull <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            @NotNull Class<T> type, ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
            throws SchemaException {
        Objects.requireNonNull(type, "Object type must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_SEARCH_OBJECTS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .addParam("options", String.valueOf(options))
                .build();

        try {
            logSearchInputParameters(type, query, "Search objects");

            query = simplifyQuery(query);
            if (isNoneQuery(query)) {
                return new SearchResultList<>();
            }

            return executeSearchObject(type, query, options, OP_SEARCH_OBJECTS);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private <T extends ObjectType> SearchResultList<PrismObject<T>> executeSearchObject(
            @NotNull Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            String operationKind)
            throws RepositoryException, SchemaException {

        long opHandle = registerOperationStart(operationKind, type);
        try {
            SearchResultList<T> result = sqlQueryExecutor.list(
                    SqaleQueryContext.from(type, repositoryContext),
                    query,
                    options);
            // TODO see the commented code from old repo lower, problems for each object must be caught
            //noinspection unchecked
            return result.map(
                    o -> (PrismObject<T>) o.asPrismObject());
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt (separate try from JDBC session)
        }
    }

    /*
    TODO from ObjectRetriever, how to do this per-object Throwable catch + record result?
     should we smuggle the OperationResult all the way to the mapping call?
    @NotNull
    private <T extends ObjectType> List<PrismObject<T>> queryResultToPrismObjects(
            List<T> objects, Class<T> type,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws SchemaException {
        List<PrismObject<T>> rv = new ArrayList<>();
        if (objects == null) {
            return rv;
        }
        for (T object : objects) {
            String oid = object.getOid();
            Holder<PrismObject<T>> partialValueHolder = new Holder<>();
            PrismObject<T> prismObject;
            try {
                prismObject = createPrismObject(object, type, oid, options, partialValueHolder);
            } catch (Throwable t) {
                if (!partialValueHolder.isEmpty()) {
                    prismObject = partialValueHolder.getValue();
                } else {
                    prismObject = prismContext.createObject(type);
                    prismObject.setOid(oid);
                    prismObject.asObjectable().setName(PolyStringType.fromOrig("Unreadable object"));
                }
                result.recordFatalError("Couldn't retrieve " + type + " " + oid + ": " + t.getMessage(), t);
                prismObject.asObjectable().setFetchResult(result.createOperationResultType());
            }
            rv.add(prismObject);
        }
        return rv;
    }
    */

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
            OperationResult parentResult) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_SEARCH_OBJECTS_ITERATIVE)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        try {
            logSearchInputParameters(type, query, "Iterative search objects");

            query = simplifyQuery(query);
            if (isNoneQuery(query)) {
                return new SearchResultMetadata().approxNumberOfAllResults(0);
            }

            return executeSearchObjectsIterative(type, query, handler, options, operationResult);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private static final ItemPath OID_PATH = ItemPath.create(PrismConstants.T_ID);

    private <T extends ObjectType> SearchResultMetadata executeSearchObjectsIterative(
            Class<T> type,
            ObjectQuery originalQuery,
            ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult operationResult) throws SchemaException, RepositoryException {

        try {
            ObjectPaging originalPaging = originalQuery != null ? originalQuery.getPaging() : null;
            // this is total requested size of the search
            Integer maxSize = originalPaging != null ? originalPaging.getMaxSize() : null;

            List<? extends ObjectOrdering> providedOrdering = originalPaging != null
                    ? originalPaging.getOrderingInstructions()
                    : null;
            if (providedOrdering != null && providedOrdering.size() > 1) {
                throw new RepositoryException("searchObjectsIterative() does not support ordering"
                        + " by multiple paths (yet): " + providedOrdering);
            }

            ObjectQuery pagedQuery = prismContext().queryFactory().createQuery();
            ObjectPaging paging = prismContext().queryFactory().createPaging();
            if (originalPaging != null && originalPaging.getOrderingInstructions() != null) {
                originalPaging.getOrderingInstructions().forEach(o ->
                        paging.addOrderingInstruction(o.getOrderBy(), o.getDirection()));
            }
            paging.addOrderingInstruction(OID_PATH, OrderDirection.ASCENDING);
            pagedQuery.setPaging(paging);

            int pageSize = Math.min(
                    repositoryConfiguration().getIterativeSearchByPagingBatchSize(),
                    defaultIfNull(maxSize, Integer.MAX_VALUE));
            pagedQuery.getPaging().setMaxSize(pageSize);

            PrismObject<T> lastProcessedObject = null;
            int handledObjectsTotal = 0;

            while (true) {
                if (maxSize != null && maxSize - handledObjectsTotal < pageSize) {
                    // relevant only for the last page
                    pagedQuery.getPaging().setMaxSize(maxSize - handledObjectsTotal);
                }

                // filterAnd() is quite null safe, even for both nulls
                pagedQuery.setFilter(ObjectQueryUtil.filterAnd(
                        originalQuery != null ? originalQuery.getFilter() : null,
                        lastOidCondition(lastProcessedObject, providedOrdering),
                        prismContext()));

                // we don't call public searchObject to avoid subresults and query simplification
                logSearchInputParameters(type, pagedQuery, "Search object iterative page ");
                List<PrismObject<T>> objects = executeSearchObject(
                        type, pagedQuery, options, OP_SEARCH_OBJECTS_ITERATIVE_PAGE);

                // process page results
                for (PrismObject<T> object : objects) {
                    lastProcessedObject = object;
                    if (!handler.handle(object, operationResult)) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal + 1)
                                .pagingCookie(lastProcessedObject.getOid())
                                .partialResults(true);
                    }
                    handledObjectsTotal += 1;

                    if (maxSize != null && handledObjectsTotal >= maxSize) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal)
                                .pagingCookie(lastProcessedObject.getOid());
                    }
                }

                if (objects.isEmpty() || objects.size() < pageSize) {
                    return new SearchResultMetadata()
                            .approxNumberOfAllResults(handledObjectsTotal)
                            .pagingCookie(lastProcessedObject != null
                                    ? lastProcessedObject.getOid() : null);
                }
            }
        } finally {
            // This just counts the operation and adds zero/minimal time not to confuse user
            // with what could be possibly very long duration.
            long opHandle = registerOperationStart(OP_SEARCH_OBJECTS_ITERATIVE, type);
            registerOperationFinish(opHandle, 1);
        }
    }

    /**
     * Without requested ordering, this is easy: `WHERE oid > lastOid`
     *
     * But with outside ordering we need to respect it and for ordering by X, Y, Z use
     * (`original conditions AND` is taken care of outside of this method):
     *
     * ----
     * ... WHERE original conditions AND (
     * X > last.X
     * OR (X = last.X AND Y > last.Y)
     * OR (X = last.X AND Y = last.Y AND Z > last.Z)
     * OR (X = last.X AND Y = last.Y ...if all equal AND OID > last.OID)
     * ----
     *
     * This is suddenly much more fun, isn't it?
     * Of course the condition `>` or `<` depends on `ASC` vs `DESC`.
     *
     * TODO: Currently, single path ordering is supported. Finish multi-path too.
     * TODO: What about nullable columns?
     */
    @Nullable
    private <T extends ObjectType> ObjectFilter lastOidCondition(
            PrismObject<T> lastProcessedObject, List<? extends ObjectOrdering> providedOrdering) {
        if (lastProcessedObject == null) {
            return null;
        }

        String lastProcessedOid = lastProcessedObject.getOid();
        if (providedOrdering == null || providedOrdering.isEmpty()) {
            return prismContext()
                    .queryFor(lastProcessedObject.getCompileTimeClass())
                    .item(OID_PATH).gt(lastProcessedOid).buildFilter();
        }

        if (providedOrdering.size() == 1) {
            ObjectOrdering objectOrdering = providedOrdering.get(0);
            ItemPath orderByPath = objectOrdering.getOrderBy();
            boolean asc = objectOrdering.getDirection() == OrderDirection.ASCENDING;
            S_ConditionEntry filter = prismContext()
                    .queryFor(lastProcessedObject.getCompileTimeClass())
                    .item(orderByPath);
            //noinspection rawtypes
            Item<PrismValue, ItemDefinition> item = lastProcessedObject.findItem(orderByPath);
            if (item.size() > 1) {
                throw new IllegalArgumentException(
                        "Multi-value property for ordering is forbidden - item: " + item);
            } else if (item.isEmpty()) {
                // TODO what if it's nullable? is it null-first or last?
                // See: https://www.postgresql.org/docs/13/queries-order.html
                // "By default, null values sort as if larger than any non-null value; that is,
                // NULLS FIRST is the default for DESC order, and NULLS LAST otherwise."
            } else {
                S_MatchingRuleEntry matchingRuleEntry =
                        asc ? filter.gt(item.getRealValue()) : filter.lt(item.getRealValue());
                filter = matchingRuleEntry.or()
                        .block()
                        .item(orderByPath).eq(item.getRealValue())
                        .and()
                        .item(OID_PATH);
                return (asc ? filter.gt(lastProcessedOid) : filter.lt(lastProcessedOid))
                        .endBlock()
                        .buildFilter();
            }
        }

        throw new IllegalArgumentException(
                "Shouldn't get here with check in executeSearchObjectsIterative()");
        /*
        TODO: Unfinished - this is painful with fluent API. Should I call
         prismContext().queryFor(lastProcessedObject.getCompileTimeClass()) for each component
         and then use ObjectQueryUtil.filterAnd/Or?
        // we need to handle the complicated case with externally provided ordering
        S_FilterEntryOrEmpty orBlock = prismContext()
                .queryFor(lastProcessedObject.getCompileTimeClass()).block();
        orLoop:
        for (ObjectOrdering orMasterOrdering : providedOrdering) {
            Iterator<? extends ObjectOrdering> iterator = providedOrdering.iterator();
            while (iterator.hasNext()) {
                S_FilterEntryOrEmpty andBlock = orBlock.block();
                ObjectOrdering ordering = iterator.next();
                if (ordering.equals(orMasterOrdering)) {
                    // ...
                    continue orLoop;
                }
                orBlock = andBlock.endBlock();
            }

        }
        return orBlock.endBlock().buildFilter();
        */
    }

    @Override
    public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        Objects.requireNonNull(type, "Container type must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult =
                parentResult.subresult(OP_NAME_PREFIX + OP_COUNT_CONTAINERS)
                        .addQualifier(type.getSimpleName())
                        .addParam("type", type.getName())
                        .addParam("query", query)
                        .build();
        try {
            logSearchInputParameters(type, query, "Count containers");

            query = simplifyQuery(query);
            if (isNoneQuery(query)) {
                return 0;
            }

            return executeCountContainers(type, query, options);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private <T extends Containerable> int executeCountContainers(
            @NotNull Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws RepositoryException {

        long opHandle = registerOperationStart(OP_COUNT_CONTAINERS, type);
        try {
            return sqlQueryExecutor.count(
                    SqaleQueryContext.from(type, repositoryContext),
                    query, options);
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt (separate try from JDBC session)
        }
    }

    @Override
    public <T extends Containerable> SearchResultList<T> searchContainers(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {

        Objects.requireNonNull(type, "Container type must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_SEARCH_CONTAINERS)
                .addQualifier(type.getSimpleName())
                .addParam("type", type.getName())
                .addParam("query", query)
                .build();

        try {
            logSearchInputParameters(type, query, "Search containers");

            query = simplifyQuery(query);
            if (isNoneQuery(query)) {
                return new SearchResultList<>();
            }

            SqaleQueryContext<T, FlexibleRelationalPathBase<Object>, Object> queryContext =
                    SqaleQueryContext.from(type, repositoryContext);
            return sqlQueryExecutor.list(queryContext, query, options);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private <T> void logSearchInputParameters(Class<T> type, ObjectQuery query, String operation) {
        ObjectPaging paging = query != null ? query.getPaging() : null;
        LOGGER.debug(
                "{} of type '{}', query on trace level, offset {}, limit {}.",
                operation, type.getSimpleName(),
                paging != null ? paging.getOffset() : "undefined",
                paging != null ? paging.getMaxSize() : "undefined");

        LOGGER.trace("Full query\n{}",
                query == null ? "undefined" : query.debugDumpLazily());
    }

    private ObjectQuery simplifyQuery(ObjectQuery query) {
        if (query != null) {
            // simplify() creates new filter instance which can be modified
            ObjectFilter filter = ObjectQueryUtil.simplify(query.getFilter(), prismContext());
            query = query.cloneWithoutFilter();
            query.setFilter(filter instanceof AllFilter ? null : filter);
        }

        return query;
    }

    private boolean isNoneQuery(ObjectQuery query) {
        return query != null && query.getFilter() instanceof NoneFilter;
    }
    // endregion

    @Override
    public <O extends ObjectType> boolean isDescendant(
            PrismObject<O> object, String ancestorOrgOid) {
        Validate.notNull(object, "object must not be null");
        Validate.notNull(ancestorOrgOid, "ancestorOrgOid must not be null");

        LOGGER.trace("Querying if object {} is descendant of {}", object.getOid(), ancestorOrgOid);
        List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
        if (objParentOrgRefs == null || objParentOrgRefs.isEmpty()) {
            return false;
        }

        List<UUID> objParentOrgOids = objParentOrgRefs.stream()
                .map(ref -> UUID.fromString(ref.getOid()))
                .collect(Collectors.toList());

        long opHandle = registerOperationStart(OP_IS_DESCENDANT, OrgType.class);
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            jdbcSession.executeStatement("CALL m_refresh_org_closure()");

            QOrgClosure oc = new QOrgClosure();
            long count = jdbcSession.newQuery()
                    .from(oc)
                    .where(oc.ancestorOid.eq(UUID.fromString(ancestorOrgOid))
                            .and(oc.descendantOid.in(objParentOrgOids)))
                    .fetchCount();
            return count != 0L;
        } finally {
            registerOperationFinish(opHandle, 1);
        }
    }

    @Override
    public <O extends ObjectType> boolean isAncestor(
            PrismObject<O> object, String descendantOrgOid) {
        Validate.notNull(object, "object must not be null");
        Validate.notNull(descendantOrgOid, "descendantOrgOid must not be null");

        LOGGER.trace("Querying if object {} is ancestor of {}", object.getOid(), descendantOrgOid);
        // object is not considered ancestor of itself
        if (object.getOid() == null || object.getOid().equals(descendantOrgOid)) {
            return false;
        }

        long opHandle = registerOperationStart(OP_IS_ANCESTOR, OrgType.class);
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            jdbcSession.executeStatement("CALL m_refresh_org_closure()");

            QOrgClosure oc = new QOrgClosure();
            long count = jdbcSession.newQuery()
                    .from(oc)
                    .where(oc.ancestorOid.eq(UUID.fromString(object.getOid()))
                            .and(oc.descendantOid.eq(UUID.fromString(descendantOrgOid))))
                    .fetchCount();
            return count != 0L;
        } finally {
            registerOperationFinish(opHandle, 1);
        }
    }

    @Override
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult =
                parentResult.subresult(OP_NAME_PREFIX + OP_SEARCH_SHADOW_OWNER)
                        .addParam("shadowOid", shadowOid)
                        .addParam("options", String.valueOf(options))
                        .build();

        try {
            ObjectQuery query = prismContext()
                    .queryFor(FocusType.class)
                    .item(FocusType.F_LINK_REF).ref(shadowOid)
                    .build();
            SearchResultList<PrismObject<FocusType>> result =
                    executeSearchObject(FocusType.class, query, options, OP_SEARCH_SHADOW_OWNER);

            if (result == null || result.isEmpty()) {
                // account shadow owner was not found
                return null;
            } else if (result.size() > 1) {
                LOGGER.warn("Found {} owners for shadow oid {}, returning first owner.",
                        result.size(), shadowOid);
            }
            //noinspection unchecked
            return (PrismObject<F>) result.get(0);
        } catch (RepositoryException | RuntimeException | SchemaException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @Override
    public long advanceSequence(String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        UUID oidUuid = checkOid(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Advancing sequence {}", oid);

        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_ADVANCE_SEQUENCE)
                .addParam("oid", oid)
                .build();

        try {
            return executeAdvanceSequence(oidUuid);
        } catch (RepositoryException | RuntimeException | SchemaException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private long executeAdvanceSequence(UUID oid)
            throws ObjectNotFoundException, SchemaException, RepositoryException {
        // TODO executeAttempts if any problems with further test, so far it looks good based on SequenceTestConcurrency
        long opHandle = registerOperationStart(OP_ADVANCE_SEQUENCE, SequenceType.class);

        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            RootUpdateContext<SequenceType, QObject<MObject>, MObject> updateContext =
                    prepareUpdateContext(jdbcSession, SequenceType.class, oid);
            SequenceType sequence = updateContext.getPrismObject().asObjectable();

            LOGGER.trace("OBJECT before:\n{}", sequence.debugDumpLazily());

            long returnValue;
            if (!sequence.getUnusedValues().isEmpty()) {
                returnValue = sequence.getUnusedValues().remove(0);
            } else {
                returnValue = advanceSequence(sequence, oid);
            }

            LOGGER.trace("Return value = {}, OBJECT after:\n{}",
                    returnValue, sequence.debugDumpLazily());

            updateContext.finishExecutionOwn();
            jdbcSession.commit();
            return returnValue;
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt
        }
    }

    private long advanceSequence(SequenceType sequence, UUID oid) {
        long returnValue;
        long counter = sequence.getCounter() != null ? sequence.getCounter() : 0L;
        long maxCounter = sequence.getMaxCounter() != null
                ? sequence.getMaxCounter() : Long.MAX_VALUE;
        boolean allowRewind = Boolean.TRUE.equals(sequence.isAllowRewind());

        if (counter < maxCounter) {
            returnValue = counter;
            sequence.setCounter(counter + 1);
        } else if (counter == maxCounter) {
            returnValue = counter;
            if (allowRewind) {
                sequence.setCounter(0L);
            } else {
                sequence.setCounter(counter + 1); // will produce exception during next run
            }
        } else { // i.e. counter > maxCounter
            if (allowRewind) { // shouldn't occur but...
                LOGGER.warn("Sequence {} overflown with allowRewind set to true. Rewinding.", oid);
                returnValue = 0;
                sequence.setCounter(1L);
            } else {
                throw new SystemException("No (next) value available from sequence " + oid
                        + ". Current counter = " + sequence.getCounter()
                        + ", max value = " + sequence.getMaxCounter());
            }
        }
        return returnValue;
    }

    @Override
    public void returnUnusedValuesToSequence(
            String oid, Collection<Long> unusedValues, OperationResult parentResult)
            throws ObjectNotFoundException {
        UUID oidUuid = checkOid(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Returning unused values of {} to sequence {}", unusedValues, oid);

        OperationResult operationResult =
                parentResult.subresult(OP_NAME_PREFIX + OP_RETURN_UNUSED_VALUES_TO_SEQUENCE)
                        .addParam("oid", oid)
                        .build();

        if (unusedValues == null || unusedValues.isEmpty()) {
            operationResult.recordSuccess();
            return;
        }

        try {
            executeReturnUnusedValuesToSequence(oidUuid, unusedValues);
        } catch (RepositoryException | RuntimeException | SchemaException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    private void executeReturnUnusedValuesToSequence(UUID oid, Collection<Long> unusedValues)
            throws SchemaException, ObjectNotFoundException, RepositoryException {
        // TODO executeAttempts
        long opHandle = registerOperationStart(
                OP_RETURN_UNUSED_VALUES_TO_SEQUENCE, SequenceType.class);

        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            RootUpdateContext<SequenceType, QObject<MObject>, MObject> updateContext =
                    prepareUpdateContext(jdbcSession, SequenceType.class, oid);
            SequenceType sequence = updateContext.getPrismObject().asObjectable();

            LOGGER.trace("OBJECT before:\n{}", sequence.debugDumpLazily());

            int maxUnusedValues = sequence.getMaxUnusedValues() != null
                    ? sequence.getMaxUnusedValues() : 0;
            Iterator<Long> valuesToReturnIterator = unusedValues.iterator();
            while (valuesToReturnIterator.hasNext()
                    && sequence.getUnusedValues().size() < maxUnusedValues) {
                Long valueToReturn = valuesToReturnIterator.next();
                if (valueToReturn == null) { // sanity check
                    continue;
                }
                if (!sequence.getUnusedValues().contains(valueToReturn)) {
                    sequence.getUnusedValues().add(valueToReturn);
                } else {
                    LOGGER.warn("UnusedValues in sequence {} already contains value of {}"
                            + " - ignoring the return request", oid, valueToReturn);
                }
            }

            LOGGER.trace("OBJECT after:\n{}", sequence.debugDumpLazily());

            updateContext.finishExecutionOwn();
            jdbcSession.commit();
        } finally {
            registerOperationFinish(opHandle, 1); // TODO attempt
        }
    }

    @Override
    public RepositoryDiag getRepositoryDiag() {
        LOGGER.debug("Getting repository diagnostics.");

        RepositoryDiag diag = new RepositoryDiag();
        diag.setImplementationShortName("SQaLe");
        diag.setImplementationDescription(
                "Implementation that stores data in PostgreSQL database using JDBC with Querydsl.");

        JdbcRepositoryConfiguration config = repositoryConfiguration();
        diag.setDriverShortName(config.getDriverClassName());
        diag.setRepositoryUrl(config.getJdbcUrl());
        diag.setEmbedded(config.isEmbedded());

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (!driver.getClass().getName().equals(config.getDriverClassName())) {
                continue;
            }

            diag.setDriverVersion(driver.getMajorVersion() + "." + driver.getMinorVersion());
        }

        List<LabeledString> details = new ArrayList<>();
        diag.setAdditionalDetails(details);
        details.add(new LabeledString("dataSource", config.getDataSource()));

        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            details.add(new LabeledString("transactionIsolation",
                    getTransactionIsolation(jdbcSession.connection(), config)));

            try {
                Properties info = jdbcSession.connection().getClientInfo();
                if (info != null) {
                    for (String name : info.stringPropertyNames()) {
                        details.add(new LabeledString("clientInfo." + name, info.getProperty(name)));
                    }
                }
            } catch (SQLException e) {
                details.add(new LabeledString("clientInfo-error", e.toString()));
            }

            long startMs = System.currentTimeMillis();
            jdbcSession.executeStatement("select 1");
            details.add(new LabeledString("select-1-round-trip-ms",
                    String.valueOf(System.currentTimeMillis() - startMs)));
        }

        details.sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getLabel(), o2.getLabel()));

        return diag;
    }

    private String getTransactionIsolation(
            Connection connection, JdbcRepositoryConfiguration config) {
        String value = config.getTransactionIsolation() != null ?
                config.getTransactionIsolation().name() + "(read from repo configuration)" : null;

        try {
            switch (connection.getTransactionIsolation()) {
                case Connection.TRANSACTION_NONE:
                    value = "TRANSACTION_NONE (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    value = "TRANSACTION_READ_COMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    value = "TRANSACTION_READ_UNCOMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    value = "TRANSACTION_REPEATABLE_READ (read from connection)";
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    value = "TRANSACTION_SERIALIZABLE (read from connection)";
                    break;
                default:
                    value = "Unknown value in connection.";
            }
        } catch (Exception ex) {
            //nowhere to report error (no operation result available)
        }

        return value;
    }

    @Override
    public void repositorySelfTest(OperationResult parentResult) {
        OperationResult operationResult =
                parentResult.createSubresult(OP_NAME_PREFIX + OP_REPOSITORY_SELF_TEST);
        try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
            long startMs = System.currentTimeMillis();
            jdbcSession.executeStatement("select 1");
            operationResult.addReturn("database-round-trip-ms", System.currentTimeMillis() - startMs);
            operationResult.recordSuccess();
        } catch (Exception e) {
            operationResult.recordFatalError(e);
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult parentResult) {
        OperationResult operationResult =
                parentResult.subresult(OP_NAME_PREFIX + OP_TEST_ORG_CLOSURE_CONSISTENCY)
                        .addParam("repairIfNecessary", repairIfNecessary)
                        .build();

        try {
            long closureCount, expectedCount;
            try (JdbcSession jdbcSession =
                    repositoryContext.newJdbcSession().startReadOnlyTransaction()) {
                QOrgClosure oc = new QOrgClosure();
                closureCount = jdbcSession.newQuery().from(oc).fetchCount();
                // this is CTE used also for m_org_closure materialized view (here with count)
                QOrg o = QOrgMapping.getOrgMapping().defaultAlias();
                QObjectReference<?> ref = QObjectReferenceMapping.getForParentOrg().newAlias("ref");
                QObjectReference<?> par = QObjectReferenceMapping.getForParentOrg().newAlias("par");
                //noinspection unchecked
                expectedCount = jdbcSession.newQuery()
                        .withRecursive(oc, oc.ancestorOid, oc.descendantOid)
                        .as(new SQLQuery<>().union(
                                // non-recursive term: initial select
                                new SQLQuery<>().select(o.oid, o.oid)
                                        .from(o)
                                        .where(new SQLQuery<>().select(Expressions.ONE)
                                                .from(ref)
                                                .where(ref.targetOid.eq(o.oid)
                                                        .or(ref.ownerOid.eq(o.oid)))
                                                .exists()),
                                new SQLQuery<>().select(par.targetOid, oc.descendantOid)
                                        .from(par, oc)
                                        .where(par.ownerOid.eq(oc.ancestorOid))))
                        .from(oc)
                        .fetchCount();
                LOGGER.info("Org closure consistency checked - closure count {}, expected count {}",
                        closureCount, expectedCount);
            }
            operationResult.addReturn("closure-count", closureCount);
            operationResult.addReturn("expected-count", expectedCount);

            if (repairIfNecessary && closureCount != expectedCount) {
                try (JdbcSession jdbcSession = repositoryContext.newJdbcSession().startTransaction()) {
                    jdbcSession.executeStatement("CALL m_refresh_org_closure(true)");
                    jdbcSession.commit();
                }
                LOGGER.info("Org closure rebuild was requested and executed");
                operationResult.addReturn("rebuild-done", true);
            } else {
                operationResult.addReturn("rebuild-done", false);
            }

            operationResult.recordSuccess();
        } catch (Exception e) {
            operationResult.recordFatalError(e);
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(
            RepositoryQueryDiagRequest request, OperationResult parentResult) {

        // TODO search like containers + dry run?
        throw new UnsupportedOperationException();
    }

    @Override
    public <O extends ObjectType> boolean selectorMatches(
            ObjectSelectorType objectSelector, PrismObject<O> object,
            ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        // this code is taken from old repo virtually as-was
        if (objectSelector == null) {
            logger.trace("{} null object specification", logMessagePrefix);
            return false;
        }

        if (object == null) {
            logger.trace("{} null object", logMessagePrefix);
            return false;
        }

        SearchFilterType specFilterType = objectSelector.getFilter();
        ObjectReferenceType specOrgRef = objectSelector.getOrgRef();
        QName specTypeQName = objectSelector.getType(); // now it does not matter if it's unqualified
        PrismObjectDefinition<O> objectDefinition = object.getDefinition();

        // Type
        if (specTypeQName != null && !object.canRepresent(specTypeQName)) {
            if (logger.isTraceEnabled()) {
                logger.trace("{} type mismatch, expected {}, was {}",
                        logMessagePrefix,
                        PrettyPrinter.prettyPrint(specTypeQName),
                        PrettyPrinter.prettyPrint(objectDefinition.getTypeName()));
            }
            return false;
        }

        // Subtype
        String specSubtype = objectSelector.getSubtype();
        if (specSubtype != null) {
            Collection<String> actualSubtypeValues = FocusTypeUtil.determineSubTypes(object);
            if (!actualSubtypeValues.contains(specSubtype)) {
                logger.trace("{} subtype mismatch, expected {}, was {}",
                        logMessagePrefix, specSubtype, actualSubtypeValues);
                return false;
            }
        }

        // Archetype
        List<ObjectReferenceType> specArchetypeRefs = objectSelector.getArchetypeRef();
        if (!specArchetypeRefs.isEmpty()) {
            if (object.canRepresent(AssignmentHolderType.class)) {
                boolean match = false;
                List<ObjectReferenceType> actualArchetypeRefs =
                        ((AssignmentHolderType) object.asObjectable()).getArchetypeRef();
                for (ObjectReferenceType specArchetypeRef : specArchetypeRefs) {
                    for (ObjectReferenceType actualArchetypeRef : actualArchetypeRefs) {
                        if (actualArchetypeRef.getOid().equals(specArchetypeRef.getOid())) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) {
                    logger.trace("{} archetype mismatch, expected {}, was {}",
                            logMessagePrefix, specArchetypeRefs, actualArchetypeRefs);
                    return false;
                }
            } else {
                logger.trace("{} archetype mismatch, expected {} but object has none (it is not of AssignmentHolderType)",
                        logMessagePrefix, specArchetypeRefs);
                return false;
            }
        }

        // Filter
        if (specFilterType != null) {
            ObjectFilter specFilter = object.getPrismContext().getQueryConverter()
                    .createObjectFilter(object.getCompileTimeClass(), specFilterType);
            if (filterEvaluator != null) {
                specFilter = filterEvaluator.evaluate(specFilter);
            }
            ObjectTypeUtil.normalizeFilter(specFilter, repositoryContext.relationRegistry()); // we assume object is already normalized
            if (specFilter != null) {
                ObjectQueryUtil.assertPropertyOnly(specFilter, logMessagePrefix + " filter is not property-only filter");
            }
            try {
                if (!ObjectQuery.match(object, specFilter, repositoryContext.matchingRuleRegistry())) {
                    logger.trace("{} object OID {}", logMessagePrefix, object.getOid());
                    return false;
                }
            } catch (SchemaException ex) {
                throw new SchemaException(logMessagePrefix + "could not apply for " + object + ": "
                        + ex.getMessage(), ex);
            }
        }

        // Org
        if (specOrgRef != null) {
            if (!isDescendant(object, specOrgRef.getOid())) {
                logger.trace("{} object OID {} (org={})",
                        logMessagePrefix, object.getOid(), specOrgRef.getOid());
                return false;
            }
        }

        return true;
    }

    @Override
    public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
        LOGGER.info("Applying full text search configuration ({} entries)",
                fullTextSearch != null ? fullTextSearch.getIndexed().size() : 0);
        fullTextSearchConfiguration = fullTextSearch;
    }

    @Override
    public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
        return fullTextSearchConfiguration;
    }

    @Override
    public void postInit(OperationResult parentResult) throws SchemaException {
        LOGGER.debug("Executing repository postInit method");
        systemConfigurationChangeDispatcher.dispatch(true, true, parentResult);
    }

    private void invokeConflictWatchers(Consumer<ConflictWatcherImpl> consumer) {
        conflictWatchersThreadLocal.get().forEach(consumer);
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
        OperationResult operationResult = parentResult.subresult(OP_NAME_PREFIX + OP_HAS_CONFLICT)
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
                    executeGetVersion(ObjectType.class, UUID.fromString(watcher.getOid()));
                } catch (ObjectNotFoundException e) {
                    // just ignore this
                }
                rv = watcher.hasConflict();
            }
            operationResult.addReturn("hasConflict", rv);
            return rv;
        } catch (Throwable t) {
            operationResult.recordFatalError(t);
            throw t;
        } finally {
            operationResult.computeStatusIfUnknown();
        }
    }

    @Override
    public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid,
            DiagnosticInformationType information, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        OperationResult operationResult =
                parentResult.subresult(OP_NAME_PREFIX + OP_ADD_DIAGNOSTIC_INFORMATION)
                        .addQualifier(type.getSimpleName())
                        .addParam("type", type)
                        .addParam("oid", oid)
                        .build();
        try {
            PrismObject<T> object = getObject(type, oid, null, operationResult);
            // TODO when on limit this calls modify twice, wouldn't single modify be better?
            boolean canStoreInfo = pruneDiagnosticInformation(type, oid, information,
                    object.asObjectable().getDiagnosticInformation(), operationResult);
            if (canStoreInfo) {
                List<ItemDelta<?, ?>> modifications = prismContext()
                        .deltaFor(type)
                        .item(ObjectType.F_DIAGNOSTIC_INFORMATION).add(information)
                        .asItemDeltas();
                modifyObject(type, oid, modifications, operationResult);
            }
            operationResult.computeStatus();
        } catch (Throwable t) {
            operationResult.recordFatalError("Couldn't add diagnostic information: " + t.getMessage(), t);
            throw t;
        }
    }

    // TODO replace by something in system configuration (postponing until this feature is used more)
    private static final Map<String, Integer> DIAG_INFO_CLEANUP_POLICY = Map.of(
            SchemaConstants.TASK_THREAD_DUMP_URI, 5);

    /** Nullable for unlimited, 0 means that no info is possible. */
    private static final Integer DIAG_INFO_DEFAULT_LIMIT = 2;

    // returns true if the new information can be stored
    private <T extends ObjectType> boolean pruneDiagnosticInformation(
            Class<T> type, String oid, DiagnosticInformationType newInformation,
            List<DiagnosticInformationType> oldInformationList, OperationResult operationResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        String infoType = newInformation.getType();
        if (infoType == null) {
            throw new IllegalArgumentException("Diagnostic information type is not specified");
        }
        Integer limit = DIAG_INFO_CLEANUP_POLICY.getOrDefault(infoType, DIAG_INFO_DEFAULT_LIMIT);
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
                List<ItemDelta<?, ?>> modifications = prismContext()
                        .deltaFor(type)
                        .item(ObjectType.F_DIAGNOSTIC_INFORMATION).deleteRealValues(toDelete)
                        .asItemDeltas();
                modifyObject(type, oid, modifications, operationResult);
            }
            return limit > 0;
        } else {
            return true;
        }
    }

    @Override
    public SqlPerformanceMonitorImpl getPerformanceMonitor() {
        return performanceMonitor;
    }

    @PreDestroy
    public void destroy() {
        if (performanceMonitor != null) {
            performanceMonitor.shutdown();
            sqlPerformanceMonitorsCollection.deregister(performanceMonitor);
            performanceMonitor = null;
        }
    }

    private PrismContext prismContext() {
        return repositoryContext.prismContext();
    }

    private JdbcRepositoryConfiguration repositoryConfiguration() {
        return repositoryContext.getJdbcRepositoryConfiguration();
    }

    /**
     * Handles exception outside of transaction - this does not handle transactional problems.
     * Returns {@link SystemException}, call with `throw` keyword.
     */
    private SystemException handledGeneralException(
            @NotNull Throwable ex, OperationResult operationResult) {
        // TODO reconsider this whole mechanism including isFatalException decision
        LOGGER.error("General checked exception occurred.", ex);
        recordException(ex, operationResult,
                repositoryConfiguration().isFatalException(ex));

        return ex instanceof SystemException
                ? (SystemException) ex
                : new SystemException(ex.getMessage(), ex);
    }

    private void recordException(
            @NotNull Throwable ex, OperationResult operationResult, boolean fatal) {
        String message = Strings.isNullOrEmpty(ex.getMessage()) ? "null" : ex.getMessage();
        if (Strings.isNullOrEmpty(message)) {
            message = ex.getMessage();
        }

        // non-fatal errors will NOT be put into OperationResult, not to confuse the user
        if (operationResult != null && fatal) {
            operationResult.recordFatalError(message, ex);
        }
    }

    private <T extends Containerable> long registerOperationStart(
            String kind, PrismContainer<T> object) {
        return performanceMonitor.registerOperationStart(kind, object.getCompileTimeClass());
    }

    private <T extends Containerable> long registerOperationStart(String kind, Class<T> type) {
        return performanceMonitor.registerOperationStart(kind, type);
    }

    private void registerOperationFinish(long opHandle, int attempt) {
        performanceMonitor.registerOperationFinish(opHandle, attempt);
    }
}
