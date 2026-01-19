/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.smart.impl.wellknownschemas;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.jetbrains.annotations.Nullable;

/**
 * System-provided mapping suggestion containing the data needed to construct a mapping.
 */
public record SystemMappingSuggestion(
        ItemPath shadowAttributePath,
        ItemPath focusPropertyPath,
        @Nullable ExpressionType expression) {
}
