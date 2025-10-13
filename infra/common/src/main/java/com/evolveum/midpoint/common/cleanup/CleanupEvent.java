/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.cleanup;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @param source either {@link PrismContainer} or {@link PrismContainerValue}
 * @param path
 * @param item
 * @param result
 * @param <I>
 */
public record CleanupEvent<I>(
        @NotNull Object source,
        @NotNull ItemPath path,
        @NotNull I item,
        @NotNull CleanupResult result) {
}
