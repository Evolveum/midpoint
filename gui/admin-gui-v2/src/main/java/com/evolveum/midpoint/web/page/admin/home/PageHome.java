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
import com.evolveum.midpoint.web.component.message.MainFeedback;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;

/**
 * @author lazyman
 */
public class PageHome extends PageAdmin {

    public PageHome() {
        initLayout();
    }

    private void initLayout() {
        Form form = new Form("mainForm");
        add(form);

        final MainFeedback feedback = new MainFeedback("feedback");
        form.add(feedback);

        AjaxLinkButton test = new AjaxLinkButton("test", createStringResource("test")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PageHome.this.error("some error message.");
                PageHome.this.info("info message.");
                PageHome.this.warn("warn message.");

                target.add(feedback);
            }
        };
        form.add(test);
    }
}
