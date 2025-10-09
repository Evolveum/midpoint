/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.error.ConfigErrorReporter;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemRefinedDefinitionTypeUtil {

    public static @NotNull ItemPath getRef(@NotNull ItemRefinedDefinitionType bean) throws ConfigurationException {
        ItemPathType ref = bean.getRef();
        if (ref == null) {
            throw new ConfigurationException("No 'ref' in " + ConfigErrorReporter.describe(bean));
        }
        return ref.getItemPath();
    }
}
