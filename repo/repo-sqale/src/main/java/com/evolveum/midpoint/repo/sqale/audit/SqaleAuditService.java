/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.util.SystemConfigurationAuditUtil.getDeltaSuccessExecutionResult;
import static com.evolveum.midpoint.schema.util.SystemConfigurationAuditUtil.isEscapingInvalidCharacters;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.dml.DefaultMapper;
import com.querydsl.sql.dml.SQLInsertClause;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.S_ConditionEntry;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.repo.api.SqlPerformanceMonitorsCollection;
import com.evolveum.midpoint.repo.sqale.*;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.*;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.evolveum.midpoint.repo.sqlbase.RepositoryObjectParseResult;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryExecutor;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Audit service using SQL DB as a store, also allows for searching (see {@link #supportsRetrieval}).
 */
public class SqaleAuditService extends SqaleServiceBase implements AuditService {

    private final SqlQueryExecutor sqlQueryExecutor;

    // set from SystemConfigurationAuditType
    private boolean escapeIllegalCharacters = false;
    @NotNull private OperationResultDetailLevel deltaSuccessExecutionResult = OperationResultDetailLevel.CLEANED_UP;

    public SqaleAuditService(
            SqaleRepoContext sqlRepoContext,
            SqlPerformanceMonitorsCollection sqlPerformanceMonitorsCollection) {
        super(sqlRepoContext, sqlPerformanceMonitorsCollection);
        this.sqlQueryExecutor = new SqlQueryExecutor(sqlRepoContext);
    }

    @Override
    public void audit(AuditEventRecord record, Task task, OperationResult parentResult) {
        Objects.requireNonNull(record, "Audit event record must not be null.");

        OperationResult operationResult = parentResult.createSubresult(opNamePrefix + OP_AUDIT);

        try {
            executeAudit(record);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private void executeAudit(AuditEventRecord record) {
        long opHandle = registerOperationStart(OP_AUDIT);
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            record.setRepoId(null); // we want DB to assign the ID
            MAuditEventRecord auditRow = insertAuditEventRecord(jdbcSession, record);
            record.setRepoId(auditRow.id);

            insertAuditDeltas(jdbcSession, auditRow);
            insertReferences(jdbcSession, auditRow, record.getReferences());

            jdbcSession.commit();
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    /**
     * Inserts audit event record aggregate root without any subentities.
     * Traditional Sqale "insert root first, then insert children" is not optimal here,
     * because to insert root we need to collect some information from children anyway.
     * So we prepare the subentities in collections, gather the needed information
     * (e.g. changed item paths) and then insert root entity.
     * Subentities are inserted later out of this method.
     *
     * @return inserted row with transient deltas prepared for insertion
     */
    private MAuditEventRecord insertAuditEventRecord(JdbcSession jdbcSession, AuditEventRecord record) {
        QAuditEventRecordMapping aerMapping = QAuditEventRecordMapping.get();
        QAuditEventRecord aer = aerMapping.defaultAlias();
        MAuditEventRecord row = aerMapping.toRowObject(record);

        Collection<MAuditDelta> deltaRows = prepareDeltas(record.getDeltas());
        row.deltas = deltaRows;

        Set<String> changedItemPaths = collectChangedItemPathsFromOriginal(record.getDeltas());
        row.changedItemPaths = changedItemPaths.isEmpty() ? null : changedItemPaths.toArray(String[]::new);

        SQLInsertClause insert = jdbcSession.newInsert(aer).populate(row);
        Map<String, ColumnMetadata> customColumns = aerMapping.getExtensionColumns();
        for (Map.Entry<String, String> property : record.getCustomColumnProperty().entrySet()) {
            String propertyName = property.getKey();
            if (!customColumns.containsKey(propertyName)) {
                throw new IllegalArgumentException("Audit event record table doesn't"
                        + " contains column for property " + propertyName);
            }
            // Like insert.set, but that one is too parameter-type-safe for our generic usage here.
            insert.columns(aer.getPath(propertyName)).values(property.getValue());
        }

        Long returnedId = insert.executeWithKey(aer.id);
        // If returned ID is null, it was likely provided, so we use that one.
        row.id = returnedId != null ? returnedId : record.getRepoId();
        return row;
    }

    private Collection<MAuditDelta> prepareDeltas(Collection<ObjectDeltaOperation<?>> deltas) {
        // we want to keep only unique deltas, checksum is also part of PK
        Set<String> seenChecksums = new HashSet<>();

        var ret = new ArrayList<MAuditDelta>(deltas.size());
        for (ObjectDeltaOperation<?> deltaOperation : deltas) {
            if (deltaOperation == null) {
                continue;
            }
            MAuditDelta mAuditDelta = convertDelta(deltaOperation);
            if (mAuditDelta != null && !seenChecksums.contains(mAuditDelta.checksum)) {
                // If we did not see checksum, store it, if we seen it already, skip it
                seenChecksums.add(mAuditDelta.checksum);
                ret.add(mAuditDelta);
            }
        }
        return ret;
    }

    /**
     * Returns prepared audit delta row without PK columns which will be added later.
     * For normal repo this code would be in mapper, but here we know exactly what type we work with.
     */
    private @Nullable MAuditDelta convertDelta(ObjectDeltaOperation<?> deltaOperation) {
        try {
            MAuditDelta deltaRow = new MAuditDelta();
            ObjectDelta<? extends ObjectType> delta = deltaOperation.getObjectDelta();
            deltaRow.deltaOid = SqaleUtils.oidToUuid(deltaOperation.getObjectOid());
            if (delta != null) {
                DeltaConversionOptions options =
                        DeltaConversionOptions.createSerializeReferenceNames();
                options.setEscapeInvalidCharacters(escapeIllegalCharacters);
                String serializedDelta = DeltaConvertor.serializeDelta(
                        delta, options, repositoryConfiguration().getFullObjectFormat());

                // serializedDelta is transient, needed for changed items later
                deltaRow.serializedDelta = serializedDelta;
                deltaRow.delta = serializedDelta.getBytes(StandardCharsets.UTF_8);

                deltaRow.deltaOid = SqaleUtils.oidToUuid(delta.getOid());

                deltaRow.deltaType = ChangeType.toChangeTypeType(delta.getChangeType());
            }

            OperationResult executionResult = deltaOperation.getExecutionResult();
            byte[] fullResultForCheckSum = null;
            if (executionResult != null) {
                executionResult = processExecutionResult(executionResult);

                OperationResultType operationResult = executionResult.createOperationResultType();
                deltaRow.status = operationResult.getStatus();

                // We need this byte[] for checksum later, even if we don't store it because of NONE.
                // It shouldn't matter if it's cleaned-up or just top, because if it's the same
                // as something else we don't need the same line in DB anyway.
                fullResultForCheckSum = sqlRepoContext.createFullResult(operationResult);

                // In other words "if (NOT (SUCCESS && store-NONE-is-set))..." or in other words
                // "only do nothing if it is SUCCESS and config says to store NONE success result".
                if (operationResult.getStatus() != OperationResultStatusType.SUCCESS
                        || deltaSuccessExecutionResult != OperationResultDetailLevel.NONE) {
                    deltaRow.fullResult = fullResultForCheckSum;
                }
            }
            deltaRow.resourceOid = SqaleUtils.oidToUuid(deltaOperation.getResourceOid());
            if (deltaOperation.getObjectName() != null) {
                deltaRow.objectNameOrig = deltaOperation.getObjectName().getOrig();
                deltaRow.objectNameNorm = deltaOperation.getObjectName().getNorm();
            }
            if (deltaOperation.getResourceName() != null) {
                deltaRow.resourceNameOrig = deltaOperation.getResourceName().getOrig();
                deltaRow.resourceNameNorm = deltaOperation.getResourceName().getNorm();
            }
            deltaRow.shadowKind = deltaOperation.getShadowKind();
            deltaRow.shadowIntent = deltaOperation.getShadowIntent();

            deltaRow.checksum = computeChecksum(deltaRow.delta, fullResultForCheckSum);
            return deltaRow;
        } catch (Exception ex) {
            logger.warn("Unexpected problem during audit delta conversion", ex);
            return null;
        }
    }

    private OperationResult processExecutionResult(OperationResult executionResult) {
        if (!executionResult.isSuccess()) {
            return executionResult; // not success, we don't touch it
        }

        switch (deltaSuccessExecutionResult) {
            case TOP -> executionResult = executionResult.keepRootOnly();
            case CLEANED_UP -> {
                executionResult = executionResult.clone();
                try {
                    executionResult.cleanup();
                } catch (Exception e) {
                    logger.warn("Execution result cleanup exception (reported, but ignored otherwise): {}", e.toString());
                }
            }
            default -> {
            }
            // full or none - nothing to do here
        }
        return executionResult;
    }

    private String computeChecksum(byte[]... objects) {
        try {
            List<InputStream> list = new ArrayList<>();
            for (byte[] data : objects) {
                if (data == null) {
                    continue;
                }
                list.add(new ByteArrayInputStream(data));
            }
            SequenceInputStream sis = new SequenceInputStream(Collections.enumeration(list));

            return DigestUtils.md5Hex(sis);
        } catch (IOException ex) {
            throw new SystemException(ex);
        }
    }

    private Set<String> collectChangedItemPaths(Collection<MAuditDelta> deltas) {
        Set<String> changedItemPaths = new HashSet<>();
        for (MAuditDelta delta : deltas) {
            try {
                // TODO: this calls compat parser, but for just serialized deltas we could be strict too
                //  See MID-7431, this currently just shows ERROR with ignore message and no stack trace.
                //  We could either check parseResult.parsingContext.has/getWarnings, or use strict and catch (probably better).
                RepositoryObjectParseResult<ObjectDeltaType> parseResult =
                        sqlRepoContext.parsePrismObject(delta.serializedDelta, ObjectDeltaType.class);
                ObjectDeltaType deltaBean = parseResult.prismValue;
                for (ItemDeltaType itemDelta : deltaBean.getItemDelta()) {
                    ItemPath path = itemDelta.getPath().getItemPath();
                    CanonicalItemPath canonical = sqlRepoContext.prismContext()
                            .createCanonicalItemPath(path, deltaBean.getObjectType());
                    for (int i = 0; i < canonical.size(); i++) {
                        changedItemPaths.add(canonical.allUpToIncluding(i).asString());
                    }
                }
            } catch (SchemaException | SystemException e) {
                // See MID-6446 - we want to throw in tests, old ones should be fixed by now
                if (InternalsConfig.isConsistencyChecks()) {
                    throw new SystemException("Problem during audit delta parse", e);
                }
                logger.warn("Serialized audit delta with OID '{}' cannot be parsed."
                        + " No changed items were created. This may cause problem later, but is not"
                        + " critical for storing the audit record.", delta.deltaOid, e);
            }
        }
        return changedItemPaths;
    }

    private Set<String> collectChangedItemPathsFromOriginal(Collection<ObjectDeltaOperation<? extends ObjectType>> deltas) {
        Set<String> changedItemPaths = new HashSet<>();
        for (var delta : deltas) {
            var objectType = objectTypeQName(delta);
            for (var itemDelta : delta.getObjectDelta().getModifications()) {
                ItemPath path = itemDelta.getPath();
                CanonicalItemPath canonical = sqlRepoContext.prismContext()
                        .createCanonicalItemPath(path, objectType);
                for (int i = 0; i < canonical.size(); i++) {
                    changedItemPaths.add(canonical.allUpToIncluding(i).asString());
                }
            }
        }
        return changedItemPaths;
    }

    private QName objectTypeQName(ObjectDeltaOperation<? extends ObjectType> delta) {
        return ObjectTypes.getObjectType(delta.getObjectDelta().getObjectTypeClass()).getTypeQName();
    }

    private void insertAuditDeltas(
            JdbcSession jdbcSession, MAuditEventRecord auditRow) {

        if (!auditRow.deltas.isEmpty()) {
            SQLInsertClause insertBatch = jdbcSession.newInsert(
                    QAuditDeltaMapping.get().defaultAlias());
            for (MAuditDelta deltaRow : auditRow.deltas) {
                deltaRow.recordId = auditRow.id;
                deltaRow.timestamp = auditRow.timestamp;

                // NULLs are important to keep the value count consistent during the batch
                insertBatch.populate(deltaRow, DefaultMapper.WITH_NULL_BINDINGS).addBatch();
            }
            insertBatch.setBatchToBulk(true);
            insertBatch.execute();
        }
    }

    private void insertReferences(JdbcSession jdbcSession,
            MAuditEventRecord auditRow, Map<String, Set<AuditReferenceValue>> references) {
        if (references.isEmpty()) {
            return;
        }

        QAuditRefValue qr = QAuditRefValueMapping.get().defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qr);
        for (String refName : references.keySet()) {
            for (AuditReferenceValue refValue : references.get(refName)) {
                // id will be generated, but we're not interested in those here
                PolyString targetName = refValue.getTargetName();
                insertBatch.set(qr.recordId, auditRow.id)
                        .set(qr.timestamp, auditRow.timestamp)
                        .set(qr.name, refName)
                        .set(qr.targetOid, SqaleUtils.oidToUuid(refValue.getOid()))
                        .set(qr.targetType, refValue.getType() != null
                                ? MObjectType.fromTypeQName(refValue.getType()) : null)
                        .set(qr.targetNameOrig, PolyString.getOrig(targetName))
                        .set(qr.targetNameNorm, PolyString.getNorm(targetName))
                        .addBatch();
            }
        }
        if (insertBatch.getBatchCount() == 0) {
            return; // strange, no values anywhere?
        }

        insertBatch.setBatchToBulk(true);
        insertBatch.execute();
    }

    @Override
    public void audit(AuditEventRecordType record, OperationResult parentResult) {
        Objects.requireNonNull(record, "Audit event record must not be null.");

        OperationResult operationResult = parentResult.createSubresult(opNamePrefix + OP_AUDIT);

        try {
            executeAudit(record);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private void executeAudit(AuditEventRecordType record) {
        long opHandle = registerOperationStart(OP_AUDIT);
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            // plenty of parameters, but it's better to have a short-lived stateful worker for it
            new AuditInsertion(record, jdbcSession, sqlRepoContext, escapeIllegalCharacters, logger)
                    .execute();

            jdbcSession.commit();
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
        Objects.requireNonNull(policy, "Cleanup policy must not be null.");
        Objects.requireNonNull(parentResult, "Operation result must not be null.");

        // TODO review monitoring performance of these cleanup operations
        // It looks like the attempts (and wasted time) are not counted correctly
        cleanupAuditMaxRecords(policy, parentResult);
        cleanupAuditMaxAge(policy, parentResult);
    }

    private void cleanupAuditMaxAge(CleanupPolicyType policy, OperationResult parentResult) {
        if (policy.getMaxAge() == null) {
            return;
        }

        OperationResult operationResult =
                parentResult.createSubresult(opNamePrefix + OP_CLEANUP_AUDIT_MAX_AGE);
        try {
            executeCleanupAuditMaxAge(policy.getMaxAge());
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private void executeCleanupAuditMaxAge(Duration maxAge) {
        long opHandle = registerOperationStart(OP_CLEANUP_AUDIT_MAX_AGE);

        if (maxAge.getSign() > 0) {
            maxAge = maxAge.negate();
        }
        // This is silly, it mutates the Date, but there seems to be no other way to do it!
        Date minValue = new Date();
        maxAge.addTo(minValue);
        Instant olderThan = Instant.ofEpochMilli(minValue.getTime());

        long start = System.currentTimeMillis();
        long deletedCount = 0;
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            logger.info("Audit cleanup, deleting records older than {}.", olderThan);

            QAuditEventRecord qae = QAuditEventRecordMapping.get().defaultAlias();
            deletedCount = jdbcSession.newDelete(qae)
                    .where(qae.timestamp.lt(olderThan))
                    .execute();
            jdbcSession.commit();
        } finally {
            registerOperationFinish(opHandle);
            logger.info("Audit cleanup based on age finished; deleted {} entries in {} seconds.",
                    deletedCount, (System.currentTimeMillis() - start) / 1000L);
        }
    }

    // TODO: Document that this is less efficient with current timestamp-partitioned repo.
    //  Not necessarily inefficient per se, just less efficient than using timestamp.
    private void cleanupAuditMaxRecords(CleanupPolicyType policy, OperationResult parentResult) {
        Integer maxRecords = policy.getMaxRecords();
        if (maxRecords == null) {
            return;
        }

        OperationResult operationResult =
                parentResult.createSubresult(opNamePrefix + OP_CLEANUP_AUDIT_MAX_RECORDS);
        try {
            executeCleanupAuditMaxRecords(maxRecords);
        } catch (RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private void executeCleanupAuditMaxRecords(int maxRecords) {
        long opHandle = registerOperationStart(OP_CLEANUP_AUDIT_MAX_RECORDS);

        long start = System.currentTimeMillis();
        long deletedCount = 0;
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startTransaction()) {
            logger.info("Audit cleanup, deleting to leave only {} records.", maxRecords);
            QAuditEventRecord qae = QAuditEventRecordMapping.get().defaultAlias();
            Long deleteFromId = jdbcSession.newQuery()
                    .select(qae.id)
                    .from(qae)
                    .orderBy(qae.id.desc())
                    .offset(maxRecords)
                    .fetchFirst();
            if (deleteFromId == null) {
                logger.info("Nothing to delete from audit, {} entries allowed.", maxRecords);
                return;
            }

            deletedCount = jdbcSession.newDelete(qae)
                    .where(qae.id.loe(deleteFromId))
                    .execute();
            jdbcSession.commit();
        } finally {
            registerOperationFinish(opHandle);
            logger.info("Audit cleanup based on record count finished; deleted {} entries in {} seconds.",
                    deletedCount, (System.currentTimeMillis() - start) / 1000L);
        }
    }

    @Override
    public boolean supportsRetrieval() {
        return true;
    }

    @Override
    public void applyAuditConfiguration(@Nullable SystemConfigurationAuditType configuration) {
        escapeIllegalCharacters = isEscapingInvalidCharacters(configuration);
        deltaSuccessExecutionResult = getDeltaSuccessExecutionResult(configuration);
    }

    @Override
    public int countObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_COUNT_OBJECTS)
                .addParam("query", query)
                .build();

        try {
            return executeCountObjects(query, options);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private int executeCountObjects(@Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options) throws RepositoryException {
        long opHandle = registerOperationStart(OP_COUNT_OBJECTS);
        try {
            var queryContext = SqaleQueryContext.from(
                    AuditEventRecordType.class, sqlRepoContext);
            return sqlQueryExecutor.count(queryContext, query, options);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    @NotNull
    public SearchResultList<AuditEventRecordType> searchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult)
            throws SchemaException {

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_OBJECTS)
                .addParam("query", query)
                .build();

        try {
            logSearchInputParameters(AuditEventRecordType.class, query, "Search audit");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return new SearchResultList<>();
            }

            return executeSearchObjects(query, options, OP_SEARCH_OBJECTS);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    private SearchResultList<AuditEventRecordType> executeSearchObjects(
            @Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            String operationKind)
            throws RepositoryException, SchemaException {

        long opHandle = registerOperationStart(operationKind);
        try {
            return sqlQueryExecutor.list(
                    SqaleQueryContext.from(AuditEventRecordType.class, sqlRepoContext),
                    query, options);
        } finally {
            registerOperationFinish(opHandle);
        }
    }

    @Override
    public SearchResultMetadata searchObjectsIterative(
            @Nullable ObjectQuery query,
            @NotNull AuditResultHandler handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) throws SchemaException {
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult operationResult = parentResult.subresult(opNamePrefix + OP_SEARCH_OBJECTS_ITERATIVE)
                .addParam("type", AuditEventRecordType.class.getName())
                .addParam("query", query)
                .build();

        try {
            logSearchInputParameters(AuditEventRecordType.class, query, "Iterative search audit");

            query = ObjectQueryUtil.simplifyQuery(query);
            if (ObjectQueryUtil.isNoneQuery(query)) {
                return new SearchResultMetadata().approxNumberOfAllResults(0);
            }

            return executeSearchObjectsIterative(query, handler, options, operationResult);
        } catch (RepositoryException | RuntimeException e) {
            throw handledGeneralException(e, operationResult);
        } catch (Throwable t) {
            recordFatalError(operationResult, t);
            throw t;
        } finally {
            operationResult.close();
        }
    }

    /*
    TODO: We should try to unify iterative search for repo and audit.
     There are some obvious differences - like the provider of the page results - the differences need to be
     captured before the common functionality.
     Then there are little nuances in filter/ordering:
     In repo there is no potential collision between provided filter/ordering and additional "technical" one for OID.
     In audit these can collide, but perhaps not every situation needs to be optimized and we can let DB do the work
     (e.g. superfluous timestamp conditions).
     See also iterativeSearchCondition() comment and ideas there.
     */
    private SearchResultMetadata executeSearchObjectsIterative(
            ObjectQuery originalQuery,
            AuditResultHandler handler,
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
            // We want to order the ref in the same direction as the provided ordering.
            // This is also reflected by GT/LT conditions in lastOidCondition() method.
            paging.addOrderingInstruction(AuditEventRecordType.F_REPO_ID,
                    providedOrdering != null && providedOrdering.size() == 1
                            && providedOrdering.get(0).getDirection() == OrderDirection.DESCENDING
                            ? OrderDirection.DESCENDING : OrderDirection.ASCENDING);
            pagedQuery.setPaging(paging);

            int pageSize = Math.min(
                    repositoryConfiguration().getIterativeSearchByPagingBatchSize(),
                    defaultIfNull(maxSize, Integer.MAX_VALUE));
            pagedQuery.getPaging().setMaxSize(pageSize);
            pagedQuery.getPaging().setOffset(offset);

            AuditEventRecordType lastProcessedObject = null;
            int handledObjectsTotal = 0;

            while (true) {
                if (maxSize != null && maxSize - handledObjectsTotal < pageSize) {
                    // relevant only for the last page
                    pagedQuery.getPaging().setMaxSize(maxSize - handledObjectsTotal);
                }

                // filterAnd() is quite null safe, even for both nulls
                ObjectFilter originalFilter = originalQuery != null ? originalQuery.getFilter() : null;
                pagedQuery.setFilter(ObjectQueryUtil.filterAndImmutable(
                        originalFilter, iterativeSearchCondition(lastProcessedObject, providedOrdering)));

                // we don't call public searchObject to avoid subresults and query simplification
                logSearchInputParameters(AuditEventRecordType.class, pagedQuery, "Search audit iterative page");
                List<AuditEventRecordType> resultPage = executeSearchObjects(
                        pagedQuery, options, OP_SEARCH_OBJECTS_ITERATIVE_PAGE);

                // process page results
                for (AuditEventRecordType auditEvent : resultPage) {
                    lastProcessedObject = auditEvent;
                    if (!handler.handle(auditEvent, operationResult)) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal + 1)
                                .pagingCookie(lastProcessedObject.getRepoId().toString())
                                .partialResults(true);
                    }
                    handledObjectsTotal += 1;

                    if (maxSize != null && handledObjectsTotal >= maxSize) {
                        return new SearchResultMetadata()
                                .approxNumberOfAllResults(handledObjectsTotal)
                                .pagingCookie(lastProcessedObject.getRepoId().toString());
                    }
                }

                if (resultPage.isEmpty() || resultPage.size() < pageSize) {
                    return new SearchResultMetadata()
                            .approxNumberOfAllResults(handledObjectsTotal)
                            .pagingCookie(lastProcessedObject != null
                                    ? lastProcessedObject.getRepoId().toString() : null);
                }
                pagedQuery.getPaging().setOffset(null);
            }
        } finally {
            // This just counts the operation and adds zero/minimal time not to confuse user
            // with what could be possibly very long duration.
            registerOperationFinish(registerOperationStart(OP_SEARCH_OBJECTS_ITERATIVE));
        }
    }

    /**
     * Similar to {@link SqaleRepositoryService#lastOidCondition}.
     *
     * TODO, lots of possible improvements:
     *
     * * Just like in repo iterative search this is added to the original filter with `AND`.
     *
     * * Support for multiple order specifications from the client is not supported.
     * Perhaps some short-term stateful filter/order object would be better to construct this,
     * especially if it could be used in both repo and audit (with strict order attribute
     * provided in constructor for example).
     *
     * TODO: Currently, single path ordering is supported. Finish multi-path too.
     * TODO: What about nullable columns?
     */
    @Nullable
    private ObjectFilter iterativeSearchCondition(
            @Nullable AuditEventRecordType lastProcessedObject,
            List<? extends ObjectOrdering> providedOrdering) {
        if (lastProcessedObject == null) {
            return null;
        }

        // It may seem like a good idea to include timestamp for better partition pruning, but order by timestamp
        // can be different from order by id; for correct behavior only id must be used for order. See MID-7928.

        Long lastProcessedId = lastProcessedObject.getRepoId();
        if (providedOrdering == null || providedOrdering.isEmpty()) {
            return prismContext()
                    .queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_REPO_ID).gt(lastProcessedId)
                    .buildFilter();
        }

        if (providedOrdering.size() == 1) {
            ObjectOrdering objectOrdering = providedOrdering.get(0);
            ItemPath orderByPath = objectOrdering.getOrderBy();
            boolean asc = objectOrdering.getDirection() != OrderDirection.DESCENDING; // null => asc
            S_ConditionEntry filter = prismContext()
                    .queryFor(AuditEventRecordType.class)
                    .item(orderByPath);
            @SuppressWarnings("unchecked")
            Item<PrismValue, ItemDefinition<?>> item =
                    lastProcessedObject.asPrismContainerValue().findItem(orderByPath);
            if (item.size() > 1) {
                throw new IllegalArgumentException(
                        "Multi-value property for ordering is forbidden - item: " + item);
            } else if (item.isEmpty()) {
                // TODO what if it's nullable? is it null-first or last?
                // See: https://www.postgresql.org/docs/13/queries-order.html
                // "By default, null values sort as if larger than any non-null value; that is,
                // NULLS FIRST is the default for DESC order, and NULLS LAST otherwise."
            } else {
                // IMPORTANT: There is no fix for poly-strings here (MID-7860) like in SqaleRepositoryService.
                // Although some poly-strings are stored in deltas and refs, they are never used for searched.
                S_MatchingRuleEntry matchingRuleEntry =
                        asc ? filter.gt(item.getRealValue()) : filter.lt(item.getRealValue());
                filter = matchingRuleEntry.or()
                        .block()
                        .item(orderByPath).eq(item.getRealValue())
                        .and()
                        .item(AuditEventRecordType.F_REPO_ID);
                return (asc ? filter.gt(lastProcessedId) : filter.lt(lastProcessedId))
                        .endBlock()
                        .buildFilter();
            }
        }

        throw new IllegalArgumentException(
                "Shouldn't get here with check in executeSearchObjectsIterative()");
    }

    protected long registerOperationStart(String kind) {
        return registerOperationStart(kind, AuditEventRecordType.class);
    }
}
