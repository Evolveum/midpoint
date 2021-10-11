/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;

/**
 * Interface for objects that provide Wicket model which represents
 * object count or similar tag. The count in usually displayed as a
 * small "bubble" in the tab, next to the menu item, etc.
 *
 * @author semancik
 */
@FunctionalInterface
public interface CountModelProvider {

    /**
     * Return count model. May return null. If null is
     * returned then no count should be displayed.
     */
    IModel<String> getCountModel();

}
