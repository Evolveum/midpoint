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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * Mapping between {@link QValuePolicy} and {@link ValuePolicyType}.
 */
public class QValuePolicyMapping
        extends QAssignmentHolderMapping<ValuePolicyType, QValuePolicy, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "vp";

    public static QValuePolicyMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QValuePolicyMapping(repositoryContext);
    }

    private QValuePolicyMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QValuePolicy.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ValuePolicyType.class, QValuePolicy.class, repositoryContext);
    }

    @Override
    protected QValuePolicy newAliasInstance(String alias) {
        return new QValuePolicy(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
