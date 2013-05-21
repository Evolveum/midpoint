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

package com.evolveum.midpoint.web.page.admin.resources.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Panel;

/**
 * @author lazyman
 */
public class ContentPanel extends Panel {

    public ContentPanel(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        AjaxLink accounts = new AjaxLink("accounts") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                accountsPerformed(target);
            }
        };
        add(accounts);

//        AjaxLink entitlements = new AjaxLink("entitlements") {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                entitlementsPerformed(target);
//            }
//        };
//        add(entitlements);
    }

    public void accountsPerformed(AjaxRequestTarget target) {

    }

    public void entitlementsPerformed(AjaxRequestTarget target) {

    }
}
