/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public void onError(AjaxRequestTarget target, Form<?> form) {
        Page page = component.getPage();

        if (page instanceof PageBase) {
            target.add(((PageBase) page).getFeedbackPanel());
        }
    }
}
