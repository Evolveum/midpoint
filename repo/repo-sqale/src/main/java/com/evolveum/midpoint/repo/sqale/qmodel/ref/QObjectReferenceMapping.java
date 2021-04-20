/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.function.BiFunction;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.repo.sqale.qmodel.QObjectTemplate;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.MResource;
import com.evolveum.midpoint.repo.sqale.qmodel.resource.QResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QObjectReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see different `INSTANCE_*` constants below.
 * These instances are not typed properly for non-leaf persistence types like object or focus,
 * so mappings for these have adapter methods to change type parameters to `<Q, R>`.
 */
public class QObjectReferenceMapping<Q extends QObject<R>, R extends MObject>
        extends QReferenceMapping<QObjectReference, MReference, Q, R> {

    public static final QObjectReferenceMapping<?, ?> INSTANCE_ARCHETYPE =
            new QObjectReferenceMapping<>("m_ref_archetype", "refa");
    public static final QObjectReferenceMapping<?, ?> INSTANCE_DELEGATED =
            new QObjectReferenceMapping<>("m_ref_delegated", "refd");
    public static final QObjectReferenceMapping<QObjectTemplate, MObject> INSTANCE_INCLUDE =
            new QObjectReferenceMapping<>("m_ref_include", "refi");
    public static final QObjectReferenceMapping<?, ?> INSTANCE_PROJECTION =
            new QObjectReferenceMapping<>("m_ref_projection", "refpj");
    public static final QObjectReferenceMapping<?, ?> INSTANCE_OBJECT_CREATE_APPROVER =
            new QObjectReferenceMapping<>("m_ref_object_create_approver", "refca");
    public static final QObjectReferenceMapping<?, ?> INSTANCE_OBJECT_MODIFY_APPROVER =
            new QObjectReferenceMapping<>("m_ref_object_modify_approver", "refma");
    public static final QObjectReferenceMapping<?, ?> INSTANCE_OBJECT_PARENT_ORG =
            new QObjectReferenceMapping<>("m_ref_object_parent_org", "refpo");
    public static final QObjectReferenceMapping<?, ?> INSTANCE_PERSONA =
            new QObjectReferenceMapping<>("m_ref_persona", "refp");
    public static final QObjectReferenceMapping<QResource, MResource> INSTANCE_RESOURCE_BUSINESS_CONFIGURATION_APPROVER =
            new QObjectReferenceMapping<>("m_ref_resource_business_configuration_approver", "refrbca");
    public static final QObjectReferenceMapping<?, ?> INSTANCE_ROLE_MEMBERSHIP =
            new QObjectReferenceMapping<>("m_ref_role_membership", "refrm");

    private QObjectReferenceMapping(String tableName, String defaultAliasName) {
        super(tableName, defaultAliasName, QObjectReference.class);
    }

    @Override
    protected QObjectReference newAliasInstance(String alias) {
        return new QObjectReference(alias, tableName());
    }

    @Override
    public MReference newRowObject(MObject ownerRow) {
        MReference row = new MReference();
        row.ownerOid = ownerRow.oid;
        return row;
    }

    @Override
    public BiFunction<Q, QObjectReference, Predicate> joinOnPredicate() {
        return (o, r) -> o.oid.eq(r.ownerOid);
    }
}
