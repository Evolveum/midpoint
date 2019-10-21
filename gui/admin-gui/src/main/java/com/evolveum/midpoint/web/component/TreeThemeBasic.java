/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;

/**
 * @author lazyman
 */
public class TreeThemeBasic extends Behavior {

    @Override
    public void onComponentTag(Component component, ComponentTag tag) {
        tag.append("class", "tree-theme-basic", " ");
    }
}
