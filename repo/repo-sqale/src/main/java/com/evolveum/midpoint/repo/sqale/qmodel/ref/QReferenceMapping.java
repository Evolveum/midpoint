/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.integerMapper;
import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.uuidMapper;

import com.evolveum.midpoint.repo.sqale.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QReference} (and its subclasses) and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see different `INSTANCE_*` constants below.
 */
public class QReferenceMapping
        extends SqaleModelMapping<ObjectReferenceType, QReference, MReference> {

    // see also subtype specific alias names defined for instances below
    public static final String DEFAULT_ALIAS_NAME = "ref";

    public static final QReferenceMapping INSTANCE =
            new QReferenceMapping(QReference.TABLE_NAME, DEFAULT_ALIAS_NAME);
    public static final QReferenceMapping INSTANCE_ARCHETYPE =
            new QReferenceMapping("m_ref_archetype", "refa");
    public static final QReferenceMapping INSTANCE_CREATE_APPROVER =
            new QReferenceMapping("m_ref_create_approver", "refca");
    public static final QReferenceMapping INSTANCE_DELEGATED =
            new QReferenceMapping("m_ref_delegated", "refd");
    public static final QReferenceMapping INSTANCE_INCLUDE =
            new QReferenceMapping("m_ref_include", "refi");
    public static final QReferenceMapping INSTANCE_MODIFY_APPROVER =
            new QReferenceMapping("m_ref_modify_approver", "refma");
    public static final QReferenceMapping INSTANCE_OBJECT_PARENT_ORG =
            new QReferenceMapping("m_ref_object_parent_org", "refopo");
    public static final QReferenceMapping INSTANCE_PERSONA =
            new QReferenceMapping("m_ref_persona", "refp");
    public static final QReferenceMapping INSTANCE_RESOURCE_BUSINESS_CONFIGURATION_APPROVER =
            new QReferenceMapping("m_ref_resource_business_configuration_approver", "refrbca");
    public static final QReferenceMapping INSTANCE_ROLE_MEMBER =
            new QReferenceMapping("m_ref_role_member", "refrm");
    public static final QReferenceMapping INSTANCE_USER_ACCOUNT =
            new QReferenceMapping("m_ref_user_account", "refua");

    private QReferenceMapping(String tableName, String defaultAliasName) {
        super(tableName, defaultAliasName,
                ObjectReferenceType.class, QReference.class);

        // TODO owner and reference type is not possible to query, probably OK
        addItemMapping(ObjectReferenceType.F_OID, uuidMapper(path(q -> q.targetOid)));
        addItemMapping(ObjectReferenceType.F_TYPE, integerMapper(path(q -> q.targetType)));
        addItemMapping(ObjectReferenceType.F_RELATION,
                UriItemFilterProcessor.mapper(path(q -> q.relationId)));
    }

    @Override
    protected QReference newAliasInstance(String alias) {
        return new QReference(alias);
    }

//    @Override TODO
//    public TriggerSqlTransformer createTransformer(
//            SqlTransformerContext transformerContext) {
//        return new TriggerSqlTransformer(transformerContext, this);
//    }

    @Override
    public MReference newRowObject() {
        return new MReference();
    }
}
