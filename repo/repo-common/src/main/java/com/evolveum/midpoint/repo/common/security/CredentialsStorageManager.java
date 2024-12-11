/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.security;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_PASSWORD_VALUE;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Prepares credentials (focus, shadow) for storage in the repository, taking into account the storage method
 * specified in the relevant security policy.
 *
 * Does not actually store the password in the repository. Just prepares the data by transforming the values, items or deltas.
 */
@Component
public class CredentialsStorageManager {

    private static final Trace LOGGER = TraceManager.getTrace(CredentialsStorageManager.class);

    @Autowired private Protector protector;

    public <O extends ObjectType> ObjectDelta<O> transformFocusExecutionDelta(
            @Nullable CredentialsPolicyType credentialsPolicy, @NotNull ObjectDelta<O> delta)
            throws SchemaException, EncryptionException {

        if (credentialsPolicy == null || delta.isDelete()) {
            return delta;
        }

        ObjectDelta<O> transformedDelta = delta.clone();
        transformFocusExecutionDeltaForCredential(
                credentialsPolicy, credentialsPolicy.getPassword(), "password",
                SchemaConstants.PATH_PASSWORD_VALUE, transformedDelta);
        // TODO: nonce and others
        return transformedDelta;
    }

    @SuppressWarnings("SameParameterValue")
    private <O extends ObjectType> void transformFocusExecutionDeltaForCredential(
            @NotNull CredentialsPolicyType credentialsPolicy,
            @Nullable CredentialPolicyType specificCredentialPolicy,
            @NotNull String specificCredentialName,
            @NotNull ItemPath valuePath,
            @NotNull ObjectDelta<O> delta) throws SchemaException, EncryptionException {
        assert !delta.isDelete();
        var storageType = SecurityUtil.getCredentialStorageType(credentialsPolicy.getDefault(), specificCredentialPolicy);
        switch (storageType) {
            case ENCRYPTION ->
                    LOGGER.trace("Credential {} should be encrypted, nothing to do", specificCredentialName);
            case HASHING -> {
                LOGGER.trace("Hashing credential: {}", specificCredentialName);
                hashCredentialValue(valuePath, delta);
            }
            case NONE -> {
                LOGGER.trace("Removing credential: {}", specificCredentialName);
                removeCredentialValue(valuePath, delta);
            }
            default -> throw new SchemaException("Unknown storage type " + storageType);
        }
    }

    private <O extends ObjectType> void hashCredentialValue(ItemPath valuePath, ObjectDelta<O> delta)
            throws SchemaException, EncryptionException {
        if (delta.isAdd()) {
            PrismProperty<ProtectedStringType> prop = delta.getObjectToAdd().findProperty(valuePath);
            if (prop != null) {
                hashValues(prop.getValues());
            }
        } else {
            //noinspection unchecked
            PropertyDelta<ProtectedStringType> valueDelta =
                    delta.findItemDelta(valuePath, PropertyDelta.class, PrismProperty.class, true);
            if (valueDelta != null) {
                hashValues(valueDelta.getValuesToAdd());
                hashValues(valueDelta.getValuesToReplace());
                hashValues(valueDelta.getValuesToDelete());  // TODO sure?
                return;
            }
            ItemPath abstractCredentialPath = valuePath.allExceptLast();
            //noinspection unchecked
            ContainerDelta<PasswordType> abstractCredentialDelta = delta.findItemDelta(abstractCredentialPath,
                    ContainerDelta.class, PrismContainer.class, true);
            if (abstractCredentialDelta != null) {
                hashPasswordPcvs(abstractCredentialDelta.getValuesToAdd());
                hashPasswordPcvs(abstractCredentialDelta.getValuesToReplace());
                // TODO what about delete? probably nothing
                return;
            }
            ItemPath credentialsPath = abstractCredentialPath.allExceptLast();
            //noinspection unchecked
            ContainerDelta<CredentialsType> credentialsDelta = delta.findItemDelta(credentialsPath, ContainerDelta.class,
                    PrismContainer.class, true);
            if (credentialsDelta != null) {
                hashCredentialsPcvs(credentialsDelta.getValuesToAdd());
                hashCredentialsPcvs(credentialsDelta.getValuesToReplace());
                // TODO what about delete? probably nothing
            }
        }
    }

    private <O extends ObjectType> void removeCredentialValue(ItemPath valuePath, ObjectDelta<O> delta) {
        if (delta.isAdd()) {
            delta.getObjectToAdd().removeProperty(valuePath);
        } else {
            PropertyDelta<ProtectedStringType> propDelta = delta.findPropertyDelta(valuePath);
            if (propDelta != null) {
                // Replace with nothing. We need this to clear any existing value that there might be.
                propDelta.setValueToReplace();
            }
        }
        // TODO remove password also when the whole credentials or credentials/password container is added/replaced
    }

    private void hashValues(Collection<PrismPropertyValue<ProtectedStringType>> values)
            throws SchemaException, EncryptionException {
        for (var value : emptyIfNull(values)) {
            ProtectedStringType ps = value.getValue();
            if (!ps.isHashed()) {
                protector.hash(ps);
            }
        }
    }

    private void encryptValues(Collection<PrismPropertyValue<ProtectedStringType>> values)
            throws EncryptionException {
        for (var value : emptyIfNull(values)) {
            ProtectedStringType ps = value.getValue();
            if (!ps.isEncrypted()) {
                protector.encrypt(ps);
            }
        }
    }

    private void hashPasswordPcvs(Collection<PrismContainerValue<PasswordType>> values)
            throws SchemaException, EncryptionException {
        for (PrismContainerValue<PasswordType> pval : emptyIfNull(values)) {
            PasswordType password = pval.getValue();
            if (password != null && password.getValue() != null) {
                if (!password.getValue().isHashed()) {
                    protector.hash(password.getValue());
                }
            }
        }
    }

    private void hashCredentialsPcvs(Collection<PrismContainerValue<CredentialsType>> values)
            throws SchemaException, EncryptionException {
        if (values != null) {
            for (PrismContainerValue<CredentialsType> pval : values) {
                CredentialsType credentials = pval.getValue();
                if (credentials != null && credentials.getPassword() != null) {
                    ProtectedStringType passwordValue = credentials.getPassword().getValue();
                    if (passwordValue != null && !passwordValue.isHashed()) {
                        protector.hash(passwordValue);
                    }
                }
            }
        }
    }

    /** We assume that only cleartext or encrypted values come in the delta. */
    public PropertyDelta<ProtectedStringType> transformShadowPasswordDelta(
            @Nullable CredentialsPolicyType credentialsPolicy,
            boolean legacyCaching,
            PropertyDelta<ProtectedStringType> delta)
            throws SchemaException, EncryptionException {
        var storageType = getStorageType(credentialsPolicy, legacyCaching);
        var clonedDelta = delta.clone();
        switch (storageType) {
            case ENCRYPTION -> encryptValues(clonedDelta.getNewValues());
            case HASHING -> hashValues(clonedDelta.getNewValues());
            case NONE -> {
                // We simply remove anything that is there
                clonedDelta.clearValuesToAdd();
                clonedDelta.clearValuesToDelete();
                clonedDelta.setValueToReplace();
            }
            default -> throw unsupported(storageType);
        }
        return clonedDelta;
    }

    /**
     * Prepares the shadow password property for storage (into a new shadow).
     * Expects that the property contains the real value.
     *
     * The legacy caching supports the hashing only.
     */
    public void transformShadowPasswordWithRealValue(
            @Nullable CredentialsPolicyType credentialsPolicy,
            boolean legacyCaching,
            @NotNull PrismProperty<ProtectedStringType> passwordProperty)
            throws SchemaException, EncryptionException {
        var storageType = getStorageType(credentialsPolicy, legacyCaching);
        switch (storageType) {
            case ENCRYPTION -> {
                var realValue = passwordProperty.getRealValue(ProtectedStringType.class);
                if (realValue.isHashed()) {
                    // This should be treated in the caller; this code just checks it is so.
                    throw new IllegalStateException("Hashed value cannot be stored in the shadow");
                } else if (realValue.hasClearValue()) {
                    protector.encrypt(realValue);
                } else {
                    // Nothing to do; either keeping the encrypted value or (in the future) the external value
                }
            }
            case HASHING ->
                    protector.hash(
                            passwordProperty.getRealValue(ProtectedStringType.class));
            case NONE ->
                    passwordProperty.clear();
            default -> throw unsupported(storageType);
        }
    }

    private static @NotNull CredentialsStorageTypeType getStorageType(
            @Nullable CredentialsPolicyType credentialsPolicy, boolean legacyCaching) {
        var declaredStorage = SecurityUtil.getPasswordStorageType(credentialsPolicy);
        return declaredStorage != CredentialsStorageTypeType.ENCRYPTION || !legacyCaching ?
                declaredStorage : CredentialsStorageTypeType.HASHING;
    }

    /**
     * Prepares the shadow password property for storage (into an existing shadow): returns a delta that does so.
     * Expects that the new value contains the real value that is either clear or encrypted.
     */
    public @Nullable PropertyDelta<ProtectedStringType> createShadowPasswordDelta(
            @Nullable CredentialsPolicyType credentialsPolicy,
            @Nullable ProtectedStringType oldValue,
            @NotNull ProtectedStringType newValue)
            throws SchemaException, EncryptionException {
        assert newValue.canGetCleartext();
        var storage = SecurityUtil.getPasswordStorageType(credentialsPolicy);
        switch (storage) {
            case ENCRYPTION -> {
                if (oldValue == null
                        || !oldValue.isEncrypted()
                        || !protector.compareCleartext(oldValue, newValue)) {
                    var clone = newValue.clone();
                    if (!clone.isEncrypted()) {
                        protector.encrypt(clone);
                    }
                    return createPasswordReplaceDelta(clone);
                } else {
                    return null;
                }
            }
            case HASHING -> {
                if (oldValue == null
                        || !oldValue.isHashed()
                        || !protector.compareCleartext(oldValue, newValue)) {
                    var clone = newValue.clone();
                    protector.hash(clone);
                    return createPasswordReplaceDelta(clone);
                } else {
                    return null;
                }
            }
            case NONE -> {
                if (oldValue != null) {
                    return createPasswordReplaceDelta();
                } else {
                    return null;
                }
            }
            default -> throw unsupported(storage);
        }
    }

    private static PropertyDelta<ProtectedStringType> createPasswordReplaceDelta() throws SchemaException {
        //noinspection unchecked
        return (PropertyDelta<ProtectedStringType>) PrismContext.get().deltaFor(ShadowType.class)
                .item(PATH_PASSWORD_VALUE).replace()
                .asItemDelta();
    }

    private static PropertyDelta<ProtectedStringType> createPasswordReplaceDelta(ProtectedStringType value)
            throws SchemaException {
        //noinspection unchecked
        return (PropertyDelta<ProtectedStringType>) PrismContext.get().deltaFor(ShadowType.class)
                .item(PATH_PASSWORD_VALUE).replace(value)
                .asItemDelta();
    }

    private static UnsupportedOperationException unsupported(CredentialsStorageTypeType storage) {
        return new UnsupportedOperationException("Unsupported credentials storage type: " + storage);
    }
}
