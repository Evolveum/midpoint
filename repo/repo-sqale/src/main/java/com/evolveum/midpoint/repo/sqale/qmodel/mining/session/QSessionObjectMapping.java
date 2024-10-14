/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining.session;

import com.evolveum.midpoint.repo.sqale.qmodel.mining.cluster.QClusterObjectMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;

import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType.*;

public class QSessionObjectMapping
        extends QAssignmentHolderMapping<RoleAnalysisSessionType, QSessionData, MSessionObject> {

    public static final String DEFAULT_ALIAS_NAME = "roleAnalysisSession";

    private static QSessionObjectMapping instance;

    public static QSessionObjectMapping getInstance() {
        return Objects.requireNonNull(instance);
    }


    public static QSessionObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instance, repositoryContext)) {
            instance =  new QSessionObjectMapping(repositoryContext);
        }
        return getInstance();
    }

    private QSessionObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QSessionData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleAnalysisSessionType.class, QSessionData.class, repositoryContext);

    }

    @Override
    protected QSessionData newAliasInstance(String alias) {
        return new QSessionData(alias);
    }

    @Override
    public MSessionObject newRowObject() {
        return new MSessionObject();
    }

    @Override
    public @NotNull MSessionObject toRowObjectWithoutFullObject(
            RoleAnalysisSessionType session, JdbcSession jdbcSession) {

        return super.toRowObjectWithoutFullObject(session, jdbcSession);
    }
}
