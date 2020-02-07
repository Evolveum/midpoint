/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.prism;

/**
 * @author semancik
 *
 */
public enum ItemVisibility {

    /**
     * Element always visible.
     */
    VISIBLE,

    /**
     * Visibility determined automatically (e.g. based on "show empty" button).
     */
    AUTO,

    /**
     * Element always hidded (not visible).
     */
    HIDDEN;

}
