/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.Objects;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.*;
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
public class UcfResourceObject
        implements DebugDumpable, ShortDumpable, Checkable, AbstractShadow {

    /**
     * The resource object itself.
     */
    @NotNull private final ShadowType bean;

    /** The value of the primary identifier of the object, e.g., ConnId UID. */
    @NotNull private final Object primaryIdentifierValue;

    /** Error state of the object. May be set e.g. if the object could not be correctly translated from ConnId. */
    @NotNull private final UcfErrorState errorState;

    private UcfResourceObject(
            @NotNull ShadowType bean,
            @NotNull Object primaryIdentifierValue,
            @NotNull UcfErrorState errorState) {
        this.bean = bean;
        this.primaryIdentifierValue = primaryIdentifierValue;
        this.errorState = errorState;
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

    @Override
    public boolean canHaveNoResourceOid() {
        return true;
    }

    public @NotNull ShadowType getBean() {
        return bean;
    }

    public @NotNull Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    public @NotNull UcfErrorState getErrorState() {
        return errorState;
    }

    @Override
    public String toString() {
        return "UcfResourceObject[%s: %s (%s)]".formatted(primaryIdentifierValue, bean, errorState);
    }

    @Override
    public void checkConsistence() {
        if (InternalsConfig.consistencyChecks) {
            bean.asPrismObject().checkConsistence();
        }
        try {
            // Class name check
            var classNameInData = getObjectClassName();
            var classNameInDefinition = ShadowUtil.getResourceObjectDefinition(bean).getTypeName();
            stateCheck(QNameUtil.match(classNameInData, classNameInDefinition),
                    "Object class mismatch in %s: data %s, definition %s",
                    this, classNameInData, classNameInDefinition);

            // Other checks
            stateNonNull(ShadowUtil.getPrimaryIdentifier(bean) != null, "No primary identifier in %s", this);
        } catch (SchemaException e) {
            throw new IllegalStateException("Consistency check failed for " + this + ": " + e.getMessage(), e);
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
    public void shortDump(StringBuilder sb) {
        sb.append(ShadowUtil.shortDumpShadow(bean));
        if (errorState.isError()) {
            sb.append(" (").append(errorState).append(")");
        }
    }

    @Override
    public @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean) {
        return new UcfResourceObject(newBean, primaryIdentifierValue, errorState);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public AbstractShadow clone() {
        return new UcfResourceObject(
                bean.clone(),
                primaryIdentifierValue,
                errorState);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (UcfResourceObject) obj;
        return Objects.equals(this.bean, that.bean)
                && Objects.equals(this.primaryIdentifierValue, that.primaryIdentifierValue)
                && Objects.equals(this.errorState, that.errorState);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bean, primaryIdentifierValue, errorState);
    }
}
