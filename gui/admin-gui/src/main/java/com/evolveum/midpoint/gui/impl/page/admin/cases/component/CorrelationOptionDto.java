/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import java.io.Serializable;
import java.util.Set;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Represents a correlation option: a candidate owner or a "new owner".
 */
public class CorrelationOptionDto implements Serializable {

    /**
     * Owner focus object: either existing one, or a new one.
     *
     * The new one contains the result of pre-mappings execution, so it is only partially filled-in.
     */
    @NotNull private final PrismObject<?> object;

    /**
     * True if the {@link #object} represents the new owner.
     */
    private final boolean newOwner;

    /**
     * Identifier corresponding to this choice. It should be sent to the case management engine when completing this request.
     */
    @NotNull private final String identifier;

    /**
     * Creates a DTO in the case of existing owner.
     */
    CorrelationOptionDto(@NotNull ResourceObjectOwnerOptionType potentialOwner) {
        this.object = MiscUtil.requireNonNull(
                ObjectTypeUtil.getPrismObjectFromReference(potentialOwner.getCandidateOwnerRef()),
                () -> new IllegalStateException("No focus object"));
        this.newOwner = false;
        this.identifier = potentialOwner.getIdentifier();
    }

    /**
     * Creates a DTO in the case of new owner (pre-focus).
     */
    CorrelationOptionDto(@NotNull ResourceObjectOwnerOptionType potentialOwner, @NotNull ObjectReferenceType preFocus) {
        this.object = MiscUtil.requireNonNull(
                ObjectTypeUtil.getPrismObjectFromReference(preFocus),
                () -> new IllegalStateException("No focus object"));
        this.newOwner = true;
        this.identifier = potentialOwner.getIdentifier();
    }

    /**
     * Returns all real values matching given item path. The path should not contain container IDs.
     */
    public CorrelationPropertyValues getPropertyValues(CorrelationPropertyDefinition def) {
        Set<String> primaryValues;
        Set<String> secondaryValues;
        if (newOwner) {
            primaryValues = getValuesForPath(def.getSourcePath());
            secondaryValues = Set.of();
        } else {
            primaryValues = getValuesForPath(def.getPrimaryTargetPath());
            secondaryValues = def.getSecondaryTargetPath() != null ?
                    getValuesForPath(def.getSecondaryTargetPath()) : Set.of();
        }
        return new CorrelationPropertyValues(primaryValues, secondaryValues);
    }

    private @NotNull Set<String> getValuesForPath(ItemPath path) {
        return MatchingUtil.getValuesForPath(object, path);
    }

    public @NotNull PrismObject<?> getObject() {
        return object;
    }

    public boolean isNewOwner() {
        return newOwner;
    }

    public String getReferenceId() {
        return object.getOid();
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    /** Returns true if the option matches given case/work item outcome URI. */
    public boolean matches(@NotNull String outcome) {
        return identifier.equals(outcome);
    }
}
