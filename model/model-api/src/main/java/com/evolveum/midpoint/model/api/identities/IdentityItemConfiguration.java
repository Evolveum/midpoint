/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
