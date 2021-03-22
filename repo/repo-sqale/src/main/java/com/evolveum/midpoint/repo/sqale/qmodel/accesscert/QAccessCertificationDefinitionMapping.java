/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAccessCertificationDefinitionType.*;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

/**
 * Mapping between {@link QAccessCertificationDefinition}
 * and {@link AccessCertificationDefinitionType}.
 */
public class QAccessCertificationDefinitionMapping
        extends QObjectMapping<AccessCertificationDefinitionType,
        QAccessCertificationDefinition, MAccessCertificationDefinition> {

    public static final String DEFAULT_ALIAS_NAME = "acd";

    public static final QAccessCertificationDefinitionMapping INSTANCE =
            new QAccessCertificationDefinitionMapping();

    private QAccessCertificationDefinitionMapping() {
        super(QAccessCertificationDefinition.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AccessCertificationDefinitionType.class, QAccessCertificationDefinition.class);

        addItemMapping(F_HANDLER_URI, UriItemFilterProcessor.mapper(path(q -> q.handlerUriId)));
        addItemMapping(F_LAST_CAMPAIGN_STARTED_TIMESTAMP,
                TimestampItemFilterProcessor.mapper(path(q -> q.lastCampaignStartedTimestamp)));
        addItemMapping(F_LAST_CAMPAIGN_CLOSED_TIMESTAMP,
                TimestampItemFilterProcessor.mapper(path(q -> q.lastCampaignClosedTimestamp)));
        addItemMapping(F_OWNER_REF, RefItemFilterProcessor.mapper(
                path(q -> q.ownerRefTargetOid),
                path(q -> q.ownerRefTargetType),
                path(q -> q.ownerRefRelationId)));
    }

    @Override
    protected QAccessCertificationDefinition newAliasInstance(String alias) {
        return new QAccessCertificationDefinition(alias);
    }

    @Override
    public AccessCertificationDefinitionSqlTransformer createTransformer(SqlTransformerSupport transformerSupport) {
        return new AccessCertificationDefinitionSqlTransformer(transformerSupport, this);
    }

    @Override
    public MAccessCertificationDefinition newRowObject() {
        return new MAccessCertificationDefinition();
    }
}
