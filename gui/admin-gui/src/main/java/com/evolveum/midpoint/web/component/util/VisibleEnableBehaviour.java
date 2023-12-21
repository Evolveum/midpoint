/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serial;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.jetbrains.annotations.Nullable;

/**
 * TODO: move to com.evolveum.midpoint.gui.api.util
 *
 * Suppliers take precedence when evaluating visibility and enabled state before {@link #isVisible()} and {@link #isEnabled()} methods.
 *
 * @author lazyman
 */
public class VisibleEnableBehaviour extends Behavior {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final VisibleEnableBehaviour ALWAYS_VISIBLE_ENABLED = new VisibleEnableBehaviour();

    public static final VisibleEnableBehaviour ALWAYS_INVISIBLE = new VisibleEnableBehaviour(() -> false);

    private final SerializableSupplier<Boolean> visible;

    private final SerializableSupplier<Boolean> enabled;

    public VisibleEnableBehaviour() {
        this(null);
    }

    public VisibleEnableBehaviour(@Nullable SerializableSupplier<Boolean> visible) {
        this(visible, null);
    }

    public VisibleEnableBehaviour(@Nullable SerializableSupplier<Boolean> visible, @Nullable SerializableSupplier<Boolean> enabled) {
        this.visible = visible;
        this.enabled = enabled;
    }

    /**
     * @return Default implementation returns true even if underlying supplier returns null (this is because of backward compatibility of this class)
     *
     * @deprecated use {@link #VisibleEnableBehaviour(SerializableSupplier)} instead
     * This method doesn't properly handle null values (when behaviour can't decide on whether component should be visible or not).
     */
    @Deprecated
    public boolean isVisible() {
        if (visible == null) {
            return true;
        }
        return BooleanUtils.isNotFalse(visible.get());
    }

    /**
     * @return Default implementation returns true even if underlying supplier returns null (this is because of backward compatibility of this class)
     *
     * @deprecated use {@link #VisibleEnableBehaviour(SerializableSupplier, SerializableSupplier)} instead.
     * This method doesn't properly handle null values (when behaviour can't decide on whether component should be enabled or not).
     */
    @Deprecated
    public boolean isEnabled() {
        if (enabled == null) {
            return true;
        }
        return BooleanUtils.isNotFalse(enabled.get());
    }

    @Override
    public void onConfigure(Component component) {
        Boolean enabled = this.enabled != null ? this.enabled.get() : isEnabled();
        if (enabled != null) {
            component.setEnabled(enabled);
        }

        Boolean visible = this.visible != null ? this.visible.get() : isVisible();
        if (visible != null) {
            component.setVisible(visible);
            component.setVisibilityAllowed(visible);
        }
    }
}
