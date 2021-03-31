/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.qmodel.object.ObjectSqlTransformer;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class FocusSqlTransformer<S extends FocusType, Q extends QFocus<R>, R extends MFocus>
        extends ObjectSqlTransformer<S, Q, R> {

    public FocusSqlTransformer(
            SqlTransformerSupport transformerSupport, QFocusMapping<S, Q, R> mapping) {
        super(transformerSupport, mapping);
    }

    @SuppressWarnings("DuplicatedCode") // activation code duplicated with assignment
    @Override
    public @NotNull R toRowObjectWithoutFullObject(S focus, JdbcSession jdbcSession) {
        R row = super.toRowObjectWithoutFullObject(focus, jdbcSession);

        row.costCenter = focus.getCostCenter();
        row.emailAddress = focus.getEmailAddress();
        row.photo = focus.getJpegPhoto();
        row.locale = focus.getLocale();
        setPolyString(focus.getLocality(), o -> row.localityOrig = o, n -> row.localityNorm = n);
        row.preferredLanguage = focus.getPreferredLanguage();
        row.telephoneNumber = focus.getTelephoneNumber();
        row.timezone = focus.getTimezone();

        // credential/password/metadata (sorry for nesting, but the gets may not be so cheap)
        CredentialsType credentials = focus.getCredentials();
        if (credentials != null) {
            PasswordType password = credentials.getPassword();
            if (password != null) {
                MetadataType passwordMetadata = password.getMetadata();
                if (passwordMetadata != null) {
                    row.passwordCreateTimestamp =
                            MiscUtil.asInstant(passwordMetadata.getCreateTimestamp());
                    row.passwordModifyTimestamp =
                            MiscUtil.asInstant(passwordMetadata.getModifyTimestamp());
                }
            }
        }

        // activation
        ActivationType activation = focus.getActivation();
        if (activation != null) {
            row.administrativeStatus = activation.getAdministrativeStatus();
            row.effectiveStatus = activation.getEffectiveStatus();
            row.enableTimestamp = MiscUtil.asInstant(activation.getEnableTimestamp());
            row.disableTimestamp = MiscUtil.asInstant(activation.getDisableTimestamp());
            row.disableReason = activation.getDisableReason();
            row.validityStatus = activation.getValidityStatus();
            row.validFrom = MiscUtil.asInstant(activation.getValidFrom());
            row.validTo = MiscUtil.asInstant(activation.getValidTo());
            row.validityChangeTimestamp = MiscUtil.asInstant(activation.getValidityChangeTimestamp());
            row.archiveTimestamp = MiscUtil.asInstant(activation.getArchiveTimestamp());
            row.lockoutStatus = activation.getLockoutStatus();
        }

        return row;
    }

    @Override
    public void storeRelatedEntities(
            @NotNull R row, @NotNull S schemaObject, @NotNull JdbcSession jdbcSession) {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        storeRefs(row, schemaObject.getLinkRef(),
                QObjectReferenceMapping.INSTANCE_PROJECTION, jdbcSession);
        storeRefs(row, schemaObject.getPersonaRef(),
                QObjectReferenceMapping.INSTANCE_PERSONA, jdbcSession);
    }
}
