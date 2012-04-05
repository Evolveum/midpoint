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

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.StringValue;

/**
 * @author lazyman
 */
public class PageUser extends PageAdminUsers {

    public static final String PARAM_USER_ID = "userId";

    private IModel<UserType> model;

    public PageUser() {
        model = new LoadableModel<UserType>(false) {

            @Override
            protected UserType load() {
                StringValue userOid = getPageParameters().get(PARAM_USER_ID);
                if (userOid == null) {
                    return new UserType();
                }

                try {
                    MidPointApplication application = PageUser.this.getMidpointApplication();
                    ModelService model = application.getModel();

                    OperationResult result = new OperationResult("aaaaaaaaaaaaaaaa");
                    PrismObject<UserType> object = model.getObject(UserType.class, userOid.toString(), null, result);
                    return object.asObjectable();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    //todo error handling
                }

                return new UserType();
            }
        };

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form("mainForm");
        add(mainForm);

        Accordion main = new Accordion("main");
        main.setMultipleSelect(true);
        main.setOpenedPanel(0);
        mainForm.add(main);

        AccordionItem details = new AccordionItem("details", createStringResource("pageUser.user"));
        main.add(details);

        AccordionItem accounts = new AccordionItem("accounts", createStringResource("pageUser.accounts"));
        main.add(accounts);

        AccordionItem roles = new AccordionItem("roles", createStringResource("pageUser.roles"));
        main.add(roles);

        AccordionItem assignments = new AccordionItem("assignments", createStringResource("pageUser.assignments"));
        main.add(assignments);

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitLinkButton save = new AjaxSubmitLinkButton("save",
                createStringResource("pageUser.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                //todo implement
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                //todo implement
            }
        };
        mainForm.add(save);

        AjaxLinkButton recalculate = new AjaxLinkButton("recalculate",
                createStringResource("pageUser.button.recalculate")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //todo implement
            }
        };
        mainForm.add(recalculate);

        AjaxLinkButton refresh = new AjaxLinkButton("refresh",
                createStringResource("pageUser.button.refresh")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //todo implement
            }
        };
        mainForm.add(refresh);

        AjaxLinkButton cancel = new AjaxLinkButton("cancel",
                createStringResource("pageUser.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                //todo implement
            }
        };
        mainForm.add(cancel);
    }
}
