/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;

/**
 * @author semancik
 *
 */
@Component
public class FocusComputer {

    @Autowired private ActivationComputer activationComputer;

    public void recompute(PrismObject<? extends FocusType> focusObject, LifecycleStateModelType lifecycleModel) {
        FocusType focus = focusObject.asObjectable();
        ActivationType activation = focus.getActivation();
        if (activation != null) {
            activationComputer.setValidityAndEffectiveStatus(focus.getLifecycleState(), activation, lifecycleModel);
        }
    }

}
