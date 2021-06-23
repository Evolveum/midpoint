/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType.F_ROW;

import java.util.List;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * Mapping between {@link QLookupTable} and {@link LookupTableType}.
 */
public class QLookupTableMapping
        extends QAssignmentHolderMapping<LookupTableType, QLookupTable, MLookupTable> {

    public static final String DEFAULT_ALIAS_NAME = "lt";

    public static QLookupTableMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QLookupTableMapping(repositoryContext);
    }

    private QLookupTableMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QLookupTable.TABLE_NAME, DEFAULT_ALIAS_NAME,
                LookupTableType.class, QLookupTable.class, repositoryContext);

        addContainerTableMapping(F_ROW,
                QLookupTableRowMapping.init(repositoryContext),
                joinOn((o, t) -> o.oid.eq(t.ownerOid)));
    }

    @Override
    protected QLookupTable newAliasInstance(String alias) {
        return new QLookupTable(alias);
    }

    @Override
    public MLookupTable newRowObject() {
        return new MLookupTable();
    }

    @Override
    public void storeRelatedEntities(
            @NotNull MLookupTable lookupTable,
            @NotNull LookupTableType schemaObject,
            @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(lookupTable, schemaObject, jdbcSession);

        List<LookupTableRowType> rows = schemaObject.getRow();
        if (!rows.isEmpty()) {
            rows.forEach(row ->
                    QLookupTableRowMapping.get().insert(row, lookupTable, jdbcSession));
        }
    }
}
