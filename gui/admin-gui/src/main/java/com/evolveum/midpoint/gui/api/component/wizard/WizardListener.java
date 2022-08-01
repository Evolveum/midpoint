/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
