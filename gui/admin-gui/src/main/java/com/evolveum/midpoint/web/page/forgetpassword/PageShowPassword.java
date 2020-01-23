/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.forgetpassword;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.application.PageDescriptor;
import org.apache.wicket.markup.html.basic.Label;

@PageDescriptor(url = "/resetpasswordsuccess", permitAll = true)
public class PageShowPassword extends PageBase {

    public final static String URL = "/resetpasswordsuccess";

    public PageShowPassword() {
        add(new Label("pass", getSession().getAttribute("pwdReset")));
        getSession().removeAttribute("pwdReset");

        success(getString("PageShowPassword.success"));
        add(getFeedbackPanel());
    }

    @Override
    protected void createBreadcrumb() {
        //don't create breadcrumb for this page
    }
}

