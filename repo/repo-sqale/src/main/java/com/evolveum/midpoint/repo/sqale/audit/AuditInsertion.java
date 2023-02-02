/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.dml.DefaultMapper;
import com.querydsl.sql.dml.SQLInsertClause;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.*;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordCustomColumnPropertyType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceValueType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Throw-away object realizing DB insertion of a single {@link AuditEventRecordType}
 * and all related subentities.
 * As this is low-level insert using `*Type` classes, it does not clean up delta execution results.
 */
public class AuditInsertion {

    private final AuditEventRecordType record;
    private final JdbcSession jdbcSession;
    private final SqaleRepoContext repoContext;
    private final boolean escapeIllegalCharacters;
    private final Trace logger;

    public AuditInsertion(AuditEventRecordType record,
            JdbcSession jdbcSession,
            SqaleRepoContext repoContext,
            boolean escapeIllegalCharacters,
            Trace logger) {
        this.record = record;
        this.jdbcSession = jdbcSession;
        this.repoContext = repoContext;
        this.escapeIllegalCharacters = escapeIllegalCharacters;
        this.logger = logger;
    }

    /**
     * Traditional Sqale "insert root first, then insert children" is not optimal here, because
     * to insert root we need to collect some information from children anyway (namely deltas).
     * So we prepare the subentities in collections, gather the needed information
     * (e.g. changed item paths) and then insert root entity.
     * Then we can insert subentities as well.
     */
    public void execute() {
        MAuditEventRecord recordRow = QAuditEventRecordMapping.get().toRowObject(record);

        Collection<MAuditDelta> deltaRows = prepareDeltas(record.getDelta());
        recordRow.changedItemPaths = collectChangedItemPaths(record.getDelta());

        MAuditEventRecord auditRow = insertAuditEventRecord(recordRow);
        record.setRepoId(auditRow.id);

        insertAuditDeltas(auditRow, deltaRows);
        insertReferences(auditRow, record.getReference());
    }

    private MAuditEventRecord insertAuditEventRecord(MAuditEventRecord row) {
        QAuditEventRecordMapping aerMapping = QAuditEventRecordMapping.get();
        QAuditEventRecord aer = aerMapping.defaultAlias();
        SQLInsertClause insert = jdbcSession.newInsert(aer).populate(row);

        // Custom columns, this better be replaced by some extension container later
        Map<String, ColumnMetadata> customColumns = aerMapping.getExtensionColumns();
        for (AuditEventRecordCustomColumnPropertyType property : record.getCustomColumnProperty()) {
            if (!customColumns.containsKey(property.getName())) {
                throw new IllegalArgumentException("Audit event record table doesn't"
                        + " contains column for property " + property.getName());
            }
            // Like insert.set, but that one is too parameter-type-safe for our generic usage here.
            insert.columns(aer.getPath(property.getName())).values(property.getValue());
        }

        Long returnedId = insert.executeWithKey(aer.id);
        // If returned ID is null, it was likely provided, so we use that one.
        row.id = returnedId != null ? returnedId : record.getRepoId();
        return row;
    }

    private Collection<MAuditDelta> prepareDeltas(List<ObjectDeltaOperationType> deltas) {
        // we want to keep only unique deltas, checksum is also part of PK
        Map<String, MAuditDelta> deltasByChecksum = new HashMap<>();
        for (ObjectDeltaOperationType deltaOperation : deltas) {
            if (deltaOperation == null) {
                continue;
            }

            MAuditDelta mAuditDelta = convertDelta(deltaOperation);
            if (mAuditDelta != null) {
                deltasByChecksum.put(mAuditDelta.checksum, mAuditDelta);
            }
        }
        return deltasByChecksum.values();
    }

    /**
     * Returns prepared audit delta row without PK columns which will be added later.
     * For normal repo this code would be in mapper, but here we know exactly what type we work with.
     */
    private @Nullable MAuditDelta convertDelta(ObjectDeltaOperationType deltaOperation) {
        try {
            MAuditDelta deltaRow = new MAuditDelta();
            ObjectDeltaType delta = deltaOperation.getObjectDelta();
            if (delta != null) {
                DeltaConversionOptions options =
                        DeltaConversionOptions.createSerializeReferenceNames();
                options.setEscapeInvalidCharacters(escapeIllegalCharacters);
                String serializedDelta = DeltaConvertor.serializeDelta(
                        delta, options, repoContext.getJdbcRepositoryConfiguration().getFullObjectFormat());

                // serializedDelta is transient, needed for changed items later
                deltaRow.serializedDelta = serializedDelta;
                deltaRow.delta = serializedDelta.getBytes(StandardCharsets.UTF_8);
                deltaRow.deltaOid = SqaleUtils.oidToUuid(delta.getOid());
                deltaRow.deltaType = delta.getChangeType();
            }

            OperationResultType executionResult = deltaOperation.getExecutionResult();
            byte[] fullResult = null;
            if (executionResult != null) {
                fullResult = repoContext.createFullResult(executionResult);

                deltaRow.status = executionResult.getStatus();
                deltaRow.fullResult = fullResult;
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
            deltaRow.checksum = computeChecksum(deltaRow.delta, fullResult);
            return deltaRow;
        } catch (Exception ex) {
            logger.warn("Unexpected problem during audit delta conversion", ex);
            return null;
        }
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

    /**
     * Returns distinct collected changed item paths, or null, never an empty array.
     */
    private String[] collectChangedItemPaths(List<ObjectDeltaOperationType> deltaOperations) {
        Set<String> changedItemPaths = new HashSet<>();
        for (ObjectDeltaOperationType deltaOperation : deltaOperations) {
            ObjectDeltaType delta = deltaOperation.getObjectDelta();
            for (ItemDeltaType itemDelta : delta.getItemDelta()) {
                ItemPath path = itemDelta.getPath().getItemPath();
                CanonicalItemPath canonical = repoContext.prismContext()
                        .createCanonicalItemPath(path, delta.getObjectType());
                for (int i = 0; i < canonical.size(); i++) {
                    changedItemPaths.add(canonical.allUpToIncluding(i).asString());
                }
            }
        }
        return changedItemPaths.isEmpty() ? null : changedItemPaths.toArray(String[]::new);
    }

    private void insertAuditDeltas(MAuditEventRecord auditRow, Collection<MAuditDelta> deltaRows) {
        if (deltaRows != null && !deltaRows.isEmpty()) {
            SQLInsertClause insertBatch = jdbcSession.newInsert(
                    QAuditDeltaMapping.get().defaultAlias());
            for (MAuditDelta deltaRow : deltaRows) {
                deltaRow.recordId = auditRow.id;
                deltaRow.timestamp = auditRow.timestamp;

                // NULLs are important to keep the value count consistent during the batch
                insertBatch.populate(deltaRow, DefaultMapper.WITH_NULL_BINDINGS).addBatch();
            }
            insertBatch.setBatchToBulk(true);
            insertBatch.execute();
        }
    }

    private void insertReferences(MAuditEventRecord auditRow, List<AuditEventRecordReferenceType> references) {
        if (references.isEmpty()) {
            return;
        }

        QAuditRefValue qr = QAuditRefValueMapping.get().defaultAlias();
        SQLInsertClause insertBatch = jdbcSession.newInsert(qr);
        for (AuditEventRecordReferenceType refSet : references) {
            for (AuditEventRecordReferenceValueType refValue : refSet.getValue()) {
                // id will be generated, but we're not interested in those here
                PolyStringType targetName = refValue.getTargetName();
                insertBatch.set(qr.recordId, auditRow.id)
                        .set(qr.timestamp, auditRow.timestamp)
                        .set(qr.name, refSet.getName())
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
}
