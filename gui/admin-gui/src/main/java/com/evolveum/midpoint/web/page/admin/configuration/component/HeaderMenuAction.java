/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;

/**
 * @author lazyman
 */
public class HeaderMenuAction extends InlineMenuItemAction {

    private Component component;

    public HeaderMenuAction(Component component) {
        this.component = component;
    }

    @Override
    public void onError(AjaxRequestTarget target) {
        Page page = component.getPage();

        if (page instanceof PageBase) {
            target.add(((PageBase) page).getFeedbackPanel());
        }
    }
}
