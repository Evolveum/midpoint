/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import com.evolveum.midpoint.xml.ns._public.common.prism_schema_3.PrismSchemaType;


import java.util.Collection;

public class PrismSchemaValueWrapperImpl extends PrismContainerValueWrapperImpl<PrismSchemaType> {

    private static final long serialVersionUID = 1L;

    public PrismSchemaValueWrapperImpl(PrismContainerWrapper<PrismSchemaType> parent, PrismContainerValue<PrismSchemaType> pcv, ValueStatus status) {
        super(parent, pcv, status);
    }

    @Override
    public Collection<ItemDelta> getDeltas() throws SchemaException {
        return super.getDeltas();
    }
}
