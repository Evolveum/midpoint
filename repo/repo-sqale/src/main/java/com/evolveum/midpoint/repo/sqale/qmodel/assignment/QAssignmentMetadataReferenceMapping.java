/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QAssignmentReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see various static `get*()` methods below.
 * Both mapping instances are initialized (`init*()` methods) in {@link QAssignmentMapping}.
 * Init methods can be called multiple times, only one instance for each sub-tables is created.
 *
 * @param <AOR> type of the row (M-bean) of the owner (assignment)
 */
public class QAssignmentMetadataReferenceMapping<AOR extends MObject>
        extends QReferenceMapping<QAssignmentReference<MAssignmentMetadata>, MAssignmentReference, QAssignmentMetadata, MAssignmentMetadata> {

    private static QAssignmentMetadataReferenceMapping<?> instanceAssignmentCreateApprover;
    private static QAssignmentMetadataReferenceMapping<?> instanceAssignmentModifyApprover;

    public static final Class<QAssignmentReference<MAssignmentMetadata>> TYPE = (Class) QAssignmentReference.class;

    public static <OR extends MObject> QAssignmentMetadataReferenceMapping<OR>
    initForAssignmentCreateApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceAssignmentCreateApprover, repositoryContext)) {
            instanceAssignmentCreateApprover = new QAssignmentMetadataReferenceMapping<>(
                    "m_assignment_ref_create_approver", "arefca", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForAssignmentCreateApprover();
    }

    public static <OR extends MObject> QAssignmentMetadataReferenceMapping<OR>
    getForAssignmentCreateApprover() {
        //noinspection unchecked
        return (QAssignmentMetadataReferenceMapping<OR>)
                Objects.requireNonNull(instanceAssignmentCreateApprover);
    }

    public static <OR extends MObject> QAssignmentMetadataReferenceMapping<OR>
    initForAssignmentModifyApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceAssignmentModifyApprover, repositoryContext)) {
            instanceAssignmentModifyApprover = new QAssignmentMetadataReferenceMapping<>(
                    "m_assignment_ref_modify_approver", "arefma", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForAssignmentModifyApprover();
    }

    public static <OR extends MObject> QAssignmentMetadataReferenceMapping<OR>
    getForAssignmentModifyApprover() {
        //noinspection unchecked
        return (QAssignmentMetadataReferenceMapping<OR>)
                Objects.requireNonNull(instanceAssignmentModifyApprover);
    }

    private <TQ extends QObject<TR>, TR extends MObject> QAssignmentMetadataReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, TYPE, repositoryContext, targetMappingSupplier);

        // assignmentCid probably can't be mapped directly
    }

    @Override
    protected QAssignmentReference newAliasInstance(String alias) {
        return new QAssignmentReference(alias, tableName());
    }

    @Override
    public MAssignmentReference newRowObject(MAssignmentMetadata ownerRow) {
        MAssignmentReference row = new MAssignmentReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = ownerRow.ownerType;
        row.assignmentCid = ownerRow.assignmentCid;
        row.metadataCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QAssignmentMetadata, QAssignmentReference<MAssignmentMetadata>, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.assignmentCid.eq(r.assignmentCid))
                .and(a.cid.eq(r.metadataCid));
    }
}
