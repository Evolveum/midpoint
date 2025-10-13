/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgeListPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface WizardStep extends Serializable {

    default void init(WizardModel wizard) {
    }

    default String getStepId() {
        return null;
    }

    default String appendCssToWizard() {
        return null;
    }

    default Component createHeaderContent(String id) {
        return new BadgeListPanel(id, getTitleBadges());
    }

    default IModel<String> getTitle() {
        String key = getClass().getSimpleName() + ".title";
        return () -> Application.get().getResourceSettings().getLocalizer().getString(key, null, key);
    }

    default IModel<List<Badge>> getTitleBadges() {
        return Model.ofList(new ArrayList<>());
    }

    default VisibleEnableBehaviour getStepsBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    default VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    default VisibleEnableBehaviour getBackBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    default VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_VISIBLE_ENABLED;
    }

    /**
     * @return flag whether default "back" button action should be executed.
     * If true, default behaviour of back button will be executed as well
     * If false, only code in this method will be executed
     */
    default boolean onBackPerformed(AjaxRequestTarget target) {
        return true;
    }

    /**
     * @return flag whether default "next" button action should be executed.
     * If true, default behaviour of next button will be executed as well
     * If false, only code in this method will be executed
     */
    default boolean onNextPerformed(AjaxRequestTarget target) {
        return true;
    }

    default void applyState() {
    }

    default IModel<Boolean> isStepVisible() { return () -> true; }
}
