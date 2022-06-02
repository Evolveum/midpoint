/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.wizard.WizardBorder;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.PersonOfInterestPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RequestAccess;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.RoleCatalogPanel;
import com.evolveum.midpoint.gui.impl.page.self.requestAccess.ShoppingCartPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.self.PageSelf;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Arrays;
import java.util.List;

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

    private IModel<RequestAccess> model = Model.of(new RequestAccess());

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
    }

    private List<WizardPanel> createSteps(WizardBorder border) {
        PersonOfInterestPanel personOfInterest = new PersonOfInterestPanel(ID_PERSON_OF_INTEREST, border.getModel(), model) {

            @Override
            protected void onNextPerformed(AjaxRequestTarget target) {
                border.getModel().getObject().nextStep();

                target.add(border);
            }

            @Override
            protected IModel<String> createNextStepLabel() {
                return () -> {
                    WizardPanel next = border.getNextPanel();
                    return next != null ? next.getTitle().getObject() : null;
                };
            }
        };

        RoleCatalogPanel roleCatalog = new RoleCatalogPanel(ID_ROLE_CATALOG);
        ShoppingCartPanel shoppingCart = new ShoppingCartPanel(ID_SHOPPING_CART);

        return Arrays.asList(personOfInterest, roleCatalog, shoppingCart);
    }
}
