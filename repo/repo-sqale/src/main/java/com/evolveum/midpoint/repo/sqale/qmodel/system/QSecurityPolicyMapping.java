/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.system;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

/**
 * Mapping between {@link QSecurityPolicy} and {@link SecurityPolicyType}.
 */
public class QSecurityPolicyMapping
        extends QObjectMapping<SecurityPolicyType, QSecurityPolicy, MSecurityPolicy> {

    public static final String DEFAULT_ALIAS_NAME = "sp";

    public static final QSecurityPolicyMapping INSTANCE = new QSecurityPolicyMapping();

    private QSecurityPolicyMapping() {
        super(QSecurityPolicy.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SecurityPolicyType.class, QSecurityPolicy.class);
    }

    @Override
    protected QSecurityPolicy newAliasInstance(String alias) {
        return new QSecurityPolicy(alias);
    }

    @Override
    public ObjectSqlTransformer<SecurityPolicyType, QSecurityPolicy, MSecurityPolicy>
    createTransformer(SqlTransformerSupport transformerSupport) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public MSecurityPolicy newRowObject() {
        return new MSecurityPolicy();
    }
}
