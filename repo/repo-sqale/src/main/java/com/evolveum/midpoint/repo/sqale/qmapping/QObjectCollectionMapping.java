/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import com.evolveum.midpoint.repo.sqale.qbean.MObjectCollection;
import com.evolveum.midpoint.repo.sqale.qmodel.QObjectCollection;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

/**
 * Mapping between {@link QObjectCollection} and {@link ObjectCollectionType}.
 */
public class QObjectCollectionMapping
        extends QObjectMapping<ObjectCollectionType, QObjectCollection, MObjectCollection> {

    public static final String DEFAULT_ALIAS_NAME = "oc";

    public static final QObjectCollectionMapping INSTANCE = new QObjectCollectionMapping();

    private QObjectCollectionMapping() {
        super(QObjectCollection.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectCollectionType.class, QObjectCollection.class);
    }

    @Override
    protected QObjectCollection newAliasInstance(String alias) {
        return new QObjectCollection(alias);
    }

    @Override
    public ObjectSqlTransformer<ObjectCollectionType, QObjectCollection, MObjectCollection>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MObjectCollection newRowObject() {
        return new MObjectCollection();
    }
}
