/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationAttemptDataType;

public class AuthenticationAttemptValueWrapper extends PrismPropertyValueWrapper<AuthenticationAttemptDataType> {

    /**
     * @param parent
     * @param value
     * @param status
     */
    public AuthenticationAttemptValueWrapper(PrismPropertyWrapper<AuthenticationAttemptDataType> parent, PrismPropertyValue<AuthenticationAttemptDataType> value, ValueStatus status) {
        super(parent, value, status);
    }

    @Override
    protected PrismPropertyValue<AuthenticationAttemptDataType> getNewValueWithMetadataApplied() throws SchemaException {
        return getNewValue().clone();
    }
}
