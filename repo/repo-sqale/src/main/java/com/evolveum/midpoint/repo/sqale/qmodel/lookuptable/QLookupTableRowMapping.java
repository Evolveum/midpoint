/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

/**
 * Mapping between {@link QLookupTableRow} and {@link LookupTableRowType}.
 */
public class QLookupTableRowMapping
        extends SqaleModelMapping<LookupTableRowType, QLookupTableRow, MLookupTableRow> {

    public static final String DEFAULT_ALIAS_NAME = "ltr";

    public static final QLookupTableRowMapping INSTANCE = new QLookupTableRowMapping();

    private QLookupTableRowMapping() {
        super(QLookupTableRow.TABLE_NAME, DEFAULT_ALIAS_NAME,
                LookupTableRowType.class, QLookupTableRow.class);

        // TODO map detail table m_lookup_table_row
    }

    @Override
    protected QLookupTableRow newAliasInstance(String alias) {
        return new QLookupTableRow(alias);
    }

    @Override
    public LookupTableRowTransformer createTransformer(
            SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        return new LookupTableRowTransformer(transformerContext, this);
    }

    @Override
    public MLookupTableRow newRowObject() {
        return new MLookupTableRow();
    }
}
