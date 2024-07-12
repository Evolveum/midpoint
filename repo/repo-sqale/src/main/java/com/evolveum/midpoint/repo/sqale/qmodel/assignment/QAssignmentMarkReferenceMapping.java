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
import com.evolveum.midpoint.repo.sqale.qmodel.tag.QMarkMapping;
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
public class QAssignmentMarkReferenceMapping<AOR extends MObject>
        extends QReferenceMapping<QAssignmentMarkReference, MAssignmentMark, QAssignment<AOR>, MAssignment> {

    private static QAssignmentMarkReferenceMapping<?> instanceEffectiveMark;

    public static final Class<QAssignmentMarkReference> TYPE = QAssignmentMarkReference.class;

    private <TQ extends QObject<TR>, TR extends MObject> QAssignmentMarkReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, TYPE, repositoryContext, targetMappingSupplier);

        // assignmentCid probably can't be mapped directly
    }

    @Override
    protected QAssignmentMarkReference newAliasInstance(String alias) {
        return new QAssignmentMarkReference(alias, tableName());
    }

    @Override
    public MAssignmentMark newRowObject(MAssignment ownerRow) {
        MAssignmentMark row = new MAssignmentMark();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = ownerRow.ownerType;
        row.assignmentCid = ownerRow.cid;
        return row;
    }

    @Override
    public BiFunction<QAssignment<AOR>, QAssignmentMarkReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.assignmentCid)
                );
    }

    public static <OR extends MObject> QAssignmentMarkReferenceMapping<OR>
    initForEffectiveMark(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceEffectiveMark, repositoryContext)) {
            instanceEffectiveMark = new QAssignmentMarkReferenceMapping<>(
                    "m_ref_assignment_effective_mark", "arefem", repositoryContext,
                    QMarkMapping::getInstance);
        }
        return getForEffectiveMark();
    }


    public static <OR extends MObject> QAssignmentMarkReferenceMapping<OR>
    getForEffectiveMark() {
        //noinspection unchecked
        return (QAssignmentMarkReferenceMapping<OR>) Objects.requireNonNull(instanceEffectiveMark);
    }
}
