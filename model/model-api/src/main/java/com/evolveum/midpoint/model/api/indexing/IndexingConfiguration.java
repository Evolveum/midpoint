/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.indexing;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface IndexingConfiguration {

    @NotNull Collection<IndexingItemConfiguration> getItems() throws ConfigurationException;

    @Nullable IndexingItemConfiguration getForPath(@NotNull ItemPath path);

    boolean hasNoItems();
}
