/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.resource;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class ResourceSqlTransformer
        extends ObjectSqlTransformer<ResourceType, QResource, MResource> {

    public ResourceSqlTransformer(
            SqlTransformerSupport transformerSupport, QResourceMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MResource toRowObjectWithoutFullObject(
            ResourceType schemaObject, JdbcSession jdbcSession) {
        MResource row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        ResourceBusinessConfigurationType business = schemaObject.getBusiness();
        if (business != null) {
            row.businessAdministrativeState = business.getAdministrativeState();
        }

        OperationalStateType operationalState = schemaObject.getOperationalState();
        if (operationalState != null) {
            row.operationalStateLastAvailabilityStatus =
                    operationalState.getLastAvailabilityStatus();
        }

        setReference(schemaObject.getConnectorRef(), jdbcSession,
                o -> row.connectorRefTargetOid = o,
                t -> row.connectorRefTargetType = t,
                r -> row.connectorRefRelationId = r);

        return row;
    }

    @Override
    public void storeRelatedEntities(@NotNull MResource row,
            @NotNull ResourceType schemaObject, @NotNull JdbcSession jdbcSession) {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        ResourceBusinessConfigurationType business = schemaObject.getBusiness();
        if (business != null) {
            storeRefs(row, business.getApproverRef(),
                    QObjectReferenceMapping.INSTANCE_RESOURCE_BUSINESS_CONFIGURATION_APPROVER,
                    jdbcSession);
        }
    }
}
