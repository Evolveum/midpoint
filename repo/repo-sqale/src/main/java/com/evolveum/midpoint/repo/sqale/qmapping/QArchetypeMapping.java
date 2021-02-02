/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import com.evolveum.midpoint.repo.sqale.qbean.MArchetype;
import com.evolveum.midpoint.repo.sqale.qmodel.QArchetype;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

/**
 * Mapping between {@link QArchetype} and {@link ArchetypeType}.
 */
public class QArchetypeMapping
        extends QAbstractRoleMapping<ArchetypeType, QArchetype, MArchetype> {

    public static final String DEFAULT_ALIAS_NAME = "a";

    public static final QArchetypeMapping INSTANCE = new QArchetypeMapping();

    private QArchetypeMapping() {
        super(QArchetype.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ArchetypeType.class, QArchetype.class);
    }

    @Override
    protected QArchetype newAliasInstance(String alias) {
        return new QArchetype(alias);
    }

    @Override
    public ObjectSqlTransformer<ArchetypeType, QArchetype, MArchetype>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MArchetype newRowObject() {
        return new MArchetype();
    }
}
