/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

    public void recompute(PrismObject<? extends FocusType> user, LifecycleStateModelType lifecycleModel) {
        FocusType focusType = user.asObjectable();
        ActivationType activationType = focusType.getActivation();
        if (activationType != null) {
            activationComputer.computeEffective(focusType.getLifecycleState(), activationType, lifecycleModel);
        }
    }

}
