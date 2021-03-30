/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.resource;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.*;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.EnumItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Mapping between {@link QResource} and {@link ResourceType}.
 */
public class QResourceMapping
        extends QObjectMapping<ResourceType, QResource, MResource> {

    public static final String DEFAULT_ALIAS_NAME = "res";

    public static final QResourceMapping INSTANCE = new QResourceMapping();

    private QResourceMapping() {
        super(QResource.TABLE_NAME, DEFAULT_ALIAS_NAME, ResourceType.class, QResource.class);

        addNestedMapping(F_BUSINESS, ResourceBusinessConfigurationType.class)
                .addItemMapping(ResourceBusinessConfigurationType.F_ADMINISTRATIVE_STATE,
                        EnumItemFilterProcessor.mapper(path(q -> q.businessAdministrativeState)))
                .addRefMapping(ResourceBusinessConfigurationType.F_APPROVER_REF,
                        QObjectReferenceMapping.INSTANCE_RESOURCE_BUSINESS_CONFIGURATION_APPROVER);
        addNestedMapping(F_OPERATIONAL_STATE, OperationalStateType.class)
                .addItemMapping(OperationalStateType.F_LAST_AVAILABILITY_STATUS,
                        EnumItemFilterProcessor.mapper(
                                path(q -> q.operationalStateLastAvailabilityStatus)));
        addItemMapping(F_CONNECTOR_REF, RefItemFilterProcessor.mapper(
                path(q -> q.connectorRefTargetOid),
                path(q -> q.connectorRefTargetType),
                path(q -> q.connectorRefRelationId)));
    }

    @Override
    protected QResource newAliasInstance(String alias) {
        return new QResource(alias);
    }

    @Override
    public ResourceSqlTransformer createTransformer(SqlTransformerSupport transformerSupport) {
        return new ResourceSqlTransformer(transformerSupport, this);
    }

    @Override
    public MResource newRowObject() {
        return new MResource();
    }
}
