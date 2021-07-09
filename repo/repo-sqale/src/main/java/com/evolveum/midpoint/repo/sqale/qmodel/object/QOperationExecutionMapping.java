/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;

/**
 * Mapping between {@link QOperationExecution} and {@link OperationExecutionType}.
 *
 * @param <OR> type of the owner row
 */
public class QOperationExecutionMapping<OR extends MObject>
        extends QContainerMapping<OperationExecutionType, QOperationExecution<OR>, MOperationExecution, OR> {

    public static final String DEFAULT_ALIAS_NAME = "opex";

    private static QOperationExecutionMapping<?> instance;

    public static <OR extends MObject> QOperationExecutionMapping<OR> init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (instance == null) {
            instance = new QOperationExecutionMapping<>(repositoryContext);
        }
        return get();
    }

    public static <OR extends MObject> QOperationExecutionMapping<OR> get() {
        //noinspection unchecked
        return (QOperationExecutionMapping<OR>) Objects.requireNonNull(instance);
    }

    // We can't declare Class<QOperationExecution<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QOperationExecutionMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QOperationExecution.TABLE_NAME, DEFAULT_ALIAS_NAME,
                OperationExecutionType.class, (Class) QOperationExecution.class, repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                new TableRelationResolver<>(QObjectMapping::getObjectMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        addItemMapping(F_STATUS, enumMapper(q -> q.status));
        addItemMapping(F_RECORD_TYPE, enumMapper(q -> q.recordType));
        addItemMapping(F_INITIATOR_REF, refMapper(
                q -> q.initiatorRefTargetOid,
                q -> q.initiatorRefTargetType,
                q -> q.initiatorRefRelationId));
        addItemMapping(F_TASK_REF, refMapper(
                q -> q.taskRefTargetOid,
                q -> q.taskRefTargetType,
                q -> q.taskRefRelationId));
        addItemMapping(OperationExecutionType.F_TIMESTAMP,
                timestampMapper(q -> q.timestampValue));
    }

    @Override
    protected QOperationExecution<OR> newAliasInstance(String alias) {
        return new QOperationExecution<>(alias);
    }

    @Override
    public MOperationExecution newRowObject() {
        return new MOperationExecution();
    }

    @Override
    public MOperationExecution newRowObject(OR ownerRow) {
        MOperationExecution row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    public MOperationExecution insert(
            OperationExecutionType schemaObject, OR ownerRow, JdbcSession jdbcSession) {
        MOperationExecution row = initRowObject(schemaObject, ownerRow);

        row.status = schemaObject.getStatus();
        row.recordType = schemaObject.getRecordType();
        setReference(schemaObject.getInitiatorRef(),
                o -> row.initiatorRefTargetOid = o,
                t -> row.initiatorRefTargetType = t,
                r -> row.initiatorRefRelationId = r);
        setReference(schemaObject.getTaskRef(),
                o -> row.taskRefTargetOid = o,
                t -> row.taskRefTargetType = t,
                r -> row.taskRefRelationId = r);
        row.timestampValue = MiscUtil.asInstant(schemaObject.getTimestamp());

        insert(row, jdbcSession);
        return row;
    }
}
