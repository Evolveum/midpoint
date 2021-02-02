/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import com.evolveum.midpoint.repo.sqale.qbean.MLookupTable;
import com.evolveum.midpoint.repo.sqale.qmodel.QLookupTable;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * Mapping between {@link QLookupTable} and {@link LookupTableType}.
 */
public class QLookupTableMapping
        extends QObjectMapping<LookupTableType, QLookupTable, MLookupTable> {

    public static final String DEFAULT_ALIAS_NAME = "lt";

    public static final QLookupTableMapping INSTANCE = new QLookupTableMapping();

    private QLookupTableMapping() {
        super(QLookupTable.TABLE_NAME, DEFAULT_ALIAS_NAME,
                LookupTableType.class, QLookupTable.class);

        // TODO map detail table m_lookup_table_row
    }

    @Override
    protected QLookupTable newAliasInstance(String alias) {
        return new QLookupTable(alias);
    }

    @Override
    public ObjectSqlTransformer<LookupTableType, QLookupTable, MLookupTable>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // TODO transformer needed to cover m_lookup_table_row
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MLookupTable newRowObject() {
        return new MLookupTable();
    }
}
