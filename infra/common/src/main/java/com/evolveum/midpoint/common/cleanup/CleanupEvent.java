/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
