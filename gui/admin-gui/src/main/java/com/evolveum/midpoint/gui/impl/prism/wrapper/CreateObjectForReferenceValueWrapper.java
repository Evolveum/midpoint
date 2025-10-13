/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

public abstract class CreateObjectForReferenceValueWrapper<T extends Referencable> extends PrismReferenceValueWrapperImpl<T>{

    public CreateObjectForReferenceValueWrapper(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    public abstract ContainerPanelConfigurationType createContainerConfiguration();

    public boolean isHeaderOfCreateObjectVisible(){
        return false;
    }
}
