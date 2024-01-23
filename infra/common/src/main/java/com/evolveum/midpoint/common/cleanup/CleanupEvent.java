/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.cleanup;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;

public record CleanupEvent<I>(
        @NotNull CleanupResult result,
        @NotNull PrismObject<?> object,
        @NotNull ItemPath path,
        @NotNull I value) {

}
