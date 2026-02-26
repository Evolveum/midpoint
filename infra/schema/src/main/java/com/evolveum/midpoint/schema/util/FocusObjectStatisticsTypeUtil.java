/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 *
 */
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getExtensionItemRealValue;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.setExtensionPropertyRealValues;

/**
 * Util methods related to focus object statistics and their embedding in {@link GenericObjectType}.
 */
public class FocusObjectStatisticsTypeUtil {

    public static ShadowObjectClassStatisticsType getFocusObjectStatisticsRequired(GenericObjectType holder) {
        return getFocusObjectStatisticsRequired(holder.asPrismObject());
    }

    public static ShadowObjectClassStatisticsType getFocusObjectStatisticsRequired(PrismObject<GenericObjectType> holder) {
        ShadowObjectClassStatisticsType statistics = MiscUtil.argNonNull(
                getExtensionItemRealValue(holder.asObjectable().getExtension(), MODEL_EXTENSION_FOCUS_OBJECT_STATISTICS),
                "No focus object statistics in %s", holder);
        statistics.getAttribute();
        statistics.getAttributeTuple();
        return statistics;
    }

    public static @NotNull GenericObjectType createFocusObjectStatisticsObject(
            String objectTypeName,
            String resourceOid,
            String kind,
            String intent,
            ShadowObjectClassStatisticsType statistics)
            throws SchemaException {
        var object = new GenericObjectType()
                .name("Focus object statistics for %s on %s/%s/%s (%s)".formatted(
                        objectTypeName, resourceOid, kind, intent, statistics.getTimestamp()));
        var holderPcv = object.asPrismContainerValue();
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_FOCUS_OBJECT_TYPE_NAME, objectTypeName);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_RESOURCE_OID, resourceOid);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_KIND_NAME, kind);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_INTENT_NAME, intent);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_FOCUS_OBJECT_STATISTICS, statistics);
        return object;
    }
}
