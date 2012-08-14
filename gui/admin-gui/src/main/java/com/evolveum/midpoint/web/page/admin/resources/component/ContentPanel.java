/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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

        AjaxLink entitlements = new AjaxLink("entitlements") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                entitlementsPerformed(target);
            }
        };
        add(entitlements);
    }

    public void accountsPerformed(AjaxRequestTarget target) {

    }

    public void entitlementsPerformed(AjaxRequestTarget target) {

    }
}
