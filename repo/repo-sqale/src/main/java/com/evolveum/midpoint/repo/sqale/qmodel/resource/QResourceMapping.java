/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.resource;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Mapping between {@link QResource} and {@link ResourceType}.
 */
public class QResourceMapping extends QObjectMapping<ResourceType, QResource, MResource> {

    public static final String DEFAULT_ALIAS_NAME = "res";

    public static final QResourceMapping INSTANCE = new QResourceMapping();

    private QResourceMapping() {
        super(QResource.TABLE_NAME, DEFAULT_ALIAS_NAME, ResourceType.class, QResource.class);

        addNestedMapping(F_BUSINESS, ResourceBusinessConfigurationType.class)
                .addItemMapping(ResourceBusinessConfigurationType.F_ADMINISTRATIVE_STATE,
                        enumMapper(q -> q.businessAdministrativeState))
                .addRefMapping(ResourceBusinessConfigurationType.F_APPROVER_REF,
                        QObjectReferenceMapping.INSTANCE_RESOURCE_BUSINESS_CONFIGURATION_APPROVER);

        addNestedMapping(F_OPERATIONAL_STATE, OperationalStateType.class)
                .addItemMapping(OperationalStateType.F_LAST_AVAILABILITY_STATUS,
                        enumMapper(q -> q.operationalStateLastAvailabilityStatus));

        addItemMapping(F_CONNECTOR_REF, refMapper(
                q -> q.connectorRefTargetOid,
                q -> q.connectorRefTargetType,
                q -> q.connectorRefRelationId));
    }

    @Override
    protected QResource newAliasInstance(String alias) {
        return new QResource(alias);
    }

    @Override
    public MResource newRowObject() {
        return new MResource();
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

        setReference(schemaObject.getConnectorRef(),
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
