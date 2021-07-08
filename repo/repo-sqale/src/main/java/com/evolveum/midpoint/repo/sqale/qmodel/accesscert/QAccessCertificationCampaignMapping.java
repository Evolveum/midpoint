/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.*;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

/**
 * Mapping between {@link QAccessCertificationCampaign}
 * and {@link AccessCertificationCampaignType}.
 */
public class QAccessCertificationCampaignMapping
        extends QAssignmentHolderMapping<AccessCertificationCampaignType,
        QAccessCertificationCampaign, MAccessCertificationCampaign> {

    public static final String DEFAULT_ALIAS_NAME = "acc";

    public static QAccessCertificationCampaignMapping init(
            @NotNull SqaleRepoContext repositoryContext) {
        return new QAccessCertificationCampaignMapping(repositoryContext);
    }

    private QAccessCertificationCampaignMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAccessCertificationCampaign.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AccessCertificationCampaignType.class, QAccessCertificationCampaign.class,
                repositoryContext);

        addItemMapping(F_DEFINITION_REF, refMapper(
                q -> q.definitionRefTargetOid,
                q -> q.definitionRefTargetType,
                q -> q.definitionRefRelationId));
        addItemMapping(F_END_TIMESTAMP,
                timestampMapper(q -> q.endTimestamp));
        addItemMapping(F_HANDLER_URI, uriMapper(q -> q.handlerUriId));
        // TODO
        addItemMapping(F_ITERATION, integerMapper(q -> q.campaignIteration));
        addItemMapping(F_OWNER_REF, refMapper(
                q -> q.ownerRefTargetOid,
                q -> q.ownerRefTargetType,
                q -> q.ownerRefRelationId));
        addItemMapping(F_STAGE_NUMBER, integerMapper(q -> q.stageNumber));
        addItemMapping(F_START_TIMESTAMP,
                timestampMapper(q -> q.startTimestamp));
        addItemMapping(F_STATE, enumMapper(q -> q.state));

        addContainerTableMapping(F_CASE,
                QAccessCertificationCaseMapping.init(repositoryContext),
                joinOn((o, acase) -> o.oid.eq(acase.ownerOid)));
    }

    @Override
    protected QAccessCertificationCampaign newAliasInstance(String alias) {
        return new QAccessCertificationCampaign(alias);
    }

    @Override
    public MAccessCertificationCampaign newRowObject() {
        return new MAccessCertificationCampaign();
    }

    @Override
    public @NotNull MAccessCertificationCampaign toRowObjectWithoutFullObject(
            AccessCertificationCampaignType schemaObject, JdbcSession jdbcSession) {
        MAccessCertificationCampaign row =
                super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        setReference(schemaObject.getDefinitionRef(),
                o -> row.definitionRefTargetOid = o,
                t -> row.definitionRefTargetType = t,
                r -> row.definitionRefRelationId = r);
        row.endTimestamp =
                MiscUtil.asInstant(schemaObject.getEndTimestamp());
        row.handlerUriId = processCacheableUri(schemaObject.getHandlerUri());
        // TODO
        row.campaignIteration = schemaObject.getIteration();
        setReference(schemaObject.getOwnerRef(),
                o -> row.ownerRefTargetOid = o,
                t -> row.ownerRefTargetType = t,
                r -> row.ownerRefRelationId = r);
        row.stageNumber = schemaObject.getStageNumber();
        row.startTimestamp =
                MiscUtil.asInstant(schemaObject.getStartTimestamp());
        row.state = schemaObject.getState();

        return row;
    }

    @Override
    public void storeRelatedEntities(
            @NotNull MAccessCertificationCampaign row, @NotNull AccessCertificationCampaignType schemaObject,
            @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        List<AccessCertificationCaseType> cases = schemaObject.getCase();
        if (!cases.isEmpty()) {
            for (AccessCertificationCaseType c : cases) {
                QAccessCertificationCaseMapping.get().insert(c, row, jdbcSession);
            }
        }
    }
}
