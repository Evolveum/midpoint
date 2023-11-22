/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ErrorState;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Represents the resource object after it was initialized at the "resource objects" level, successfully or not.
 *
 * It is intended as a return value from various methods that return a resource object.
 * (Except for {@link ResourceObjectConverter#searchResourceObjects(ProvisioningContext, ResourceObjectHandler, ObjectQuery,
 * boolean, FetchErrorReportingMethodType, OperationResult)} that should return lazily-initializable objects!)
 *
 * @see ResourceObjectFound
 */
public record CompleteResourceObject (
        @NotNull ResourceObject resourceObject,
        @NotNull LimitationReason limitationReason,
        @NotNull ErrorState errorState) implements Serializable, DebugDumpable {

    public static @NotNull CompleteResourceObject of(@NotNull ResourceObject resourceObject, @NotNull ErrorState errorState) {
        if (errorState.isOk()) {
            return new CompleteResourceObject(resourceObject, LimitationReason.NONE, errorState);
        } else {
            return new CompleteResourceObject(resourceObject, LimitationReason.ERROR, errorState);
        }
    }

    @Contract("null -> null; !null -> !null")
    static CompleteResourceObject deletedNullable(@Nullable ResourceObject resourceObject) {
        if (resourceObject != null) {
            return new CompleteResourceObject(resourceObject, LimitationReason.OBJECT_DELETION, ErrorState.ok());
        } else {
            return null;
        }
    }

    public @NotNull ShadowType getBean() {
        return resourceObject.getBean();
    }

    public @NotNull PrismObject<ShadowType> getPrismObject() {
        return resourceObject.getPrismObject();
    }

    @Contract("null -> null; !null -> !null")
    public static ShadowType getBean(CompleteResourceObject completeResourceObject) {
        return completeResourceObject != null ? completeResourceObject.getBean() : null;
    }

    @Contract("null -> null; !null -> !null")
    public static ResourceObject getResourceObject(CompleteResourceObject completeResourceObject) {
        return completeResourceObject != null ? completeResourceObject.resourceObject : null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + resourceObject +
                ", limitationReason=" + limitationReason +
                ", errorState=" + errorState +
                '}';
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "limitationReason", limitationReason, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "errorState", String.valueOf(errorState), indent + 1);
        return sb.toString();
    }

    /** Why the full object data may be missing. */
    public enum LimitationReason {

        /** The object is complete. */
        NONE,

        /** The object was deleted on the resource. */
        OBJECT_DELETION,

        /** There was an error in processing, see the error state. */
        ERROR
    }
}
