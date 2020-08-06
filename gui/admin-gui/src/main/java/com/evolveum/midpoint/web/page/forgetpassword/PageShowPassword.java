/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.forgetpassword;

import org.apache.wicket.markup.html.basic.Label;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.application.PageDescriptor;

@PageDescriptor(url = "/resetpasswordsuccess", permitAll = true)
public class PageShowPassword extends PageBase {

    public static final String URL = "/resetpasswordsuccess";

    public PageShowPassword() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
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

