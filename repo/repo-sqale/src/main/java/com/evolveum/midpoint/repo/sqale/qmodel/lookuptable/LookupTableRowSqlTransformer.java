/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;

public class LookupTableRowSqlTransformer
        extends SqaleTransformerBase<LookupTableRowType, QLookupTableRow, MLookupTableRow> {

    public LookupTableRowSqlTransformer(
            SqlTransformerSupport transformerSupport, QLookupTableRowMapping mapping) {
        super(transformerSupport, mapping);
    }

    // TODO ROWS
}
