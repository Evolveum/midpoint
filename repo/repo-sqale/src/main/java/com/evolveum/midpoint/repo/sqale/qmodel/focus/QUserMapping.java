/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.stringMapper;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserType.*;

import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mapping between {@link QUser} and {@link UserType}.
 */
public class QUserMapping
        extends QFocusMapping<UserType, QUser, MUser> {

    public static final String DEFAULT_ALIAS_NAME = "u";

    public static final QUserMapping INSTANCE = new QUserMapping();

    private QUserMapping() {
        super(QUser.TABLE_NAME, DEFAULT_ALIAS_NAME,
                UserType.class, QUser.class);

        addItemMapping(F_ADDITIONAL_NAME,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.additionalNameOrig), path(q -> q.additionalNameNorm)));
        addItemMapping(F_EMPLOYEE_NUMBER, stringMapper(path(q -> q.employeeNumber)));
        addItemMapping(F_FAMILY_NAME,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.familyNameOrig), path(q -> q.familyNameNorm)));
        addItemMapping(F_FULL_NAME,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.fullNameOrig), path(q -> q.fullNameNorm)));
        addItemMapping(F_GIVEN_NAME,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.givenNameOrig), path(q -> q.givenNameNorm)));
        addItemMapping(F_HONORIFIC_PREFIX,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.honorificPrefixOrig), path(q -> q.honorificPrefixNorm)));
        addItemMapping(F_HONORIFIC_SUFFIX,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.honorificSuffixOrig), path(q -> q.honorificSuffixNorm)));
        addItemMapping(F_NICK_NAME,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.nickNameOrig), path(q -> q.nickNameNorm)));
        addItemMapping(F_TITLE,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.titleOrig), path(q -> q.titleNorm)));
    }

    @Override
    protected QUser newAliasInstance(String alias) {
        return new QUser(alias);
    }

    @Override
    public UserSqlTransformer createTransformer(
            SqlTransformerContext transformerContext) {
        return new UserSqlTransformer(transformerContext, this);
    }

    @Override
    public MUser newRowObject() {
        return new MUser();
    }
}
