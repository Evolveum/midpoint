/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * TODO
 */
public class ExternalResourceObjectChange extends ResourceObjectChange {

    public ExternalResourceObjectChange(int localSequenceNumber,
            @NotNull Object primaryIdentifierRealValue,
            @NotNull Collection<ResourceAttribute<?>> identifiers,
            PrismObject<ShadowType> resourceObject,
            ObjectDelta<ShadowType> objectDelta) {
        super(localSequenceNumber, primaryIdentifierRealValue, identifiers, resourceObject, objectDelta);
    }

    @Override
    protected String toStringExtra() {
        return "";
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
    }

    public void preprocess(ProvisioningContext ctx) {
        this.context = ctx;
        processingState.setPreprocessed();
    }
}
