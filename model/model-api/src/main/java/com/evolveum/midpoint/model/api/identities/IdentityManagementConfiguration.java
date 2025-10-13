/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.identities;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface IdentityManagementConfiguration {

    @NotNull Collection<? extends IdentityItemConfiguration> getItems();

    @Nullable IdentityItemConfiguration getForPath(@NotNull ItemPath path);
}
