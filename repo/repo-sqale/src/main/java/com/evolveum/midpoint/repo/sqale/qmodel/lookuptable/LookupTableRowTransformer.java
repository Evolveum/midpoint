/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleTransformerBase;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class LookupTableRowTransformer
        extends SqaleTransformerBase<LookupTableRowType, QLookupTableRow, MLookupTableRow> {

    public LookupTableRowTransformer(
            SqlTransformerContext transformerContext, QLookupTableRowMapping mapping) {
        super(transformerContext, mapping);
    }

    @Override
    public LookupTableRowType toSchemaObject(MLookupTableRow row) {
        LookupTableRowType ltr = new LookupTableRowType()
                .id((long) row.cid)
                .key(row.rowKey)
                .value(row.rowValue)
                .lastChangeTimestamp(MiscUtil.asXMLGregorianCalendar(row.lastChangeTimestamp));
        if (row.labelOrig != null || row.labelNorm != null) {
            ltr.setLabel(new PolyStringType(
                    new PolyString(row.labelOrig, row.labelNorm)));
        }
        return ltr;
    }
}
