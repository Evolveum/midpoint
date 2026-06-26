/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serializable;

/**
 * Represents a single step in a {@link PopupStepperPanel} workflow.
 *
 * <p>A step provides the content component to be displayed and may customize
 * navigation labels, icons, visibility, and styling. Steps can be dynamically
 * shown or hidden using {@link #isStepVisible()}.</p>
 *
 * <p>For most use cases, extend {@link BasicPopupStepPanel} instead of
 * implementing this interface directly.</p>
 */
public interface PopupStep extends Serializable {

    default IModel<String> getTitle() {
        return Model.of();
    }
    ;

    default IModel<String> getSubTitle() {
        return Model.of();
    }

    default IModel<Boolean> isStepVisible() {
        return Model.of(true);
    }

    default void init(PopupStepperModel model) {
    }

    Component getPanel();

    default IModel<String> getExitLabel() {
        return null;
    }

    default IModel<String> getBackLabel() {
        return null;
    }

    default IModel<String> getNextLabel() {
        return null;
    }

    default IModel<String> getFinishLabel() {
        return null;
    }

    default IModel<String> getBackIcon() {
        return null;
    }

    default IModel<String> getNextIcon() {
        return null;
    }

    default IModel<String> getFinishIcon() {
        return null;
    }

    default String getNextCssClass() {
        return null;
    }

    default String getFinishCssClass() {
        return null;
    }
}
