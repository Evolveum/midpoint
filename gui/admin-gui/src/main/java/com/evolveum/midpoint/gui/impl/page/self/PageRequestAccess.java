/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.WizardBorder;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.DetailsMenuPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PersonOfInterestPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RoleCatalogPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.ShoppingCartPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.self.PageSelf;

/**
 * @author Viliam Repan (lazyman)
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/requestAccess")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL,
                        label = "PageRequestAccess.auth.requestAccess.label",
                        description = "PageRequestAccess.auth.requestAccess.description") })
public class PageRequestAccess extends PageSelf {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageRequestAccess.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_WIZARD = "wizard";
    private static final String ID_PERSON_OF_INTEREST = "personOfInterest";
    private static final String ID_ROLE_CATALOG = "roleCatalog";
    private static final String ID_SHOPPING_CART = "shoppingCart";

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        WizardBorder wizard = new WizardBorder(ID_WIZARD) {

            @Override
            protected List<WizardPanel> createSteps() {
                return PageRequestAccess.this.createSteps(this);
            }
        };
        wizard.setOutputMarkupId(true);
        mainForm.add(wizard);

        //todo delete
        add(new DetailsMenuPanel("menu"));
    }

    private List<WizardPanel> createSteps(WizardBorder border) {
        PersonOfInterestPanel personOfInterest = new PersonOfInterestPanel(ID_PERSON_OF_INTEREST) {

            @Override
            protected void onNextPerformed(AjaxRequestTarget target) {
                border.getModel().getObject().nextStep();

                target.add(border);
            }
        };

        RoleCatalogPanel roleCatalog = new RoleCatalogPanel(ID_ROLE_CATALOG);
        ShoppingCartPanel shoppingCart = new ShoppingCartPanel(ID_SHOPPING_CART);

        return Arrays.asList(personOfInterest, roleCatalog, shoppingCart);
    }
}
