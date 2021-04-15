/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ContainerSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

public class LookupTableRowSqlTransformer
        extends ContainerSqlTransformer<LookupTableRowType, QLookupTableRow, MLookupTableRow> {

    public LookupTableRowSqlTransformer(
            SqlTransformerSupport transformerSupport, QLookupTableRowMapping mapping) {
        super(transformerSupport, mapping);
    }

    public void insert(LookupTableRowType lookupTableRow,
            MLookupTable ownerRow, JdbcSession jdbcSession) {

        MLookupTableRow row = initRowObject(lookupTableRow, ownerRow.oid);
        row.key = lookupTableRow.getKey();
        row.value = lookupTableRow.getValue();
        setPolyString(lookupTableRow.getLabel(), o -> row.labelOrig = o, n -> row.labelNorm = n);
        row.lastChangeTimestamp = MiscUtil.asInstant(lookupTableRow.getLastChangeTimestamp());

        insert(row, jdbcSession);
    }
}
