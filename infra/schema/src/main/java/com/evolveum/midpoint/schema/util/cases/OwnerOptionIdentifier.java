/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.util.QNameUtil;

import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Internalized (parsed) form of correlation option identifier (like `none` or `existing-XXX`).
 *
 * @see ResourceObjectOwnerOptionType#getIdentifier()
 */
public class OwnerOptionIdentifier {

    @Nullable private final String existingOwnerId;
    @Nullable private final String invalidStringValue;

    private OwnerOptionIdentifier(@Nullable String existingOwnerId, @Nullable String invalidStringValue) {
        this.existingOwnerId = existingOwnerId;
        this.invalidStringValue = invalidStringValue;
    }

    /**
     * Converts string value of the option identifier (like `none` or `existing-XXX`) to
     * the internalized form (this class).
     */
    public static @NotNull OwnerOptionIdentifier fromStringValue(@NotNull String value) throws SchemaException {
        OwnerOptionIdentifier identifier = fromStringValueForgiving(value);
        if (identifier.isValid()) {
            return identifier;
        } else {
            throw new SchemaException("Invalid value of potential owner identifier: " + value);
        }
    }

    private boolean isValid() {
        return invalidStringValue == null;
    }

    /**
     * As {@link #fromStringValue(String)} but more forgiving: It accepts also invalid string values.
     */
    public static @NotNull OwnerOptionIdentifier fromStringValueForgiving(@NotNull String value) {
        // The string value may contain a namespace (although it should not).
        String localPart = QNameUtil.uriToQName(value, true)
                .getLocalPart();
        if (SchemaConstants.CORRELATION_NONE.equals(localPart)) {
            return forNoOwner();
        } else if (localPart.startsWith(SchemaConstants.CORRELATION_EXISTING_PREFIX)) {
            return forExistingOwner(
                    localPart.substring(SchemaConstants.CORRELATION_EXISTING_PREFIX.length()));
        } else {
            return new OwnerOptionIdentifier(null, value);
        }
    }

    public static @NotNull OwnerOptionIdentifier forNoOwner() {
        return new OwnerOptionIdentifier(null, null);
    }

    public static @NotNull OwnerOptionIdentifier forExistingOwner(@NotNull String ownerId) {
        return new OwnerOptionIdentifier(ownerId, null);
    }

    public static OwnerOptionIdentifier of(@NotNull ResourceObjectOwnerOptionType potentialOwner) throws SchemaException {
        if (potentialOwner.getIdentifier() == null) {
            throw new SchemaException("No identifier for " + potentialOwner);
        } else {
            return fromStringValue(potentialOwner.getIdentifier());
        }
    }

    public @NotNull String getStringValue() {
        if (isValid()) {
            return existingOwnerId != null ?
                    SchemaConstants.CORRELATION_EXISTING_PREFIX + existingOwnerId :
                    SchemaConstants.CORRELATION_NONE;
        } else {
            return invalidStringValue;
        }
    }

    public boolean isNewOwner() {
        return isValid() && existingOwnerId == null;
    }

    public @Nullable String getExistingOwnerId() {
        return existingOwnerId;
    }
}
