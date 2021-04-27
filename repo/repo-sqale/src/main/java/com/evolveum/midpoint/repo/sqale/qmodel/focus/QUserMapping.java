/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserType.*;

import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
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

        addItemMapping(F_ADDITIONAL_NAME, polyStringMapper(
                q -> q.additionalNameOrig, q -> q.additionalNameNorm));
        addItemMapping(F_EMPLOYEE_NUMBER, stringMapper(q -> q.employeeNumber));
        addItemMapping(F_FAMILY_NAME, polyStringMapper(
                q -> q.familyNameOrig, q -> q.familyNameNorm));
        addItemMapping(F_FULL_NAME, polyStringMapper(
                q -> q.fullNameOrig, q -> q.fullNameNorm));
        addItemMapping(F_GIVEN_NAME, polyStringMapper(
                q -> q.givenNameOrig, q -> q.givenNameNorm));
        addItemMapping(F_HONORIFIC_PREFIX, polyStringMapper(
                q -> q.honorificPrefixOrig, q -> q.honorificPrefixNorm));
        addItemMapping(F_HONORIFIC_SUFFIX, polyStringMapper(
                q -> q.honorificSuffixOrig, q -> q.honorificSuffixNorm));
        addItemMapping(F_NICK_NAME, polyStringMapper(
                q -> q.nickNameOrig, q -> q.nickNameNorm));
        addItemMapping(F_TITLE, polyStringMapper(
                q -> q.titleOrig, q -> q.titleNorm));
    }

    @Override
    protected QUser newAliasInstance(String alias) {
        return new QUser(alias);
    }

    @Override
    public UserSqlTransformer createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new UserSqlTransformer(transformerSupport, this);
    }

    @Override
    public MUser newRowObject() {
        return new MUser();
    }
}
