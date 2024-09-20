/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExternalResourceObjectChange;

/**
 * Shadowed "external" change.
 */
public class ShadowedExternalChange extends ShadowedChange<ExternalResourceObjectChange> {

    public ShadowedExternalChange(@NotNull ExternalResourceObjectChange resourceObjectChange) {
        super(resourceObjectChange);
    }

    @Override
    protected String getDefaultChannel() {
        return null; // TODO ?
    }
}
