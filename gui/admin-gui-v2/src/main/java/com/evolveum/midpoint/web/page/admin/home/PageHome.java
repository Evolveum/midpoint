/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class PageHome extends PageAdmin {

    public PageHome() {
        initLayout();
    }

    private void initLayout() {
        AjaxLinkButton button = new AjaxLinkButton("simpleButton", new Model<String>("Simple")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(button);

        button = new AjaxLinkButton("leftButton", AjaxLinkButton.Type.LEFT, new Model<String>("Left")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(button);
        button = new AjaxLinkButton("middleButton", AjaxLinkButton.Type.MIDDLE, new Model<String>("Middle")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(button);
        button = new AjaxLinkButton("rightButton", AjaxLinkButton.Type.RIGHT, new Model<String>("Right")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(button);

        button = new AjaxLinkButton("saveButton", AjaxLinkButton.Type.SIMPLE, new Model<String>("Save"), "img/disk.png") {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {

            }
        };
        add(button);
    }
}
