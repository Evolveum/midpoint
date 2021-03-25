/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.shadow;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ShadowSqlTransformer
        extends ObjectSqlTransformer<ShadowType, QShadow, MShadow> {

    public ShadowSqlTransformer(
            SqlTransformerSupport transformerSupport, QShadowMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MShadow toRowObjectWithoutFullObject(
            ShadowType shadow, JdbcSession jdbcSession) {
        MShadow row = super.toRowObjectWithoutFullObject(shadow, jdbcSession);

        row.objectClassId = processCacheableUri(shadow.getObjectClass(), jdbcSession);
        setReference(shadow.getResourceRef(), jdbcSession,
                o -> row.resourceRefTargetOid = o,
                t -> row.resourceRefTargetType = t,
                r -> row.resourceRefRelationId = r);
        row.intent = shadow.getIntent();
        row.kind = shadow.getKind();
//        row.attemptNumber = shadow.att; TODO not set in RShadow, probably just with deltas? Where does it come from?
        row.dead = shadow.isDead();
        row.exist = shadow.isExists();
        row.fullSynchronizationTimestamp =
                MiscUtil.asInstant(shadow.getFullSynchronizationTimestamp());
        row.pendingOperationCount = shadow.getPendingOperation().size();
        row.primaryIdentifierValue = shadow.getPrimaryIdentifierValue();
        row.synchronizationSituation = shadow.getSynchronizationSituation();
        row.synchronizationTimestamp = MiscUtil.asInstant(shadow.getSynchronizationTimestamp());

        // TODO extension attributes
        //  copyExtensionOrAttributesFromJAXB(jaxb.getAttributes().asPrismContainerValue(), repo, repositoryContext, RObjectExtensionType.ATTRIBUTES, generatorResult);
        return row;
    }
}
