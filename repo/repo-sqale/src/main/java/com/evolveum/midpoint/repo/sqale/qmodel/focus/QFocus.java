/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import java.sql.Types;
import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath;

import com.querydsl.core.types.dsl.ArrayPath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeIntervalStatusType;

import static com.evolveum.midpoint.repo.sqale.jsonb.JsonbPath.JSONB_TYPE;

/**
 * Querydsl query type for {@value #TABLE_NAME} table.
 */
@SuppressWarnings("unused")
public class QFocus<R extends MFocus> extends QAssignmentHolder<R> {

    private static final long serialVersionUID = -535915621882761789L;

    /** If {@code QFocus.class} is not enough because of generics, try {@code QFocus.CLASS}. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final Class<QFocus<MFocus>> CLASS = (Class) QFocus.class;

    public static final String TABLE_NAME = "m_focus";

    public static final ColumnMetadata COST_CENTER =
            ColumnMetadata.named("costCenter").ofType(Types.VARCHAR);
    public static final ColumnMetadata EMAIL_ADDRESS =
            ColumnMetadata.named("emailAddress").ofType(Types.VARCHAR);
    public static final ColumnMetadata PHOTO =
            ColumnMetadata.named("photo").ofType(Types.BINARY);
    public static final ColumnMetadata LOCALE =
            ColumnMetadata.named("locale").ofType(Types.VARCHAR);
    public static final ColumnMetadata LOCALITY_ORIG =
            ColumnMetadata.named("localityOrig").ofType(Types.VARCHAR);
    public static final ColumnMetadata LOCALITY_NORM =
            ColumnMetadata.named("localityNorm").ofType(Types.VARCHAR);
    public static final ColumnMetadata PREFERRED_LANGUAGE =
            ColumnMetadata.named("preferredLanguage").ofType(Types.VARCHAR);
    public static final ColumnMetadata TELEPHONE_NUMBER =
            ColumnMetadata.named("telephoneNumber").ofType(Types.VARCHAR);
    public static final ColumnMetadata TIMEZONE =
            ColumnMetadata.named("timezone").ofType(Types.VARCHAR);
    // credential/password/metadata columns
    public static final ColumnMetadata PASSWORD_CREATE_TIMESTAMP =
            ColumnMetadata.named("passwordCreateTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata PASSWORD_MODIFY_TIMESTAMP =
            ColumnMetadata.named("passwordModifyTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    // activation columns
    public static final ColumnMetadata ADMINISTRATIVE_STATUS =
            ColumnMetadata.named("administrativeStatus").ofType(Types.OTHER);
    public static final ColumnMetadata EFFECTIVE_STATUS =
            ColumnMetadata.named("effectiveStatus").ofType(Types.OTHER);
    public static final ColumnMetadata ENABLE_TIMESTAMP =
            ColumnMetadata.named("enableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DISABLE_TIMESTAMP =
            ColumnMetadata.named("disableTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata DISABLE_REASON =
            ColumnMetadata.named("disableReason").ofType(Types.VARCHAR);
    public static final ColumnMetadata VALIDITY_STATUS =
            ColumnMetadata.named("validityStatus").ofType(Types.OTHER);
    public static final ColumnMetadata VALID_FROM =
            ColumnMetadata.named("validFrom").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALID_TO =
            ColumnMetadata.named("validTo").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata VALIDITY_CHANGE_TIMESTAMP =
            ColumnMetadata.named("validityChangeTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata ARCHIVE_TIMESTAMP =
            ColumnMetadata.named("archiveTimestamp").ofType(Types.TIMESTAMP_WITH_TIMEZONE);
    public static final ColumnMetadata LOCKOUT_STATUS =
            ColumnMetadata.named("lockoutStatus").ofType(Types.OTHER);
    public static final ColumnMetadata NORMALIZED_DATA =
            ColumnMetadata.named("normalizedData").ofType(JSONB_TYPE);

    public final StringPath costCenter = createString("costCenter", COST_CENTER);
    public final StringPath emailAddress = createString("emailAddress", EMAIL_ADDRESS);
    public final ArrayPath<byte[], Byte> photo = createByteArray("photo", PHOTO);
    public final StringPath locale = createString("locale", LOCALE);
    public final StringPath localityOrig = createString("localityOrig", LOCALITY_ORIG);
    public final StringPath localityNorm = createString("localityNorm", LOCALITY_NORM);
    public final StringPath preferredLanguage =
            createString("preferredLanguage", PREFERRED_LANGUAGE);
    public final StringPath telephoneNumber = createString("telephoneNumber", TELEPHONE_NUMBER);
    public final StringPath timezone = createString("timezone", TIMEZONE);
    // credential/password/metadata attributes
    public final DateTimePath<Instant> passwordCreateTimestamp =
            createInstant("passwordCreateTimestamp", PASSWORD_CREATE_TIMESTAMP);
    public final DateTimePath<Instant> passwordModifyTimestamp =
            createInstant("passwordModifyTimestamp", PASSWORD_MODIFY_TIMESTAMP);
    // activation attributes
    public final EnumPath<ActivationStatusType> administrativeStatus =
            createEnum("administrativeStatus", ActivationStatusType.class, ADMINISTRATIVE_STATUS);
    public final EnumPath<ActivationStatusType> effectiveStatus =
            createEnum("effectiveStatus", ActivationStatusType.class, EFFECTIVE_STATUS);
    public final DateTimePath<Instant> enableTimestamp =
            createInstant("enableTimestamp", ENABLE_TIMESTAMP);
    public final DateTimePath<Instant> disableTimestamp =
            createInstant("disableTimestamp", DISABLE_TIMESTAMP);
    public final StringPath disableReason = createString("disableReason", DISABLE_REASON);
    public final EnumPath<TimeIntervalStatusType> validityStatus =
            createEnum("validityStatus", TimeIntervalStatusType.class, VALIDITY_STATUS);
    public final DateTimePath<Instant> validFrom = createInstant("validFrom", VALID_FROM);
    public final DateTimePath<Instant> validTo = createInstant("validTo", VALID_TO);
    public final DateTimePath<Instant> validityChangeTimestamp =
            createInstant("validityChangeTimestamp", VALIDITY_CHANGE_TIMESTAMP);
    public final DateTimePath<Instant> archiveTimestamp =
            createInstant("archiveTimestamp", ARCHIVE_TIMESTAMP);
    public final EnumPath<LockoutStatusType> lockoutStatus =
            createEnum("lockoutStatus", LockoutStatusType.class, LOCKOUT_STATUS);
    public final JsonbPath normalizedData = addMetadata(
            add(new JsonbPath(forProperty("normalizedData"))), NORMALIZED_DATA);

    public QFocus(Class<R> type, String variable) {
        this(type, variable, DEFAULT_SCHEMA_NAME, TABLE_NAME);
    }

    public QFocus(Class<R> type, String variable, String schema, String table) {
        super(type, variable, schema, table);
    }
}
