/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAccessCertificationDefinitionType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

/**
 * Mapping between {@link QAccessCertificationDefinition}
 * and {@link AccessCertificationDefinitionType}.
 */
public class QAccessCertificationDefinitionMapping
        extends QAssignmentHolderMapping<AccessCertificationDefinitionType,
        QAccessCertificationDefinition, MAccessCertificationDefinition> {

    public static final String DEFAULT_ALIAS_NAME = "acd";

    private static QAccessCertificationDefinitionMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAccessCertificationDefinitionMapping init(
            @NotNull SqaleRepoContext repositoryContext) {
        instance = new QAccessCertificationDefinitionMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAccessCertificationDefinitionMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAccessCertificationDefinitionMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAccessCertificationDefinition.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AccessCertificationDefinitionType.class, QAccessCertificationDefinition.class,
                repositoryContext);

        addItemMapping(F_HANDLER_URI, uriMapper(q -> q.handlerUriId));
        addItemMapping(F_LAST_CAMPAIGN_STARTED_TIMESTAMP,
                timestampMapper(q -> q.lastCampaignStartedTimestamp));
        addItemMapping(F_LAST_CAMPAIGN_CLOSED_TIMESTAMP,
                timestampMapper(q -> q.lastCampaignClosedTimestamp));
        addRefMapping(F_OWNER_REF,
                q -> q.ownerRefTargetOid,
                q -> q.ownerRefTargetType,
                q -> q.ownerRefRelationId,
                QUserMapping::getUserMapping);
    }

    @Override
    protected QAccessCertificationDefinition newAliasInstance(String alias) {
        return new QAccessCertificationDefinition(alias);
    }

    @Override
    public MAccessCertificationDefinition newRowObject() {
        return new MAccessCertificationDefinition();
    }

    @Override
    public @NotNull MAccessCertificationDefinition toRowObjectWithoutFullObject(
            AccessCertificationDefinitionType schemaObject, JdbcSession jdbcSession) {
        MAccessCertificationDefinition row =
                super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.handlerUriId = processCacheableUri(schemaObject.getHandlerUri());
        row.lastCampaignStartedTimestamp =
                MiscUtil.asInstant(schemaObject.getLastCampaignStartedTimestamp());
        row.lastCampaignClosedTimestamp =
                MiscUtil.asInstant(schemaObject.getLastCampaignClosedTimestamp());
        setReference(schemaObject.getOwnerRef(),
                o -> row.ownerRefTargetOid = o,
                t -> row.ownerRefTargetType = t,
                r -> row.ownerRefRelationId = r);

        return row;
    }
}
