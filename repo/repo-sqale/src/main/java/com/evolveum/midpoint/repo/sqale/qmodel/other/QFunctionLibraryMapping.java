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
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;

/**
 * Mapping between {@link QFunctionLibrary} and {@link FunctionLibraryType}.
 */
public class QFunctionLibraryMapping
        extends QObjectMapping<FunctionLibraryType, QFunctionLibrary, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "flib";

    public static final QFunctionLibraryMapping INSTANCE = new QFunctionLibraryMapping();

    private QFunctionLibraryMapping() {
        super(QFunctionLibrary.TABLE_NAME, DEFAULT_ALIAS_NAME,
                FunctionLibraryType.class, QFunctionLibrary.class);
    }

    @Override
    protected QFunctionLibrary newAliasInstance(String alias) {
        return new QFunctionLibrary(alias);
    }

    @Override
    public ObjectSqlTransformer<FunctionLibraryType, QFunctionLibrary, MObject>
    createTransformer(SqlTransformerSupport transformerSupport) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
