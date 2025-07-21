/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getExtensionItemRealValue;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.setExtensionPropertyRealValues;

/**
 * Util methods related to {@link ShadowObjectClassStatisticsType} and its embedding in {@link GenericObjectType} (which
 * is a temporary solution until extra object type is designed for this).
 */
public class ShadowObjectClassStatisticsTypeUtil {

    public static ShadowObjectClassStatisticsType getStatisticsRequired(GenericObjectType holder) {
        return getStatisticsRequired(holder.asPrismObject());
    }

    public static ShadowObjectClassStatisticsType getStatisticsRequired(PrismObject<GenericObjectType> holder) {
        return MiscUtil.argNonNull(
                getExtensionItemRealValue(holder.asObjectable().getExtension(), SchemaConstants.MODEL_EXTENSION_STATISTICS),
                "No statistics in %s", holder);
    }

    public static GenericObjectType createStatisticsObject(
            String resourceOid, String resourceName, QName objectClassName, ShadowObjectClassStatisticsType statistics)
            throws SchemaException {
        var object = new GenericObjectType()
                .name("Statistics for %s:%s (%s)".formatted(
                        resourceName, objectClassName.getLocalPart(), statistics.getTimestamp()));
        var holderPcv = object.asPrismContainerValue();
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_RESOURCE_OID, resourceOid);
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_OBJECT_CLASS_LOCAL_NAME, objectClassName.getLocalPart());
        setExtensionPropertyRealValues(holderPcv, MODEL_EXTENSION_STATISTICS, statistics);
        return object;
    }
}
