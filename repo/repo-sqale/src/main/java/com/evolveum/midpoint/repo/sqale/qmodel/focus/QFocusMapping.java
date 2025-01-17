/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_FOCUS_IDENTITY;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_FOCUS_NORMALIZED_DATA;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType.*;

import java.util.*;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Path;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.sqale.ExtensionProcessor;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.ext.MExtItemHolderType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Mapping between {@link QFocus} and {@link FocusType}.
 *
 * @param <S> schema type for the focus object
 * @param <Q> type of entity path
 * @param <R> row type related to the {@link Q}
 */
public class QFocusMapping<S extends FocusType, Q extends QFocus<R>, R extends MFocus>
        extends QAssignmentHolderMapping<S, Q, R> {

    public static final String DEFAULT_ALIAS_NAME = "f";

    private static QFocusMapping<FocusType, QFocus<MFocus>, MFocus> instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QFocusMapping<?, ?, ?> initFocusMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QFocusMapping<>(QFocus.TABLE_NAME, DEFAULT_ALIAS_NAME,
                FocusType.class, QFocus.CLASS,
                repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QFocusMapping<?, ?, ?> getFocusMapping() {
        return Objects.requireNonNull(instance);
    }

    protected QFocusMapping(
            @NotNull String tableName,
            @NotNull String defaultAliasName,
            @NotNull Class<S> schemaType,
            @NotNull Class<Q> queryType,
            @NotNull SqaleRepoContext repositoryContext) {
        super(tableName, defaultAliasName, schemaType, queryType, repositoryContext);

        addItemMapping(F_COST_CENTER, stringMapper(q -> q.costCenter));
        addItemMapping(F_EMAIL_ADDRESS, stringMapper(q -> q.emailAddress));
        // photo is not filterable, obviously
        addItemMapping(F_JPEG_PHOTO, binaryMapper(q -> q.photo));
        addItemMapping(F_LOCALE, stringMapper(q -> q.locale));
        addItemMapping(F_LOCALITY, polyStringMapper(
                q -> q.localityOrig, q -> q.localityNorm));
        addItemMapping(F_PREFERRED_LANGUAGE, stringMapper(q -> q.preferredLanguage));
        addItemMapping(F_TIMEZONE, stringMapper(q -> q.timezone));
        addItemMapping(F_TELEPHONE_NUMBER, stringMapper(q -> q.telephoneNumber));
        // passwordModify/CreateTimestamps are just a bit deeper
        addNestedMapping(F_CREDENTIALS, CredentialsType.class)
                .addNestedMapping(CredentialsType.F_PASSWORD, PasswordType.class)
                .addNestedMapping(PasswordType.F_METADATA, MetadataType.class)
                .addItemMapping(MetadataType.F_CREATE_TIMESTAMP,
                        timestampMapper(q -> q.passwordCreateTimestamp))
                .addItemMapping(MetadataType.F_MODIFY_TIMESTAMP,
                        timestampMapper(q -> q.passwordModifyTimestamp));
        addNestedMapping(F_ACTIVATION, ActivationType.class)
                .addItemMapping(ActivationType.F_ADMINISTRATIVE_STATUS,
                        enumMapper(q -> q.administrativeStatus))
                .addItemMapping(ActivationType.F_EFFECTIVE_STATUS,
                        enumMapper(q -> q.effectiveStatus))
                .addItemMapping(ActivationType.F_ENABLE_TIMESTAMP,
                        timestampMapper(q -> q.enableTimestamp))
                .addItemMapping(ActivationType.F_DISABLE_TIMESTAMP,
                        timestampMapper(q -> q.disableTimestamp))
                .addItemMapping(ActivationType.F_DISABLE_REASON,
                        stringMapper(q -> q.disableReason))
                .addItemMapping(ActivationType.F_VALIDITY_STATUS,
                        enumMapper(q -> q.validityStatus))
                .addItemMapping(ActivationType.F_VALID_FROM,
                        timestampMapper(q -> q.validFrom))
                .addItemMapping(ActivationType.F_VALID_TO,
                        timestampMapper(q -> q.validTo))
                .addItemMapping(ActivationType.F_VALIDITY_CHANGE_TIMESTAMP,
                        timestampMapper(q -> q.validityChangeTimestamp))
                .addItemMapping(ActivationType.F_ARCHIVE_TIMESTAMP,
                        timestampMapper(q -> q.archiveTimestamp))
                .addItemMapping(ActivationType.F_LOCKOUT_STATUS,
                        enumMapper(q -> q.lockoutStatus));

        addRefMapping(F_PERSONA_REF, QObjectReferenceMapping.initForPersona(repositoryContext));
        addRefMapping(F_LINK_REF, QObjectReferenceMapping.initForProjection(repositoryContext));

        addNestedMapping(F_IDENTITIES, FocusIdentitiesType.class)
                .addContainerTableMapping(FocusIdentitiesType.F_IDENTITY,
                        QFocusIdentityMapping.init(repositoryContext),
                        joinOn((o, fi) -> o.oid.eq(fi.ownerOid)))
                .addExtensionMapping(FocusIdentitiesType.F_NORMALIZED_DATA,
                        MExtItemHolderType.EXTENSION, q -> q.normalizedData, repositoryContext);
    }

    @Override
    public @NotNull Path<?>[] selectExpressions(
            Q entity, Collection<SelectorOptions<GetOperationOptions>> options) {

        List<Path<?>> paths = new ArrayList<>();
        Collections.addAll(paths, super.selectExpressions(entity, options));
        if (SelectorOptions.hasToFetchPathNotRetrievedByDefault(F_JPEG_PHOTO, options)) {
            paths.add(entity.photo);
        }
        if (SelectorOptions.hasToFetchPathNotRetrievedByDefault(PATH_FOCUS_NORMALIZED_DATA, options)) {
            paths.add(entity.normalizedData);
            // The rest of F_IDENTITIES is handled with another select via QFocusIdentityMapping.
        }
        return paths.toArray(new Path[0]);
    }

    @Override
    protected Q newAliasInstance(String alias) {
        //noinspection unchecked
        return (Q) new QFocus<>(MFocus.class, alias);
    }

    @Override
    public R newRowObject() {
        //noinspection unchecked
        return (R) new MFocus();
    }

    @Override
    protected void customizeFullObjectItemsToSkip(PathSet mutableSet) {
        super.customizeFullObjectItemsToSkip(mutableSet);
        mutableSet.add(F_JPEG_PHOTO);
        mutableSet.add(PATH_FOCUS_IDENTITY);
        mutableSet.add(PATH_FOCUS_NORMALIZED_DATA);
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

        FocusIdentitiesType identities = focus.getIdentities();
        if (identities != null) {
            row.normalizedData = processExtensions(identities.getNormalizedData(), MExtItemHolderType.EXTENSION);
        }

        return row;
    }

    @Override
    public S toSchemaObject(@NotNull Tuple row, @NotNull Q entityPath, @NotNull JdbcSession jdbcSession,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        S focus = super.toSchemaObject(row, entityPath, jdbcSession, options);

        byte[] photo = row.get(entityPath.photo);
        if (photo != null) {
            PrismObject<?> focusPrismObject = focus.asPrismObject();
            PrismProperty<byte[]> resultProperty =
                    focusPrismObject.findOrCreateProperty(F_JPEG_PHOTO);
            resultProperty.setRealValue(photo);
            resultProperty.setIncomplete(false);
        } else if (SelectorOptions.hasToFetchPathNotRetrievedByDefault(F_JPEG_PHOTO, options)) {
            PrismUtil.setPropertyNullAndComplete(focus.asPrismObject(), F_JPEG_PHOTO);
        }
        if (SelectorOptions.hasToFetchPathNotRetrievedByDefault(PATH_FOCUS_NORMALIZED_DATA, options)) {
            loadFocusIdentitiesNormalizedData(row.get(entityPath.normalizedData), focus);
        }
        if (SelectorOptions.hasToFetchPathNotRetrievedByDefault(PATH_FOCUS_IDENTITY, options)) {
            loadFocusIdentities(focus, jdbcSession);
        }

        return focus;
    }

    private void loadFocusIdentitiesNormalizedData(Jsonb normalizedDataJson, S focus) throws SchemaException {
        if (normalizedDataJson != null) {
            Map<String, Object> normalizedDataMap = Jsonb.toMap(normalizedDataJson);
            // begin/setItems() replaces incomplete container from fullObject, which is the right thing here.
            FocusNormalizedDataType normalizedData = focus.getIdentities().beginNormalizedData();
            new ExtensionProcessor(repositoryContext()).extensionsToContainer(
                    normalizedDataMap, normalizedData);
        }
    }

    private void loadFocusIdentities(S focus, JdbcSession jdbcSession) throws SchemaException {
        // Currently we don't consider container ids and load all identities/identity values.
        QFocusIdentityMapping<MFocus> focusIdentityMapping = QFocusIdentityMapping.get();
        QFocusIdentity<?> q = focusIdentityMapping.defaultAlias();
        var query = jdbcSession.newQuery()
                .from(q)
                .select(q) // no complications here, we load it whole
                .where(q.ownerOid.eq(SqaleUtils.oidToUuid(focus.getOid())));
        for (MFocusIdentity row : query.fetch()) {
            FocusIdentityType focusIdentity = focusIdentityMapping.toSchemaObject(row);
            focus.getIdentities().identity(focusIdentity);
        }

        // Setting "complete" for multi-value containers is quite verbose.
        PrismContainer<Containerable> identityContainer = focus.asPrismObject().findContainer(PATH_FOCUS_IDENTITY);
        if (identityContainer != null) {
            identityContainer.setIncomplete(false);
        }
    }

    @Override
    public void storeRelatedEntities(
            @NotNull R row, @NotNull S schemaObject, @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        storeRefs(row, schemaObject.getLinkRef(),
                QObjectReferenceMapping.getForProjection(), jdbcSession);
        storeRefs(row, schemaObject.getPersonaRef(),
                QObjectReferenceMapping.getForPersona(), jdbcSession);

        FocusIdentitiesType identitiesWrapper = schemaObject.getIdentities();
        if (identitiesWrapper != null) {
            List<FocusIdentityType> identities = identitiesWrapper.getIdentity();
            if (!identities.isEmpty()) {
                QFocusIdentityMapping<MFocus> focusIdentityMapping = QFocusIdentityMapping.get();
                for (FocusIdentityType identity : identities) {
                    focusIdentityMapping.insert(identity, row, jdbcSession);
                }
            }
        }
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> updateGetOptions(
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications, boolean forceReindex) {
        List<SelectorOptions<GetOperationOptions>> ret = new ArrayList<>(super.updateGetOptions(options, modifications, forceReindex));

        if (modifications.stream().anyMatch(m -> F_IDENTITIES.isSubPath(m.getPath()))) {
            ret.addAll(SchemaService.get().getOperationOptionsBuilder().item(F_IDENTITIES).retrieve().build());
        }
        return ret;
    }
}
