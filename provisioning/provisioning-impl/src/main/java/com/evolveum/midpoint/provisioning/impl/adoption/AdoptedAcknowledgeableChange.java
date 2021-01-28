/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.adoption;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectChange;
import com.evolveum.midpoint.provisioning.impl.sync.ChangeProcessingBeans;
import com.evolveum.midpoint.schema.AcknowledgementSink;

/**
 * Adopted change that can also receive acknowledgements.
 */
public abstract class AdoptedAcknowledgeableChange<ROC extends ResourceObjectChange>
        extends AdoptedChange<ROC>
        implements AcknowledgementSink {

    AdoptedAcknowledgeableChange(@NotNull ROC resourceObjectChange, boolean simulate,
            ChangeProcessingBeans beans) {
        super(resourceObjectChange, simulate, beans);
    }
}
