/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.other;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;

/**
 * Mapping between {@link QDashboard} and {@link DashboardType}.
 */
public class QDashboardMapping
        extends QAssignmentHolderMapping<DashboardType, QDashboard, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "d";

    public static QDashboardMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QDashboardMapping(repositoryContext);
    }

    private QDashboardMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QDashboard.TABLE_NAME, DEFAULT_ALIAS_NAME,
                DashboardType.class, QDashboard.class, repositoryContext);
    }

    @Override
    protected QDashboard newAliasInstance(String alias) {
        return new QDashboard(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
