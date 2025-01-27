/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import com.evolveum.midpoint.schema.internals.InternalsConfig;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Represents a resource object at the UCF layer, usually retrieved from the resource (directly or as part of a change).
 *
 * The object must have a correct definition, including auxiliary object classes. For other conditions, see
 * {@link #checkConsistence()}.
 *
 * NOTE: This class implements {@link AbstractShadow}, although it exists without resource OID.
 * We believe the functionality provided by that mixin provides more benefits than complications presented
 * by missing resource OID. However, this is to be reconsidered some day.
 */
public class UcfResourceObject extends UcfResourceObjectFragment {

    /** The value of the primary identifier of the object, e.g., ConnId UID. */
    @NotNull private final Object primaryIdentifierValue;

    private UcfResourceObject(
            @NotNull ShadowType bean,
            @NotNull Object primaryIdentifierValue,
            @NotNull UcfErrorState errorState) {
        super(bean, errorState);
        this.primaryIdentifierValue = primaryIdentifierValue;
        checkConsistence();
    }

    public static UcfResourceObject of(@NotNull PrismObject<ShadowType> resourceObject, @NotNull Object primaryIdentifierValue) {
        return new UcfResourceObject(resourceObject.asObjectable(), primaryIdentifierValue, UcfErrorState.success());
    }

    public static UcfResourceObject of(@NotNull ShadowType resourceObject, @NotNull Object primaryIdentifierValue) {
        return new UcfResourceObject(resourceObject, primaryIdentifierValue, UcfErrorState.success());
    }

    public static UcfResourceObject of(
            @NotNull PrismObject<ShadowType> resourceObject, @NotNull Object primaryIdentifierValue, @NotNull UcfErrorState errorState) {
        return new UcfResourceObject(resourceObject.asObjectable(), primaryIdentifierValue, errorState);
    }

    public static UcfResourceObject of(
            @NotNull ShadowType resourceObject, @NotNull Object primaryIdentifierValue, @NotNull UcfErrorState errorState) {
        return new UcfResourceObject(resourceObject, primaryIdentifierValue, errorState);
    }

    public @NotNull Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[%s: %s (%s)]".formatted(primaryIdentifierValue, bean, errorState);
    }

    @Override
    public void checkConsistence() {
        super.checkConsistence();
        if (!InternalsConfig.consistencyChecks) {
            return;
        }
        try {
            var primaryIdentifierAttribute = stateNonNull(
                    ShadowUtil.getPrimaryIdentifier(bean),
                    "No primary identifier in %s", this);
            var valueFromAttribute = primaryIdentifierAttribute.getStringOrigValue();
            stateCheck(valueFromAttribute.equals(primaryIdentifierValue),
                    "Primary identifier value mismatch in %s: attribute: %s, explicit value: %s",
                    this, primaryIdentifierAttribute, primaryIdentifierValue);
        } catch (SchemaException e) {
            throw checkFailedException(e);
        }
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(
                this.getClass().getSimpleName() + " [" + primaryIdentifierValue + "]", indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "errorState", errorState, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "bean", bean, indent + 1);
        return sb.toString();
    }

    @Override
    public @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean) {
        return new UcfResourceObject(newBean, primaryIdentifierValue, errorState);
    }

    @Override
    public UcfResourceObject clone() {
        return new UcfResourceObject(
                bean.clone(),
                primaryIdentifierValue,
                errorState);
    }
}
