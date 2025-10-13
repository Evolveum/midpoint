/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
