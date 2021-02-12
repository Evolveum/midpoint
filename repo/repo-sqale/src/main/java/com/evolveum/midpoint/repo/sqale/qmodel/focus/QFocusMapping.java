/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.stringMapper;

import java.util.Collection;

import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

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

/*        addItemMapping(FocusType.F_ACTIVATION, null); // TODO sub-mapping for activation
    public final static ItemName F_ADMINISTRATIVE_STATUS = new ItemName(SchemaConstantsGenerated.NS_COMMON, "administrativeStatus");
    public final static ItemName F_EFFECTIVE_STATUS = new ItemName(SchemaConstantsGenerated.NS_COMMON, "effectiveStatus");
    public final static ItemName F_VALID_FROM = new ItemName(SchemaConstantsGenerated.NS_COMMON, "validFrom");
    public final static ItemName F_VALID_TO = new ItemName(SchemaConstantsGenerated.NS_COMMON, "validTo");
    public final static ItemName F_VALIDITY_STATUS = new ItemName(SchemaConstantsGenerated.NS_COMMON, "validityStatus");
    public final static ItemName F_DISABLE_REASON = new ItemName(SchemaConstantsGenerated.NS_COMMON, "disableReason");
    public final static ItemName F_DISABLE_TIMESTAMP = new ItemName(SchemaConstantsGenerated.NS_COMMON, "disableTimestamp");
    public final static ItemName F_ENABLE_TIMESTAMP = new ItemName(SchemaConstantsGenerated.NS_COMMON, "enableTimestamp");
    public final static ItemName F_ARCHIVE_TIMESTAMP = new ItemName(SchemaConstantsGenerated.NS_COMMON, "archiveTimestamp");
    public final static ItemName F_VALIDITY_CHANGE_TIMESTAMP = new ItemName(SchemaConstantsGenerated.NS_COMMON, "validityChangeTimestamp");

    // these are not mapped to DB columns so it seems
    public final static ItemName F_LOCKOUT_STATUS = new ItemName(SchemaConstantsGenerated.NS_COMMON, "lockoutStatus");
    public final static ItemName F_LOCKOUT_EXPIRATION_TIMESTAMP = new ItemName(SchemaConstantsGenerated.NS_COMMON, "lockoutExpirationTimestamp");
 */
        addItemMapping(FocusType.F_COST_CENTER, stringMapper(path(q -> q.costCenter)));
        addItemMapping(FocusType.F_EMAIL_ADDRESS, stringMapper(path(q -> q.emailAddress)));
        // TODO byte[] mapping for F_JPEG_PHOTO -> q.photo
        addItemMapping(FocusType.F_LOCALE, stringMapper(path(q -> q.locale)));
        addItemMapping(FocusType.F_LOCALITY,
                PolyStringItemFilterProcessor.mapper(
                        path(q -> q.localityOrig), path(q -> q.localityNorm)));
        addItemMapping(FocusType.F_PREFERRED_LANGUAGE, stringMapper(path(q -> q.preferredLanguage)));
        addItemMapping(FocusType.F_TIMEZONE, stringMapper(path(q -> q.timezone)));
        addItemMapping(FocusType.F_TELEPHONE_NUMBER, stringMapper(path(q -> q.telephoneNumber)));
        // TODO F_CREDENTIALS mappings to passwordCreateTimestamp+passwordModifyTimestamp
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {
        // TODO process photo option
        return new Path[] { entity.oid, entity.fullObject };
    }

    // TODO verify that this allows creation of QFocus alias and that it suffices for "generic query"
    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QFocus<>(MFocus.class, alias);
    }

    @Override
    public FocusSqlTransformer<S, Q, R> createTransformer(
            SqlTransformerContext transformerContext) {
        return new FocusSqlTransformer<>(transformerContext, this);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MFocus();
    }
}
