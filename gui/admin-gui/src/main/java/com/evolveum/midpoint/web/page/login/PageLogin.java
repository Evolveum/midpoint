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

package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.web.component.login.LocalePanel;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mserbak
 */
public class PageLogin extends PageBase {

    private static final String ID_LOGIN_PANEL = "loginPanel";
    private static final String ID_LOGIN_FORM = "loginForm";
    private static final String ID_IMAGE = "image";
    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";

    public PageLogin() {
        LocalePanel locale = new LocalePanel(ID_LOGIN_PANEL);
        addOrReplace(locale);

        Form form = new Form(ID_LOGIN_FORM) {

            @Override
            protected void onSubmit() {
                MidPointAuthWebSession session = MidPointAuthWebSession.getSession();

                RequiredTextField<String> username = (RequiredTextField) get(ID_USERNAME);
                PasswordTextField password = (PasswordTextField) get(ID_PASSWORD);
                if (session.authenticate(username.getModelObject(), password.getModelObject())) {
                    //continueToOriginalDestination();
                    setResponsePage(PageDashboard.class);
                }
            }
        };

        form.add(new Image(ID_IMAGE, new PackageResourceReference(PageLogin.class, "Key.png")));

        form.add(new RequiredTextField(ID_USERNAME, new Model<String>()));
        form.add(new PasswordTextField(ID_PASSWORD, new Model<String>()));
        add(form);
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return new Model<String>("");
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(new PackageResourceReference(PageLogin.class, "PageLogin.css")));
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
