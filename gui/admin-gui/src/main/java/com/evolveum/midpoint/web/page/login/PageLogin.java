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

package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.web.component.login.LocalePanel;
import com.evolveum.midpoint.web.component.menu.left.LeftMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.PageHome;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mserbak
 */
public class PageLogin extends PageBase {

    private static final long serialVersionUID = 4077059807145096420L;

    public PageLogin() {
        LocalePanel locale = new LocalePanel("loginPanel");
        addOrReplace(locale);

        Form form = new Form("loginForm") {

            @Override
            protected void onSubmit() {
                MidPointAuthWebSession session = MidPointAuthWebSession.getSession();

                RequiredTextField<String> username = (RequiredTextField) get("username");
                PasswordTextField password = (PasswordTextField) get("password");
                if (session.authenticate(username.getModelObject(), password.getModelObject())) {
                    if (!continueToOriginalDestination()) {
                        setResponsePage(PageHome.class);
                    }
                }
            }
        };
        
        form.add(new Image("image", new PackageResourceReference(PageLogin.class, "Key.png")));
        
        form.add(new RequiredTextField("username", new Model<String>()));
        form.add(new PasswordTextField("password", new Model<String>()));
        add(form);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new Model<String>("");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.renderCSSReference(new PackageResourceReference(PageLogin.class, "PageLogin.css"));
    }

    @Override
    public List<LeftMenuItem> getLeftMenuItems() {
        return new ArrayList<LeftMenuItem>();
    }

    @Override
    public List<TopMenuItem> getTopMenuItems() {
        return new ArrayList<TopMenuItem>();
    }

    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        return new ArrayList<BottomMenuItem>();
    }
}
