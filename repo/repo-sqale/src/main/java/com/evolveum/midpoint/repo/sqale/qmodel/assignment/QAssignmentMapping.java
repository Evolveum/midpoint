/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.util.MiscUtil;
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
    private static QAssignmentMapping<?> instanceAssignment;

    /** Inducement mapping instance, this must be used for inserting inducements. */
    private static QAssignmentMapping<?> instanceInducement;

    public static <OR extends MObject> QAssignmentMapping<OR>
    initAssignment(@NotNull SqaleRepoContext repositoryContext) {
        if (instanceAssignment == null) {
            instanceAssignment = new QAssignmentMapping<>(
                    MContainerType.ASSIGNMENT, repositoryContext);
        }
        return getAssignment();
    }

    public static <OR extends MObject> QAssignmentMapping<OR> getAssignment() {
        //noinspection unchecked
        return (QAssignmentMapping<OR>) Objects.requireNonNull(instanceAssignment);
    }

    public static <OR extends MObject> QAssignmentMapping<OR>
    initInducement(@NotNull SqaleRepoContext repositoryContext) {
        if (instanceInducement == null) {
            instanceInducement = new QAssignmentMapping<>(
                    MContainerType.INDUCEMENT, repositoryContext);
        }
        return getInducement();
    }

    public static <OR extends MObject> QAssignmentMapping<OR> getInducement() {
        //noinspection unchecked
        return (QAssignmentMapping<OR>) Objects.requireNonNull(instanceInducement);
    }

    private final MContainerType containerType;

    // We can't declare Class<QAssignment<OR>>.class, so we cheat a bit.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QAssignmentMapping(
            @NotNull MContainerType containerType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(QAssignment.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AssignmentType.class, (Class) QAssignment.class, repositoryContext);
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
        addItemMapping(F_POLICY_SITUATION, multiUriMapper(q -> q.policySituations));

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
                .addRefMapping(MetadataType.F_CREATE_APPROVER_REF,
                        QAssignmentReferenceMapping.initForAssignmentCreateApprover(
                                repositoryContext))
                .addRefMapping(MetadataType.F_MODIFY_APPROVER_REF,
                        QAssignmentReferenceMapping.initForAssignmentModifyApprover(
                                repositoryContext));
    }

    @Override
    protected QAssignment<OR> newAliasInstance(String alias) {
        return new QAssignment<>(alias);
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

    // about duplication see the comment in QObjectMapping.toRowObjectWithoutFullObject
    @SuppressWarnings("DuplicatedCode")
    @Override
    public MAssignment insert(AssignmentType assignment, OR ownerRow, JdbcSession jdbcSession) {
        MAssignment row = initRowObject(assignment, ownerRow);

        row.ownerType = ownerRow.objectType;
        row.lifecycleState = assignment.getLifecycleState();
        row.orderValue = assignment.getOrder();
        setReference(assignment.getOrgRef(),
                o -> row.orgRefTargetOid = o,
                t -> row.orgRefTargetType = t,
                r -> row.orgRefRelationId = r);
        setReference(assignment.getTargetRef(),
                o -> row.targetRefTargetOid = o,
                t -> row.targetRefTargetType = t,
                r -> row.targetRefRelationId = r);
        setReference(assignment.getTenantRef(),
                o -> row.tenantRefTargetOid = o,
                t -> row.tenantRefTargetType = t,
                r -> row.tenantRefRelationId = r);

        // TODO no idea how to do this yet, somehow related to RAssignment.extension
//        row.extId = assignment.getExtension()...id?;
//        row.extOid =;
        row.policySituations = processCacheableUris(assignment.getPolicySituation());
        row.ext = processExtensions(assignment.getExtension(), MExtItemHolderType.EXTENSION);

        ConstructionType construction = assignment.getConstruction();
        if (construction != null) {
            setReference(construction.getResourceRef(),
                    o -> row.resourceRefTargetOid = o,
                    t -> row.resourceRefTargetType = t,
                    r -> row.resourceRefRelationId = r);
        }

        ActivationType activation = assignment.getActivation();
        if (activation != null) {
            row.administrativeStatus = activation.getAdministrativeStatus();
            row.effectiveStatus = activation.getEffectiveStatus();
            row.enableTimestamp = MiscUtil.asInstant(activation.getEnableTimestamp());
            row.disableTimestamp = MiscUtil.asInstant(activation.getDisableTimestamp());
            row.disableReason = activation.getDisableReason();
            row.validityStatus = activation.getValidityStatus();
            row.validFrom = MiscUtil.asInstant(activation.getValidFrom());
            row.validTo = MiscUtil.asInstant(activation.getValidTo());
            row.validityChangeTimestamp = MiscUtil.asInstant(activation.getValidityChangeTimestamp());
            row.archiveTimestamp = MiscUtil.asInstant(activation.getArchiveTimestamp());
        }

        MetadataType metadata = assignment.getMetadata();
        if (metadata != null) {
            setReference(metadata.getCreatorRef(),
                    o -> row.creatorRefTargetOid = o,
                    t -> row.creatorRefTargetType = t,
                    r -> row.creatorRefRelationId = r);
            row.createChannelId = processCacheableUri(metadata.getCreateChannel());
            row.createTimestamp = MiscUtil.asInstant(metadata.getCreateTimestamp());

            setReference(metadata.getModifierRef(),
                    o -> row.modifierRefTargetOid = o,
                    t -> row.modifierRefTargetType = t,
                    r -> row.modifierRefRelationId = r);
            row.modifyChannelId = processCacheableUri(metadata.getModifyChannel());
            row.modifyTimestamp = MiscUtil.asInstant(metadata.getModifyTimestamp());
        }

        // insert before treating sub-entities
        insert(row, jdbcSession);

        if (metadata != null) {
            storeRefs(row, metadata.getCreateApproverRef(),
                    QAssignmentReferenceMapping.getForAssignmentCreateApprover(), jdbcSession);
            storeRefs(row, metadata.getModifyApproverRef(),
                    QAssignmentReferenceMapping.getForAssignmentModifyApprover(), jdbcSession);
        }

        return row;
    }
}
