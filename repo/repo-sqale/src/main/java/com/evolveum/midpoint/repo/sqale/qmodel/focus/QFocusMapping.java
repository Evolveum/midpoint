/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType.*;

import java.util.Collection;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.EnumItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.TimestampItemFilterProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Mapping between {@link QFocus} and {@link FocusType}.
 */
public class QFocusMapping<S extends FocusType, Q extends QFocus<R>, R extends MFocus>
        extends QObjectMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "f";

    public static final QFocusMapping<FocusType, QFocus<MFocus>, MFocus> INSTANCE =
            new QFocusMapping<>(QFocus.TABLE_NAME, DEFAULT_ALIAS_NAME,
                    FocusType.class, QFocus.CLASS);

    protected QFocusMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType) {
        super(tableName, defaultAliasName, schemaType, queryType);

        addItemMapping(F_COST_CENTER, stringMapper(path(q -> q.costCenter)));
        addItemMapping(F_EMAIL_ADDRESS, stringMapper(path(q -> q.emailAddress)));
        // TODO byte[] mapping for F_JPEG_PHOTO -> q.photo
        addItemMapping(F_LOCALE, stringMapper(path(q -> q.locale)));
        addItemMapping(F_LOCALITY,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.localityOrig), path(q -> q.localityNorm)));
        addItemMapping(F_PREFERRED_LANGUAGE, stringMapper(path(q -> q.preferredLanguage)));
        addItemMapping(F_TIMEZONE, stringMapper(path(q -> q.timezone)));
        addItemMapping(F_TELEPHONE_NUMBER, stringMapper(path(q -> q.telephoneNumber)));
        // passwordModify/CreateTimestamps are just a bit deeper
        addNestedMapping(F_CREDENTIALS, CredentialsType.class)
                .addNestedMapping(CredentialsType.F_PASSWORD, PasswordType.class)
                .addNestedMapping(PasswordType.F_METADATA, MetadataType.class)
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.passwordCreateTimestamp)))
                .addItemMapping(MetadataType.F_MODIFY_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.passwordModifyTimestamp)));
        addNestedMapping(F_ACTIVATION, ActivationType.class)
                .addItemMapping(ActivationType.F_ADMINISTRATIVE_STATUS,
                        EnumItemFilterProcessor.mapper(path(q -> q.administrativeStatus)))
                .addItemMapping(ActivationType.F_EFFECTIVE_STATUS,
                        EnumItemFilterProcessor.mapper(path(q -> q.effectiveStatus)))
                .addItemMapping(ActivationType.F_ENABLE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.enableTimestamp)))
                .addItemMapping(ActivationType.F_DISABLE_REASON,
                        TimestampItemFilterProcessor.mapper(path(q -> q.disableTimestamp)))
                .addItemMapping(ActivationType.F_DISABLE_REASON,
                        stringMapper(path(q -> q.disableReason)))
                .addItemMapping(ActivationType.F_VALIDITY_STATUS,
                        EnumItemFilterProcessor.mapper(path(q -> q.validityStatus)))
                .addItemMapping(ActivationType.F_VALID_FROM,
                        TimestampItemFilterProcessor.mapper(path(q -> q.validFrom)))
                .addItemMapping(ActivationType.F_VALID_TO,
                        TimestampItemFilterProcessor.mapper(path(q -> q.validTo)))
                .addItemMapping(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.validityChangeTimestamp)))
                .addItemMapping(ActivationType.F_ARCHIVE_TIMESTAMP,
                        TimestampItemFilterProcessor.mapper(path(q -> q.archiveTimestamp)))
                .addItemMapping(ActivationType.F_LOCKOUT_STATUS,
                        EnumItemFilterProcessor.mapper(path(q -> q.lockoutStatus)));

        addRefMapping(F_DELEGATED_REF, QObjectReferenceMapping.INSTANCE_DELEGATED);
        addRefMapping(F_PERSONA_REF, QObjectReferenceMapping.INSTANCE_PERSONA);
        addRefMapping(F_LINK_REF, QObjectReferenceMapping.INSTANCE_PROJECTION);
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {
        // TODO process photo option
        return new Path[] { entity.oid, entity.fullObject };
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QFocus<>(MFocus.class, alias);
    }

    @Override
    public FocusSqlTransformer<S, Q, R> createTransformer(
            SqlTransformerSupport transformerSupport) {
        return new FocusSqlTransformer<>(transformerSupport, this);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MFocus();
    }
}
