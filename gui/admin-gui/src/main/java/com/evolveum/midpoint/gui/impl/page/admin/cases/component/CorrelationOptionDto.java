/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PotentialOwnerType;

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
     * URI corresponding to this choice. It should be sent to the case management engine when completing this request.
     */
    @NotNull private final String uri;

    /**
     * Creates a DTO in the case of existing owner.
     */
    CorrelationOptionDto(@NotNull PotentialOwnerType potentialOwner) {
        this.object = MiscUtil.requireNonNull(
                ObjectTypeUtil.getPrismObjectFromReference(potentialOwner.getCandidateOwnerRef()),
                () -> new IllegalStateException("No focus object"));
        this.newOwner = false;
        this.uri = potentialOwner.getUri();
    }

    /**
     * Creates a DTO in the case of new owner.
     */
    CorrelationOptionDto(@NotNull ObjectReferenceType reference) {
        this.object = MiscUtil.requireNonNull(
                ObjectTypeUtil.getPrismObjectFromReference(reference),
                () -> new IllegalStateException("No focus object"));
        this.newOwner = true;
        this.uri = SchemaConstants.CORRELATION_NONE_URI;
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
        return object.getAllValues(path).stream()
                .filter(Objects::nonNull)
                .map(PrismValue::getRealValue)
                .map(String::valueOf)
                .collect(Collectors.toSet());
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

    public @NotNull String getUri() {
        return uri;
    }
}
