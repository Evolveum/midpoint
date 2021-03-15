/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

public class TriggerSqlTransformer
        extends ContainerSqlTransformer<TriggerType, QTrigger, MTrigger> {

    public TriggerSqlTransformer(
            SqlTransformerSupport transformerSupport, QTriggerMapping mapping) {
        super(transformerSupport, mapping);
    }

    public void insert(TriggerType schemaObject, MObject ownerRow, JdbcSession jdbcSession) {
        MTrigger row = initRowObject(schemaObject, ownerRow.oid);

        row.handlerUriId = resolveUriToId(schemaObject.getHandlerUri());
        row.timestampValue = MiscUtil.asInstant(schemaObject.getTimestamp());

        insert(row, jdbcSession);
    }
}
