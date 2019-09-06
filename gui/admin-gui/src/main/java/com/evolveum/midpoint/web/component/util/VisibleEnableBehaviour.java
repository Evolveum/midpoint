/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;

/**
 * TODO: move to com.evolveum.midpoint.gui.api.util
 *
 * @author lazyman
 */
public class VisibleEnableBehaviour extends Behavior {
	private static final long serialVersionUID = 1L;

	public static final VisibleEnableBehaviour ALWAYS_VISIBLE_ENABLED = new VisibleEnableBehaviour();

    public boolean isVisible() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onConfigure(Component component) {
        component.setEnabled(isEnabled());

        boolean visible = isVisible();
        component.setVisible(visible);
        component.setVisibilityAllowed(visible);
    }
}
