/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.system;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

/**
 * Mapping between {@link QSecurityPolicy} and {@link SecurityPolicyType}.
 */
public class QSecurityPolicyMapping
        extends QAssignmentHolderMapping<SecurityPolicyType, QSecurityPolicy, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "sp";

    public static QSecurityPolicyMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QSecurityPolicyMapping(repositoryContext);
    }

    private QSecurityPolicyMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSecurityPolicy.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SecurityPolicyType.class, QSecurityPolicy.class, repositoryContext);
    }

    @Override
    protected QSecurityPolicy newAliasInstance(String alias) {
        return new QSecurityPolicy(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
