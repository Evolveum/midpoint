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
import com.evolveum.midpoint.xml.ns._public.common.common_3.LifecycleStateModelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@Component
public class UserComputer {

    @Autowired(required = true)
    private ActivationComputer activationComputer;

    public void recompute(PrismObject<UserType> user, LifecycleStateModelType lifecycleModel) {
        UserType userType = user.asObjectable();
        ActivationType activationType = userType.getActivation();
        if (activationType != null) {
            activationComputer.computeEffective(userType.getLifecycleState(), activationType, lifecycleModel);
        }
    }

}
