/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.resource;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.connector.QConnectorMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdministrativeOperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBusinessConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Mapping between {@link QResource} and {@link ResourceType}.
 */
public class QResourceMapping
        extends QAssignmentHolderMapping<ResourceType, QResource, MResource> {

    public static final String DEFAULT_ALIAS_NAME = "res";

    private static QResourceMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QResourceMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QResourceMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QResourceMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QResourceMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QResource.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ResourceType.class, QResource.class, repositoryContext);

        addNestedMapping(F_BUSINESS, ResourceBusinessConfigurationType.class)
                .addItemMapping(ResourceBusinessConfigurationType.F_ADMINISTRATIVE_STATE,
                        enumMapper(q -> q.businessAdministrativeState))
                .addRefMapping(ResourceBusinessConfigurationType.F_APPROVER_REF,
                        QObjectReferenceMapping.initForResourceBusinessConfigurationApprover(
                                repositoryContext));

        addNestedMapping(F_ADMINISTRATIVE_OPERATIONAL_STATE, AdministrativeOperationalStateType.class)
                .addItemMapping(AdministrativeOperationalStateType.F_ADMINISTRATIVE_AVAILABILITY_STATUS,
                        enumMapper(q -> q.administrativeOperationalStateAdministrativeAvailabilityStatus));
        addNestedMapping(F_OPERATIONAL_STATE, OperationalStateType.class)
                .addItemMapping(OperationalStateType.F_LAST_AVAILABILITY_STATUS,
                        enumMapper(q -> q.operationalStateLastAvailabilityStatus));

        addRefMapping(F_CONNECTOR_REF,
                q -> q.connectorRefTargetOid,
                q -> q.connectorRefTargetType,
                q -> q.connectorRefRelationId,
                QConnectorMapping::get);
        addItemMapping(F_TEMPLATE, booleanMapper(q -> q.template));
        addItemMapping(F_ABSTRACT, booleanMapper(q -> q.abstractValue));
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

        var administrativeOperationalState = schemaObject.getAdministrativeOperationalState();
        if (administrativeOperationalState != null) {
            row.administrativeOperationalStateAdministrativeAvailabilityStatus =
                    administrativeOperationalState.getAdministrativeAvailabilityStatus();
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
        row.template = schemaObject.isTemplate();
        row.abstractValue = schemaObject.isAbstract();

        return row;
    }

    @Override
    public void storeRelatedEntities(@NotNull MResource row,
            @NotNull ResourceType schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        ResourceBusinessConfigurationType business = schemaObject.getBusiness();
        if (business != null) {
            storeRefs(row, business.getApproverRef(),
                    QObjectReferenceMapping.getForResourceBusinessConfigurationApprover(),
                    jdbcSession);
        }
    }
}
