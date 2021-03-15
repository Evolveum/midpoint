/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

public class AccessCertificationDefinitionSqlTransformer
        extends ObjectSqlTransformer<AccessCertificationDefinitionType,
        QAccessCertificationDefinition, MAccessCertificationDefinition> {

    public AccessCertificationDefinitionSqlTransformer(
            SqlTransformerSupport transformerSupport,
            QAccessCertificationDefinitionMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MAccessCertificationDefinition toRowObjectWithoutFullObject(
            AccessCertificationDefinitionType schemaObject, JdbcSession jdbcSession) {
        MAccessCertificationDefinition row =
                super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.handlerUriId = processCacheableUri(schemaObject.getHandlerUri(), jdbcSession);
        row.lastCampaignStartedTimestamp =
                MiscUtil.asInstant(schemaObject.getLastCampaignStartedTimestamp());
        row.lastCampaignClosedTimestamp =
                MiscUtil.asInstant(schemaObject.getLastCampaignClosedTimestamp());
        setReference(schemaObject.getOwnerRef(), jdbcSession,
                o -> row.ownerRefTargetOid = o,
                t -> row.ownerRefTargetType = t,
                r -> row.ownerRefRelationId = r);

        return row;
    }
}
