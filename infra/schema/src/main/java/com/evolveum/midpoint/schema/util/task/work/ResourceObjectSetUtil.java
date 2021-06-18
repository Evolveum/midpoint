/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import static com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil.getQueryLegacy;
import static com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil.getSearchOptionsLegacy;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

public class ResourceObjectSetUtil {

    public static @NotNull ResourceObjectSetType fromLegacySource(@NotNull LegacyWorkDefinitionSource source) {
        PrismContainerValue<?> extension = source.getTaskExtension();
        return new ResourceObjectSetType(PrismContext.get())
                .resourceRef(source.getObjectRef())
                .objectclass(getItemRealValue(extension, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS, QName.class))
                .kind(getItemRealValue(extension, SchemaConstants.MODEL_EXTENSION_KIND, ShadowKindType.class))
                .intent(getItemRealValue(extension, SchemaConstants.MODEL_EXTENSION_INTENT, String.class))
                .objectQuery(getQueryLegacy(source))
                .searchOptions(getSearchOptionsLegacy(extension));
    }

    // TODO move to PCV
    static <T> T getItemRealValue(PrismContainerValue<?> pcv, ItemName name, Class<T> type) {
        return pcv != null ? pcv.getItemRealValue(name, type) : null;
    }
}
