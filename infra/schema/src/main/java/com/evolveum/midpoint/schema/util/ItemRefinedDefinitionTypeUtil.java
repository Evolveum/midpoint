/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

import org.jetbrains.annotations.NotNull;

public class ItemRefinedDefinitionTypeUtil {

    public static @NotNull ItemPath getRef(@NotNull ItemRefinedDefinitionType bean) throws ConfigurationException {
        return MiscUtil.configNonNull(
                        bean.getRef(), () -> "No 'ref' in " + bean)
                .getItemPath();
    }
}
