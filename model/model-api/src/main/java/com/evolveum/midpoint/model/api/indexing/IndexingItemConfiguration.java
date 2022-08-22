/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
