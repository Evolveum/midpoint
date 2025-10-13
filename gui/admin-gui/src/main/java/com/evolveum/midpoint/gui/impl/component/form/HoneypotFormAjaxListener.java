/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.form;

import com.evolveum.midpoint.web.component.form.MidpointForm;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.attributes.AjaxCallListener;

public class HoneypotFormAjaxListener extends AjaxCallListener {

    private final MidpointForm<?> mainForm;

    public HoneypotFormAjaxListener(MidpointForm<?> mainForm) {
        this.mainForm = mainForm;
    }

    @Override
    public CharSequence getBeforeHandler(Component component) {
        return "window.MidPointHoneypot.addRightAttributeForForm(attrs, '" + mainForm.getMarkupId() + "')";
    }
}
