/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerType;
import com.evolveum.midpoint.repo.sqale.qmodel.common.QContainerMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResourceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.mapping.TableRelationResolver;
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

    // Explanation in class Javadoc for SqaleTableMapping
    public static <OR extends MObject> QAssignmentMapping<OR>
    initAssignmentMapping(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceAssignment, repositoryContext)) {
            instanceAssignment = new QAssignmentMapping<>(
                    MContainerType.ASSIGNMENT, repositoryContext);
        }
        return getAssignmentMapping();
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static <OR extends MObject> QAssignmentMapping<OR> getAssignmentMapping() {
        //noinspection unchecked
        return (QAssignmentMapping<OR>) Objects.requireNonNull(instanceAssignment);
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static <OR extends MObject> QAssignmentMapping<OR>
    initInducementMapping(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceInducement, repositoryContext)) {
            instanceInducement = new QAssignmentMapping<>(
                    MContainerType.INDUCEMENT, repositoryContext);
        }
        return getInducementMapping();
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static <OR extends MObject> QAssignmentMapping<OR> getInducementMapping() {
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

        addRelationResolver(PrismConstants.T_PARENT,
                // mapping supplier is used to avoid cycles in the initialization code
                TableRelationResolver.usingJoin(
                        QAssignmentHolderMapping::getAssignmentHolderMapping,
                        // Adding and(q.ownerType.eq(p.objectType) doesn't help the planner.
                        (q, p) -> q.ownerOid.eq(p.oid)));

        // TODO OWNER_TYPE is new thing and can help avoid join to concrete object table
        //  But this will likely require special treatment/heuristic.
        addItemMapping(F_LIFECYCLE_STATE, stringMapper(q -> q.lifecycleState));
        addItemMapping(F_ORDER, integerMapper(q -> q.orderValue));
        addRefMapping(F_ORG_REF,
                q -> q.orgRefTargetOid,
                q -> q.orgRefTargetType,
                q -> q.orgRefRelationId,
                QOrgMapping::getOrgMapping);
        addRefMapping(F_TARGET_REF,
                q -> q.targetRefTargetOid,
                q -> q.targetRefTargetType,
                q -> q.targetRefRelationId,
                QAssignmentHolderMapping::getAssignmentHolderMapping);
        addRefMapping(F_TENANT_REF,
                q -> q.tenantRefTargetOid,
                q -> q.tenantRefTargetType,
                q -> q.tenantRefRelationId,
                QOrgMapping::getOrgMapping);
        addItemMapping(F_POLICY_SITUATION, multiUriMapper(q -> q.policySituations));
        addItemMapping(F_SUBTYPE, multiStringMapper(q -> q.subtypes));

        addExtensionMapping(F_EXTENSION, MExtItemHolderType.EXTENSION, q -> q.ext);
        addNestedMapping(F_CONSTRUCTION, ConstructionType.class)
                .addRefMapping(ConstructionType.F_RESOURCE_REF,
                        q -> q.resourceRefTargetOid,
                        q -> q.resourceRefTargetType,
                        q -> q.resourceRefRelationId,
                        QResourceMapping::get);
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
                .addRefMapping(MetadataType.F_CREATOR_REF,
                        q -> q.creatorRefTargetOid,
                        q -> q.creatorRefTargetType,
                        q -> q.creatorRefRelationId,
                        QUserMapping::getUserMapping)
                .addItemMapping(MetadataType.F_CREATE_CHANNEL,
                        uriMapper(q -> q.createChannelId))
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.createTimestamp))
                .addRefMapping(MetadataType.F_MODIFIER_REF,
                        q -> q.modifierRefTargetOid,
                        q -> q.modifierRefTargetType,
                        q -> q.modifierRefRelationId,
                        QUserMapping::getUserMapping)
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
    public AssignmentType toSchemaObject(MAssignment row) {
        // TODO is there any place we can put row.ownerOid reasonably?
        //  repositoryContext().prismContext().itemFactory().createObject(... definition?)
        //  assignment.asPrismContainerValue().setParent(new ObjectType().oid(own)); abstract not possible
        //  For assignments we can use ownerType, but this is not general for all containers.
        //  Inspiration: com.evolveum.midpoint.repo.sql.helpers.CertificationCaseHelper.updateLoadedCertificationCase
        //  (if even possible with abstract type definition)
        AssignmentType assignment = new AssignmentType()
                .id(row.cid)
                .lifecycleState(row.lifecycleState)
                .order(row.orderValue)
                .orgRef(objectReference(row.orgRefTargetOid,
                        row.orgRefTargetType, row.orgRefRelationId))
                .targetRef(objectReference(row.targetRefTargetOid,
                        row.targetRefTargetType, row.targetRefRelationId))
                .tenantRef(objectReference(row.tenantRefTargetOid,
                        row.tenantRefTargetType, row.tenantRefRelationId));

        // TODO ext... wouldn't serialized fullObject part of the assignment be better after all?

        if (row.policySituations != null) {
            for (Integer policySituationId : row.policySituations) {
                assignment.policySituation(resolveIdToUri(policySituationId));
            }
        }
        if (row.subtypes != null) {
            for (String subtype : row.subtypes) {
                assignment.subtype(subtype);
            }
        }

        if (row.resourceRefTargetOid != null) {
            assignment.construction(new ConstructionType()
                    .resourceRef(objectReference(row.resourceRefTargetOid,
                            row.resourceRefTargetType, row.resourceRefRelationId)));
        }

        ActivationType activation = new ActivationType()
                .administrativeStatus(row.administrativeStatus)
                .effectiveStatus(row.effectiveStatus)
                .enableTimestamp(asXMLGregorianCalendar(row.enableTimestamp))
                .disableTimestamp(asXMLGregorianCalendar(row.disableTimestamp))
                .disableReason(row.disableReason)
                .validityStatus(row.validityStatus)
                .validFrom(asXMLGregorianCalendar(row.validFrom))
                .validTo(asXMLGregorianCalendar(row.validTo))
                .validityChangeTimestamp(asXMLGregorianCalendar(row.validityChangeTimestamp))
                .archiveTimestamp(asXMLGregorianCalendar(row.archiveTimestamp));
        if (!activation.asPrismContainerValue().isEmpty()) {
            assignment.activation(activation);
        }

        MetadataType metadata = new MetadataType()
                .creatorRef(objectReference(row.creatorRefTargetOid,
                        row.creatorRefTargetType, row.creatorRefRelationId))
                .createChannel(resolveIdToUri(row.createChannelId))
                .createTimestamp(asXMLGregorianCalendar(row.createTimestamp))
                .modifierRef(objectReference(row.modifierRefTargetOid,
                        row.modifierRefTargetType, row.modifierRefRelationId))
                .modifyChannel(resolveIdToUri(row.modifyChannelId))
                .modifyTimestamp(asXMLGregorianCalendar(row.modifyTimestamp));
        if (!metadata.asPrismContainerValue().isEmpty()) {
            assignment.metadata(metadata);
        }

        return assignment;
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
        row.ownerType = ownerRow.objectType;
        return row;
    }

    // about duplication see the comment in QObjectMapping.toRowObjectWithoutFullObject
    @SuppressWarnings("DuplicatedCode")
    @Override
    public MAssignment insert(AssignmentType assignment, OR ownerRow, JdbcSession jdbcSession) {
        MAssignment row = initRowObject(assignment, ownerRow);

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

        row.policySituations = processCacheableUris(assignment.getPolicySituation());
        row.subtypes = stringsToArray(assignment.getSubtype());
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
