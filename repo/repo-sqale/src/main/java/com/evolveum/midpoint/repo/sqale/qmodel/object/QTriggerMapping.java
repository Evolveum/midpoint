/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * Mapping between {@link QTrigger} and {@link TriggerType}.
 *
 * @param <OR> type of the owner row
 */
public class QTriggerMapping<OR extends MObject>
        extends QContainerMapping<TriggerType, QTrigger<OR>, MTrigger, OR> {

    public static final String DEFAULT_ALIAS_NAME = "trg";

    private static QTriggerMapping<?> instance;

    public static <OR extends MObject> QTriggerMapping<OR> init(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance = new QTriggerMapping<>(repositoryContext);
        }
        return get();
    }

    public static <OR extends MObject> QTriggerMapping<OR> get() {
        //noinspection unchecked
        return (QTriggerMapping<OR>) Objects.requireNonNull(instance);
    }

    // We can't declare Class<QTrigger<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QTriggerMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QTrigger.TABLE_NAME, DEFAULT_ALIAS_NAME,
                TriggerType.class, (Class) QTrigger.class, repositoryContext);

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QObjectMapping::getObjectMapping,
                        (q, p) -> q.ownerOid.eq(p.oid)));

        addItemMapping(TriggerType.F_HANDLER_URI, uriMapper(q -> q.handlerUriId));
        addItemMapping(TriggerType.F_TIMESTAMP, timestampMapper(q -> q.timestamp));
    }

    @Override
    protected QTrigger<OR> newAliasInstance(String alias) {
        return new QTrigger<>(alias);
    }

    @Override
    public MTrigger newRowObject() {
        return new MTrigger();
    }

    @Override
    public MTrigger newRowObject(OR ownerRow) {
        MTrigger row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    public MTrigger insert(TriggerType schemaObject, OR ownerRow, JdbcSession jdbcSession) {
        MTrigger row = initRowObject(schemaObject, ownerRow);

        row.handlerUriId = processCacheableUri(schemaObject.getHandlerUri());
        row.timestamp = MiscUtil.asInstant(schemaObject.getTimestamp());

        insert(row, jdbcSession);
        return row;
    }
}
