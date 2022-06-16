/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.jetbrains.annotations.NotNull;

/**
 * TODO: move to com.evolveum.midpoint.gui.api.util
 *
 * @author lazyman
 */
public class VisibleEnableBehaviour extends Behavior {

    private static final long serialVersionUID = 1L;

    public static final VisibleEnableBehaviour ALWAYS_VISIBLE_ENABLED = new VisibleEnableBehaviour();

    public static final VisibleEnableBehaviour ALWAYS_INVISIBLE = new VisibleEnableBehaviour(() -> false);

    private SerializableSupplier<Boolean> visible;

    private SerializableSupplier<Boolean> enabled;

    public VisibleEnableBehaviour() {
        this(() -> true);
    }

    public VisibleEnableBehaviour(@NotNull SerializableSupplier<Boolean> visible) {
        this(visible, () -> true);
    }

    public VisibleEnableBehaviour(@NotNull SerializableSupplier<Boolean> visible, @NotNull SerializableSupplier<Boolean> enabled) {
        this.visible = visible;
        this.enabled = enabled;
    }

    public boolean isVisible() {
        return visible.get();
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void onConfigure(Component component) {
        component.setEnabled(isEnabled());

        boolean visible = isVisible();
        component.setVisible(visible);
        component.setVisibilityAllowed(visible);
    }
}
