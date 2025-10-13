/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.identities;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public interface IdentityItemConfiguration {

    @NotNull QName getName();

    @SuppressWarnings("WeakerAccess")

    @NotNull String getLocalName();

    @NotNull ItemName getDefaultSearchItemName();

    @NotNull ItemPath getPath();
}
