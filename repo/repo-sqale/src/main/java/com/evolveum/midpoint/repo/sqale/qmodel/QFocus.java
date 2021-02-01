/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel;

import java.sql.Types;
import java.time.Instant;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qbean.MFocus;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QFocus<T extends MFocus> extends QObject<T> {

    private static final long serialVersionUID = -535915621882761789L;

    public static final String TABLE_NAME = "m_focus";

    public static final ColumnMetadata ROLE_TYPE =
            ColumnMetadata.named("roleType").ofType(Types.VARCHAR).withSize(255);

    public static final ColumnMetadata ADMINISTRATIVE_STATUS =
            ColumnMetadata.named("administrativeStatus").ofType(Types.INTEGER);
    public static final ColumnMetadata EFFECTIVE_STATUS =
            ColumnMetadata.named("effectiveStatus").ofType(Types.INTEGER);
    public static final ColumnMetadata ENABLE_TIMESTAMP =
            ColumnMetadata.named("enableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DISABLE_TIMESTAMP =
            ColumnMetadata.named("disableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DISABLE_REASON =
            ColumnMetadata.named("disableReason").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata ARCHIVE_TIMESTAMP =
            ColumnMetadata.named("archiveTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALID_FROM =
            ColumnMetadata.named("validFrom").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALID_TO =
            ColumnMetadata.named("validTo").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALIDITY_CHANGE_TIMESTAMP =
            ColumnMetadata.named("validityChangeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALIDITY_STATUS =
            ColumnMetadata.named("validityStatus").ofType(Types.INTEGER);
    public static final ColumnMetadata COST_CENTER =
            ColumnMetadata.named("costCenter").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata EMAIL_ADDRESS =
            ColumnMetadata.named("emailAddress").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata PHOTO =
            ColumnMetadata.named("photo").ofType(Types.BINARY);
    public static final ColumnMetadata LOCALE =
            ColumnMetadata.named("locale").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata LOCALITY_NORM =
            ColumnMetadata.named("locality_norm").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata LOCALITY_ORIG =
            ColumnMetadata.named("locality_orig").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata PREFERRED_LANGUAGE =
            ColumnMetadata.named("preferredLanguage").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TELEPHONE_NUMBER =
            ColumnMetadata.named("telephoneNumber").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata TIMEZONE =
            ColumnMetadata.named("timezone").ofType(Types.VARCHAR).withSize(255);
    public static final ColumnMetadata PASSWORD_CREATE_TIMESTAMP =
            ColumnMetadata.named("passwordCreateTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata PASSWORD_MODIFY_TIMESTAMP =
            ColumnMetadata.named("passwordModifyTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);

    public final NumberPath<Integer> administrativeStatus =
            createInteger("administrativeStatus", ADMINISTRATIVE_STATUS);
    public final NumberPath<Integer> effectiveStatus =
            createInteger("effectiveStatus", EFFECTIVE_STATUS);
    public final DateTimePath<Instant> enableTimestamp =
            createInstant("enableTimestamp", ENABLE_TIMESTAMP);
    public final DateTimePath<Instant> disableTimestamp =
            createInstant("disableTimestamp", DISABLE_TIMESTAMP);
    public final StringPath disableReason = createString("disableReason", DISABLE_REASON);
    public final DateTimePath<Instant> archiveTimestamp =
            createInstant("archiveTimestamp", ARCHIVE_TIMESTAMP);
    public final DateTimePath<Instant> validFrom = createInstant("validFrom", VALID_FROM);
    public final DateTimePath<Instant> validTo = createInstant("validTo", VALID_TO);
    public final DateTimePath<Instant> validityChangeTimestamp =
            createInstant("validityChangeTimestamp", VALIDITY_CHANGE_TIMESTAMP);
    public final NumberPath<Integer> validityStatus =
            createInteger("validityStatus", VALIDITY_STATUS);
    public final StringPath costCenter = createString("costCenter", COST_CENTER);
    public final StringPath emailAddress = createString("emailAddress", EMAIL_ADDRESS);
    public final ArrayPath<byte[], Byte> photo = createByteArray("photo", PHOTO);
    public final StringPath locale = createString("locale", LOCALE);
    public final StringPath localityNorm = createString("localityNorm", LOCALITY_NORM);
    public final StringPath localityOrig = createString("localityOrig", LOCALITY_ORIG);
    public final StringPath preferredLanguage =
            createString("preferredLanguage", PREFERRED_LANGUAGE);
    public final StringPath telephoneNumber = createString("telephoneNumber", TELEPHONE_NUMBER);
    public final StringPath timezone = createString("timezone", TIMEZONE);
    public final DateTimePath<Instant> passwordCreateTimestamp =
            createInstant("passwordCreateTimestamp", PASSWORD_CREATE_TIMESTAMP);
    public final DateTimePath<Instant> passwordModifyTimestamp =
            createInstant("passwordModifyTimestamp", PASSWORD_MODIFY_TIMESTAMP);

    public QFocus(Class<? extends T> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QFocus(Class<? extends T> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }

    /**
     * Class representing generic {@code QFocus<MFocus>.class} which is otherwise impossible.
     * There should be no need to instantiate this, so the class is private and final.
     */
    public static final class QFocusReal extends QFocus<MFocus> {
        public QFocusReal(String variable) {
            super(MFocus.class, variable);
        }
    }
}
