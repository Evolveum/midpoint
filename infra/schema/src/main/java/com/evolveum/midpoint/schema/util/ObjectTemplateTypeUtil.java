/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

public class ObjectTemplateTypeUtil {

    public static @Nullable CompositeCorrelatorType getCorrelators(@Nullable ObjectTemplateType template) {
        if (template == null) {
            return null;
        }
        ObjectTemplateCorrelationType correlation = template.getCorrelation();
        return correlation != null ? correlation.getCorrelators() : null;
    }
}
