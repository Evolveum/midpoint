/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface WizardListener {

    default void onStepChanged(WizardStep newStep) {
    }

    default void onCancel() {
    }

    default void onFinish() {
    }
}
