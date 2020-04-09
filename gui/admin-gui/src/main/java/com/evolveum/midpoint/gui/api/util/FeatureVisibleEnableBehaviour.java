/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.util;

import com.evolveum.midpoint.gui.api.GuiFeature;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

/**
 * Behaviour class that determines visibility of (configurable) UI feature.
 *
 * @author semancik
 */
public class FeatureVisibleEnableBehaviour extends VisibleEnableBehaviour {
    private static final long serialVersionUID = 1L;

    UserInterfaceElementVisibilityType visibility;

    public FeatureVisibleEnableBehaviour(GuiFeature feature, CompiledGuiProfile userProfile) {
        super();
        visibility = userProfile.getFeatureVisibility(feature.getUri());
    }

    @Override
    public boolean isVisible() {
        return CompiledGuiProfile.isVisible(visibility, this::isVisibleAutomatic);
    }

    /**
     * Method to be overridden in subclasses, determines automatic visibility of
     * the feature. It will be called only if needed.
     */
    public boolean isVisibleAutomatic() {
        return true;
    }
}
