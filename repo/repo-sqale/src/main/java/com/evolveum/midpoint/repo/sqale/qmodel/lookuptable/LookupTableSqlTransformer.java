/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class LookupTableSqlTransformer
        extends ObjectSqlTransformer<LookupTableType, QLookupTable, MLookupTable> {

    public LookupTableSqlTransformer(
            SqlTransformerSupport transformerSupport, QLookupTableMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public void storeRelatedEntities(
            @NotNull MLookupTable lookupTable,
            @NotNull LookupTableType schemaObject,
            @NotNull JdbcSession jdbcSession) {
        super.storeRelatedEntities(lookupTable, schemaObject, jdbcSession);

        List<LookupTableRowType> rows = schemaObject.getRow();
        if (!rows.isEmpty()) {
            LookupTableRowSqlTransformer transformer =
                    QLookupTableRowMapping.INSTANCE.createTransformer(transformerSupport);
            rows.forEach(row -> transformer.insert(row, lookupTable, jdbcSession));
        }
    }
}
