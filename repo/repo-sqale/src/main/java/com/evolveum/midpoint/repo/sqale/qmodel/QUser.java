/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.sql.Types;

import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qbean.MUser;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QUser extends QObject<MUser> {

    private static final long serialVersionUID = -6556210963622526756L;

    public static final String TABLE_NAME = "m_user";

    public static final ColumnMetadata ADDITIONAL_NAME_NORM =
            ColumnMetadata.named("additionalName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata ADDITIONAL_NAME_ORIG =
            ColumnMetadata.named("additionalName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata EMPLOYEE_NUMBER =
            ColumnMetadata.named("employeeNumber").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata FAMILY_NAME_NORM =
            ColumnMetadata.named("familyName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata FAMILY_NAME_ORIG =
            ColumnMetadata.named("familyName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata FULL_NAME_NORM =
            ColumnMetadata.named("fullName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata FULL_NAME_ORIG =
            ColumnMetadata.named("fullName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata GIVEN_NAME_NORM =
            ColumnMetadata.named("givenName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata GIVEN_NAME_ORIG =
            ColumnMetadata.named("givenName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata HONORIFIC_PREFIX_NORM =
            ColumnMetadata.named("honorificPrefix_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata HONORIFIC_PREFIX_ORIG =
            ColumnMetadata.named("honorificPrefix_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata HONORIFIC_SUFFIX_NORM =
            ColumnMetadata.named("honorificSuffix_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata HONORIFIC_SUFFIX_ORIG =
            ColumnMetadata.named("honorificSuffix_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata NICK_NAME_NORM =
            ColumnMetadata.named("nickName_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata NICK_NAME_ORIG =
            ColumnMetadata.named("nickName_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TITLE_NORM =
            ColumnMetadata.named("title_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TITLE_ORIG =
            ColumnMetadata.named("title_orig").ofType(Types.VARCHAR).withSize(255);

    public final StringPath additionalNameNorm = createString("additionalNameNorm", ADDITIONAL_NAME_NORM);
    public final StringPath additionalNameOrig = createString("additionalNameOrig", ADDITIONAL_NAME_ORIG);
    public final StringPath employeeNumber = createString("employeeNumber", EMPLOYEE_NUMBER);
    public final StringPath familyNameNorm = createString("familyNameNorm", FAMILY_NAME_NORM);
    public final StringPath familyNameOrig = createString("familyNameOrig", FAMILY_NAME_ORIG);
    public final StringPath fullNameNorm = createString("fullNameNorm", FULL_NAME_NORM);
    public final StringPath fullNameOrig = createString("fullNameOrig", FULL_NAME_ORIG);
    public final StringPath givenNameNorm = createString("givenNameNorm", GIVEN_NAME_NORM);
    public final StringPath givenNameOrig = createString("givenNameOrig", GIVEN_NAME_ORIG);
    public final StringPath honorificPrefixNorm = createString("honorificPrefixNorm", HONORIFIC_PREFIX_NORM);
    public final StringPath honorificPrefixOrig = createString("honorificPrefixOrig", HONORIFIC_PREFIX_ORIG);
    public final StringPath honorificSuffixNorm = createString("honorificSuffixNorm", HONORIFIC_SUFFIX_NORM);
    public final StringPath honorificSuffixOrig = createString("honorificSuffixOrig", HONORIFIC_SUFFIX_ORIG);
    public final StringPath nickNameNorm = createString("nickNameNorm", NICK_NAME_NORM);
    public final StringPath nickNameOrig = createString("nickNameOrig", NICK_NAME_ORIG);
    public final StringPath titleNorm = createString("titleNorm", TITLE_NORM);
    public final StringPath titleOrig = createString("titleOrig", TITLE_ORIG);

    public final PrimaryKey<MUser> pk = createPrimaryKey(oid);

    public QUser(String variable) {
        this(variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QUser(String variable, String schema, String table) {
        super(MUser.class, variable, schema, table);
    }
}
