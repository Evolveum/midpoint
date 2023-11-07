/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * `primaryIdentifierValue` provides the value of the object primary identifier (e.g. ConnId UID).
 *
 * Conditions:
 *
 * - when the object was successfully retrieved: Not null.
 * - if there was an error: Usually not null (e.g. never in ConnId 1.x). But this may change in the future.
 *
 * (The error is not recorded here.)
 */
public record UcfResourceObject(
        @NotNull ShadowType bean,
        Object primaryIdentifierValue)
        implements DebugDumpable, ShortDumpable {

    public static UcfResourceObject of(@NotNull PrismObject<ShadowType> resourceObject, Object primaryIdentifierValue) {
        return new UcfResourceObject(resourceObject.asObjectable(), primaryIdentifierValue);
    }

    public @NotNull PrismObject<ShadowType> getPrismObject() {
        return bean.asPrismObject();
    }

    @Override
    public String toString() {
        return "UcfResourceObject[%s: %s]".formatted(primaryIdentifierValue, bean);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder(
                this.getClass().getSimpleName() + " [" + primaryIdentifierValue + "]", indent);
        DebugUtil.debugDumpWithLabel(sb, "bean", bean, indent + 1);
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(ShadowUtil.shortDumpShadow(bean));
    }

    public @NotNull Collection<ResourceAttribute<?>> getAllIdentifiers() {
        return emptyIfNull(ShadowUtil.getAllIdentifiers(bean));
    }
}
