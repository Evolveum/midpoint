/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocusMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.other.QObjectTemplate;
import com.evolveum.midpoint.repo.sqale.qmodel.other.QObjectTemplateMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.MResource;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResource;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRoleMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QArchetypeMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.shadow.QShadowMapping;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QObjectReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all sub-tables, see various static `get*()` methods below.
 * Mapping instances are initialized (`init*()` methods) in {@link QObjectMapping} subclasses.
 * Both `init*` and `get*` methods are flexibly parametrized to adapt to the client code.
 * Init methods can be called multiple times, only one instance for each sub-tables is created.
 *
 * @param <OQ> query type of the reference owner
 * @param <OR> row type of the reference owner (related to {@link OQ})
 */
public class QObjectReferenceMapping<OQ extends QObject<OR>, OR extends MObject>
        extends QReferenceMapping<QObjectReference<OR>, MReference, OQ, OR> {

    public static QObjectReferenceMapping<?, ?> instanceArchetype;
    public static QObjectReferenceMapping<?, ?> instanceDelegated;
    public static QObjectReferenceMapping<QObjectTemplate, MObject> instanceInclude;
    public static QObjectReferenceMapping<?, ?> instanceProjection;
    // word "object" not used, as it is already implied in the class name
    public static QObjectReferenceMapping<?, ?> instanceCreateApprover;
    public static QObjectReferenceMapping<?, ?> instanceModifyApprover;
    public static QObjectReferenceMapping<?, ?> instanceParentOrg;
    public static QObjectReferenceMapping<?, ?> instancePersona;
    public static QObjectReferenceMapping<QResource, MResource>
            instanceResourceBusinessConfigurationApprover;
    public static QObjectReferenceMapping<?, ?> instanceRoleMembership;

    // region static init/get methods
    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> initForArchetype(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceArchetype, repositoryContext)) {
            instanceArchetype = new QObjectReferenceMapping<>(
                    "m_ref_archetype", "refa", repositoryContext,
                    QArchetypeMapping::getArchetypeMapping);
        }
        return getForArchetype();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForArchetype() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceArchetype);
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> initForDelegated(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceDelegated, repositoryContext)) {
            instanceDelegated = new QObjectReferenceMapping<>(
                    "m_ref_delegated", "refd", repositoryContext,
                    QFocusMapping::getFocusMapping);
        }
        return getForDelegated();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForDelegated() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceDelegated);
    }

    public static QObjectReferenceMapping<QObjectTemplate, MObject> initForInclude(
            @NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceInclude, repositoryContext)) {
            instanceInclude = new QObjectReferenceMapping<>(
                    "m_ref_include", "refi", repositoryContext,
                    QObjectTemplateMapping::getObjectTemplateMapping);
        }
        return instanceInclude;
    }

    public static QObjectReferenceMapping<QObjectTemplate, MObject> getForInclude() {
        return Objects.requireNonNull(instanceInclude);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<Q, R>
    initForProjection(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceProjection, repositoryContext)) {
            instanceProjection = new QObjectReferenceMapping<>(
                    "m_ref_projection", "refpj", repositoryContext,
                    QShadowMapping::getShadowMapping);
        }
        return getForProjection();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForProjection() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceProjection);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<Q, R>
    initForCreateApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceCreateApprover, repositoryContext)) {
            instanceCreateApprover = new QObjectReferenceMapping<>(
                    "m_ref_object_create_approver", "refca", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForCreateApprover();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForCreateApprover() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceCreateApprover);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<Q, R>
    initForModifyApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceModifyApprover, repositoryContext)) {
            instanceModifyApprover = new QObjectReferenceMapping<>(
                    "m_ref_object_modify_approver", "refma", repositoryContext,
                    QUserMapping::getUserMapping);
        }
        return getForModifyApprover();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForModifyApprover() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceModifyApprover);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<Q, R>
    initForParentOrg(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceParentOrg, repositoryContext)) {
            instanceParentOrg = new QObjectReferenceMapping<>(
                    "m_ref_object_parent_org", "refpo", repositoryContext,
                    QOrgMapping::getOrgMapping);
        }
        return getForParentOrg();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForParentOrg() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceParentOrg);
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> initForPersona(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instancePersona, repositoryContext)) {
            instancePersona = new QObjectReferenceMapping<>(
                    "m_ref_persona", "refp", repositoryContext,
                    QFocusMapping::getFocusMapping);
        }
        return getForPersona();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForPersona() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instancePersona);
    }

    public static QObjectReferenceMapping<QResource, MResource>
    initForResourceBusinessConfigurationApprover(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceResourceBusinessConfigurationApprover, repositoryContext)) {
            instanceResourceBusinessConfigurationApprover = new QObjectReferenceMapping<>(
                    "m_ref_resource_business_configuration_approver", "refrbca", repositoryContext,
                    QObjectMapping::getObjectMapping);
        }
        return instanceResourceBusinessConfigurationApprover;
    }

    public static QObjectReferenceMapping<QResource, MResource>
    getForResourceBusinessConfigurationApprover() {
        return Objects.requireNonNull(instanceResourceBusinessConfigurationApprover);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<Q, R>
    initForRoleMembership(@NotNull SqaleRepoContext repositoryContext) {
        if (needsInitialization(instanceRoleMembership, repositoryContext)) {
            instanceRoleMembership = new QObjectReferenceMapping<>(
                    "m_ref_role_membership", "refrm", repositoryContext,
                    QAbstractRoleMapping::getAbstractRoleMapping);
        }
        return getForRoleMembership();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForRoleMembership() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceRoleMembership);
    }
    // endregion

    private <TQ extends QObject<TR>, TR extends MObject> QObjectReferenceMapping(
            String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext,
            @NotNull Supplier<QueryTableMapping<?, TQ, TR>> targetMappingSupplier) {
        //noinspection unchecked,rawtypes
        super(tableName, defaultAliasName, (Class) QObjectReference.class,
                repositoryContext, targetMappingSupplier);
    }

    @Override
    protected QObjectReference<OR> newAliasInstance(String alias) {
        return new QObjectReference<>(alias, tableName());
    }

    @Override
    public MReference newRowObject(MObject ownerRow) {
        MReference row = new MReference();
        row.ownerOid = ownerRow.oid;
        row.ownerType = ownerRow.objectType;
        return row;
    }

    @Override
    public BiFunction<OQ, QObjectReference<OR>, Predicate> correlationPredicate() {
        return (o, r) -> o.oid.eq(r.ownerOid);
    }
}
