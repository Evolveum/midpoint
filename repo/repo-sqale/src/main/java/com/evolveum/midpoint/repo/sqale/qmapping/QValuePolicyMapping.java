/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmapping;

import com.evolveum.midpoint.repo.sqale.qbean.MValuePolicy;
import com.evolveum.midpoint.repo.sqale.qmodel.QValuePolicy;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * Mapping between {@link QValuePolicy} and {@link ValuePolicyType}.
 */
public class QValuePolicyMapping
        extends QObjectMapping<ValuePolicyType, QValuePolicy, MValuePolicy> {

    public static final String DEFAULT_ALIAS_NAME = "vp";

    public static final QValuePolicyMapping INSTANCE = new QValuePolicyMapping();

    private QValuePolicyMapping() {
        super(QValuePolicy.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ValuePolicyType.class, QValuePolicy.class);
    }

    @Override
    protected QValuePolicy newAliasInstance(String alias) {
        return new QValuePolicy(alias);
    }

    @Override
    public ObjectSqlTransformer<ValuePolicyType, QValuePolicy, MValuePolicy>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MValuePolicy newRowObject() {
        return new MValuePolicy();
    }
}
