/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.attributes.AjaxCallListener;

public class QueryPlaygroundPanelAjaxListener extends AjaxCallListener {

    @Override
    public CharSequence getBeforeHandler(Component component) {
        return "window.MidPointAceEditor.addCursorPositionAttribute(attrs)";
    }
}
