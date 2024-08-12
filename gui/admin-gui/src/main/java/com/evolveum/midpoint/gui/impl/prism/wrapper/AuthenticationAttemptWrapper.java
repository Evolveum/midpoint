/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.Collection;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationAttemptDataType;

public class AuthenticationAttemptWrapper extends PrismPropertyWrapperImpl<AuthenticationAttemptDataType> {
    private static final long serialVersionUID = 1L;

    public AuthenticationAttemptWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<AuthenticationAttemptDataType> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public <D extends ItemDelta<?, ?>> Collection<D> getDelta() throws SchemaException {
        return computeDeltaInternal();
    }


}
