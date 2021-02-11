/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadowcache;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExternalResourceObjectChange;
import com.evolveum.midpoint.provisioning.impl.shadowcache.sync.ChangeProcessingBeans;

/**
 * Adopted "external" change.
 */
public class AdoptedExternalChange extends AdoptedChange<ExternalResourceObjectChange> {

    public AdoptedExternalChange(@NotNull ExternalResourceObjectChange resourceObjectChange, boolean simulate,
            ChangeProcessingBeans beans) {
        super(resourceObjectChange, simulate, beans);
    }
}
