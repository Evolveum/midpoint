/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
