/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QAccessCertificationWorkItemReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see various static `get*()` methods below.
 * Both mapping instances are initialized (`init*()` methods) in {@link QAccessCertificationWorkItemMapping}.
 * Init methods can be called multiple times, only one instance for each sub-tables is created.
 */
public class QAccessCertificationWorkItemReferenceMapping
        extends QReferenceMapping<QAccessCertificationWorkItemReference, MAccessCertificationWorkItemReference, QAccessCertificationWorkItem, MAccessCertificationWorkItem> {

    private static QAccessCertificationWorkItemReferenceMapping instanceAssignee;
    private static QAccessCertificationWorkItemReferenceMapping instanceCandidate;

    public static QAccessCertificationWorkItemReferenceMapping
    initForCaseWorkItemAssignee(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceAssignee, repositoryContext)) {
            instanceAssignee = new QAccessCertificationWorkItemReferenceMapping(
                    "m_access_cert_wi_assignee", "acwirefa", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForCaseWorkItemAssignee();
    }

    public static QAccessCertificationWorkItemReferenceMapping getForCaseWorkItemAssignee() {
        return Objects.requireNonNull(instanceAssignee);
    }

    public static QAccessCertificationWorkItemReferenceMapping
    initForCaseWorkItemCandidate(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceCandidate, repositoryContext)) {
            instanceCandidate = new QAccessCertificationWorkItemReferenceMapping(
                    "m_access_cert_wi_candidate", "acwirefc", repositoryContext,
                    QFocusMapping::getFocusMapping);
        }
        return getForCaseWorkItemCandidate();
    }

    public static QAccessCertificationWorkItemReferenceMapping getForCaseWorkItemCandidate() {
        return Objects.requireNonNull(instanceCandidate);
    }

    private <TQ extends QObject<TR>, TR extends MObject> QAccessCertificationWorkItemReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        super(tableName, defaultAliasName, QAccessCertificationWorkItemReference.class,
                repositoryContext, targetMappingSupplier);

        // workItemCid probably can't be mapped directly
    }

    @Override
    protected QAccessCertificationWorkItemReference newAliasInstance(String alias) {
        return new QAccessCertificationWorkItemReference(alias, tableName());
    }

    @Override
    public MAccessCertificationWorkItemReference newRowObject(MAccessCertificationWorkItem ownerRow) {
        MAccessCertificationWorkItemReference row = new MAccessCertificationWorkItemReference();
        row.ownerOid = ownerRow.ownerOid;
        row.ownerType = MObjectType.ACCESS_CERTIFICATION_CAMPAIGN;
        row.accessCertWorkItemCid = ownerRow.cid;
        row.accessCertCaseCid = ownerRow.accessCertCaseCid;
        return row;
    }

    @Override
    public BiFunction<QAccessCertificationWorkItem, QAccessCertificationWorkItemReference, Predicate> correlationPredicate() {
        return (a, r) -> a.ownerOid.eq(r.ownerOid)
                .and(a.cid.eq(r.accessCertWorkItemCid))
                .and(a.accessCertCaseCid.eq(r.accessCertCaseCid));
    }
}
