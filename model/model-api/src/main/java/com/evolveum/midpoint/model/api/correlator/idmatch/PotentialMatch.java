/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator.idmatch;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchAttributesType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a potential match obtained from ID Match service.
 */
public class PotentialMatch implements DebugDumpable {

    /**
     * Integer [0,100]. May be missing.
     */
    private final Integer confidence;

    /**
     * Reference ID. Null value means "create a new identity". (If the service provides such option.)
     */
    @Nullable private final String referenceId;

    /**
     * Attributes of the identity.
     */
    @NotNull private final IdMatchAttributesType attributes;

    public PotentialMatch(Integer confidence, @Nullable String referenceId, @NotNull IdMatchAttributesType attributes) {
        this.confidence = confidence;
        this.referenceId = referenceId;
        this.attributes = attributes;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public Double getConfidenceScaledToOne() {
        return confidence != null ?
                confidence / 100.0 : null;
    }

    public @Nullable String getReferenceId() {
        return referenceId;
    }

    /** Is this the option to create a new reference ID (i.e. new identity)? */
    public boolean isNewIdentity() {
        return referenceId == null;
    }

    public @NotNull IdMatchAttributesType getAttributes() {
        return attributes;
    }

    /**
     * Note that comparing attributes may be tricky (because of raw values).
     * But let's try it.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PotentialMatch that = (PotentialMatch) o;
        return Objects.equals(confidence, that.confidence)
                && Objects.equals(referenceId, that.referenceId)
                && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confidence, referenceId);
    }

    @Override
    public String toString() {
        return "PotentialMatch{" +
                "confidence=" + confidence +
                ", referenceId='" + referenceId + '\'' +
                ", attributes=" + attributes +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "confidence", confidence, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "referenceId", referenceId, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "attributes", attributes, indent + 1);
        return sb.toString();
    }
}
