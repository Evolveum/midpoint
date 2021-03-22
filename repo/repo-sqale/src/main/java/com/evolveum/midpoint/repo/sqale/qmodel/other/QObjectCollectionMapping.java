/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.other;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

/**
 * Mapping between {@link QObjectCollection} and {@link ObjectCollectionType}.
 */
public class QObjectCollectionMapping
        extends QObjectMapping<ObjectCollectionType, QObjectCollection, MObject> {

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
    public ObjectSqlTransformer<ObjectCollectionType, QObjectCollection, MObject>
    createTransformer(SqlTransformerSupport transformerSupport) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
