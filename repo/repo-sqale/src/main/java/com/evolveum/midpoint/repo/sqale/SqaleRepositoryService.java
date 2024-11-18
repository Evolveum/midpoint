/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.GetOperationOptions.isAllowNotFound;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.util.MiscUtil;

import com.evolveum.midpoint.util.backoff.BackoffComputer;
import com.evolveum.midpoint.util.backoff.ExponentialBackoffComputer;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.google.common.base.Strings;
import com.google.common.collect.ObjectArrays;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.sql.SQLQuery;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PSQLException;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.SequenceUtil;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_ConditionEntry;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrg;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgClosure;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.simulation.QProcessedObject;
import com.evolveum.midpoint.repo.sqale.qmodel.simulation.QProcessedObjectMapping;
import com.evolveum.midpoint.repo.sqale.update.AddObjectContext;
import com.evolveum.midpoint.repo.sqale.update.RootUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.*;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Repository implementation based on SQL, JDBC and Querydsl without any ORM.
 *
 * Typical structure of main public methods:
 *
 * - Argument checks.
 * - Debug log.
 * This can be postponed after operation result creation, if more complex log is needed.
 * - Create {@link OperationResult} from provided parent, immediately followed by try/catch/finally.
 * See {@link #addObject} for example.
 * - More arg checks if necessary, fast fail scenarios.
 * - Call to `executeMethodName(...)` where perf monitor is initialized followed by try/catch/finally.
 * See {@link #executeAddObject} for example.
 *
 * Always try to keep starting {@link JdbcSession} in try-with-resource construct.
 * Positive flows require explicit {@link JdbcSession#commit()} otherwise the changes are rolled back.
 */
public class SqaleRepositoryService extends SqaleServiceBase implements RepositoryService {

    public static final int INITIAL_VERSION_NUMBER = 0;
    public static final String INITIAL_VERSION_STRING = String.valueOf(INITIAL_VERSION_NUMBER);

    private static final int MAX_CONFLICT_WATCHERS = 10;

    private static final Collection<SelectorOptions<GetOperationOptions>> GET_FOR_UPDATE_OPTIONS =
            SchemaService.get().getOperationOptionsBuilder().build();

    private static final Collection<SelectorOptions<GetOperationOptions>> GET_FOR_REINDEX_OPTIONS =
            SchemaService.get().getOperationOptionsBuilder()
                    .retrieve()
                    .raw()
                    .build();

    private final SqlQueryExecutor sqlQueryExecutor;

    private final SqaleSystemConfigurationListener configurationChangeListener;

    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    private final ThreadLocal<List<ConflictWatcherImpl>> conflictWatchersThreadLocal =
            ThreadLocal.withInitial(ArrayList::new);

    private FullTextSearchConfigurationType fullTextSearchConfiguration;

    public SqaleRepositoryService(
            SqaleRepoContext repositoryContext,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        super(repositoryContext, sqlPerformanceMonitorsCollection);
        this.sqlQueryExecutor = new SqlQueryExecutor(repositoryContext);
        this.configurationChangeListener = new SqaleSystemConfigurationListener(repositoryContext);
    }

    // region getObject/getVersion
    @Override
    public @NotNull <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        Objects.requireNonNull(type, "Object type must not be null.");
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        logger.debug("Getting object '{}' with OID '{}': {}",
                type.getSimpleName(), oid, parentResult.getOperation());
        InternalMonitor.recordRepositoryRead(type, oid);

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_GET_OBJECT)
                .addQualifier(type.getSimpleName())
                .setMinor()
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_OID, oid)
                .build();

        PrismObject<T> object = null;
        long opHandle = registerOperationStart(OP_GET_OBJECT, type);

        try (var sqResult = SqlBaseOperationTracker.with(operationResult)) {
            var finalObject = executeRetriable(OP_GET_OBJECT, oidUuid, opHandle,
                    () -> (PrismObject<T>) readByOid(type, oidUuid, options).asPrismObject());
            object = finalObject;
            invokeConflictWatchers((w) -> w.afterGetObject(finalObject));
            return object;
        } catch (ObjectNotFoundException e) {
            throw handleObjectNotFound(e, operationResult, options);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (ObjectAlreadyExistsException | RepositoryException e) {
            throw new SystemException(e);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
            registerOperationFinish(opHandle);
            OperationLogger.logGetObject(type, oid, options, object, operationResult);
        }
    }

    /** Read object with internally created JDBC session/transaction. */
    <T extends ObjectType> T readByOid(
            Class<T> type, UUID oidUuid, Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException, ObjectNotFoundException {
        T object;
        try (JdbcSession jdbcSession =
                sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            object = readByOid(jdbcSession, type, oidUuid, options);
            jdbcSession.commit();
        }
        return object;
    }

    /** Read object using provided {@link JdbcSession} as a part of already running transaction. */

    private static class MappedTuple<S> {
        Tuple tuple;
        S schemaObject;
    }

    private <S extends ObjectType> S readByOid(
            @NotNull JdbcSession jdbcSession,
            @NotNull Class<S> schemaType,
            @NotNull UUID oid,
            Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException {
        SqaleTableMapping<S, QObject<MObject>, MObject> rootMapping =
                sqlRepoContext.getMappingBySchemaType(schemaType);
        return internalReadByOid(jdbcSession, rootMapping, oid, options, false).schemaObject;
    }

    private <S extends ObjectType, Q extends QObject<R>, R extends MObject> MappedTuple<S> internalReadByOid(
            @NotNull JdbcSession jdbcSession,
            SqaleTableMapping<S, Q, R> rootMapping,
            @NotNull UUID oid,
            Collection<SelectorOptions<GetOperationOptions>> options, boolean forUpdate)
            throws SchemaException, ObjectNotFoundException {
        Q root = rootMapping.defaultAlias();

        var expressions = rootMapping.selectExpressions(root, options);
        if (forUpdate) {
            expressions = ObjectArrays.concat(expressions, root.containerIdSeq);
        }
        var query = jdbcSession.newQuery()
                .from(root)
                .select(expressions)
                .where(root.oid.eq(oid));
        if (forUpdate) {
            query.forUpdate();
        }
        Tuple result;
        var opResult = SqlBaseOperationTracker.fetchPrimary();
        try {
            result = query.fetchOne();
        } finally {
            opResult.close();
        }
        // USed to be result.get(root.fullObject) == null (with excludes it is allowed to have fullObject == null
        if (result == null ) {
            throw new ObjectNotFoundException(rootMapping.schemaType(), oid.toString(), isAllowNotFound(options));
        }

        var ret = new MappedTuple<S>();
        ret.tuple = result;
        var queryContext = SqaleQueryContext.from(rootMapping, sqlRepoContext, query, this::readByOid);
        var rowTransformer = rootMapping.createRowTransformer(queryContext, jdbcSession, options);
        rowTransformer.beforeTransformation(List.of(result), root);
        ret.schemaObject = rowTransformer.transform(result, root);
        rowTransformer.finishTransformation();
        return ret;
    }

    @Override
    public <T extends ObjectType> String getVersion(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        UUID uuid = SqaleUtils.oidToUuidMandatory(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        logger.debug("Getting version for {} with oid '{}'.", type.getSimpleName(), oid);

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_GET_VERSION)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_OID, oid)
                .build();

        try {
            return executeGetVersion(type, uuid);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private <T extends ObjectType> String executeGetVersion(Class<T> type, UUID oid)
            throws ObjectNotFoundException {
        long opHandle = registerOperationStart(OP_GET_VERSION, type);
        try {
            return executeRetriable(OP_GET_VERSION, oid, opHandle, () -> {
                try (JdbcSession jdbcSession =
                        sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
                    SqaleTableMapping<T, QObject<MObject>, MObject> rootMapping =
                            sqlRepoContext.getMappingBySchemaType(type);
                    QObject<MObject> root = rootMapping.defaultAlias();

                    Integer version = jdbcSession.newQuery()
                            .select(root.version)
                            .from(root)
                            .where(root.oid.eq(oid))
                            .fetchOne();
                    if (version == null) {
                        throw new ObjectNotFoundException(type, oid.toString(), false);
                    }

                    String versionString = version.toString();
                    invokeConflictWatchers((w) -> w.afterGetVersion(oid.toString(), versionString));
                    return versionString;
                }
            });
        } catch (ObjectAlreadyExistsException | SchemaException | RepositoryException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    private AssertionError shouldNotHappen(Throwable e) {
        return new AssertionError("Should not happen", e);
    }
    // endregion

    // region Add/modify/delete
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

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_ADD_OBJECT)
                .addQualifier(object.asObjectable().getClass().getSimpleName())
                .addParam(OperationResult.PARAM_OBJECT, object)
                .addParam(OperationResult.PARAM_OPTIONS, options.toString())
                .build();

        try {
            PolyString name = object.getName();
            if (name == null || Strings.isNullOrEmpty(name.getOrig())) {
                throw new SchemaException("Attempt to add object without name.");
            }

            //noinspection ConstantConditions
            logger.debug(
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

            return object.getOid() == null || !options.isOverwrite()
                    ? executeAddObject(object)
                    : executeOverwriteObject(object);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
            OperationLogger.logAdd(object, options, operationResult);
        }
    }

    private <T extends ObjectType> String executeAddObject(
            @NotNull PrismObject<T> object)
            throws SchemaException, ObjectAlreadyExistsException {
        long opHandle = registerOperationStart(OP_ADD_OBJECT, object);

        try {
            String oid = executeRetriable(OP_ADD_OBJECT, null, opHandle,
                    () -> new AddObjectContext<>(sqlRepoContext, object).execute());

            invokeConflictWatchers((w) -> w.afterAddObject(oid, object));
            return oid;
        } catch (ObjectNotFoundException | RepositoryException e) {
            // Should not happen
            throw new SystemException(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    /** Overwrite is more like update than add. */
    private <T extends ObjectType> String executeOverwriteObject(
            @NotNull PrismObject<T> newObject)
            throws SchemaException, RepositoryException, ObjectAlreadyExistsException {

        String oid = newObject.getOid();
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);

        long opHandle = registerOperationStart(OP_ADD_OBJECT_OVERWRITE, newObject);

        try {
            executeRetriable(OP_ADD_OBJECT_OVERWRITE, oidUuid, opHandle, () -> {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
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

                        logger.trace("overwriteAddObjectAttempt: originalOid={}, modifications={}",
                                oid, modifications);
                        Collection<? extends ItemDelta<?, ?>> executedModifications =
                                updateContext.execute(modifications, false);
                        replaceObject(updateContext, updateContext.getPrismObject());
                        if (!executedModifications.isEmpty()) {
                            invokeConflictWatchers((w) -> w.afterModifyObject(oid));
                        }
                        logger.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());
                    } catch (ObjectNotFoundException e) {
                        // so it is just plain addObject after all
                        new AddObjectContext<>(sqlRepoContext, newObject)
                                .execute(jdbcSession);
                        invokeConflictWatchers((w) -> w.afterAddObject(oid, newObject));
                    }
                    jdbcSession.commit();
                    return null;
                }
            });
            return oid;
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Should not happen", e);
        } catch (RuntimeException e) {
            SqaleUtils.handlePostgresException(e);
            throw e;
        } finally {
            registerOperationFinish(opHandle);
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
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_MODIFY_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_OID, oid)
                .addParam(OperationResult.PARAM_OPTIONS, String.valueOf(options))
                .addArbitraryObjectCollectionAsParam("modifications", modifications)
                .build();

        try {
            return executeModifyObject(type, oidUuid, modifications, precondition, options, operationResult);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
            OperationLogger.logModify(type, oid, modifications, precondition, options, operationResult);
        }
    }

    @NotNull
    private <T extends ObjectType> ModifyObjectResult<T> executeModifyObject(
            @NotNull Class<T> type,
            @NotNull UUID oidUuid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable ModificationPrecondition<T> precondition,
            @Nullable RepoModifyOptions options,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, PreconditionViolationException, RepositoryException {
        long opHandle = registerOperationStart(OP_MODIFY_OBJECT, type);
        try {
            return executeRetriable(OP_MODIFY_OBJECT, oidUuid, opHandle, () -> {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
                    RootUpdateContext<T, QObject<MObject>, MObject> updateContext =
                            prepareUpdateContext(jdbcSession, type, modifications, oidUuid, options);

                    ModifyObjectResult<T> rv = modifyObjectInternal(
                            updateContext, modifications, precondition, options, parentResult);
                    jdbcSession.commit();
                    return rv;
                } catch (PreconditionViolationException e) {
                    throw new TunnelException(e);
                }
            });
        } catch (TunnelException e) {
            if (e.getCause() instanceof PreconditionViolationException pve) {
                throw pve;
            }
            throw e;
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Should not happen", e);
        } finally {
            registerOperationFinish(opHandle);
        }
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
        Objects.requireNonNull(modificationsSupplier, "Modifications supplier must not be null.");
        Objects.requireNonNull(type, "Object class in delta must not be null.");
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_MODIFY_OBJECT_DYNAMICALLY)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam("getOptions", String.valueOf(getOptions))
                .addParam("modifyOptions", String.valueOf(modifyOptions))
                .addParam(OperationResult.PARAM_OID, oid)
                .build();

        ModifyObjectResult<T> rv = null;
        long opHandle = registerOperationStart(OP_MODIFY_OBJECT_DYNAMICALLY, type);
        try {
            rv = executeRetriable(OP_MODIFY_OBJECT_DYNAMICALLY, oidUuid, opHandle, () -> executeModifyObjectDynamically(
                    type, oidUuid, getOptions, modificationsSupplier, modifyOptions, operationResult));
            return rv;
        } catch (ObjectNotFoundException e) {
            throw handleObjectNotFound(e, operationResult, getOptions);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
            OperationLogger.logModifyDynamically(type, oid, rv, modifyOptions, operationResult);
            registerOperationFinish(opHandle);
        }
    }

    private ObjectNotFoundException handleObjectNotFound(
            ObjectNotFoundException e, OperationResult operationResult,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> getOptions) throws ObjectNotFoundException {
        if (!isAllowNotFound(SelectorOptions.findRootOptions(getOptions))) {
            operationResult.recordFatalError(e);
        } else {
            operationResult.recordHandledError(e);
        }
        return e;
    }

    private <T extends ObjectType> ModifyObjectResult<T> executeModifyObjectDynamically(
            @NotNull Class<T> type,
            UUID oidUuid, @Nullable Collection<SelectorOptions<GetOperationOptions>> getOptions,
            @NotNull ModificationsSupplier<T> modificationsSupplier,
            @Nullable RepoModifyOptions modifyOptions,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, RepositoryException {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            RootUpdateContext<T, QObject<MObject>, MObject> updateContext =
                    prepareUpdateContext(jdbcSession, type, oidUuid, getOptions, modifyOptions);

            PrismObject<T> object = updateContext.getPrismObject().clone();
            Collection<? extends ItemDelta<?, ?>> modifications =
                    modificationsSupplier.get(object.asObjectable());

            ModifyObjectResult<T> rv = modifyObjectInternal(
                    updateContext, modifications, null, modifyOptions, parentResult);
            jdbcSession.commit();
            return rv;
        } catch (PreconditionViolationException e) {
            // no precondition is checked in this scenario, this should not happen
            throw new AssertionError(e);
        }
    }

    private <T extends ObjectType> ModifyObjectResult<T> modifyObjectInternal(
            @NotNull RootUpdateContext<T, QObject<MObject>, MObject> updateContext,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable ModificationPrecondition<T> precondition,
            @Nullable RepoModifyOptions options,
            @NotNull OperationResult operationResult)
            throws SchemaException, PreconditionViolationException, RepositoryException {
        try (var sqaleResult = SqlBaseOperationTracker.with(operationResult)) {

            if (options == null) {
                options = new RepoModifyOptions();
            }

            PrismObject<T> prismObject = updateContext.getPrismObject();
            //noinspection ConstantConditions
            logger.debug("Modify object type '{}', oid={}, reindex={}",
                    prismObject.getCompileTimeClass().getSimpleName(),
                    prismObject.getOid(),
                    options.isForceReindex());

            if (modifications.isEmpty() && !RepoModifyOptions.isForceReindex(options)) {
                logger.debug("Modification list is empty, nothing was modified.");
                operationResult.recordStatus(OperationResultStatus.SUCCESS,
                        "Modification list is empty, nothing was modified.");
                return new ModifyObjectResult<>(modifications);
            }

            checkModifications(modifications);
            logTraceModifications(modifications);

            if (precondition != null && !precondition.holds(prismObject)) {
                // will be rolled back automatically
                throw new PreconditionViolationException(
                        "Modification precondition does not hold for " + prismObject);
            }
            invokeConflictWatchers(w -> w.beforeModifyObject(prismObject));
            PrismObject<T> originalObject = prismObject.clone(); // for result later

            // Use reindex instead of modify if reindex is required by user, or repository
            // itself detected need for reindex during preparation read for modify.
            boolean reindex = updateContext.reindexNeeded() || options.isForceReindex();
            // Only reindex if mapping supports reindex (most mappings, except simulation result)
            reindex = reindex && updateContext.mapping().isReindexSupported();
            // FIXME: If reindex needed is detected and this is partial update (without full object)


            // object needs to be reloaded.

            if (reindex) {
                // UpdateTables is false, we want only to process modifications on fullObject
                // do not modify nested items.
                modifications = updateContext.execute(modifications, false);
                replaceObject(updateContext, updateContext.getPrismObject());
            } else {
                modifications = updateContext.execute(modifications);
            }
            logger.trace("OBJECT after:\n{}", prismObject.debugDumpLazily());

            if (!modifications.isEmpty()) {
                invokeConflictWatchers((w) -> w.afterModifyObject(prismObject.getOid()));
            }
            return new ModifyObjectResult<>(originalObject, prismObject, modifications);
        }
    }

    private <T extends ObjectType> void replaceObject(
            @NotNull RootUpdateContext<?, QObject<MObject>, MObject> updateContext,
            PrismObject<T> newObject)
            throws RepositoryException {
        // We delete original object and cascade of referenced tables, this will also
        // remove additional rows, which may not be present in full object
        // after desync
        updateContext.jdbcSession().newDelete(updateContext.entityPath())
                .where(updateContext.entityPath().oid.eq(updateContext.objectOid()))
                .execute();
        try {
            // We add object again, this will ensure recreation of all indices and correct
            // table rows again
            new AddObjectContext<>(sqlRepoContext, newObject)
                    .executeReindexed(updateContext.jdbcSession());
        } catch (SchemaException | ObjectAlreadyExistsException e) {
            throw new RepositoryException("Update with reindex failed", e);
        }

    }

    private <S extends ObjectType, Q extends QObject<R>, R extends MObject>
    RootUpdateContext<S, Q, R> prepareUpdateContext(
            @NotNull JdbcSession jdbcSession,
            @NotNull Class<S> schemaType,
            @NotNull UUID oid)
            throws SchemaException, ObjectNotFoundException {
        return prepareUpdateContext(jdbcSession, schemaType, Collections.emptyList(), oid, null);
    }

    /**
     * Read object for update and returns update context that contains it.
     **/
    private <S extends ObjectType, Q extends QObject<R>, R extends MObject>
    RootUpdateContext<S, Q, R> prepareUpdateContext(
            @NotNull JdbcSession jdbcSession,
            @NotNull Class<S> schemaType,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull UUID oid,
            @Nullable RepoModifyOptions options)
            throws SchemaException, ObjectNotFoundException {

        QueryTableMapping<S, FlexibleRelationalPathBase<Object>, Object> rootMapping =
                sqlRepoContext.getMappingBySchemaType(schemaType);
        Collection<SelectorOptions<GetOperationOptions>> getOptions =
                rootMapping.updateGetOptions(
                        RepoModifyOptions.isForceReindex(options) ? GET_FOR_REINDEX_OPTIONS : GET_FOR_UPDATE_OPTIONS,
                        modifications);

        return prepareUpdateContext(jdbcSession, schemaType, oid, getOptions, options);
    }

    /** Read object for update and returns update context that contains it with specific get options. */
    private <S extends ObjectType, Q extends QObject<R>, R extends MObject>
    RootUpdateContext<S, Q, R> prepareUpdateContext(
            @NotNull JdbcSession jdbcSession,
            @NotNull Class<S> schemaType,
            @NotNull UUID oid,
            Collection<SelectorOptions<GetOperationOptions>> getOptions,
            RepoModifyOptions options)
            throws SchemaException, ObjectNotFoundException {

        SqaleTableMapping<S, QObject<R>, R> rootMapping =
                sqlRepoContext.getMappingBySchemaType(schemaType);
        QObject<R> entityPath = rootMapping.defaultAlias();

        Path<?>[] selectExpressions = ObjectArrays.concat(
                rootMapping.selectExpressions(entityPath, getOptions),
                entityPath.containerIdSeq);

        MappedTuple<S> mapped = internalReadByOid(jdbcSession, rootMapping, oid, getOptions, true);
        R rootRow = rootMapping.newRowObject();
        rootRow.oid = oid;
        rootRow.containerIdSeq = mapped.tuple.get(entityPath.containerIdSeq);
        // This column is generated, some sub-entities need it, but we can't push it to DB.
        rootRow.objectType = MObjectType.fromSchemaType(mapped.schemaObject.getClass());
        // we don't care about full object in row

        var skipFullObject = SqaleUtils.isWithoutFullObject(mapped.schemaObject);
        return new RootUpdateContext<>(sqlRepoContext, jdbcSession, mapped.schemaObject, rootRow, skipFullObject);
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

        // additional check to prevent storing invalid reference OIDs
        modifications.forEach(delta -> {
            delta.accept(visitable -> {
                if (visitable instanceof PrismReferenceValue prv) {
                    String oid = prv.getOid();
                    if (oid != null) {
                        try {
                             UUID.fromString(oid);
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("Cannot convert OID '" + oid + "' to UUID", e);
                        }
                    }
                }
            }, false);
        });
    }

    private void logTraceModifications(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        if (logger.isTraceEnabled()) {
            for (ItemDelta<?, ?> modification : modifications) {
                if (modification instanceof PropertyDelta<?>) {
                    PropertyDelta<?> propDelta = (PropertyDelta<?>) modification;
                    if (propDelta.getPath().equivalent(ObjectType.F_NAME)) {
                        Collection<PrismPropertyValue<PolyString>> values = propDelta.getValues(PolyString.class);
                        for (PrismPropertyValue<PolyString> pval : values) {
                            PolyString value = pval.getValue();
                            logger.trace("NAME delta: {} - {}", value.getOrig(), value.getNorm());
                        }
                    }
                }
            }
        }
    }

    @Override
    public ModifyObjectResult<SimulationResultType> deleteSimulatedProcessedObjects(String oid,
            @Nullable String transactionId, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
        if (transactionId == null) {
            // Transaction ID is null, we can delegate to normal modifyObjectOperation, which takes care
            // of partition drops if necessary

            var modifications = PrismContext.get().deltaFor(SimulationResultType.class)
                    .item(SimulationResultType.F_PROCESSED_OBJECT)
                    .replace()
                    .asItemDeltas();
            try {
                return modifyObject(SimulationResultType.class, oid, modifications, parentResult);
            } catch (ObjectAlreadyExistsException e) {
                throw new SystemException(e);
            }
        }

        var operationResult = parentResult.createSubresult("deleteSimulatedProcessedObjects");
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            RootUpdateContext<SimulationResultType, QObject<MObject>, MObject> update = prepareUpdateContext(jdbcSession, SimulationResultType.class, SqaleUtils.oidToUuidMandatory(oid));

            QProcessedObject alias = QProcessedObjectMapping.getProcessedObjectMapping().defaultAlias();

            // transactionId is not null, delete only ones in particular transaction
            var predicate = alias.ownerOid.eq(SqaleUtils.oidToUuidMandatory(oid))
                    .and(alias.transactionId.eq(transactionId));

            jdbcSession.newDelete(alias).where(predicate).execute();
            update.finishExecutionOwn();
            jdbcSession.commit();
            return new ModifyObjectResult<>(update.getPrismObject(), update.getPrismObject(), Collections.emptyList());
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    @Override
    public @NotNull <T extends ObjectType> DeleteObjectResult deleteObject(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException {

        Validate.notNull(type, "Object type must not be null.");
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        logger.debug("Deleting object type '{}' with oid '{}'", type.getSimpleName(), oid);

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_DELETE_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_OID, oid)
                .build();
        long opHandle = registerOperationStart(OP_DELETE_OBJECT, type);

        try {
            return executeRetriable(OP_DELETE_OBJECT, oidUuid, opHandle, () -> executeDeleteObject(type, oid, oidUuid));
        } catch (SchemaException | ObjectAlreadyExistsException | RepositoryException e) {
            throw new SystemException("Should not happen", e);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
            registerOperationFinish(opHandle);
        }
    }

    @NotNull
    private <T extends ObjectType> DeleteObjectResult executeDeleteObject(
            Class<T> type, String oid, UUID oidUuid) throws ObjectNotFoundException {

        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            DeleteObjectResult result = deleteObjectAttempt(type, oidUuid, jdbcSession);
            invokeConflictWatchers((w) -> w.afterDeleteObject(oid));

            jdbcSession.commit();
            return result;
        }
    }

    private <T extends ObjectType, Q extends QObject<R>, R extends MObject>
    DeleteObjectResult deleteObjectAttempt(Class<T> type, UUID oid, JdbcSession jdbcSession)
            throws ObjectNotFoundException {

        QueryTableMapping<T, Q, R> mapping = sqlRepoContext.getMappingBySchemaType(type);
        QObject<?> entityPath = mapping.defaultAlias();
        Tuple result = jdbcSession.newQuery()
                .select(entityPath.objectType, entityPath.fullObject)
                .forUpdate()
                .from(entityPath)
                .where(entityPath.oid.eq(oid))
                .fetchOne();
        if (result == null) {
            throw new ObjectNotFoundException(type, oid.toString(), false);
        }
        var fullObject = result.get(entityPath.fullObject);

        if (ObjectType.class.equals(type)) {
            mapping = (QueryTableMapping) sqlRepoContext.getMappingBySchemaType(result.get(entityPath.objectType).getSchemaType());
            entityPath = mapping.defaultAlias();
        }
        // object delete cascades to all owned related rows
        jdbcSession.newDelete(entityPath)
                .where(entityPath.oid.eq(oid))
                .execute();

        return new DeleteObjectResult(new String(fullObject, StandardCharsets.UTF_8));
    }
    // endregion

    // region Counting/searching
    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        Objects.requireNonNull(type, "Object type must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_COUNT_OBJECTS)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_QUERY, query)
                .build();

        try {
            logSearchInputParameters(type, query, "Count objects");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return 0;
            }

            return executeCountObjects(type, query, options);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private <T extends ObjectType> int executeCountObjects(
            @NotNull Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws RepositoryException {

        long opHandle = registerOperationStart(OP_COUNT_OBJECTS, type);
        try {
            return executeRetriable(OP_COUNT_OBJECTS, null, opHandle, () -> sqlQueryExecutor.count(
                    SqaleQueryContext.from(type, sqlRepoContext),
                    query, options));
        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
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

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_OBJECTS)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_QUERY, query)
                .addParam(OperationResult.PARAM_OPTIONS, String.valueOf(options))
                .build();

        try (var sqaleResult = SqlBaseOperationTracker.with(operationResult)) {
            logSearchInputParameters(type, query, "Search objects");

            var finalQuery = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(finalQuery)) {
                return new SearchResultList<>();
            }
            return executeSearchObjects(type, finalQuery, options, OP_SEARCH_OBJECTS);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();

        }
    }

    private <T extends ObjectType> SearchResultList<PrismObject<T>> executeSearchObjects(
            @NotNull Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            String operationKind)
            throws RepositoryException, SchemaException {
        long opHandle = registerOperationStart(operationKind, type);
        try {
            SearchResultList<T> result = executeRetriable(operationKind, null, opHandle, () -> sqlQueryExecutor.list(
                    SqaleQueryContext.from(type, sqlRepoContext),
                    query,
                    options));
            //noinspection unchecked
            return result.map(
                    o -> (PrismObject<T>) o.asPrismObject());
        } catch (ObjectNotFoundException | ObjectAlreadyExistsException e) {
            throw new SystemException("Should not happen", e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
            OperationResult parentResult) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_OBJECTS_ITERATIVE)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_QUERY, query)
                .build();

        try (var sqaleResult = SqlBaseOperationTracker.with(operationResult)) {
            logSearchInputParameters(type, query, "Iterative search objects");

            var finalQuery = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(finalQuery)) {
                return new SearchResultMetadata().approxNumberOfAllResults(0);
            }

            return executeSearchObjectsIterative(type, query, handler, options, operationResult);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private static final ItemPath OID_PATH = PrismConstants.T_ID;
    private static final ItemPath CONTAINER_ID_PATH = PrismConstants.T_ID;

    private static final ItemPath OWNER_OID_PATH = ItemPath.create(PrismConstants.T_PARENT, PrismConstants.T_ID);

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
            Integer offset = originalPaging != null ? originalPaging.getOffset() : null;

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
            // We want to order OID in the same direction as the provided ordering.
            // This is also reflected by GT/LT conditions in lastOidCondition() method.
            paging.addOrderingInstruction(OID_PATH,
                    providedOrdering != null && providedOrdering.size() == 1
                            && providedOrdering.get(0).getDirection() == OrderDirection.DESCENDING
                            ? OrderDirection.DESCENDING : OrderDirection.ASCENDING);
            pagedQuery.setPaging(paging);

            int pageSize = Math.min(
                    getIterationPageSize(options),
                    defaultIfNull(maxSize, Integer.MAX_VALUE));
            pagedQuery.getPaging().setMaxSize(pageSize);
            pagedQuery.getPaging().setOffset(offset);

            PrismObject<T> lastProcessedObject = null;
            int handledObjectsTotal = 0;

            while (true) {
                if (maxSize != null && maxSize - handledObjectsTotal < pageSize) {
                    // relevant only for the last page
                    pagedQuery.getPaging().setMaxSize(maxSize - handledObjectsTotal);
                }

                // null safe, even for both nulls - don't use filterAnd which mutates original AND filter
                pagedQuery.setFilter(ObjectQueryUtil.filterAndImmutable(
                        originalQuery != null ? originalQuery.getFilter() : null,
                        lastOidCondition(lastProcessedObject, providedOrdering)));

                // we don't call public searchObject to avoid subresults and query simplification
                logSearchInputParameters(type, pagedQuery, "Search object iterative page");

                // Should we do retries here?
                List<PrismObject<T>> objects = executeSearchObjects(
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
                pagedQuery.getPaging().setOffset(null);
            }
        } finally {
            // This just counts the operation and adds zero/minimal time not to confuse user
            // with what could be possibly very long duration.
            long opHandle = registerOperationStart(OP_SEARCH_OBJECTS_ITERATIVE, type);
            registerOperationFinish(opHandle);
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
            boolean asc = objectOrdering.getDirection() != OrderDirection.DESCENDING; // null => asc
            S_ConditionEntry filter = prismContext()
                    .queryFor(lastProcessedObject.getCompileTimeClass())
                    .item(orderByPath);
            Item<PrismValue, ItemDefinition<?>> item = lastProcessedObject.findItem(orderByPath);
            if (item.size() > 1) {
                throw new IllegalArgumentException(
                        "Multi-value property for ordering is forbidden - item: " + item);
            } else if (item.isEmpty()) {
                // TODO what if it's nullable? is it null-first or last?
                // See: https://www.postgresql.org/docs/13/queries-order.html
                // "By default, null values sort as if larger than any non-null value; that is,
                // NULLS FIRST is the default for DESC order, and NULLS LAST otherwise."
            } else {
                /*
                IMPL NOTE: Compare this code with SqaleAuditService.iterativeSearchCondition, there is a couple of differences.
                This one seems bloated, but each branch is simple; on the other hand it's not obvious what is different in each.
                Also, audit version does not require polystring treatment.
                Finally, this works for a single provided ordering, but not for multiple (unsupported commented code lower).
                 */
                boolean isPolyString = QNameUtil.match(
                        PolyStringType.COMPLEX_TYPE, item.getDefinition().getTypeName());
                Object realValue = item.getRealValue();
                if (isPolyString) {
                    // We need to use matchingOrig for polystring, see MID-7860
                    if (asc) {
                        return filter.gt(realValue).matchingOrig().or()
                                .block()
                                .item(orderByPath).eq(realValue).matchingOrig()
                                .and()
                                .item(OID_PATH).gt(lastProcessedOid)
                                .endBlock()
                                .buildFilter();
                    } else {
                        return filter.lt(realValue).matchingOrig().or()
                                .block()
                                .item(orderByPath).eq(realValue).matchingOrig()
                                .and()
                                .item(OID_PATH).lt(lastProcessedOid)
                                .endBlock()
                                .buildFilter();
                    }
                } else {
                    if (asc) {
                        return filter.gt(realValue).or()
                                .block()
                                .item(orderByPath).eq(realValue)
                                .and()
                                .item(OID_PATH).gt(lastProcessedOid)
                                .endBlock()
                                .buildFilter();
                    } else {
                        return filter.lt(realValue).or()
                                .block()
                                .item(orderByPath).eq(realValue)
                                .and()
                                .item(OID_PATH).lt(lastProcessedOid)
                                .endBlock()
                                .buildFilter();
                    }
                }
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
                parentResult.subresult(opNamePrefix + OP_COUNT_CONTAINERS)
                        .addQualifier(type.getSimpleName())
                        .addParam(OperationResult.PARAM_TYPE, type.getName())
                        .addParam(OperationResult.PARAM_QUERY, query)
                        .build();
        try {
            logSearchInputParameters(type, query, "Count containers");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return 0;
            }

            return executeCountContainers(type, query, options);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private <T extends Containerable> int executeCountContainers(
            @NotNull Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws RepositoryException {

        long opHandle = registerOperationStart(OP_COUNT_CONTAINERS, type);
        try {
            return executeRetriable(OP_COUNT_CONTAINERS, null, opHandle,
                    () -> sqlQueryExecutor.count(SqaleQueryContext.from(type, sqlRepoContext), query, options));
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public @NotNull <T extends Containerable> SearchResultList<T> searchContainers(
            @NotNull Class<T> type, @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult)
            throws SchemaException {

        Objects.requireNonNull(type, "Container type must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_CONTAINERS)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_QUERY, query)
                .build();

        try {
            logSearchInputParameters(type, query, "Search containers");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return new SearchResultList<>();
            }

            return executeSearchContainers(type, query, options, OP_SEARCH_CONTAINERS);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private @NotNull <T extends Containerable> SearchResultList<T> executeSearchContainers(
            Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, String opName)
            throws RepositoryException, SchemaException {
        long opHandle = registerOperationStart(opName, type);
        try {
            return executeRetriable(opName, null, opHandle, () -> {
                SqaleQueryContext<T, FlexibleRelationalPathBase<Object>, Object> queryContext =
                        SqaleQueryContext.from(type, sqlRepoContext, this::readByOid);
                return sqlQueryExecutor.list(queryContext, query, options);
            });
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public int countReferences(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        Objects.requireNonNull(query, "Query must be provided for reference search");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_COUNT_REFERENCES)
                .addParam(OperationResult.PARAM_QUERY, query)
                .build();

        try {
            logSearchInputParameters(ObjectReferenceType.class, query, "Count references");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return 0;
            }

            return executeCountReferences(query, options);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private int executeCountReferences(
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options)
            throws RepositoryException {
        long opHandle = registerOperationStart(OP_COUNT_REFERENCES, ObjectReferenceType.class);
        try {
            QReferenceMapping<?, ?, ?, ?> refMapping = determineMapping(query.getFilter());
            SqaleQueryContext<ObjectReferenceType, ?, ?> queryContext =
                    SqaleQueryContext.from(
                            refMapping, sqlRepoContext, sqlRepoContext.newQuery(), null);
            return sqlQueryExecutor.count(queryContext, query, options);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public @NotNull SearchResultList<ObjectReferenceType> searchReferences(
            @NotNull ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException {
        Objects.requireNonNull(query, "Query must be provided for reference search");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_REFERENCES)
                .addParam(OperationResult.PARAM_QUERY, query)
                .build();

        try {
            logSearchInputParameters(ObjectReferenceType.class, query, "Search references");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return new SearchResultList<>();
            }

            return executeSearchReferences(query, options, OP_SEARCH_REFERENCES);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    SearchResultList<ObjectReferenceType> executeSearchReferences(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            String operationKind)
            throws SchemaException, RepositoryException {
        long opHandle = registerOperationStart(operationKind, ObjectReferenceType.class);
        QReferenceMapping<?, ?, ?, ?> refMapping = determineMapping(query.getFilter());

        try {
            return executeRetriable(operationKind, null, opHandle, () -> {
                SqaleQueryContext<ObjectReferenceType, ?, ?> queryContext =
                        SqaleQueryContext.from(
                                refMapping, sqlRepoContext, sqlRepoContext.newQuery(), null);

                return sqlQueryExecutor.list(queryContext, query, options);
            });
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @NotNull
    private QReferenceMapping<?, ?, ?, ?> determineMapping(ObjectFilter filter) throws QueryException {
        OwnedByFilter ownedByFilter = extractOwnedByFilterForReferenceSearch(filter);

        ComplexTypeDefinition type = ownedByFilter.getType();
        ItemPath path = ownedByFilter.getPath();
        QReferenceMapping<?, ?, ?, ?> refMapping =
                QReferenceMapping.getByOwnerTypeAndPath(type.getCompileTimeClass(), path);

        if (refMapping == null) {
            throw new QueryException(
                    "Reference search is not supported for " + type + " and item path " + path);
        }
        return refMapping;
    }

    private OwnedByFilter extractOwnedByFilterForReferenceSearch(ObjectFilter filter)
            throws QueryException {
        if (filter instanceof OwnedByFilter) {
            return (OwnedByFilter) filter;
        } else if (filter instanceof AndFilter) {
            OwnedByFilter ownedByFilter = null;
            for (ObjectFilter condition : ((AndFilter) filter).getConditions()) {
                if (condition instanceof OwnedByFilter) {
                    if (ownedByFilter != null) {
                        throw new QueryException("Exactly one main OWNED-BY filter must be used"
                                + " for reference search, but multiple found. Filter: " + filter);
                    }
                    ownedByFilter = (OwnedByFilter) condition;
                }
            }
            if (ownedByFilter == null) {
                throw new QueryException("Exactly one main OWNED-BY filter must be used"
                        + " for reference search, but none found. Filter: " + filter);
            }
            return ownedByFilter;
        } else {
            throw new QueryException("Invalid filter for reference search: " + filter
                    + "\nReference search filter should be OWNED-BY filter or an AND filter containing it.");
        }
    }

    @Override
    public SearchResultMetadata searchReferencesIterative(
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<ObjectReferenceType> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
            throws SchemaException {
        Objects.requireNonNull(query, "Query must be provided for reference search");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");
        Objects.requireNonNull(handler, "Result handler must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_REFERENCES_ITERATIVE)
                .addParam(OperationResult.PARAM_QUERY, query)
                .build();

        try {
            logSearchInputParameters(ObjectReferenceType.class, query, "Search references");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return new SearchResultMetadata().approxNumberOfAllResults(0);
            }
            // Here only for checks, to make it throw sooner than inside per-page calls.
            determineMapping(query.getFilter());

            return new ReferenceIterativeSearch(this)
                    .execute(query, handler, options, operationResult);
        } catch (ObjectNotFoundException | RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    @Override
    public <T extends Containerable> SearchResultMetadata searchContainersIterative(
            @NotNull Class<T> type,
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<T> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException {

        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");
        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_CONTAINERS_ITERATIVE)
                .addQualifier(type.getSimpleName())
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_QUERY, query)
                .build();

        try {
            logSearchInputParameters(type, query, "Iterative search objects");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return new SearchResultMetadata().approxNumberOfAllResults(0);
            }

            return executeSearchContainersIterative(type, query, handler, options, operationResult);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private <T extends Containerable> SearchResultMetadata executeSearchContainersIterative(
            Class<T> type,
            ObjectQuery originalQuery,
            ObjectHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult operationResult) throws SchemaException, RepositoryException {

        try {
            ObjectPaging originalPaging = originalQuery != null ? originalQuery.getPaging() : null;
            // this is total requested size of the search
            Integer maxSize = originalPaging != null ? originalPaging.getMaxSize() : null;
            Integer offset = originalPaging != null ? originalPaging.getOffset() : null;

            List<? extends ObjectOrdering> originalOrdering = originalPaging != null
                    ? originalPaging.getOrderingInstructions()
                    : null;
            if (originalOrdering != null && originalOrdering.size() > 1) {
                throw new RepositoryException("searchObjectsIterative() does not support ordering"
                        + " by multiple paths (yet): " + originalOrdering);
            }

            // Single value tested previously
            var providedOrdering = (originalOrdering == null || originalOrdering.isEmpty()) ? null : originalOrdering.get(0);

            ObjectQuery pagedQuery = prismContext().queryFactory().createQuery();
            ObjectPaging paging = prismContext().queryFactory().createPaging();
            if (originalPaging != null && originalPaging.getOrderingInstructions() != null) {
                originalPaging.getOrderingInstructions().forEach(o ->
                        paging.addOrderingInstruction(o.getOrderBy(), o.getDirection()));
            }

            var direction = (providedOrdering != null && providedOrdering.getDirection() == OrderDirection.DESCENDING) ? OrderDirection.DESCENDING : OrderDirection.ASCENDING;

            // Ordering first by owner oid, then by container oid in ordering direction based on only orderBy statements
            var mapping = (QContainerMapping) sqlRepoContext.getMappingBySchemaType(type);
            var depth = mapping.containerDepth();

            // Order first by topmost object
            // then by containers
            for (int i = 0; i <= depth; i++) {
                paging.addOrderingInstruction(createParentPath(depth - i).append(PrismConstants.T_ID), direction);
            }

            pagedQuery.setPaging(paging);

            int pageSize = Math.min(getIterationPageSize(options), defaultIfNull(maxSize, Integer.MAX_VALUE));
            pagedQuery.getPaging().setMaxSize(pageSize);
            pagedQuery.getPaging().setOffset(offset);

            T lastProcessedObject = null;
            int handledObjectsTotal = 0;

            while (true) {
                if (maxSize != null && maxSize - handledObjectsTotal < pageSize) {
                    // relevant only for the last page
                    pagedQuery.getPaging().setMaxSize(maxSize - handledObjectsTotal);
                }

                // null safe, even for both nulls - don't use filterAnd which mutates original AND filter
                pagedQuery.setFilter(ObjectQueryUtil.filterAndImmutable(
                        originalQuery != null ? originalQuery.getFilter() : null,
                        lastContainerCondition(type, lastProcessedObject, providedOrdering, direction)));

                // we don't call public searchObject to avoid subresults and query simplification
                logSearchInputParameters(type, pagedQuery, "Search object iterative page");
                List<T> objects = executeSearchContainers(
                        type, pagedQuery, options, OP_SEARCH_CONTAINERS_ITERATIVE_PAGE);

                // process page results
                for (T object : objects) {
                    lastProcessedObject = object;
                    if (!handler.handle(object, operationResult)) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal + 1)
                                .pagingCookie(pagingCookie(lastProcessedObject))
                                .partialResults(true);
                    }
                    handledObjectsTotal += 1;

                    if (maxSize != null && handledObjectsTotal >= maxSize) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal)
                                .pagingCookie(pagingCookie(lastProcessedObject));
                    }
                }

                if (objects.isEmpty() || objects.size() < pageSize) {
                    return new SearchResultMetadata()
                            .approxNumberOfAllResults(handledObjectsTotal)
                            .pagingCookie(lastProcessedObject != null
                                    ? pagingCookie(lastProcessedObject) : null);
                }
                pagedQuery.getPaging().setOffset(null);
            }
        } finally {
            // This just counts the operation and adds zero/minimal time not to confuse user
            // with what could be possibly very long duration.
            long opHandle = registerOperationStart(OP_SEARCH_CONTAINERS_ITERATIVE, type);
            registerOperationFinish(opHandle);
        }
    }

    public Integer getIterationPageSize(Collection<SelectorOptions<GetOperationOptions>> options) {
        if (options != null) {
            for (var option : options) {
                if (option.isRoot() && option.getOptions() != null) {
                    if (option.getOptions().getIterationPageSize() != null) {
                        return option.getOptions().getIterationPageSize();
                    }
                }
            }
        }
        return repositoryConfiguration().getIterativeSearchByPagingBatchSize();
    }

    /**
     * So continuation filter for depth 1: should be like:
     * (orderingValue > $last/orderingValue)
     * or (
     * (orderingValue = $lastOrderingValue)
     * and (
     * (ownerOid > $last/ownerOid) or (ownerOid = $last/ownerOid and id > $last/id)
     * )
     * )
     * If we are ordering by value, we need to search larger values, but they may be others with same
     *
     * Filter for depth 2:
     *
     * (orderingValue > $last/orderingValue)
     * or (
     * (orderingValue = $lastOrderingValue)
     * and (
     * (ownerOid > $last/ownerOid)
     * or (ownerOid = $last/ownerOid and ../id > $last/../id)
     * or (ownerOid = $last/ownerOid and  ../id = $last/../id and id > $last/id
     *
     * )
     * )
     **/
    private <T extends Containerable> ObjectFilter lastContainerCondition(Class<T> type, T lastProcessedObject, ObjectOrdering providedOrdering, OrderDirection direction) {

        // queryFor
        var mapping = (QContainerMapping) sqlRepoContext.getMappingBySchemaType(type);
        ObjectFilter afterLastSeenContainer = null;
        if (lastProcessedObject != null) {
            var containerIdPath = fullIdPath(lastProcessedObject);

            // This filter should match containers, which are ordered after last seen container
            // We need to match any objects which follow last seen object and any containers in last seen objects
            // which follows last seen container
            afterLastSeenContainer = filterContainersAfter(direction, mapping.containerDepth(), containerIdPath);
        }
        if (providedOrdering != null && lastProcessedObject != null) {
            // TODO: we should get last value of ordering (and this could be funky if ordering is based on dereferencing)
            // at least internally we should return not just found object, but all the values, which were used for ordering
            // so we can reuse them in follow-up filters.
            //
            // Dereferencing allows for these values to be outside of searched object.
            // So currently we support only nested ordering path
            ObjectOrdering objectOrdering = providedOrdering;
            ItemPath orderByPath = objectOrdering.getOrderBy();
            boolean asc = objectOrdering.getDirection() != OrderDirection.DESCENDING; // null => asc
            S_ConditionEntry filter = prismContext()
                    .queryFor(lastProcessedObject.getClass())
                    .item(orderByPath);
            //noinspection unchecked
            Item<PrismValue, ItemDefinition<?>> item = lastProcessedObject.asPrismContainerValue().findItem(orderByPath);
            // Unify somehow with

            Object lastRealValue = null;
            if (item == null && isNameDereferencing(orderByPath)) {
                // Item was not found, can be dereferencing of name?
                var refItem = lastProcessedObject.asPrismContainerValue().findItem(tillDereferencing(orderByPath));
                if (refItem instanceof PrismReference ref) {
                    lastRealValue = ref.getValue().getTargetName();
                } else {
                    throw new IllegalStateException("Something went wrong");
                }


            } else if (item.size() > 1) {
                throw new IllegalArgumentException(
                        "Multi-value property for ordering is forbidden - item: " + item);
            } else if (item.isEmpty()) {
                // TODO what if it's nullable? is it null-first or last?
                // See: https://www.postgresql.org/docs/13/queries-order.html
                // "By default, null values sort as if larger than any non-null value; that is,
                // NULLS FIRST is the default for DESC order, and NULLS LAST otherwise."
                lastRealValue = null;
            } else {
                lastRealValue = item.getRealValue();
            }
            if (lastRealValue != null) {
                /*
                IMPL NOTE: Compare this code with SqaleAuditService.iterativeSearchCondition, there is a couple of differences.
                This one seems bloated, but each branch is simple; on the other hand it's not obvious what is different in each.
                Also, audit version does not require polystring treatment.
                Finally, this works for a single provided ordering, but not for multiple (unsupported commented code lower).
                 */
                boolean isPolyString = lastRealValue instanceof PolyString;
                Object realValue = lastRealValue;
                if (isPolyString) {
                    // We need to use matchingOrig for polystring, see MID-7860
                    if (asc) {
                        return filter.gt(realValue).matchingOrig().or()
                                .block()
                                .item(orderByPath).eq(realValue).matchingOrig()
                                .and()
                                .filter(afterLastSeenContainer)
                                .endBlock()
                                .buildFilter();
                    } else {
                        return filter.lt(realValue).matchingOrig().or()
                                .block()
                                .item(orderByPath).eq(realValue).matchingOrig()
                                .and()
                                .filter(afterLastSeenContainer)
                                .endBlock()
                                .buildFilter();
                    }
                } else {
                    if (asc) {
                        return filter.gt(realValue).or()
                                .block()
                                .item(orderByPath).eq(realValue)
                                .and()
                                .filter(afterLastSeenContainer)
                                .endBlock()
                                .buildFilter();
                    } else {
                        return filter.lt(realValue).or()
                                .block()
                                .item(orderByPath).eq(realValue)
                                .and()
                                .filter(afterLastSeenContainer)
                                .endBlock()
                                .buildFilter();
                    }
                }
            }
            throw new IllegalStateException("Unsupported combination of ordering");
        }

        return afterLastSeenContainer;
    }

    private boolean isNameDereferencing(ItemPath orderByPath) {
        if (!QNameUtil.match(ObjectType.F_NAME,orderByPath.lastName())) {
            return false;
        }
        ItemPath upToDereference = tillDereferencing(orderByPath);
        return upToDereference.size() + 2 == orderByPath.size();
    }

    private ItemPath tillDereferencing(ItemPath orderByPath) {
        var components = new ArrayList<>();
        for (var seg : orderByPath.getSegments()) {
            if (ItemPath.isObjectReference(seg)) {
                break;
            }
            components.add(seg);
        }
        return ItemPath.create(components);
    }

    private <T extends Containerable> String ownerOid(T lastProcessedObject) {
        return (String) lastProcessedObject.asPrismContainerValue().getUserData(SqaleUtils.OWNER_OID);
    }

    private <T extends Containerable> List<Object> fullIdPath(T lastProcessedObject) {
        return (List<Object>) lastProcessedObject.asPrismContainerValue().getUserData(SqaleUtils.FULL_ID_PATH);
    }

    /**
     * Creates filter, which selects containers after last seen based on ordering
     *
     * Generates composite filter in form of
     *
     * @param direction
     * @param depth How deep container is inside multi-value containers (usually 1, sometimes more)
     * @param containerIds List of OID and all container identifier of previous object
     * @return
     */
    private ObjectFilter filterContainersAfter(OrderDirection direction, int depth, List<Object> containerIds) {
        List<ItemPath> paths = new ArrayList<>(depth);
        // We want to start with oid, then continue to nested container
        // Prepares list of paths from top level ID to deepest ID
        // eg. for depth 2: ../../id (oid), ../id, id

        for (int i = 0; i <= depth; i++) {
            paths.add(createParentPath(depth - i).append(PrismConstants.T_ID));
        }
        var queryFactory = PrismContext.get().queryFactory();
        List<ObjectFilter> filters = new ArrayList<>();

        // Creates set of filters, where parents are equals / deepest conditions are compared
        // For depth 1:
        // (parent/id > $last/parent/id) or  (parent/id = $last/parent/id and id > $last/id)
        // For depth 2:
        // (parent/parent/id > $last/parent/parent//id) or  (parent/parent/id = $last/parent/parent/id and parent/id > $last/parent/id)
        for (int i = 0; i <= depth; i++) {
            List<ObjectFilter> conditions = new ArrayList<>();
            var lastPath = paths.get(i);
            var lastId = containerIds.get(i);
            // Equal values
            for (int j = 0; j < i; j++) {
                var parentPath = paths.get(j);
                var id = containerIds.get(j);
                conditions.add(queryFactory.createEqual(parentPath, null, null, id));
            }

            ObjectFilter lastFilter;
            if (OrderDirection.DESCENDING.equals(direction)) {
                lastFilter = queryFactory.createLess(lastPath, null, null, lastId, false);
            } else {
                lastFilter = queryFactory.createGreater(lastPath, null, null, lastId, false);
            }
            conditions.add(lastFilter);
            if (conditions.size() > 1) {
                filters.add(queryFactory.createAnd(conditions));
            } else {
                // For single item we do not need to wrap it in AND filter
                filters.add(lastFilter);
            }
        }
        return queryFactory.createOr(filters);
    }

    ItemPath createParentPath(int depth) {
        var path = ItemPath.create();
        for (int i = 0; i < depth; i++) {
            path = path.append(PrismConstants.T_PARENT);
        }
        return path;
    }

    private String pagingCookie(Containerable t) {
        return ownerOid(t) + "." + t.asPrismContainerValue().getId();
    }

    // endregion

    @Override
    public <O extends ObjectType> boolean isDescendant(
            PrismObject<O> object, String ancestorOrgOid) {
        Validate.notNull(object, "object must not be null");
        Validate.notNull(ancestorOrgOid, "ancestorOrgOid must not be null");

        logger.trace("Querying if object {} is descendant of {}", object.getOid(), ancestorOrgOid);
        List<ObjectReferenceType> objParentOrgRefs = object.asObjectable().getParentOrgRef();
        if (objParentOrgRefs == null || objParentOrgRefs.isEmpty()) {
            return false;
        }

        List<UUID> objParentOrgOids = objParentOrgRefs.stream()
                .map(ref -> UUID.fromString(ref.getOid()))
                .collect(Collectors.toList());

        long opHandle = registerOperationStart(OP_IS_DESCENDANT, OrgType.class);
        try {
            return executeRetriable(OP_IS_DESCENDANT, SqaleUtils.oidToUuid(object.getOid()), opHandle, () -> {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
                    jdbcSession.executeStatement("CALL m_refresh_org_closure()");

                    QOrgClosure oc = new QOrgClosure();
                    long count = jdbcSession.newQuery()
                            .from(oc)
                            .where(oc.ancestorOid.eq(UUID.fromString(ancestorOrgOid))
                                    .and(oc.descendantOid.in(objParentOrgOids)))
                            .fetchCount();
                    return count != 0L;
                }
            });
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | RepositoryException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public <O extends ObjectType> boolean isAncestor(
            PrismObject<O> object, String descendantOrgOid) {
        Validate.notNull(object, "object must not be null");
        Validate.notNull(descendantOrgOid, "descendantOrgOid must not be null");

        logger.trace("Querying if object {} is ancestor of {}", object.getOid(), descendantOrgOid);
        // object is not considered ancestor of itself
        if (object.getOid() == null || object.getOid().equals(descendantOrgOid)) {
            return false;
        }

        long opHandle = registerOperationStart(OP_IS_ANCESTOR, OrgType.class);
        try {
            return executeRetriable(OP_IS_ANCESTOR, SqaleUtils.oidToUuid(object.getOid()), opHandle, () -> {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
                    jdbcSession.executeStatement("CALL m_refresh_org_closure()");

                    QOrgClosure oc = new QOrgClosure();
                    long count = jdbcSession.newQuery()
                            .from(oc)
                            .where(oc.ancestorOid.eq(UUID.fromString(object.getOid()))
                                    .and(oc.descendantOid.eq(UUID.fromString(descendantOrgOid))))
                            .fetchCount();
                    return count != 0L;
                }
            });
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException | RepositoryException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public long advanceSequence(String oid, OperationResult parentResult)
            throws ObjectNotFoundException {
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        logger.debug("Advancing sequence {}", oid);

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_ADVANCE_SEQUENCE)
                .addParam(OperationResult.PARAM_OID, oid)
                .build();
        long opHandle = registerOperationStart(OP_ADVANCE_SEQUENCE, SequenceType.class);
        try {
            return executeRetriable(opNamePrefix + OP_ADVANCE_SEQUENCE, oidUuid, opHandle,
                    () -> executeAdvanceSequence(oidUuid));
        } catch (ObjectAlreadyExistsException | RepositoryException | RuntimeException | SchemaException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            registerOperationFinish(opHandle);
            operationResult.close();
        }
    }

    private long executeAdvanceSequence(UUID oid)
            throws ObjectNotFoundException, SchemaException, RepositoryException {

        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            RootUpdateContext<SequenceType, QObject<MObject>, MObject> updateContext =
                    prepareUpdateContext(jdbcSession, SequenceType.class, oid, Collections.emptyList(), RepoModifyOptions.createForceReindex());
            SequenceType sequence = updateContext.getPrismObject().asObjectable();

            logger.trace("OBJECT before:\n{}", sequence.debugDumpLazily());

            long returnValue = SequenceUtil.advanceSequence(sequence);

            logger.trace("Return value = {}, OBJECT after:\n{}",
                    returnValue, sequence.debugDumpLazily());

            updateContext.finishExecutionOwn();
            jdbcSession.commit();
            return returnValue;
        }
    }

    @Override
    public void returnUnusedValuesToSequence(
            String oid, Collection<Long> unusedValues, OperationResult parentResult)
            throws ObjectNotFoundException {
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);
        Validate.notNull(parentResult, "Operation result must not be null.");

        logger.debug("Returning unused values of {} to sequence {}", unusedValues, oid);

        OperationResult operationResult =
                parentResult.subresult(opNamePrefix + OP_RETURN_UNUSED_VALUES_TO_SEQUENCE)
                        .addParam(OperationResult.PARAM_OID, oid)
                        .build();

        if (unusedValues == null || unusedValues.isEmpty()) {
            operationResult.recordSuccess();
            return;
        }
        long opHandle = registerOperationStart(
                OP_RETURN_UNUSED_VALUES_TO_SEQUENCE, SequenceType.class);
        try {
            executeRetriable(opNamePrefix + OP_RETURN_UNUSED_VALUES_TO_SEQUENCE, oidUuid, opHandle,
                    () -> executeReturnUnusedValuesToSequence(oidUuid, unusedValues));
        } catch (RepositoryException | RuntimeException | ObjectAlreadyExistsException | SchemaException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
            registerOperationFinish(opHandle);
        }
    }

    private Void executeReturnUnusedValuesToSequence(UUID oid, Collection<Long> unusedValues)
            throws SchemaException, ObjectNotFoundException, RepositoryException {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            RootUpdateContext<SequenceType, QObject<MObject>, MObject> updateContext =
                    prepareUpdateContext(jdbcSession, SequenceType.class, oid, Collections.emptyList(), RepoModifyOptions.createForceReindex());
            SequenceType sequence = updateContext.getPrismObject().asObjectable();

            logger.trace("OBJECT before:\n{}", sequence.debugDumpLazily());

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
                    logger.warn("UnusedValues in sequence {} already contains value of {}"
                            + " - ignoring the return request", oid, valueToReturn);
                }
            }

            logger.trace("OBJECT after:\n{}", sequence.debugDumpLazily());

            updateContext.finishExecutionOwn();
            jdbcSession.commit();
            return null;
        }
    }

    @Override
    public @NotNull <T extends ObjectType> Collection<Long> allocateContainerIdentifiers(
            @NotNull Class<T> type, @NotNull String oid, int howMany, @NotNull OperationResult parentResult)
            throws ObjectNotFoundException {
        UUID oidUuid = SqaleUtils.oidToUuidMandatory(oid);

        logger.debug("Allocating container identifiers {}", oid);

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_ALLOCATE_CONTAINER_IDENTIFIERS)
                .addParam(OperationResult.PARAM_TYPE, type.getName())
                .addParam(OperationResult.PARAM_OID, oid)
                .addParam("howMany", howMany)
                .build();

        try {
            return executeAllocateContainerIdentifiers(type, oidUuid, howMany);
        } catch (ObjectNotFoundException e) {
            operationResult.recordHandledError(e);
            throw e;
        } catch (RepositoryException | RuntimeException | SchemaException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private <T extends ObjectType> Collection<Long> executeAllocateContainerIdentifiers(Class<T> type, UUID oid, int howMany)
            throws ObjectNotFoundException, SchemaException, RepositoryException {
        MiscUtil.argCheck(howMany > 0, "howMany must be positive");
        long opHandle = registerOperationStart(OP_ALLOCATE_CONTAINER_IDENTIFIERS, type);
        try {
            return executeRetriable(OP_ALLOCATE_CONTAINER_IDENTIFIERS, oid, opHandle, () -> {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
                    RootUpdateContext<T, QObject<MObject>, MObject> updateContext =
                            prepareUpdateContext(jdbcSession, type, oid);
                    long sequenceBefore = MiscUtil.stateNonNull(
                            updateContext.row().containerIdSeq,
                            "no container ID seq in %s", oid);

                    long sequenceAfter = sequenceBefore;
                    List<Long> allocatedIdentifiers = new ArrayList<>(howMany);
                    for (int i = 0; i < howMany; i++) {
                        allocatedIdentifiers.add(sequenceAfter++);
                    }
                    logger.trace("Container identifiers allocated: from {} to {} (inclusive) for {}/{}",
                            sequenceBefore, sequenceAfter - 1, type.getSimpleName(), oid);

                    updateContext.finishExecutionSetCidOnly(sequenceAfter);
                    jdbcSession.commit();
                    return allocatedIdentifiers;
                }
            });
        } catch (ObjectAlreadyExistsException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public @NotNull String getRepositoryType() {
        return REPOSITORY_IMPL_NAME;
    }

    @Override
    public void repositorySelfTest(OperationResult parentResult) {
        // Does not need retries, because does not select from data tables.
        OperationResult operationResult =
                parentResult.createSubresult(opNamePrefix + OP_REPOSITORY_SELF_TEST);
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            long startMs = System.currentTimeMillis();
            jdbcSession.executeStatement("select 1");
            operationResult.addReturn("database-round-trip-ms", System.currentTimeMillis() - startMs);
            operationResult.recordSuccess();
        } catch (Exception e) {
            recordFatalError(operationResult, e);
        } finally {
            operationResult.close();
        }
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult parentResult) {
        OperationResult operationResult =
                parentResult.subresult(opNamePrefix + OP_TEST_ORG_CLOSURE_CONSISTENCY)
                        .addParam("repairIfNecessary", repairIfNecessary)
                        .build();
        // FIXME: RetriableRead: Make retriable
        try {
            long closureCount, expectedCount;
            try (JdbcSession jdbcSession =
                    sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
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
                logger.info("Org closure consistency checked - closure count {}, expected count {}",
                        closureCount, expectedCount);
            }
            operationResult.addReturn("closure-count", closureCount);
            operationResult.addReturn("expected-count", expectedCount);

            if (repairIfNecessary && closureCount != expectedCount) {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
                    jdbcSession.executeStatement("CALL m_refresh_org_closure(true)");
                    jdbcSession.commit();
                }
                logger.info("Org closure rebuild was requested and executed");
                operationResult.addReturn("rebuild-done", true);
            } else {
                operationResult.addReturn("rebuild-done", false);
            }

            operationResult.recordSuccess();
        } catch (Exception e) {
            recordFatalError(operationResult, e);
        } finally {
            operationResult.close();
        }
    }

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(
            RepositoryQueryDiagRequest request, OperationResult parentResult) {

        Objects.requireNonNull(request, "Request must not be null.");
        Objects.requireNonNull(request.getType(), "request.type must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        logger.debug("Executing arbitrary query '{}'.", request);

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_EXECUTE_QUERY_DIAGNOSTICS)
                .setMinor()
                .addParam("request", request.toString())
                .build();

        try {
            ObjectQuery query = request.getQuery();
            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return new RepositoryQueryDiagResponse(null, null, null); // or List.of() and Map.of()?
            }

            // The first execute is the conventional prefix in this class. :-)
            return executeExecuteQueryDiagnostics(request, request.getType());
        } catch (SchemaException | RepositoryException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private <S extends Containerable, Q extends FlexibleRelationalPathBase<R>, R> RepositoryQueryDiagResponse
    executeExecuteQueryDiagnostics(RepositoryQueryDiagRequest request, @NotNull Class<S> type)
            throws RepositoryException, SchemaException {
        long opHandle = registerOperationStart(OP_EXECUTE_QUERY_DIAGNOSTICS, (Class<? extends Containerable>) null);

        try {
            return executeRetriable(OP_EXECUTE_QUERY_DIAGNOSTICS, null, opHandle, () -> {
                try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
                    // Modified code from SqlQueryExecutor.list()
                    SimulatedSqlQuery<Object> simulatedQuery = new SimulatedSqlQuery<>(
                            sqlRepoContext.getQuerydslConfiguration(), jdbcSession.connection(), request.isTranslateOnly());

                    // Special handling for references?
                    SqaleQueryContext<S, Q, R> context;
                    if (ObjectReferenceType.class.isAssignableFrom(type)) {
                        SqaleTableMapping mapping = determineMapping(request.getQuery().getFilter());
                        context = SqaleQueryContext.from(mapping, sqlRepoContext, simulatedQuery, null);
                    } else {
                        context = SqaleQueryContext.from(type, sqlRepoContext, simulatedQuery, null);
                    }
                    ObjectQuery query = request.getQuery();
                    if (query != null) {
                        context.processFilter(query.getFilter());
                        context.processObjectPaging(query.getPaging());
                    }
                    context.processOptions(request.getOptions());

                    List<?> resultList = null; // default for case when query is just translated
                    context.beforeQuery();
                    PageOf<Tuple> result;
                    try {
                        result = context.executeQuery(jdbcSession);
                        PageOf<S> transformedResult = context.transformToSchemaType(result, jdbcSession);
                        //noinspection unchecked
                        if (ObjectReferenceType.class.isAssignableFrom(type)) {
                            resultList = transformedResult.content();
                        } else {
                            resultList = transformedResult.map(o -> (PrismContainerValue<S>) o.asPrismContainerValue()).content();
                        }
                    } catch (RuntimeException e) {
                        if (e != SimulatedSqlQuery.SIMULATION_EXCEPTION) {
                            throw e; // OK, this was unexpected, so rethrow it
                        }
                    }

                    return new RepositoryQueryDiagResponse(
                            resultList, simulatedQuery.toString(), simulatedQuery.paramsMap());
                }
            });
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException e) {
            throw shouldNotHappen(e);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public synchronized void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
        if (PrismUtil.realValueEquals(fullTextSearchConfiguration, fullTextSearch)) {
            logger.trace("Ignoring full text search configuration update => the real value has not changed");
            return;
        }
        logger.info("Applying full text search configuration ({} entries)",
                fullTextSearch != null ? fullTextSearch.getIndexed().size() : 0);
        fullTextSearchConfiguration = fullTextSearch;
        sqlRepoContext.setFullTextSearchConfiguration(fullTextSearch);
    }

    @Override
    public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
        return fullTextSearchConfiguration;
    }

    @Override
    public void postInit(OperationResult parentResult) throws SchemaException {
        logger.debug("Executing repository postInit method");
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
        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_HAS_CONFLICT)
                .setMinor()
                .addParam(OperationResult.PARAM_OID, watcher.getOid())
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
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    @Override
    public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid,
            DiagnosticInformationType information, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        OperationResult operationResult =
                parentResult.subresult(opNamePrefix + OP_ADD_DIAGNOSTIC_INFORMATION)
                        .addQualifier(type.getSimpleName())
                        .addParam(OperationResult.PARAM_TYPE, type)
                        .addParam(OperationResult.PARAM_OID, oid)
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
        } finally {
            operationResult.close();
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
        logger.trace("Limit for diagnostic information of type '{}': {}", infoType, limit);
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
                logger.trace("Going to delete {} diagnostic information values", toDelete.size());
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
    public boolean supports(@NotNull Class<? extends ObjectType> type) {
        return true;
    }

    // region Aggregate functions

    @Override
    public @NotNull SearchResultList<PrismContainerValue<?>> searchAggregate(AggregateQuery<?> query, OperationResult parentResult) throws SchemaException {
        var result = parentResult.createSubresult(OP_SEARCH_AGGREGATE);
        var handler = aggregateQueryHandler(query, result);
        var opHandle = registerOperationStart(OP_SEARCH_AGGREGATE, ObjectType.class);
        try {
            return executeRetriable(OP_SEARCH_AGGREGATE, null, opHandle, handler::search);
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException e) {
            throw shouldNotHappen(e);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, result);
        } catch (Throwable t) {
            recordFatalError(result, t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @Override
    public int countAggregate(AggregateQuery<?> query, OperationResult parentResult) throws SchemaException {
        var result = parentResult.createSubresult(OP_COUNT_AGGREGATE);
        var handler = aggregateQueryHandler(query, result);
        var opHandle = registerOperationStart(OP_COUNT_AGGREGATE, ObjectType.class);
        try {
            return executeRetriable(OP_COUNT_AGGREGATE, null, opHandle, handler::count);
        } catch (ObjectAlreadyExistsException | ObjectNotFoundException e) {
            throw shouldNotHappen(e);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, result);
        } catch (Throwable t) {
            recordFatalError(result, t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
            registerOperationFinish(opHandle);
        }
    }

    private AggregateSearchContext aggregateQueryHandler(AggregateQuery<?> query, OperationResult result) throws SchemaException {
        var type = query.getRoot();
        var queryContext = SqaleQueryContext.from(type, sqlRepoContext);
        if (queryContext == null) {
            throw new SchemaException("Aggregate search not supported for " + type.getSimpleName());
        }
        return new AggregateSearchContext(query, queryContext, result);
    }

    // region Retries

    private <R, E> R executeRetriable(String opName, UUID oid, long opHandle, RetriableOperation<R> operation) throws ObjectNotFoundException, SchemaException, RepositoryException, ObjectAlreadyExistsException {
        var maxAttempts = 100;
        var attempt = 1;
        while (attempt < maxAttempts) {

            try {
                return operation.execute();
            } catch (Exception e) {
                if (!isRetriableException(e)) {
                    throw e;
                }
                performanceMonitor.registerOperationNewAttempt(opHandle, attempt);
                attempt = prepareNextRetry(opName, oid, attempt, e);
            }
        }
        throw new SystemException("MAX Attempt count reached");
    }

    /**
     * Error for repeateble read isolation - this error message is part of PostgreSQL contract as described in docs.
     *
     * https://www.postgresql.org/docs/16/transaction-iso.html
     */
    private static final String PSQL_CONCURRENT_UPDATE_MESSAGE = "ERROR: could not serialize access due to concurrent update";

    private static final String PSQL_FOREIGN_KEY_VIOLATION = "23503";
    private static final String PSQL_CHECK_VIOLATION = "23514";

    private static final String PSQL_DEADLOCK_DETECTED = "40P01";


    private boolean isRetriableException(Exception e) {
        Throwable toCheck = e;
        while (toCheck != null) {
            if (toCheck instanceof PSQLException pgEx) {
                if (pgEx.getMessage().startsWith(PSQL_CONCURRENT_UPDATE_MESSAGE)) {
                    // Concurrency issue - concurrent update
                    return true;
                }
                if (PSQL_FOREIGN_KEY_VIOLATION.equals(pgEx.getSQLState()) && pgEx.getMessage().contains("m_uri")) {
                    // This could be immediate retry - because of URI cache.
                    return true;
                }
                if (PSQL_CHECK_VIOLATION.equals(pgEx.getSQLState()) && pgEx.getMessage().contains("partition constraint")) {
                    // Retry on partition constraints failed - partition may be added during insert from another client
                    // and now shadow belong to other partition.
                    return true;
                }
                if (PSQL_DEADLOCK_DETECTED.equals(pgEx.getSQLState())) {
                    // Sometimes there can be deadlock if one thread is updating shadow in default partition
                    // and other thread (client) triggered partition move.
                    return true;
                }
            }
            toCheck = toCheck.getCause();
        }
        return false;
    }

    private interface RetriableOperation<T> {
        T execute() throws ObjectNotFoundException, SchemaException, RepositoryException, ObjectAlreadyExistsException;

    }

    private static final Trace CONTENTION_LOGGER =
            TraceManager.getTrace(SqaleRepositoryService.class.getName() + ".contention");

    /**
     * How many times we want to repeat operation after lock acquisition,
     * pessimistic, optimistic exception.
     */
    public static final int LOCKING_MAX_RETRIES = 40;

    /**
     * Timeout will be a random number between 0 and LOCKING_DELAY_INTERVAL_BASE * 2^exp
     * where exp is either real attempt # minus 1, or LOCKING_EXP_THRESHOLD (whatever is lesser).
     */
    public static final long LOCKING_DELAY_INTERVAL_BASE = 50;

    public static final int LOCKING_EXP_THRESHOLD = 7; // i.e. up to 6400ms wait time

    public static final int CONTENTION_LOG_DEBUG_THRESHOLD = 3;
    public static final int MAIN_LOG_WARN_THRESHOLD = 8;

    private int prepareNextRetry(String operation, UUID oid, int attempt, Exception ex) {
        BackoffComputer backoffComputer = new ExponentialBackoffComputer(LOCKING_MAX_RETRIES, LOCKING_DELAY_INTERVAL_BASE, LOCKING_EXP_THRESHOLD, null);
        long waitTime;
        try {
            waitTime = backoffComputer.computeDelay(attempt);
        } catch (BackoffComputer.NoMoreRetriesException e) {
            CONTENTION_LOGGER.error("A serialization-related problem occurred, maximum attempts ({}) reached.", attempt, ex);
            LOGGER.error("A serialization-related problem occurred, maximum attempts ({}) reached.", attempt, ex);
            /*
            if (result != null) {
                result.recordFatalError("A serialization-related problem occurred.", ex);
            }
            */
            throw new SystemException(ex.getMessage() + " [attempts: " + attempt + "]", ex);
        }
        String message = "A serialization-related problem occurred when {} object with oid '{}', retrying after "
                + "{} ms (this is retry {} of {})\n{}: {}";
        Object[] objects = { operation, oid, waitTime, attempt, LOCKING_MAX_RETRIES, ex.getClass().getSimpleName(), ex.getMessage() };
        if (attempt >= CONTENTION_LOG_DEBUG_THRESHOLD) {
            CONTENTION_LOGGER.debug(message, objects);
        } else {
            CONTENTION_LOGGER.trace(message, objects);
        }
        if (attempt >= MAIN_LOG_WARN_THRESHOLD) {
            LOGGER.warn(message, objects);
        } else {
            LOGGER.debug(message, objects);
        }
        if (waitTime > 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ex1) {
                // ignore this
            }
        }
        return attempt + 1;
    }

    @Override
    public void createPartitionsForExistingData(OperationResult parentResult) throws SchemaException {
        // Currently we support only partitioning for shadow type
        // If partitioning is added for other types we should also call their partition
        // manager


        var shadowMapping = (SqaleTableMapping) sqlRepoContext.getMappingBySchemaType(ShadowType.class);
        var partitionManager = shadowMapping.getPartitionManager();
        if (partitionManager == null) {
            return;
        }

        var result = parentResult.createSubresult(OP_CREATE_PARTITIONS_FOR_EXISTING_DATA);
        try {
            long opHandle = registerOperationStart(OP_CREATE_PARTITIONS_FOR_EXISTING_DATA, ShadowType.class);
            executeRetriable("createPartitions", null, opHandle, () -> {
                partitionManager.createMissingPartitions(result);
                return null;
            });
            result.computeStatus();
        } catch (Exception e) {
            result.recordFatalError(e);
        } finally {
            result.close();
        }
    }

    @Override
    public void applyRepositoryConfiguration(@Nullable RepositoryConfigurationType repositoryConfig) {
        configurationChangeListener.update(repositoryConfig);
    }
}
