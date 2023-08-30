/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath.JSONB_TYPE;

import java.sql.Types;

import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QUser extends QFocus<MUser> {

    private static final long serialVersionUID = 4995959722218007882L;

    public static final String TABLE_NAME = "m_user";

    public static final ColumnMetadata ADDITIONAL_NAME_ORIG =
            ColumnMetadata.named("additionalNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata ADDITIONAL_NAME_NORM =
            ColumnMetadata.named("additionalNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata EMPLOYEE_NUMBER =
            ColumnMetadata.named("employeeNumber").ofType(Types.VARCHAR);
    public static final ColumnMetadata FAMILY_NAME_ORIG =
            ColumnMetadata.named("familyNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata FAMILY_NAME_NORM =
            ColumnMetadata.named("familyNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata FULL_NAME_ORIG =
            ColumnMetadata.named("fullNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata FULL_NAME_NORM =
            ColumnMetadata.named("fullNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata GIVEN_NAME_ORIG =
            ColumnMetadata.named("givenNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata GIVEN_NAME_NORM =
            ColumnMetadata.named("givenNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata HONORIFIC_PREFIX_ORIG =
            ColumnMetadata.named("honorificPrefixOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata HONORIFIC_PREFIX_NORM =
            ColumnMetadata.named("honorificPrefixNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata HONORIFIC_SUFFIX_ORIG =
            ColumnMetadata.named("honorificSuffixOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata HONORIFIC_SUFFIX_NORM =
            ColumnMetadata.named("honorificSuffixNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata NICK_NAME_ORIG =
            ColumnMetadata.named("nickNameOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata NICK_NAME_NORM =
            ColumnMetadata.named("nickNameNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata TITLE_ORIG =
            ColumnMetadata.named("titleOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata TITLE_NORM =
            ColumnMetadata.named("titleNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata ORGANIZATIONS =
            ColumnMetadata.named("organizations").ofType(JSONB_TYPE);
    public static final ColumnMetadata ORGANIZATION_UNITS =
            ColumnMetadata.named("organizationUnits").ofType(JSONB_TYPE);
    private static final ColumnMetadata PERSONAL_NUMBER = ColumnMetadata.named("personalNumber").ofType(Types.VARCHAR);

    public final StringPath additionalNameOrig = createString("additionalNameOrig", ADDITIONAL_NAME_ORIG);
    public final StringPath additionalNameNorm = createString("additionalNameNorm", ADDITIONAL_NAME_NORM);
    public final StringPath employeeNumber = createString("employeeNumber", EMPLOYEE_NUMBER);
    public final StringPath familyNameOrig = createString("familyNameOrig", FAMILY_NAME_ORIG);
    public final StringPath familyNameNorm = createString("familyNameNorm", FAMILY_NAME_NORM);
    public final StringPath fullNameOrig = createString("fullNameOrig", FULL_NAME_ORIG);
    public final StringPath fullNameNorm = createString("fullNameNorm", FULL_NAME_NORM);
    public final StringPath givenNameOrig = createString("givenNameOrig", GIVEN_NAME_ORIG);
    public final StringPath givenNameNorm = createString("givenNameNorm", GIVEN_NAME_NORM);
    public final StringPath honorificPrefixOrig = createString("honorificPrefixOrig", HONORIFIC_PREFIX_ORIG);
    public final StringPath honorificPrefixNorm = createString("honorificPrefixNorm", HONORIFIC_PREFIX_NORM);
    public final StringPath honorificSuffixOrig = createString("honorificSuffixOrig", HONORIFIC_SUFFIX_ORIG);
    public final StringPath honorificSuffixNorm = createString("honorificSuffixNorm", HONORIFIC_SUFFIX_NORM);
    public final StringPath nickNameOrig = createString("nickNameOrig", NICK_NAME_ORIG);
    public final StringPath nickNameNorm = createString("nickNameNorm", NICK_NAME_NORM);
    public final StringPath titleOrig = createString("titleOrig", TITLE_ORIG);
    public final StringPath titleNorm = createString("titleNorm", TITLE_NORM);
    public final JsonbPath organizations =
            addMetadata(add(new JsonbPath(forProperty("organizations"))), ORGANIZATIONS);
    public final JsonbPath organizationUnits =
            addMetadata(add(new JsonbPath(forProperty("organizationUnits"))), ORGANIZATION_UNITS);

    public StringPath personalNumber = createString("personalNumber", PERSONAL_NUMBER);

    public QUser(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QUser(String variable, String schema, String table) {
        super(MUser.class, variable, schema, table);
    }
}
