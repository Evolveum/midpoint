/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType.*;

import java.util.*;
import java.util.stream.Collectors;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.task.QTaskMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.ResultListRowTransformer;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
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
        if (needsInitialization(instance, repositoryContext)) {
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
                TableRelationResolver.usingJoin(
                        QObjectMapping::getObjectMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        addItemMapping(F_STATUS, enumMapper(q -> q.status));
        addItemMapping(F_RECORD_TYPE, enumMapper(q -> q.recordType));
        addRefMapping(F_INITIATOR_REF,
                q -> q.initiatorRefTargetOid,
                q -> q.initiatorRefTargetType,
                q -> q.initiatorRefRelationId,
                QFocusMapping::getFocusMapping);
        addRefMapping(F_TASK_REF,
                q -> q.taskRefTargetOid,
                q -> q.taskRefTargetType,
                q -> q.taskRefRelationId,
                QTaskMapping::get);
        addItemMapping(OperationExecutionType.F_TIMESTAMP,
                timestampMapper(q -> q.timestamp));
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
        row.timestamp = MiscUtil.asInstant(schemaObject.getTimestamp());

        insert(row, jdbcSession);
        return row;
    }

    @Override
    public OperationExecutionType toSchemaObject(MOperationExecution row) {
        return new OperationExecutionType()
                .status(row.status)
                .recordType(row.recordType)
                .initiatorRef(objectReference(row.initiatorRefTargetOid,
                        row.initiatorRefTargetType, row.initiatorRefRelationId))
                .taskRef(objectReference(row.taskRefTargetOid,
                        row.taskRefTargetType, row.taskRefRelationId))
                .timestamp(MiscUtil.asXMLGregorianCalendar(row.timestamp));
    }

    @Override
    public ResultListRowTransformer<OperationExecutionType, QOperationExecution<OR>, MOperationExecution> createRowTransformer(
            SqlQueryContext<OperationExecutionType, QOperationExecution<OR>, MOperationExecution> sqlQueryContext,
            JdbcSession jdbcSession) {
        Map<UUID, ObjectType> owners = new HashMap<>();
        return new ResultListRowTransformer<>() {
            @Override
            public void beforeTransformation(List<Tuple> rowTuples, QOperationExecution<OR> entityPath)
                    throws SchemaException {
                Set<UUID> ownerOids = rowTuples.stream()
                        .map(row -> Objects.requireNonNull(row.get(entityPath)).ownerOid)
                        .collect(Collectors.toSet());

                // TODO do we need get options here as well? Is there a scenario where we load container
                //  and define what to load for referenced/owner object?
                QObject<?> o = QObjectMapping.getObjectMapping().defaultAlias();
                List<Tuple> result = jdbcSession.newQuery()
                        .select(o.oid, o.fullObject)
                        .from(o)
                        .where(o.oid.in(ownerOids))
                        .fetch();
                for (Tuple row : result) {
                    UUID oid = Objects.requireNonNull(row.get(o.oid));
                    ObjectType owner = parseSchemaObject(row.get(o.fullObject), oid.toString(), ObjectType.class);
                    owners.put(oid, owner);
                }
            }

            @Override
            public OperationExecutionType transform(Tuple rowTuple,
                    QOperationExecution<OR> entityPath, Collection<SelectorOptions<GetOperationOptions>> options) {
                MOperationExecution row = Objects.requireNonNull(rowTuple.get(entityPath));
                ObjectType object = Objects.requireNonNull(owners.get(row.ownerOid),
                        () -> "Missing owner with OID " + row.ownerOid + " for OperationExecution with ID " + row.cid);

                PrismContainer<OperationExecutionType> opexContainer =
                        object.asPrismObject().findContainer(ObjectType.F_OPERATION_EXECUTION);
                if (opexContainer == null) {
                    throw new SystemException("Object " + object + " has no operation execution as expected from " + row);
                }
                PrismContainerValue<OperationExecutionType> pcv = opexContainer.findValue(row.cid);
                if (pcv == null) {
                    throw new SystemException("Object " + object + " has no operation execution with ID " + row.cid);
                }
                return pcv.asContainerable();
            }
        };
    }
}
