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

package com.evolveum.midpoint.web.page.admin.users;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author lazyman
 */
public class PageUsers extends PageAdminUsers {

    public PageUsers() {
        Form<String> form = new Form<String>("form");
        final IModel<String> model = new Model<String>();
        final RequiredTextField<String> text = new RequiredTextField<String>("userId", model);
        form.add(text);

        AjaxSubmitLink button = new AjaxSubmitLink("button", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                userDetailsPerformed(target, model);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        form.add(button);
//        AjaxLinkButton link = new AjaxLinkButton("link", new Model<String>("Show user details")) {
//
//            @Override
//            public void onClick(AjaxRequestTarget target) {
//                System.out.println(text.getModelObject());
////                userDetailsPerformed(target, model);
//            }
//        };
//        form.add(link);
        add(form);
    }

    public void userDetailsPerformed(AjaxRequestTarget target, IModel<String> userIdModel) {
        PageParameters parameters = new PageParameters();
        parameters.add(PageUser.PARAM_USER_ID, userIdModel.getObject());
        setResponsePage(PageUser.class, parameters);
    }
}
