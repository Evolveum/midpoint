/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class UserSqlTransformer
        extends FocusSqlTransformer<UserType, QUser, MUser> {

    public UserSqlTransformer(
            SqlTransformerSupport transformerSupport, QUserMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MUser toRowObjectWithoutFullObject(
            UserType user, JdbcSession jdbcSession) {
        MUser row = super.toRowObjectWithoutFullObject(user, jdbcSession);

        setPolyString(user.getAdditionalName(),
                o -> row.additionalNameOrig = o, n -> row.additionalNameNorm = n);
        row.employeeNumber = user.getEmployeeNumber();
        setPolyString(user.getFamilyName(),
                o -> row.familyNameOrig = o, n -> row.familyNameNorm = n);
        setPolyString(user.getFullName(), o -> row.fullNameOrig = o, n -> row.fullNameNorm = n);
        setPolyString(user.getGivenName(), o -> row.givenNameOrig = o, n -> row.givenNameNorm = n);
        setPolyString(user.getHonorificPrefix(),
                o -> row.honorificPrefixOrig = o, n -> row.honorificPrefixNorm = n);
        setPolyString(user.getHonorificSuffix(),
                o -> row.honorificSuffixOrig = o, n -> row.honorificSuffixNorm = n);
        setPolyString(user.getNickName(), o -> row.nickNameOrig = o, n -> row.nickNameNorm = n);
        setPolyString(user.getTitle(), o -> row.titleOrig = o, n -> row.titleNorm = n);

        // TODO:
        // user.getOrganizationalUnit() -> m_user_organizational_unit
        // user.getOrganization() -> m_user_organization

        return row;
    }
}
