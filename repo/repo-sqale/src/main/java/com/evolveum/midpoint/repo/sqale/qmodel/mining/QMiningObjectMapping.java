/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.mining;


import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType.*;

public class QMiningObjectMapping
        extends QAssignmentHolderMapping<MiningType, QMiningData, MMiningObject> {

    public static final String DEFAULT_ALIAS_NAME = "mining";

    public static QMiningObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QMiningObjectMapping(repositoryContext);
    }

    private QMiningObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QMiningData.TABLE_NAME, DEFAULT_ALIAS_NAME,
                MiningType.class, QMiningData.class, repositoryContext);

        addItemMapping(F_IDENTIFIER, stringMapper(q -> q.identifier));
        addItemMapping(F_RISK_LEVEL, stringMapper(q -> q.riskLevel));
        addItemMapping(F_ROLES, multiStringMapper(q -> q.roles));
        addItemMapping(F_MEMBERS, multiStringMapper(q -> q.members));
        addItemMapping(F_SIMILAR_GROUPS_ID, multiStringMapper(q -> q.similarGroups));
        addItemMapping(F_ROLES_COUNT, integerMapper(q -> q.rolesCount));
        addItemMapping(F_MEMBERS_COUNT, integerMapper(q -> q.membersCount));
        addItemMapping(F_SIMILAR_GROUPS_COUNT, integerMapper(q -> q.similarGroupsCount));

    }

    @Override
    protected QMiningData newAliasInstance(String alias) {
        return new QMiningData(alias);
    }

    @Override
    public MMiningObject newRowObject() {
        return new MMiningObject();
    }

    @Override
    public @NotNull MMiningObject toRowObjectWithoutFullObject(
            MiningType miningObject, JdbcSession jdbcSession) {
        MMiningObject row = super.toRowObjectWithoutFullObject(miningObject, jdbcSession);

        row.identifier = miningObject.getIdentifier();
        row.riskLevel = miningObject.getRiskLevel();
        row.rolesCount = miningObject.getRolesCount();
        row.membersCount = miningObject.getMembersCount();
        row.similarGroupsCount = miningObject.getSimilarGroupsCount();
        row.roles = stringsToArray(miningObject.getRoles());
        row.members = stringsToArray(miningObject.getMembers());
        row.similarGroups = stringsToArray(miningObject.getSimilarGroupsId());

        return row;
    }
}
