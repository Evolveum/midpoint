/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.UserType.*;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.jsonb.JsonbUtils;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mapping between {@link QUser} and {@link UserType}.
 */
public class QUserMapping
        extends QFocusMapping<UserType, QUser, MUser> {

    public static final String DEFAULT_ALIAS_NAME = "u";

    private static QUserMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QUserMapping initUserMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QUserMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QUserMapping getUserMapping() {
        return Objects.requireNonNull(instance);
    }

    private QUserMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QUser.TABLE_NAME, DEFAULT_ALIAS_NAME,
                UserType.class, QUser.class, repositoryContext);

        addItemMapping(F_ADDITIONAL_NAME, polyStringMapper(
                q -> q.additionalNameOrig, q -> q.additionalNameNorm));
        addItemMapping(F_EMPLOYEE_NUMBER, stringMapper(q -> q.employeeNumber));
        addItemMapping(F_PERSONAL_NUMBER, stringMapper(q -> q.personalNumber));
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
        addItemMapping(F_ORGANIZATION, multiPolyStringMapper(q -> q.organizations));
        addItemMapping(F_ORGANIZATIONAL_UNIT, multiPolyStringMapper(q -> q.organizationUnits));
    }

    @Override
    protected QUser newAliasInstance(String alias) {
        return new QUser(alias);
    }

    @Override
    public MUser newRowObject() {
        return new MUser();
    }

    @Override
    public @NotNull MUser toRowObjectWithoutFullObject(
            UserType user, JdbcSession jdbcSession) {
        MUser row = super.toRowObjectWithoutFullObject(user, jdbcSession);

        setPolyString(user.getAdditionalName(),
                o -> row.additionalNameOrig = o, n -> row.additionalNameNorm = n);
        row.employeeNumber = user.getEmployeeNumber();
        row.personalNumber = user.getPersonalNumber();
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
        row.organizations = JsonbUtils.polyStringTypesToJsonb(user.getOrganization());
        row.organizationUnits = JsonbUtils.polyStringTypesToJsonb(user.getOrganizationalUnit());

        return row;
    }
}
