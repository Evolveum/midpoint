/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.uuidMapper;

import com.evolveum.midpoint.repo.sqale.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.EnumItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QObjectReference} and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see different `INSTANCE_*` constants below.
 */
public class QObjectReferenceMapping
        extends QReferenceMapping<QObjectReference, MReference> {

    public static final QObjectReferenceMapping INSTANCE_ARCHETYPE =
            new QObjectReferenceMapping("m_ref_archetype", "refa");
    public static final QObjectReferenceMapping INSTANCE_DELEGATED =
            new QObjectReferenceMapping("m_ref_delegated", "refd");
    public static final QObjectReferenceMapping INSTANCE_INCLUDE =
            new QObjectReferenceMapping("m_ref_include", "refi");
    public static final QObjectReferenceMapping INSTANCE_OBJECT_CREATE_APPROVER =
            new QObjectReferenceMapping("m_ref_object_create_approver", "refca");
    public static final QObjectReferenceMapping INSTANCE_OBJECT_MODIFY_APPROVER =
            new QObjectReferenceMapping("m_ref_object_modify_approver", "refma");
    public static final QObjectReferenceMapping INSTANCE_OBJECT_PARENT_ORG =
            new QObjectReferenceMapping("m_ref_object_parent_org", "refpo");
    public static final QObjectReferenceMapping INSTANCE_PERSONA =
            new QObjectReferenceMapping("m_ref_persona", "refp");
    public static final QObjectReferenceMapping INSTANCE_RESOURCE_BUSINESS_CONFIGURATION_APPROVER =
            new QObjectReferenceMapping("m_ref_resource_business_configuration_approver", "refrbca");
    public static final QObjectReferenceMapping INSTANCE_ROLE_MEMBERSHIP =
            new QObjectReferenceMapping("m_ref_role_membership", "refrm");
    public static final QObjectReferenceMapping INSTANCE_USER_ACCOUNT =
            new QObjectReferenceMapping("m_ref_user_account", "refua");

    private QObjectReferenceMapping(String tableName, String defaultAliasName) {
        super(tableName, defaultAliasName, QObjectReference.class);

        // TODO owner and reference type is not possible to query, probably OK
        //  not sure about this mapping yet, does it make sense to query ref components?
        addItemMapping(ObjectReferenceType.F_OID, uuidMapper(path(q -> q.targetOid)));
        addItemMapping(ObjectReferenceType.F_TYPE,
                EnumItemFilterProcessor.mapper(path(q -> q.targetType)));
        addItemMapping(ObjectReferenceType.F_RELATION,
                UriItemFilterProcessor.mapper(path(q -> q.relationId)));
    }

    @Override
    protected QObjectReference newAliasInstance(String alias) {
        return new QObjectReference(alias);
    }

    @Override
    public ObjectReferenceSqlTransformer createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new ObjectReferenceSqlTransformer(transformerSupport, this);
    }

    @Override
    public MReference newRowObject() {
        return new MReference();
    }
}
