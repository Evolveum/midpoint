/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.Objects;
import java.util.function.BiFunction;

import com.querydsl.core.types.Predicate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.QObjectTemplate;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.MResource;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResource;
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
        if (instanceArchetype == null) {
            instanceArchetype = new QObjectReferenceMapping<>(
                    "m_ref_archetype", "refa", repositoryContext);
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
        if (instanceDelegated == null) {
            instanceDelegated = new QObjectReferenceMapping<>(
                    "m_ref_delegated", "refd", repositoryContext);
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
        if (instanceInclude == null) {
            instanceInclude = new QObjectReferenceMapping<>(
                    "m_ref_include", "refi", repositoryContext);
        }
        return instanceInclude;
    }

    public static QObjectReferenceMapping<QObjectTemplate, MObject> getForInclude() {
        return Objects.requireNonNull(instanceInclude);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<Q, R>
    initForProjection(@NotNull SqaleRepoContext repositoryContext) {
        if (instanceProjection == null) {
            instanceProjection = new QObjectReferenceMapping<>(
                    "m_ref_projection", "refpj", repositoryContext);
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
        if (instanceCreateApprover == null) {
            instanceCreateApprover = new QObjectReferenceMapping<>(
                    "m_ref_object_create_approver", "refca", repositoryContext);
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
        if (instanceModifyApprover == null) {
            instanceModifyApprover = new QObjectReferenceMapping<>(
                    "m_ref_object_modify_approver", "refma", repositoryContext);
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
        if (instanceParentOrg == null) {
            instanceParentOrg = new QObjectReferenceMapping<>(
                    "m_ref_object_parent_org", "refpo", repositoryContext);
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
        if (instancePersona == null) {
            instancePersona = new QObjectReferenceMapping<>(
                    "m_ref_persona", "refp", repositoryContext);
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
        if (instanceResourceBusinessConfigurationApprover == null) {
            instanceResourceBusinessConfigurationApprover = new QObjectReferenceMapping<>(
                    "m_ref_resource_business_configuration_approver", "refrbca", repositoryContext);
        }
        return instanceResourceBusinessConfigurationApprover;
    }

    public static QObjectReferenceMapping<QResource, MResource>
    getForResourceBusinessConfigurationApprover() {
        return Objects.requireNonNull(instanceResourceBusinessConfigurationApprover);
    }

    public static <Q extends QObject<R>, R extends MObject> QObjectReferenceMapping<Q, R>
    initForRoleMembership(@NotNull SqaleRepoContext repositoryContext) {
        if (instanceRoleMembership == null) {
            instanceRoleMembership = new QObjectReferenceMapping<>(
                    "m_ref_role_membership", "refrm", repositoryContext);
        }
        return getForRoleMembership();
    }

    public static <Q extends QObject<R>, R extends MObject>
    QObjectReferenceMapping<Q, R> getForRoleMembership() {
        //noinspection unchecked
        return (QObjectReferenceMapping<Q, R>) Objects.requireNonNull(instanceRoleMembership);
    }
    // endregion

    // Sad but true, we can't declare Class<QObjectReference<OR>>.class, we declare defeat instead.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private QObjectReferenceMapping(String tableName,
            String defaultAliasName,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, (Class) QObjectReference.class, repositoryContext);
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
    public BiFunction<OQ, QObjectReference<OR>, Predicate> joinOnPredicate() {
        return (o, r) -> o.oid.eq(r.ownerOid);
    }
}
