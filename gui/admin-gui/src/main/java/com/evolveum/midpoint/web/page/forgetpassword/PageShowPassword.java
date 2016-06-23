package com.evolveum.midpoint.web.page.forgetpassword;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.application.PageDescriptor;
import org.apache.wicket.markup.html.basic.Label;

@PageDescriptor(url = "/resetpasswordsuccess")
public class PageShowPassword extends PageBase {

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

