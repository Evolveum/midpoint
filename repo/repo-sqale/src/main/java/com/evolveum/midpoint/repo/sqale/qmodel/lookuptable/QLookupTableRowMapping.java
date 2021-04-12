/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType.*;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

/**
 * Mapping between {@link QLookupTableRow} and {@link LookupTableRowType}.
 */
public class QLookupTableRowMapping
        extends SqaleTableMapping<LookupTableRowType, QLookupTableRow, MLookupTableRow> {

    public static final String DEFAULT_ALIAS_NAME = "ltr";

    public static final QLookupTableRowMapping INSTANCE = new QLookupTableRowMapping();

    private QLookupTableRowMapping() {
        super(QLookupTableRow.TABLE_NAME, DEFAULT_ALIAS_NAME,
                LookupTableRowType.class, QLookupTableRow.class);

        addItemMapping(F_KEY, stringMapper(path(q -> q.rowKey)));
        addItemMapping(F_LABEL, polyStringMapper(
                path(q -> q.labelOrig), path(q -> q.labelNorm)));
        addItemMapping(F_VALUE, stringMapper(path(q -> q.rowValue)));
        addItemMapping(F_LAST_CHANGE_TIMESTAMP,
                timestampMapper(path(q -> q.lastChangeTimestamp)));
    }

    @Override
    protected QLookupTableRow newAliasInstance(String alias) {
        return new QLookupTableRow(alias);
    }

    @Override
    public LookupTableRowSqlTransformer createTransformer(SqlTransformerSupport transformerSupport) {
        return new LookupTableRowSqlTransformer(transformerSupport, this);
    }

    @Override
    public MLookupTableRow newRowObject() {
        return new MLookupTableRow();
    }
}
