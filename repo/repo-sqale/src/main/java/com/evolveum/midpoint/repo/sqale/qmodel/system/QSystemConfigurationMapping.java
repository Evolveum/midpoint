/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.system;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Mapping between {@link QSystemConfiguration} and {@link SystemConfigurationType}.
 */
public class QSystemConfigurationMapping
        extends QAssignmentHolderMapping<SystemConfigurationType, QSystemConfiguration, MObject> {

    public static final String DEFAULT_ALIAS_NAME = "sc";

    public static QSystemConfigurationMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QSystemConfigurationMapping(repositoryContext);
    }

    private QSystemConfigurationMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSystemConfiguration.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SystemConfigurationType.class, QSystemConfiguration.class, repositoryContext);
    }

    @Override
    protected QSystemConfiguration newAliasInstance(String alias) {
        return new QSystemConfiguration(alias);
    }

    @Override
    public MObject newRowObject() {
        return new MObject();
    }
}
