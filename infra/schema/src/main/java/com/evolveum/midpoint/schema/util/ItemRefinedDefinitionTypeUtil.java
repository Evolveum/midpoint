/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
