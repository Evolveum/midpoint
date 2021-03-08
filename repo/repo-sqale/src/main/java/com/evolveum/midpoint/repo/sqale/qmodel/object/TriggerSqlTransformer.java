/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

public class TriggerSqlTransformer
        extends ContainerSqlTransformer<TriggerType, QTrigger, MTrigger> {

    public TriggerSqlTransformer(
            SqlTransformerContext transformerContext, QTriggerMapping mapping) {
        super(transformerContext, mapping);
    }

    @Override
    public MTrigger toRowObject(TriggerType schemaObject, JdbcSession jdbcSession) {
        MTrigger row = super.toRowObject(schemaObject, jdbcSession);
        row.handlerUriId = processCachedUri(schemaObject.getHandlerUri(), jdbcSession);
        row.timestampValue = MiscUtil.asInstant(schemaObject.getTimestamp());
        return row;
    }
}
