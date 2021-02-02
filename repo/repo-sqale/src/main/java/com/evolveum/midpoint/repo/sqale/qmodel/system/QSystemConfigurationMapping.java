/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.system;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Mapping between {@link QSystemConfiguration} and {@link SystemConfigurationType}.
 */
public class QSystemConfigurationMapping
        extends QObjectMapping<SystemConfigurationType, QSystemConfiguration, MSystemConfiguration> {

    public static final String DEFAULT_ALIAS_NAME = "sc";

    public static final QSystemConfigurationMapping INSTANCE = new QSystemConfigurationMapping();

    private QSystemConfigurationMapping() {
        super(QSystemConfiguration.TABLE_NAME, DEFAULT_ALIAS_NAME,
                SystemConfigurationType.class, QSystemConfiguration.class);
    }

    @Override
    protected QSystemConfiguration newAliasInstance(String alias) {
        return new QSystemConfiguration(alias);
    }

    @Override
    public ObjectSqlTransformer<SystemConfigurationType, QSystemConfiguration, MSystemConfiguration>
    createTransformer(SqlTransformerContext transformerContext, SqlRepoContext sqlRepoContext) {
        // no special class needed, no additional columns
        return new ObjectSqlTransformer<>(transformerContext, this);
    }

    @Override
    public MSystemConfiguration newRowObject() {
        return new MSystemConfiguration();
    }
}
