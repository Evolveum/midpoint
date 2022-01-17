/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator.idmatch;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;

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
     * Reference ID. If a record comes from ID Match without reference ID, it is not included here.
     */
    @NotNull private final String referenceId;

    /**
     * Attributes of the identity.
     */
    @NotNull private final ShadowAttributesType attributes;

    public PotentialMatch(Integer confidence, @NotNull String referenceId, @NotNull ShadowAttributesType attributes) {
        this.confidence = confidence;
        this.referenceId = referenceId;
        this.attributes = attributes;
    }

    public int getConfidence() {
        return confidence;
    }

    public @NotNull String getReferenceId() {
        return referenceId;
    }

    public @NotNull ShadowAttributesType getAttributes() {
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
                && referenceId.equals(that.referenceId)
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
