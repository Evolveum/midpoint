/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BehaviourDelegator extends Behavior {

    private SerializableSupplier<Behavior> behaviour;

    public BehaviourDelegator(@NotNull SerializableSupplier<Behavior> behaviour) {
        this.behaviour = behaviour;
    }

    @Override
    public void onConfigure(Component component) {
        Behavior real = behaviour.get();
        if (real != null) {
            real.onConfigure(component);
        }
    }
}
