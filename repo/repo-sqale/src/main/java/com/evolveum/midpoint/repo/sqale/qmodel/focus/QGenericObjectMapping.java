/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;

/**
 * Mapping between {@link QGenericObject} and {@link GenericObjectType}.
 */
public class QGenericObjectMapping
        extends QFocusMapping<GenericObjectType, QGenericObject, MGenericObject> {

    public static final String DEFAULT_ALIAS_NAME = "go";

    public static final QGenericObjectMapping INSTANCE = new QGenericObjectMapping();

    private QGenericObjectMapping() {
        super(QGenericObject.TABLE_NAME, DEFAULT_ALIAS_NAME,
                GenericObjectType.class, QGenericObject.class);
    }

    @Override
    protected QGenericObject newAliasInstance(String alias) {
        return new QGenericObject(alias);
    }

    @Override
    public GenericObjectSqlTransformer createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new GenericObjectSqlTransformer(transformerSupport, this);
    }

    @Override
    public MGenericObject newRowObject() {
        return new MGenericObject();
    }
}
