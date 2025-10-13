/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.indexing;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ConfigurationException;

public interface IndexingItemConfiguration {

    @NotNull String getName();

    @NotNull ItemName getQualifiedName();

    @NotNull ItemPath getPath();

    @NotNull Collection<IndexedItemValueNormalizer> getNormalizers();

    IndexedItemValueNormalizer findNormalizer(@Nullable String index) throws ConfigurationException;

    IndexedItemValueNormalizer getDefaultNormalizer() throws ConfigurationException;
}
