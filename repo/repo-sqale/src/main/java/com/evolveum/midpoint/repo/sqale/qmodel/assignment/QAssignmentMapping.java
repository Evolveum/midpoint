/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.*;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.MAbstractRole;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * Mapping between {@link QAssignment} and {@link AssignmentType}.
 * There are separate instances for assignments and inducements and the instance also knows
 * the {@link MAssignment#containerType} it should set.
 * Only the instance for assignments is registered for queries as there is no way to distinguish
 * between assignments and inducements when searching containers in the Query API anyway.
 *
 * @param <OR> type of the owner row
 */
public class QAssignmentMapping<OR extends MObject>
        extends QContainerMapping<AssignmentType, QAssignment<OR>, MAssignment, OR> {

    public static final String DEFAULT_ALIAS_NAME = "a";

    /** Default assignment mapping instance, for queries it works for inducements too. */
    public static final QAssignmentMapping<MObject> INSTANCE =
            new QAssignmentMapping<>(MContainerType.ASSIGNMENT);

    /** Inducement mapping instance, this must be used for inserting inducements. */
    public static final QAssignmentMapping<MAbstractRole> INSTANCE_INDUCEMENT =
            new QAssignmentMapping<>(MContainerType.INDUCEMENT);

    private final MContainerType containerType;

    // We can't declare Class<QAssignment<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QAssignmentMapping(MContainerType containerType) {
        super(QAssignment.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AssignmentType.class, (Class) QAssignment.class);
        this.containerType = containerType;

        // TODO OWNER_TYPE is new thing and can help avoid join to concrete object table
        //  But this will likely require special treatment/heuristic.
        addItemMapping(F_LIFECYCLE_STATE, stringMapper(q -> q.lifecycleState));
        addItemMapping(F_ORDER, integerMapper(q -> q.orderValue));
        addItemMapping(F_ORG_REF, refMapper(
                q -> q.orgRefTargetOid,
                q -> q.orgRefTargetType,
                q -> q.orgRefRelationId));
        addItemMapping(F_TARGET_REF, refMapper(
                q -> q.targetRefTargetOid,
                q -> q.targetRefTargetType,
                q -> q.targetRefRelationId));
        addItemMapping(F_TENANT_REF, refMapper(
                q -> q.tenantRefTargetOid,
                q -> q.tenantRefTargetType,
                q -> q.tenantRefRelationId));
        // TODO no idea how extId/Oid works, see RAssignment.getExtension
        // TODO ext mapping can't be done statically
        addNestedMapping(F_CONSTRUCTION, ConstructionType.class)
                .addItemMapping(ConstructionType.F_RESOURCE_REF, refMapper(
                        q -> q.resourceRefTargetOid,
                        q -> q.resourceRefTargetType,
                        q -> q.resourceRefRelationId));
        addNestedMapping(F_ACTIVATION, ActivationType.class)
                .addItemMapping(ActivationType.F_ADMINISTRATIVE_STATUS,
                        enumMapper(q -> q.administrativeStatus))
                .addItemMapping(ActivationType.F_EFFECTIVE_STATUS,
                        enumMapper(q -> q.effectiveStatus))
                .addItemMapping(ActivationType.F_ENABLE_TIMESTAMP,
                        timestampMapper(q -> q.enableTimestamp))
                .addItemMapping(ActivationType.F_DISABLE_REASON,
                        timestampMapper(q -> q.disableTimestamp))
                .addItemMapping(ActivationType.F_DISABLE_REASON,
                        stringMapper(q -> q.disableReason))
                .addItemMapping(ActivationType.F_VALIDITY_STATUS,
                        enumMapper(q -> q.validityStatus))
                .addItemMapping(ActivationType.F_VALID_FROM,
                        timestampMapper(q -> q.validFrom))
                .addItemMapping(ActivationType.F_VALID_TO,
                        timestampMapper(q -> q.validTo))
                .addItemMapping(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP,
                        timestampMapper(q -> q.validityChangeTimestamp))
                .addItemMapping(ActivationType.F_ARCHIVE_TIMESTAMP,
                        timestampMapper(q -> q.archiveTimestamp));
        addNestedMapping(F_METADATA, MetadataType.class)
                .addItemMapping(MetadataType.F_CREATOR_REF, refMapper(
                        q -> q.creatorRefTargetOid,
                        q -> q.creatorRefTargetType,
                        q -> q.creatorRefRelationId))
                .addItemMapping(MetadataType.F_CREATE_CHANNEL,
                        uriMapper(q -> q.createChannelId))
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.createTimestamp))
                .addItemMapping(MetadataType.F_MODIFIER_REF, refMapper(
                        q -> q.modifierRefTargetOid,
                        q -> q.modifierRefTargetType,
                        q -> q.modifierRefRelationId))
                .addItemMapping(MetadataType.F_MODIFY_CHANNEL,
                        uriMapper(q -> q.modifyChannelId))
                .addItemMapping(MetadataType.F_MODIFY_TIMESTAMP,
                        timestampMapper(q -> q.modifyTimestamp))
                .addRefMapping(MetadataType.F_CREATE_APPROVER_REF, referenceMapping(
                        QAssignmentReferenceMapping.INSTANCE_ASSIGNMENT_CREATE_APPROVER))
                .addRefMapping(MetadataType.F_MODIFY_APPROVER_REF, referenceMapping(
                        QAssignmentReferenceMapping.INSTANCE_ASSIGNMENT_MODIFY_APPROVER));
    }

    /** Fixes rigid parametric types of static mapping instance to this instance. */
    private QReferenceMapping<?, ?, QAssignment<OR>, MAssignment> referenceMapping(
            QAssignmentReferenceMapping<?> referenceMapping) {
        //noinspection unchecked
        return (QAssignmentReferenceMapping<OR>) referenceMapping;
    }

    @Override
    protected QAssignment<OR> newAliasInstance(String alias) {
        return new QAssignment<>(alias);
    }

    @Override
    public AssignmentSqlTransformer<OR> createTransformer(SqlTransformerSupport transformerSupport) {
        return new AssignmentSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public MAssignment newRowObject() {
        MAssignment row = new MAssignment();
        row.containerType = this.containerType;
        return row;
    }

    @Override
    public MAssignment newRowObject(OR ownerRow) {
        MAssignment row = newRowObject();
        row.ownerOid = ownerRow.oid;
        return row;
    }
}
